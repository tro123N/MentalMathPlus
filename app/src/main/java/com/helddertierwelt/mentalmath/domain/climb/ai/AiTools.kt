/*
Mental Math Plus - Kletterweg zum Kopfrechnen
Copyright (C) 2026 tro123N
Based on Mental Math, Copyright (C) 2025-2026 HeldDerTierwelt

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.md.
*/

package com.helddertierwelt.mentalmath.domain.climb.ai

import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
import com.helddertierwelt.mentalmath.domain.climb.curriculum.Curriculum
import com.helddertierwelt.mentalmath.domain.climb.engine.ApForecast
import com.helddertierwelt.mentalmath.domain.climb.engine.ApReview
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.engine.Stats
import com.helddertierwelt.mentalmath.domain.climb.math.AnswerCheck
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Palette
import com.helddertierwelt.mentalmath.domain.climb.model.PlusSettings
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.roundToInt
import kotlin.random.Random

/** Ein Parameter eines Werkzeugs. */
data class ToolParam(
    val name: String,
    /** „string“, „integer“, „boolean“ oder „array“. */
    val type: String,
    val description: String,
    val required: Boolean = false,
    val enum: List<String> = emptyList(),
    /** Typ der Einträge bei Listen. */
    val itemType: String? = null,
)

/** Beschreibung eines Werkzeugs, wie das Modell sie sieht. */
data class ToolSpec(val name: String, val description: String, val params: List<ToolParam> = emptyList()) {

    /** OpenAPI-Beschreibung, wie LiteRT-LM sie erwartet. */
    fun json(): JsonObject = buildJsonObject {
        put("name", name)
        put("description", description)
        putJsonObject("parameters") {
            put("type", "object")
            putJsonObject("properties") {
                for (p in params) {
                    putJsonObject(p.name) {
                        put("type", p.type)
                        put("description", p.description)
                        if (p.enum.isNotEmpty()) putJsonArray("enum") { p.enum.forEach { add(it) } }
                        if (p.itemType != null) putJsonObject("items") { put("type", p.itemType) }
                    }
                }
            }
            val required = params.filter { it.required }
            if (required.isNotEmpty()) putJsonArray("required") { required.forEach { add(it.name) } }
        }
    }
}

/** Alles, was die Werkzeuge über die App wissen dürfen – ein fester Stand für eine Antwort. */
class AiContext(
    val progress: ClimbProgress,
    val settings: PlusSettings,
    /** Jetzt in ms. */
    val now: Long,
    /** Heutiger Tag seit dem 1.1.1970 (Ortszeit). */
    val today: Long,
    /** Abstand der Ortszeit zu UTC in ms. */
    val offsetMs: Long = 0,
    /** Aufgabe aus der App, über die gerade gesprochen wird. */
    val focus: AiTaskContext? = null,
    /** Aufgaben, die die KI im Gespräch gestellt hat. */
    val quiz: List<AiQuizTask> = emptyList(),
    val random: Random = Random.Default,
    val worlds: List<World> = Curriculum.worlds,
) {
    fun localDay(ms: Long): Long = Math.floorDiv(ms + offsetMs, Progression.DAY_MS)
}

/** Ergebnis eines Werkzeugaufrufs. */
data class ToolOutcome(
    val name: String,
    /** Antwort für das Modell. */
    val result: JsonObject,
    /** Kurzer Hinweis unter der Antwort, z. B. „96 : 0,8 = 120“. */
    val label: String,
    val ok: Boolean = true,
    /** Vorschlag, der als Knopf erscheint. */
    val action: AiAction? = null,
    /** Neue Liste der im Gespräch gestellten Aufgaben (null = unverändert). */
    val quiz: List<AiQuizTask>? = null,
)

/** Die Werkzeuge des KI-Coachs: Beschreibungen für das Modell und ihre Ausführung. */
object AiToolbox {

    const val CALCULATE = "calculate"
    const val TMSNAT_STATUS = "get_tmsnat_status"
    const val CLIMB_STATUS = "get_climb_status"
    const val SEARCH_METHODS = "search_methods"
    const val CREATE_TASK = "create_task"
    const val CHECK_ANSWER = "check_answer"
    const val GET_HINT = "get_hint"
    const val PREPARE_PRACTICE = "prepare_practice"
    const val PREPARE_SIMULATION = "prepare_simulation"
    const val PREPARE_ERROR_TRAINING = "prepare_error_training"
    const val PREPARE_SIMILAR = "prepare_similar_tasks"
    const val OPEN_LEVEL = "open_level"
    const val CHANGE_SETTING = "change_setting"

    private val categoryList = ApCategory.entries.joinToString(", ") { it.title }

    val specs: List<ToolSpec> = listOf(
        ToolSpec(
            CALCULATE,
            "Rechnet einen Ausdruck exakt aus. Nutze das für JEDE Rechnung, bevor du ein Ergebnis nennst.",
            listOf(
                ToolParam(
                    "expression", "string",
                    "Nur Zahlen und Rechenzeichen, z. B. „(96 : 0,8) * 1,25“, „15 % von 240“, „2^5“ oder „5!“. Kommazahlen mit Komma, keine Tausenderpunkte.",
                    required = true,
                ),
            ),
        ),
        ToolSpec(
            TMSNAT_STATUS,
            "Lernstand im TMSnat-Training: Trefferquote und Zeit je Kategorie, letzte Simulationen, Punkt-Prognose, fälliges Fehlertraining, Tage bis zum Test.",
        ),
        ToolSpec(
            CLIMB_STATUS,
            "Lernstand im Kletterweg (Kopfrechnen): Welten, Sterne, nächstes Level, fällige Wiederholungen, Stärken und Schwächen, Übungstage.",
        ),
        ToolSpec(
            SEARCH_METHODS,
            "Sucht Rechentricks und Methoden der App (Levels des Kletterwegs und Kategorien des TMSnat-Trainings) und liefert ihre Erklärung. Nutze das, bevor du eine Methode erklärst.",
            listOf(ToolParam("query", "string", "Stichwort, z. B. „mal 11“, „Quadrate“, „Dreisatz“ oder „Prozente“.", required = true)),
        ),
        ToolSpec(
            CREATE_TASK,
            "Erzeugt eine neue Übungsaufgabe mit geprüfter Lösung, die du im Chat stellst. Die Lösung bekommst du erst über check_answer.",
            listOf(
                ToolParam("topic", "string", "Kategorie oder Level, z. B. „Prozente“, „Dreisatz“, „Wahrscheinlichkeiten“, „mal 11“. „ähnlich“ = wie die besprochene Aufgabe.", required = true),
                ToolParam("difficulty", "integer", "Stufe 1 (leicht) bis 3 (schwer), Standard 2."),
            ),
        ),
        ToolSpec(
            CHECK_ANSWER,
            "Prüft die Antwort der Person zu einer Aufgabe aus create_task und liefert richtige Antwort, Denkfehler und Lösungsweg.",
            listOf(
                ToolParam("task_id", "string", "Kennung der Aufgabe, z. B. „A1“.", required = true),
                ToolParam("answer", "string", "Antwort der Person, genau wie sie sie geschrieben hat.", required = true),
            ),
        ),
        ToolSpec(
            GET_HINT,
            "Liefert den nächsten Schritt des Lösungswegs einer Aufgabe aus create_task als Tipp, ohne das Ergebnis zu verraten.",
            listOf(ToolParam("task_id", "string", "Kennung der Aufgabe, z. B. „A1“.", required = true)),
        ),
        ToolSpec(
            PREPARE_PRACTICE,
            "Bereitet eine Übung im TMSnat-Training vor; die Person startet sie per Knopf. Ohne Kategorien wird über alle gemischt.",
            listOf(ToolParam("categories", "array", "Kategorien, z. B. [„Prozente“, „Dreisatz“]. Möglich: $categoryList.", itemType = "string")),
        ),
        ToolSpec(
            PREPARE_SIMULATION,
            "Bereitet eine Testsimulation vor: tmsnat = 16 Aufgaben in 25 Minuten wie im Test, kurztest = 8 Aufgaben in 12,5 Minuten, tempo = 16 Aufgaben in 20 Minuten.",
            listOf(ToolParam("variant", "string", "Art der Simulation.", required = true, enum = listOf("tmsnat", "kurztest", "tempo"))),
        ),
        ToolSpec(
            PREPARE_ERROR_TRAINING,
            "Bereitet das Fehlertraining vor: Aufgabentypen, die zuletzt falsch gelöst wurden, mit neuen Zahlen.",
        ),
        ToolSpec(
            PREPARE_SIMILAR,
            "Bereitet mehrere Aufgaben vom selben Typ wie die besprochene Aufgabe vor (mit neuen Zahlen).",
            listOf(ToolParam("count", "integer", "Anzahl, 3 bis 10 (Standard 5).")),
        ),
        ToolSpec(
            OPEN_LEVEL,
            "Bereitet ein Level des Kletterwegs vor: zum Lernen, Üben oder für die Prüfung.",
            listOf(
                ToolParam("level", "string", "Name, Stichwort oder ID des Levels, z. B. „Additionsmethode“ oder „w6.addmethod“.", required = true),
                ToolParam("mode", "string", "Was die Person tun soll.", enum = listOf("lernen", "ueben", "pruefung")),
            ),
        ),
        ToolSpec(
            CHANGE_SETTING,
            "Bereitet eine Änderung der Einstellungen vor, die die Person bestätigt: reminder = tägliche Erinnerung (Uhrzeit wie „18:30“ oder „aus“), trick_tips und adaptive (an/aus), theme = Farbthema (${Palette.entries.joinToString(", ") { it.title }}).",
            listOf(
                ToolParam("setting", "string", "Welche Einstellung.", required = true, enum = listOf("reminder", "trick_tips", "adaptive", "theme")),
                ToolParam("value", "string", "Neuer Wert, z. B. „18:30“, „aus“, „an“ oder „Ozean“.", required = true),
            ),
        ),
    )

    fun spec(name: String): ToolSpec? = specs.firstOrNull { it.name == name }

    /** Werkzeug ausführen. Fehler werden als Antwort an das Modell zurückgegeben, nicht geworfen. */
    fun execute(name: String, argsJson: String, ctx: AiContext): ToolOutcome {
        val args = try {
            Json.parseToJsonElement(argsJson.ifBlank { "{}" }).jsonObject
        } catch (_: Exception) {
            JsonObject(emptyMap())
        }
        return try {
            when (name) {
                CALCULATE -> calculate(args)
                TMSNAT_STATUS -> tmsnatStatus(ctx)
                CLIMB_STATUS -> climbStatus(ctx)
                SEARCH_METHODS -> searchMethods(args, ctx)
                CREATE_TASK -> createTask(args, ctx)
                CHECK_ANSWER -> checkAnswer(args, ctx)
                GET_HINT -> hint(args, ctx)
                PREPARE_PRACTICE -> preparePractice(args)
                PREPARE_SIMULATION -> prepareSimulation(args)
                PREPARE_ERROR_TRAINING -> prepareErrorTraining(ctx)
                PREPARE_SIMILAR -> prepareSimilar(args, ctx)
                OPEN_LEVEL -> openLevel(args, ctx)
                CHANGE_SETTING -> changeSetting(args, ctx)
                else -> fail(name, "Dieses Werkzeug gibt es nicht. Verfügbar: ${specs.joinToString { it.name }}.")
            }
        } catch (e: Exception) {
            fail(name, "Beim Ausführen ist ein Fehler passiert (${e.javaClass.simpleName}).")
        }
    }

    // --- Hilfen ----------------------------------------------------------------------------------

    private fun fail(name: String, message: String, extra: JsonObject? = null): ToolOutcome = ToolOutcome(
        name,
        buildJsonObject {
            put("fehler", message)
            extra?.forEach { (k, v) -> put(k, v) }
        },
        label = message,
        ok = false,
    )

    private fun JsonElement.text(): String? = (this as? JsonPrimitive)?.let { if (it is kotlinx.serialization.json.JsonNull) null else it.content }

    private fun JsonObject.str(vararg keys: String): String? {
        for (k in keys) this[k]?.text()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return null
    }

    /** Erster Text-Parameter, falls das Modell den Namen des Parameters verwechselt. */
    private fun JsonObject.anyStr(): String? = values.firstNotNullOfOrNull { it.text()?.trim()?.takeIf { s -> s.isNotEmpty() } }

    private fun JsonObject.int(vararg keys: String): Int? {
        for (k in keys) {
            val p = this[k] as? JsonPrimitive ?: continue
            p.intOrNull?.let { return it }
            p.longOrNull?.let { return it.toInt() }
            p.content.trim().toDoubleOrNull()?.let { return it.roundToInt() }
        }
        return null
    }

    /** Liste von Texten – auch wenn das Modell nur einen Text mit Kommas schickt. */
    private fun JsonObject.list(vararg keys: String): List<String> {
        for (k in keys) {
            when (val v = this[k]) {
                is JsonArray -> return v.mapNotNull { it.text()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                is JsonPrimitive -> v.text()?.let { s ->
                    return s.split(Regex(",|;|\\bund\\b|/")).map { it.trim() }.filter { it.isNotEmpty() }
                }
                else -> {}
            }
        }
        return emptyList()
    }

    private fun short(text: String, max: Int = 48): String = if (text.length <= max) text else text.take(max - 1).trimEnd() + "…"

    private fun percent(part: Int, total: Int): Int = if (total == 0) 0 else (100.0 * part / total).roundToInt()

    private fun seconds(ms: Double): String = Calculator.german(java.math.BigDecimal(ms / 1000).setScale(1, java.math.RoundingMode.HALF_UP))

    // --- Rechnen ---------------------------------------------------------------------------------

    private fun calculate(args: JsonObject): ToolOutcome {
        val expr = args.str("expression", "ausdruck", "expr") ?: args.anyStr() ?: return fail(CALCULATE, "Es fehlt der Ausdruck (expression).")
        return when (val r = Calculator.evaluate(expr)) {
            is Calculator.Result.Ok -> ToolOutcome(
                CALCULATE,
                buildJsonObject {
                    put("ausdruck", expr)
                    put("ergebnis", r.text)
                    r.fraction?.let { put("als_bruch", it) }
                    r.rounded?.let { put("ungefaehr", it) }
                    r.rounded2?.let { put("auf_2_stellen", it) }
                },
                label = "${short(expr.trim(), 40)} = ${r.text}",
            )
            is Calculator.Result.Error -> fail(
                CALCULATE,
                r.message,
                buildJsonObject { put("tipp", "Schicke nur Zahlen und Rechenzeichen, z. B. „(96 : 0,8) * 1,25“.") },
            )
        }
    }

    // --- Lernstand -------------------------------------------------------------------------------

    private fun tmsnatStatus(ctx: AiContext): ToolOutcome {
        val p = ctx.progress
        val days = AiLookup.TMSNAT_DAY - ctx.today
        val withData = ApCategory.entries.map { c -> c to p.of(c.skillId) }.filter { it.second.recent.isNotEmpty() }
        val result = buildJsonObject {
            if (days >= 0) put("tage_bis_tmsnat", days)
            put("test", "16 Aufgaben in 25 Minuten, Antworten A–E, kein Taschenrechner")
            putJsonArray("kategorien") {
                for ((c, sp) in withData) {
                    val recent = sp.recent
                    val right = recent.filter { it > 0 }
                    add(
                        buildJsonObject {
                            put("name", c.title)
                            put("ergebnisse", recent.size)
                            put("richtig_prozent", percent(right.size, recent.size))
                            if (right.isNotEmpty()) put("zeit_s_richtige", seconds(right.average()))
                            if (sp.stars > 0) put("sterne", sp.stars)
                        },
                    )
                }
            }
            val without = ApCategory.entries.filter { c -> withData.none { it.first == c } }
            if (without.isNotEmpty()) putJsonArray("ohne_ergebnisse") { without.forEach { add(it.title) } }
            val rated = withData.filter { it.second.recent.size >= 3 }
            if (rated.size >= 2) {
                val byRate = rated.sortedBy { (_, sp) -> sp.recent.count { it > 0 }.toDouble() / sp.recent.size }
                val weakest = byRate.first()
                val strongest = byRate.last()
                put("schwaechste_kategorie", "${weakest.first.title} (${percent(weakest.second.recent.count { it > 0 }, weakest.second.recent.size)} % richtig)")
                put("staerkste_kategorie", "${strongest.first.title} (${percent(strongest.second.recent.count { it > 0 }, strongest.second.recent.size)} % richtig)")
            }
            val runs = p.apRuns.takeLast(5).asReversed()
            put("simulationen_gesamt", p.apRuns.size)
            if (runs.isNotEmpty()) {
                putJsonArray("letzte_simulationen") {
                    for (run in runs) {
                        add(
                            buildJsonObject {
                                put("datum", AiLookup.shortDate(ctx.localDay(run.at)))
                                put("art", ApTasks.simulationName(run.total, run.seconds))
                                put("punkte", "${run.score}/${run.total}")
                                put("minuten", Calculator.german(java.math.BigDecimal(run.usedMs / 60_000.0).setScale(1, java.math.RoundingMode.HALF_UP)))
                            },
                        )
                    }
                }
            }
            when (val f = ApForecast.forecast(p, runs = 3_000)) {
                is ApForecast.Outcome.Ready -> putJsonObject("prognose") {
                    val fc = f.forecast
                    put("punkte_erwartet", "${fc.median} von ${fc.total}")
                    put("bereich_80_prozent", "${fc.low} bis ${fc.high}")
                    put("zeit_reicht_nicht_prozent", (fc.timeShort * 100).roundToInt())
                    put("zeit_pro_aufgabe_s", seconds(fc.avgTaskMs.toDouble()))
                    fc.weakest?.let { put("groesster_hebel", it.title) }
                }
                is ApForecast.Outcome.NeedMore -> put(
                    "prognose",
                    "Noch nicht möglich: Es fehlen ${f.results} gelöste Aufgaben" + if (f.categories > 0) " und Ergebnisse in ${f.categories} weiteren Kategorien." else ".",
                )
            }
            val due = ApReview.due(p, ctx.now)
            putJsonObject("fehlertraining") {
                put("faellig", due.size)
                if (due.isNotEmpty()) putJsonArray("typen") { due.take(6).forEach { add(ApTasks.typeName(it.type)) } }
                put("in_wiederholung", p.apMistakes.size)
                ApReview.nextDue(p, ctx.now)?.let { put("naechster_termin", AiLookup.shortDate(ctx.localDay(it))) }
            }
        }
        return ToolOutcome(TMSNAT_STATUS, result, label = "TMSnat-Lernstand gelesen")
    }

    private fun climbStatus(ctx: AiContext): ToolOutcome {
        val p = ctx.progress
        val worlds = ctx.worlds
        val main = worlds.filter { !it.independent }
        val result = buildJsonObject {
            put("serie_tage", Progression.visibleStreak(p, ctx.today))
            put("beste_serie", p.bestStreak)
            put("geloest_gesamt", p.totalSolved)
            if (p.totalSolved > 0) put("richtig_prozent_gesamt", percent(p.totalCorrect.toInt(), p.totalSolved.toInt()))
            put("geloest_letzte_7_tage", Stats.solvedInLast(p, ctx.today, 7))
            put("uebungstage", Stats.activeDays(p))
            putJsonArray("welten") {
                for (w in main) {
                    val s = Stats.worldSummary(w, p)
                    add(
                        buildJsonObject {
                            put("welt", "${w.number}: ${w.title}")
                            put("geschafft", "${s.mastered}/${w.skills.size}")
                            put("sterne", "${s.stars}/${w.skills.size * 3}")
                            s.form?.let { put("sicherheit", it) }
                            if (s.attention > 0) put("auffrischen", s.attention)
                        },
                    )
                }
            }
            Progression.nextSkill(worlds, p)?.let { next ->
                AiLookup.locate(worlds, next.id)?.let { f ->
                    putJsonObject("naechstes_level") {
                        put("name", f.name)
                        put("id", next.id)
                        put("methode", next.method)
                    }
                }
            }
            val due = Progression.dueSkills(worlds, p, ctx.now)
            put("faellige_wiederholungen", due.size)
            if (due.isNotEmpty()) putJsonArray("faellig") { due.take(5).forEach { add(it.title) } }
            val refresh = Progression.needsRefresh(worlds, p)
            if (refresh.isNotEmpty()) putJsonArray("auffrischen") { refresh.take(5).forEach { add(it.title) } }
            val strengths = Stats.strengths(worlds, p)
            if (strengths.isNotEmpty()) putJsonArray("staerken") { strengths.forEach { add("${it.skill.title} (${it.label}, ${it.score}/100)") } }
            val weaknesses = Stats.weaknesses(worlds, p)
            if (weaknesses.isNotEmpty()) putJsonArray("schwaechen") { weaknesses.forEach { add("${it.skill.title} (${it.label}, ${it.score}/100)") } }
        }
        return ToolOutcome(CLIMB_STATUS, result, label = "Kletterweg-Lernstand gelesen")
    }

    // --- Methoden --------------------------------------------------------------------------------

    private fun searchMethods(args: JsonObject, ctx: AiContext): ToolOutcome {
        val query = args.str("query", "suche", "topic") ?: args.anyStr() ?: return fail(SEARCH_METHODS, "Es fehlt das Stichwort (query).")
        val found = AiLookup.search(ctx.worlds, query, 3)
        if (found.isEmpty()) {
            return fail(SEARCH_METHODS, "Keine Methode zu „$query“ gefunden. Versuche ein anderes Stichwort, z. B. „Prozente“, „mal 11“ oder „Quadrate“.")
        }
        val result = buildJsonObject {
            putJsonArray("treffer") {
                found.forEachIndexed { i, f ->
                    val sp = ctx.progress.of(f.skill.id)
                    add(
                        buildJsonObject {
                            put("level", f.name)
                            put("id", f.skill.id)
                            put("methode", f.skill.method)
                            put("kurz", f.skill.summary)
                            // Die ausführliche Erklärung nur beim besten Treffer, damit die Antwort kurz bleibt.
                            put("erklaerung", AiLookup.explanationText(f.skill, examples = i == 0, maxChars = if (i == 0) 1_400 else 300))
                            put("quelle", f.skill.book.label)
                            put("sterne", sp.stars)
                        },
                    )
                }
            }
        }
        return ToolOutcome(SEARCH_METHODS, result, label = found.first().skill.title)
    }

    // --- Aufgaben im Chat ------------------------------------------------------------------------

    private val similarWords = setOf("aehnlich", "aehnliche", "gleich", "gleiche", "dieselbe", "besprochen", "aktuell", "wie oben", "so eine")

    private fun sourceFor(topic: String, ctx: AiContext): String? {
        val simple = AiLookup.simplify(topic)
        val focus = ctx.focus
        if (focus != null && (simple.isEmpty() || similarWords.any { simple.contains(it) })) {
            return ApTasks.typeOf(focus.problem)?.let { "type:$it" } ?: "skill:${focus.problem.skillId}"
        }
        if (simple.isEmpty()) return null
        AiLookup.category(topic)?.let { return "ap:${it.name}" }
        val skill = AiLookup.search(ctx.worlds, topic, 1).firstOrNull()?.skill ?: return null
        ApCategory.of(skill.id)?.let { return "ap:${it.name}" }
        if (skill.id == "w13.mixed") return "ap:${ApCategory.entries[ctx.random.nextInt(ApCategory.entries.size)].name}"
        return "skill:${skill.id}"
    }

    internal fun answerFormat(p: Problem): String {
        val unit = p.prompt.unit ?: p.prompt.text.trim().removePrefix("?").trim().takeIf { it.isNotEmpty() && p.prompt.text.trim().startsWith("?") }
        val kind = when (p.input) {
            InputKind.INTEGER -> "ganze Zahl"
            InputKind.DECIMAL -> "Zahl, Kommazahl erlaubt"
            InputKind.FRACTION -> "Bruch wie 3/8"
            InputKind.REMAINDER -> "Ergebnis mit Rest, z. B. 25 R 4"
            InputKind.CHOICE -> "Buchstabe der richtigen Antwort"
            InputKind.DIGITS -> "Ziffernfolge"
        }
        return if (unit != null) "$kind (in $unit)" else kind
    }

    private fun createTask(args: JsonObject, ctx: AiContext): ToolOutcome {
        val topic = args.str("topic", "thema", "category", "kategorie") ?: args.anyStr() ?: ""
        val difficulty = (args.int("difficulty", "stufe") ?: 2).coerceIn(1, 3)
        val source = sourceFor(topic, ctx)
            ?: return fail(CREATE_TASK, "Zu „$topic“ habe ich keine Aufgaben. Möglich sind die TMSnat-Kategorien ($categoryList) oder Levels des Kletterwegs.")
        val id = "A" + ((ctx.quiz.mapNotNull { it.id.removePrefix("A").toIntOrNull() }.maxOrNull() ?: 0) + 1)
        val task = AiQuizTask(id = id, source = source, difficulty = difficulty, seed = ctx.random.nextLong())
        val problem = AiQuiz.problem(task) ?: return fail(CREATE_TASK, "Die Aufgabe konnte nicht erzeugt werden.")
        if (problem.prompt.hideAfterMs != null) {
            return fail(CREATE_TASK, "Diese Übung braucht die Anzeige der App (Merkaufgabe). Bereite sie mit open_level vor.")
        }
        val result = buildJsonObject {
            put("id", id)
            put("aufgabe", AiLookup.taskText(problem))
            if (problem.choices.isNotEmpty()) putJsonArray("antwortmoeglichkeiten") { problem.choices.forEach { add(it) } }
            put("antwortformat", answerFormat(problem))
            put(
                "hinweis",
                "Die App zeigt die Aufgabe als Karte unter deiner Antwort (mit allen Antwortmöglichkeiten). Schreib nur einen kurzen Satz wie " +
                    "„Hier ist deine Aufgabe – schreib mir dein Ergebnis.“ Es gibt keinen Knopf zum Starten. Wiederhole die Aufgabe nicht und verrate " +
                    "weder Lösung noch Rechenweg. Antwortet die Person, prüfe mit check_answer; bittet sie um Hilfe, nutze get_hint.",
            )
        }
        return ToolOutcome(CREATE_TASK, result, label = "Aufgabe $id: ${AiQuiz.title(source)}", quiz = (ctx.quiz + task).takeLast(AiQuiz.KEPT))
    }

    private fun quizTask(args: JsonObject, ctx: AiContext): AiQuizTask? {
        val id = args.str("task_id", "id", "aufgabe")
        return if (id == null) ctx.quiz.lastOrNull()
        else ctx.quiz.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
            ?: ctx.quiz.firstOrNull { it.id.equals("A" + id.filter(Char::isDigit), ignoreCase = true) }
    }

    private fun checkAnswer(args: JsonObject, ctx: AiContext): ToolOutcome {
        val task = quizTask(args, ctx) ?: return fail(CHECK_ANSWER, "Diese Aufgabe kenne ich nicht. Erzeuge zuerst eine mit create_task.")
        val problem = AiQuiz.problem(task) ?: return fail(CHECK_ANSWER, "Die Aufgabe konnte nicht wiederhergestellt werden.")
        val raw = args.str("answer", "antwort") ?: return fail(CHECK_ANSWER, "Es fehlt die Antwort der Person (answer).")
        val input = AiQuiz.normalizeAnswer(raw, problem)
        val updated = ctx.quiz.map { if (it.id == task.id) it.copy(answered = true) else it }
        if (input == null) {
            return fail(CHECK_ANSWER, "In „${short(raw)}“ finde ich keine Antwort, die ich prüfen kann. Frag die Person nach ihrem Ergebnis.")
        }
        val verdict = AnswerCheck.check(problem.expected, input)
        val why = if (verdict.correct) null else problem.mistakeFor(input)
        val result = buildJsonObject {
            put("aufgabe", task.id)
            put("richtig", verdict.correct)
            // Gleich am Anfang, damit auch kleine Modelle sie beachten.
            put(
                "anleitung",
                if (verdict.correct) {
                    "Die Antwort ist richtig. Lobe kurz und konkret, erfinde keinen Fehler und biete eine neue Aufgabe an."
                } else {
                    "Die Antwort ist falsch. Nenne die richtige Antwort und erkläre mit dem Lösungsweg, wo der Fehler liegt" +
                        (if (why != null) " (typischer_denkfehler)." else ".")
                },
            )
            put("antwort_der_person", raw)
            put("richtige_antwort", problem.answerText())
            verdict.note?.let { put("hinweis", it) }
            why?.let { put("typischer_denkfehler", it) }
            problem.method?.let { put("methode", it) }
            putJsonArray("loesungsweg") { AiLookup.solutionLines(problem).forEach { add(it) } }
        }
        return ToolOutcome(CHECK_ANSWER, result, label = "${task.id}: " + if (verdict.correct) "richtig" else "falsch", quiz = updated)
    }

    private fun hint(args: JsonObject, ctx: AiContext): ToolOutcome {
        val task = quizTask(args, ctx) ?: return fail(GET_HINT, "Diese Aufgabe kenne ich nicht. Erzeuge zuerst eine mit create_task.")
        val problem = AiQuiz.problem(task) ?: return fail(GET_HINT, "Die Aufgabe konnte nicht wiederhergestellt werden.")
        val k = task.hints
        if (k >= problem.steps.size - 1) {
            return ToolOutcome(
                GET_HINT,
                buildJsonObject {
                    put("aufgabe", task.id)
                    put("tipp", "Alle Zwischenschritte sind verraten – der letzte Schritt ist das Ergebnis. Lass die Person rechnen.")
                    put("letzter_schritt", problem.steps.last().label)
                },
                label = "Tipp zu ${task.id}",
            )
        }
        val step = problem.steps[k]
        val result = buildJsonObject {
            put("aufgabe", task.id)
            put("schritt", "${k + 1} von ${problem.steps.size}")
            put("rechne", step.label)
            step.hint?.let { put("hinweis", it) }
            if (k == 0) problem.method?.let { put("methode", it) }
            if (k > 0) put("ergebnis_vorheriger_schritt", Fmt.value(problem.steps[k - 1].expected, problem.choices))
        }
        val updated = ctx.quiz.map { if (it.id == task.id) it.copy(hints = it.hints + 1) else it }
        return ToolOutcome(GET_HINT, result, label = "Tipp zu ${task.id} (Schritt ${k + 1})", quiz = updated)
    }

    // --- Vorschläge ------------------------------------------------------------------------------

    private fun proposal(name: String, action: AiAction, button: String, extra: JsonObject? = null): ToolOutcome {
        val label = AiActions.label(action)
        return ToolOutcome(
            name,
            buildJsonObject {
                put("vorbereitet", "${label.title}: ${label.detail}")
                put("hinweis", "Unter deiner Antwort erscheint jetzt ein Knopf „$button“. Die Person entscheidet selbst, ob sie ihn antippt.")
                extra?.forEach { (k, v) -> put(k, v) }
            },
            label = label.title,
            action = action,
        )
    }

    private fun preparePractice(args: JsonObject): ToolOutcome {
        val names = args.list("categories", "kategorien", "category", "topic")
        val cats = names.mapNotNull { AiLookup.category(it) }.distinct()
        if (names.isNotEmpty() && cats.isEmpty()) {
            return fail(PREPARE_PRACTICE, "Diese Kategorien kenne ich nicht: ${names.joinToString()}. Möglich: $categoryList.")
        }
        val action = AiAction.ApPractice(cats.map { it.name })
        return proposal(PREPARE_PRACTICE, action, AiActions.label(action).button)
    }

    private fun prepareSimulation(args: JsonObject): ToolOutcome {
        val v = AiLookup.simplify(args.str("variant", "art", "variante") ?: args.anyStr() ?: "tmsnat")
        val action = when {
            v.contains("kurz") -> AiAction.ApSimulation(8, 750)
            v.contains("tempo") || v.contains("schnell") -> AiAction.ApSimulation(ApTasks.TEST_COUNT, 1_200)
            else -> AiAction.ApSimulation(ApTasks.TEST_COUNT, ApTasks.TEST_SECONDS)
        }
        return proposal(PREPARE_SIMULATION, action, "Starten")
    }

    private fun prepareErrorTraining(ctx: AiContext): ToolOutcome {
        val due = ApReview.due(ctx.progress, ctx.now)
        if (due.isEmpty()) {
            val next = ApReview.nextDue(ctx.progress, ctx.now)
            return ToolOutcome(
                PREPARE_ERROR_TRAINING,
                buildJsonObject {
                    put("faellig", 0)
                    put("hinweis", if (next != null) "Gerade ist nichts fällig. Der nächste Aufgabentyp ist am ${AiLookup.shortDate(ctx.localDay(next))} dran." else "Im Fehlertraining ist nichts: Bisher wurde kein Aufgabentyp falsch gelöst.")
                },
                label = "Fehlertraining: nichts fällig",
            )
        }
        return proposal(
            PREPARE_ERROR_TRAINING,
            AiAction.ApReview,
            "Starten",
            buildJsonObject {
                put("faellig", due.size)
                putJsonArray("typen") { due.take(ApReview.SESSION_MAX).forEach { add(ApTasks.typeName(it.type)) } }
            },
        )
    }

    private fun prepareSimilar(args: JsonObject, ctx: AiContext): ToolOutcome {
        val focus = ctx.focus ?: return fail(PREPARE_SIMILAR, "Gerade wird keine Aufgabe aus der App besprochen. Nutze prepare_practice oder open_level.")
        val count = (args.int("count", "anzahl") ?: 5).coerceIn(3, 10)
        val type = ApTasks.typeOf(focus.problem)
        if (type != null && ApTasks.taskOfType(type, Random(1)) != null) {
            return proposal(PREPARE_SIMILAR, AiAction.ApTypes(type, count), "Starten")
        }
        val skill = Curriculum.skill(focus.problem.skillId) ?: return fail(PREPARE_SIMILAR, "Zu dieser Aufgabe kann ich keine ähnlichen erzeugen.")
        return proposal(PREPARE_SIMILAR, AiAction.Level(skill.id, AiAction.MODE_PRACTICE), "Starten")
    }

    private fun openLevel(args: JsonObject, ctx: AiContext): ToolOutcome {
        val query = args.str("level", "name", "id") ?: return fail(OPEN_LEVEL, "Es fehlt der Name des Levels (level).")
        val found = AiLookup.search(ctx.worlds, query, 1).firstOrNull() ?: return fail(OPEN_LEVEL, "Kein Level zu „$query“ gefunden.")
        val mode = AiLookup.simplify(args.str("mode", "modus") ?: "").let {
            when {
                it.startsWith("lern") || it == "learn" -> AiAction.MODE_LEARN
                it.startsWith("ueb") || it.startsWith("practice") -> AiAction.MODE_PRACTICE
                it.startsWith("pruef") || it == "test" -> AiAction.MODE_TEST
                else -> AiAction.MODE_OPEN
            }
        }
        val id = found.skill.id
        val unlocked = Progression.skillUnlocked(ctx.worlds, ctx.progress, id)
        val sp = ctx.progress.of(id)
        val extra = buildJsonObject {
            put("level", found.name)
            put("methode", found.skill.method)
            put("sterne", sp.stars)
            if (!unlocked) {
                put("gesperrt", true)
                val blockers = Progression.blockers(ctx.worlds, ctx.progress, id)
                if (blockers.isNotEmpty()) putJsonArray("vorher_schaffen") { blockers.take(3).forEach { add(it.title) } }
            }
        }
        // Gesperrte Levels nur öffnen (dort steht, was vorher nötig ist); Lernen geht immer.
        val effective = if (!unlocked && mode != AiAction.MODE_LEARN) AiAction.MODE_OPEN else mode
        val action = AiAction.Level(id, effective)
        return proposal(OPEN_LEVEL, action, AiActions.label(action).button, extra)
    }

    private fun onOff(value: String): Boolean? {
        val v = AiLookup.simplify(value)
        return when {
            v in setOf("an", "ein", "on", "ja", "true", "einschalten", "aktivieren", "aktiv") -> true
            v in setOf("aus", "off", "nein", "false", "ausschalten", "deaktivieren", "keine", "stop") -> false
            v.startsWith("ein") || v.startsWith("akt") -> true
            v.startsWith("aus") || v.startsWith("deakt") -> false
            else -> null
        }
    }

    private fun changeSetting(args: JsonObject, ctx: AiContext): ToolOutcome {
        val key = AiLookup.simplify(args.str("setting", "einstellung") ?: "").replace(' ', '_')
        val value = args.str("value", "wert") ?: ""
        val s = ctx.settings
        val action: AiAction.Setting = when {
            key.startsWith("remind") || key.startsWith("erinner") -> {
                val off = onOff(value) == false
                if (off) {
                    if (!s.reminderOn) return fail(CHANGE_SETTING, "Die Erinnerung ist schon ausgeschaltet.")
                    AiAction.Setting(AiAction.SETTING_REMINDER, "off")
                } else {
                    val m = Regex("(\\d{1,2})(?:\\s*[:.]\\s*(\\d{2}))?").find(value)
                        ?: return fail(CHANGE_SETTING, "Für die Erinnerung brauche ich eine Uhrzeit wie „18:30“ oder „aus“.")
                    val h = m.groupValues[1].toInt()
                    val min = m.groupValues[2].ifEmpty { "0" }.toInt()
                    if (h !in 0..23 || min !in 0..59) return fail(CHANGE_SETTING, "Die Uhrzeit „$value“ gibt es nicht.")
                    AiAction.Setting(AiAction.SETTING_REMINDER, "$h:${min.toString().padStart(2, '0')}")
                }
            }
            key.startsWith("trick") || key.contains("tipp") || key.contains("tips") -> {
                val on = onOff(value) ?: return fail(CHANGE_SETTING, "Trick-Tipps kann ich nur an- oder ausschalten.")
                if (on == s.trickTips) return fail(CHANGE_SETTING, "Die Trick-Tipps sind schon ${if (on) "an" else "aus"}.")
                AiAction.Setting(AiAction.SETTING_TRICK_TIPS, if (on) "on" else "off")
            }
            key.startsWith("adapt") || key.startsWith("anpass") || key.contains("schwier") -> {
                val on = onOff(value) ?: return fail(CHANGE_SETTING, "Die anpassende Schwierigkeit kann ich nur an- oder ausschalten.")
                if (on == s.adaptive) return fail(CHANGE_SETTING, "Die anpassende Schwierigkeit ist schon ${if (on) "an" else "aus"}.")
                AiAction.Setting(AiAction.SETTING_ADAPTIVE, if (on) "on" else "off")
            }
            key.startsWith("theme") || key.startsWith("farb") || key.startsWith("palette") || key.startsWith("design") -> {
                val v = AiLookup.simplify(value)
                val palette = Palette.entries.firstOrNull { AiLookup.simplify(it.title) == v || it.name.equals(value.trim(), true) }
                    ?: Palette.entries.firstOrNull { v.isNotEmpty() && (AiLookup.simplify(it.title).startsWith(v) || AiLookup.simplify(it.subtitle).contains(v)) }
                    ?: return fail(CHANGE_SETTING, "Dieses Farbthema gibt es nicht. Möglich: ${Palette.entries.joinToString { it.title }}.")
                if (palette.name == s.palette) return fail(CHANGE_SETTING, "Das Farbthema „${palette.title}“ ist schon eingestellt.")
                AiAction.Setting(AiAction.SETTING_THEME, palette.name)
            }
            else -> return fail(CHANGE_SETTING, "Diese Einstellung kann ich nicht ändern. Möglich: reminder, trick_tips, adaptive, theme.")
        }
        return proposal(CHANGE_SETTING, action, "Übernehmen")
    }
}

/** Aufgaben, die die KI im Gespräch stellt. */
object AiQuiz {

    /** So viele Aufgaben merkt sich ein Gespräch. */
    const val KEPT = 30

    /** Eine im Chat gestellte Aufgabe, wie sie als Karte unter der Antwort erscheint. */
    data class Card(
        val id: String,
        val title: String,
        val text: String,
        val choices: List<String>,
        val format: String,
        val answered: Boolean,
    )

    /** Kurzer Name der Aufgabenart, z. B. „Prozente in Textaufgaben“. */
    fun title(source: String): String = when {
        source.startsWith("ap:") -> AiLookup.category(source.removePrefix("ap:"))?.title
        source.startsWith("type:") -> ApTasks.typeName(source.removePrefix("type:"))
        else -> Curriculum.skill(source.removePrefix("skill:"))?.title
    } ?: ""

    fun card(task: AiQuizTask): Card? {
        val p = problem(task) ?: return null
        return Card(task.id, title(task.source), AiLookup.taskText(p), p.choices, AiToolbox.answerFormat(p), task.answered)
    }

    /** Für das Gedächtnis des Modells: Welche Aufgabe stand als Karte unter der Antwort? */
    fun note(card: Card): String = buildString {
        append("(Aufgabe ").append(card.id).append(", als Karte gezeigt: ").append(card.text.replace("\n", " "))
        if (card.choices.isNotEmpty()) append(" Antwortmöglichkeiten: ").append(card.choices.joinToString("  "))
        append(")")
    }

    /** Die Aufgabe zu einem Eintrag – mit demselben Startwert immer dieselbe. */
    fun problem(task: AiQuizTask): Problem? {
        val r = Random(task.seed)
        val src = task.source
        return when {
            src.startsWith("ap:") -> ApCategory.entries.firstOrNull { it.name == src.removePrefix("ap:") }
                ?.let { ApTasks.typed(ApTasks.task(it, task.difficulty, r)) }
            src.startsWith("type:") -> ApTasks.taskOfType(src.removePrefix("type:"), r)?.let { ApTasks.typed(it) }
            src.startsWith("skill:") -> Curriculum.skill(src.removePrefix("skill:"))?.generate(r)
            else -> null
        }
    }

    /**
     * Antwort aus einem Satz der Person herauslösen, so wie sie in der App eingetippt würde:
     * z. B. „ich glaube 1.250 €“ → „1250“, „drei Viertel, also 3/4“ → „3/4“, „25 Rest 4“ → „25R4“.
     */
    fun normalizeAnswer(raw: String, problem: Problem): String? {
        val text = raw.trim()
        when (problem.expected) {
            is Expected.Choice -> return choiceIndex(text, problem.choices)?.toString()
            is Expected.Frac -> {
                Regex("(\\d+)\\s*/\\s*(\\d+)").findAll(text).lastOrNull()?.let { return "${it.groupValues[1]}/${it.groupValues[2]}" }
            }
            is Expected.Remainder -> {
                Regex("(\\d+)\\s*(?:r|rest)\\s*(\\d+)", RegexOption.IGNORE_CASE).findAll(text).lastOrNull()
                    ?.let { return "${it.groupValues[1]}R${it.groupValues[2]}" }
            }
            is Expected.Digits -> {
                return Regex("\\d+").findAll(text.replace(" ", "")).lastOrNull()?.value
            }
            else -> {}
        }
        val m = Regex("\\d+(?:[.,]\\d+)*").findAll(text).lastOrNull() ?: return null
        val token = m.value
        val dots = token.count { it == '.' }
        val commas = token.count { it == ',' }
        return when {
            dots > 0 && commas > 0 -> {
                val decimal = if (token.lastIndexOf('.') > token.lastIndexOf(',')) '.' else ','
                val thousands = if (decimal == '.') ',' else '.'
                token.replace(thousands.toString(), "").replace(decimal, ',')
            }
            dots > 1 || commas > 1 -> token.replace(".", "").replace(",", "")
            // „1.250“ mit genau drei Ziffern danach ist im Deutschen meist ein Tausenderpunkt.
            dots == 1 && Regex("\\d{1,3}\\.\\d{3}").matches(token) && !expectsFraction(problem) -> token.replace(".", "")
            else -> token.replace('.', ',')
        }
    }

    /** Text einer Antwortmöglichkeit ohne Buchstaben davor, z. B. „(B) 120 €“ → „120 €“. */
    private fun choiceBody(choice: String): String = choice.replace(Regex("^\\s*\\(?[A-Ea-e]\\)\\s*"), "").trim()

    /**
     * Welche Antwortmöglichkeit gemeint ist: zuerst über den Text („Mittwoch“, „120 €“), dann über
     * den Buchstaben („B“), zuletzt über die Nummer („die zweite“ = 2).
     */
    private fun choiceIndex(text: String, choices: List<String>): Int? {
        val simple = " " + AiLookup.simplify(text) + " "
        val byText = choices.mapIndexedNotNull { i, c ->
            val body = AiLookup.simplify(choiceBody(c))
            if (body.isNotEmpty() && simple.contains(" $body ")) i to body.length else null
        }.maxByOrNull { it.second }
        if (byText != null) return byText.first
        Regex("(?<![A-Za-zÄÖÜäöüß])\\(?([A-Ea-e])\\)?(?![A-Za-zÄÖÜäöüß])").findAll(text).lastOrNull()?.let { m ->
            val i = m.groupValues[1].uppercase()[0] - 'A'
            if (i in choices.indices) return i
        }
        Regex("\\d+").findAll(text).lastOrNull()?.value?.toIntOrNull()?.let { n -> if (n - 1 in choices.indices) return n - 1 }
        return null
    }

    /** Erwartet die Aufgabe eine Kommazahl mit drei Nachkommastellen? Dann ist „1.250“ ein Komma. */
    private fun expectsFraction(problem: Problem): Boolean {
        val e = problem.expected as? Expected.Exact ?: return false
        return e.value.stripTrailingZeros().scale() >= 3
    }
}
