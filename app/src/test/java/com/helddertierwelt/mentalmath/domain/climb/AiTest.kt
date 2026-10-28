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

package com.helddertierwelt.mentalmath.domain.climb

import com.helddertierwelt.mentalmath.domain.climb.ai.AgentEvent
import com.helddertierwelt.mentalmath.domain.climb.ai.AiAction
import com.helddertierwelt.mentalmath.domain.climb.ai.AiActions
import com.helddertierwelt.mentalmath.domain.climb.ai.AiAgent
import com.helddertierwelt.mentalmath.domain.climb.ai.AiContext
import com.helddertierwelt.mentalmath.domain.climb.ai.AiHistory
import com.helddertierwelt.mentalmath.domain.climb.ai.AiLookup
import com.helddertierwelt.mentalmath.domain.climb.ai.AiMarkdown
import com.helddertierwelt.mentalmath.domain.climb.ai.AiMessage
import com.helddertierwelt.mentalmath.domain.climb.ai.AiModel
import com.helddertierwelt.mentalmath.domain.climb.ai.AiPrompt
import com.helddertierwelt.mentalmath.domain.climb.ai.AiQuiz
import com.helddertierwelt.mentalmath.domain.climb.ai.AiQuizTask
import com.helddertierwelt.mentalmath.domain.climb.ai.AiRole
import com.helddertierwelt.mentalmath.domain.climb.ai.AiStatus
import com.helddertierwelt.mentalmath.domain.climb.ai.AiTaskContext
import com.helddertierwelt.mentalmath.domain.climb.ai.AiText
import com.helddertierwelt.mentalmath.domain.climb.ai.AiToolbox
import com.helddertierwelt.mentalmath.domain.climb.ai.Calculator
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmChunk
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmInput
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmSession
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmToolCall
import com.helddertierwelt.mentalmath.domain.climb.ai.MdBlock
import com.helddertierwelt.mentalmath.domain.climb.ai.ToolOutcome
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
import com.helddertierwelt.mentalmath.domain.climb.curriculum.Curriculum
import com.helddertierwelt.mentalmath.domain.climb.engine.ProblemResult
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.ApMistakeEntry
import com.helddertierwelt.mentalmath.domain.climb.model.ApRun
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.Palette
import com.helddertierwelt.mentalmath.domain.climb.model.PlusSettings
import com.helddertierwelt.mentalmath.domain.climb.model.SkillProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AiTest {

    private fun calc(expr: String): String = when (val r = Calculator.evaluate(expr)) {
        is Calculator.Result.Ok -> r.text
        is Calculator.Result.Error -> "Fehler: " + r.message
    }

    /** 28. Oktober 2026, 12 Uhr. */
    private val today = AiLookup.epochDay(2026, 10, 28)
    private val now = today * 86_400_000L + 12 * 3_600_000L

    private fun ctx(
        progress: ClimbProgress = ClimbProgress(),
        settings: PlusSettings = PlusSettings(),
        focus: AiTaskContext? = null,
        quiz: List<AiQuizTask> = emptyList(),
        seed: Int = 1,
    ) = AiContext(progress, settings, now, today, 0, focus, quiz, Random(seed))

    private fun run(name: String, args: String, c: AiContext = ctx()): ToolOutcome = AiToolbox.execute(name, args, c)

    private fun JsonObject.str(key: String): String = this[key]!!.jsonPrimitive.content

    /** Fortschritt mit Ergebnissen in allen TMSnat-Kategorien (für Status und Prognose). */
    private fun apProgress(): ClimbProgress {
        var p = ClimbProgress()
        ApCategory.entries.forEachIndexed { i, c ->
            val recent = List(6) { k -> if ((k + i) % 4 == 0) -80_000L else 70_000L + 1_000L * k }
            p = p.with(c.skillId) { it.copy(recent = recent, stars = i % 3) }
        }
        return p.copy(
            apRuns = listOf(ApRun(now - 3 * 86_400_000L, 11, 16, 1_400_000, 1_500, 5)),
            apMistakes = listOf(ApMistakeEntry("PERCENT/base", 0, now - 1_000, 2)),
            totalSolved = 200,
            totalCorrect = 160,
        )
    }

    // --- Taschenrechner ----------------------------------------------------------------------

    @Test
    fun calculatorIsExact() {
        assertEquals("120", calc("96 / 0,8"))
        assertEquals("120", calc("96 : 0.8"))
        assertEquals("0,3", calc("0,1 + 0,2"))
        assertEquals("1/3", calc("1/3"))
        assertEquals("36", calc("15 % von 240"))
        assertEquals("36", calc("15% * 240"))
        assertEquals("180", calc("3/4 von 240"))
        assertEquals("1024", calc("2^10"))
        assertEquals("1024", calc("2 hoch 10"))
        assertEquals("0,125", calc("2^-3"))
        assertEquals("-9", calc("-3^2"))
        assertEquals("9", calc("(-3)^2"))
        assertEquals("14", calc("2(3 + 4)"))
        assertEquals("21", calc("(1 + 2)(3 + 4)"))
        assertEquals("12", calc("3 x 4"))
        assertEquals("12", calc("3 mal 4"))
        assertEquals("12", calc("3 × 4"))
        assertEquals("1,5", calc("3 ÷ 2"))
        assertEquals("-1", calc("3 − 4"))
        assertEquals("120", calc("5!"))
        assertEquals("10", calc("5! / (3! * 2!)"))
        assertEquals("12", calc("sqrt(144)"))
        assertEquals("1,5", calc("√(9/4)"))
        assertEquals("1234,5", calc("1.234,5"))
        assertEquals("1234,5", calc("1,234.5"))
        assertEquals("1000000", calc("1.000.000"))
        assertEquals("1000", calc("1 000"))
        assertEquals("96", calc("120 € * 0,8"))
        assertEquals("150", calc("60 km/h * 2,5"))
        assertEquals("1,25", calc("Rechne 5 / 4."))
        assertEquals("12", calc("3 * 4 = ?"))
        assertEquals("0,75", calc("3/4"))
    }

    @Test
    fun calculatorShowsFractionsAndRounding() {
        val r = Calculator.evaluate("2/3") as Calculator.Result.Ok
        assertEquals("2/3", r.fraction)
        assertEquals("0,666667", r.rounded)
        assertEquals("0,67", r.rounded2)
        val money = Calculator.evaluate("19,99 * 3 / 4") as Calculator.Result.Ok
        assertEquals("14,9925", money.text)
        assertEquals("14,99", money.rounded2)
        val whole = Calculator.evaluate("7 * 6") as Calculator.Result.Ok
        assertNull(whole.rounded2)
        val root = Calculator.evaluate("sqrt(2)") as Calculator.Result.Ok
        assertNull(root.exact)
        assertEquals("≈ 1,414214", root.text)
    }

    @Test
    fun calculatorExplainsErrors() {
        assertTrue(calc("5 / 0").startsWith("Fehler: Division durch 0"))
        assertTrue(calc("(3 + 4").startsWith("Fehler: Eine Klammer fehlt"))
        assertTrue(calc("3 + 4)").startsWith("Fehler: Eine Klammer zu viel"))
        assertTrue(calc("").startsWith("Fehler"))
        assertTrue(calc("3 4").startsWith("Fehler: Zwischen zwei Zahlen"))
        assertTrue(calc("10^100000").startsWith("Fehler"))
        assertTrue(calc("((10^300)^300)^300").startsWith("Fehler: Die Zahl wird zu groß"))
        assertTrue(calc("3 # 4").startsWith("Fehler"))
        assertTrue(calc("1000!").startsWith("Fehler"))
    }

    // --- Modelle -----------------------------------------------------------------------------

    @Test
    fun modelCatalogIsConsistent() {
        assertEquals(AiModel.GEMMA4_E4B, AiModel.DEFAULT)
        for (m in AiModel.entries) {
            assertTrue(m.sha256.matches(Regex("[0-9a-f]{64}")))
            assertTrue(m.revision.matches(Regex("[0-9a-f]{40}")))
            assertTrue(m.url.startsWith("https://huggingface.co/litert-community/"))
            assertTrue(m.url.contains("/resolve/${m.revision}/${m.file}"))
            assertEquals(m, AiModel.of(m.id))
        }
        assertEquals("3,7 GB", AiModel.GEMMA4_E4B.sizeLabel)
        assertEquals("6,9 GB", AiModel.GEMMA4_12B.sizeLabel)
        assertEquals("2,6 GB", AiModel.GEMMA4_E2B.sizeLabel)
        assertTrue(AiModel.GEMMA4_12B.experimental)
        assertEquals(AiModel.entries.toSet(), AiModel.fallbackOrder.toSet())
    }

    // --- Werkzeuge ---------------------------------------------------------------------------

    @Test
    fun toolSpecsAreValidJson() {
        val names = mutableSetOf<String>()
        for (spec in AiToolbox.specs) {
            val json = Json.parseToJsonElement(spec.json().toString()).jsonObject
            assertTrue(names.add(json.str("name")))
            assertTrue(json.str("description").length in 20..400)
            val params = json["parameters"]!!.jsonObject
            assertEquals("object", params.str("type"))
            val props = params["properties"]!!.jsonObject
            params["required"]?.jsonArray?.forEach { assertTrue(it.jsonPrimitive.content in props.keys) }
        }
        assertEquals(13, names.size)
    }

    @Test
    fun calculateToolAcceptsWrongParameterNames() {
        val a = run(AiToolbox.CALCULATE, """{"expression": "96 : 0,8"}""")
        assertTrue(a.ok)
        assertEquals("120", a.result.str("ergebnis"))
        assertEquals("96 : 0,8 = 120", a.label)
        val b = run(AiToolbox.CALCULATE, """{"ausdruck": "3*4"}""")
        assertEquals("12", b.result.str("ergebnis"))
        val c = run(AiToolbox.CALCULATE, """{"x": "2/3"}""")
        assertEquals("2/3", c.result.str("als_bruch"))
        val bad = run(AiToolbox.CALCULATE, """{"expression": "5/0"}""")
        assertFalse(bad.ok)
        assertTrue(bad.result.str("fehler").contains("Division durch 0"))
        val broken = run(AiToolbox.CALCULATE, "kein json")
        assertFalse(broken.ok)
        val unknown = run("fly_to_moon", "{}")
        assertFalse(unknown.ok)
    }

    @Test
    fun statusToolsWorkWithAndWithoutData() {
        val empty = run(AiToolbox.TMSNAT_STATUS, "{}")
        assertTrue(empty.ok)
        assertEquals(192, empty.result.str("tage_bis_tmsnat").toInt())
        assertTrue(empty.result.str("prognose").startsWith("Noch nicht möglich"))
        val full = run(AiToolbox.TMSNAT_STATUS, "{}", ctx(apProgress()))
        assertEquals(12, full.result["kategorien"]!!.jsonArray.size)
        assertNotNull(full.result["prognose"]!!.jsonObject["punkte_erwartet"])
        assertEquals("11/16", full.result["letzte_simulationen"]!!.jsonArray[0].jsonObject.str("punkte"))
        assertEquals("25.10.", full.result["letzte_simulationen"]!!.jsonArray[0].jsonObject.str("datum"))
        val review = full.result["fehlertraining"]!!.jsonObject
        assertEquals("1", review.str("faellig"))
        assertEquals("Preis vorher (Grundwert)", review["typen"]!!.jsonArray[0].jsonPrimitive.content)

        val climbEmpty = run(AiToolbox.CLIMB_STATUS, "{}")
        assertTrue(climbEmpty.ok)
        assertEquals(12, climbEmpty.result["welten"]!!.jsonArray.size)
        assertEquals("w1.complement", climbEmpty.result["naechstes_level"]!!.jsonObject.str("id"))
        val climbFull = run(AiToolbox.CLIMB_STATUS, "{}", ctx(apProgress()))
        assertEquals("80", climbFull.result.str("richtig_prozent_gesamt"))
    }

    @Test
    fun methodSearchFindsTheRightLevels() {
        fun first(q: String): String = run(AiToolbox.SEARCH_METHODS, """{"query": "$q"}""").result["treffer"]!!.jsonArray[0].jsonObject.str("id")
        assertEquals("w2.times11a", first("mal 11"))
        assertEquals("w6.addmethod", first("Additionsmethode"))
        assertEquals("w13.ratio", first("Dreisatz"))
        assertEquals("w12.weekday", first("Wochentag für ein Datum"))
        assertEquals("w10.code", first("phonetischer Code"))
        assertEquals("w4.square2", first("Quadrate zweistelliger Zahlen"))
        assertEquals("w3.sub2", first("3.4"))
        assertEquals("w6.factor", first("w6.factor"))
        val hit = run(AiToolbox.SEARCH_METHODS, """{"query": "Faktorisierungsmethode"}""").result["treffer"]!!.jsonArray[0].jsonObject
        assertTrue(hit.str("erklaerung").length > 200)
        assertTrue(hit.str("quelle").startsWith("Kapitel 3"))
        assertFalse(run(AiToolbox.SEARCH_METHODS, """{"query": "Quantenphysik"}""").ok)
    }

    @Test
    fun categoriesAreRecognized() {
        assertEquals(ApCategory.PERCENT, AiLookup.category("Prozente"))
        assertEquals(ApCategory.PERCENT, AiLookup.category("PERCENT"))
        assertEquals(ApCategory.RATIO, AiLookup.category("Dreisatz"))
        assertEquals(ApCategory.UNITS, AiLookup.category("Dosierung"))
        assertEquals(ApCategory.MIX, AiLookup.category("Mischungen"))
        assertEquals(ApCategory.PROB, AiLookup.category("Wahrscheinlichkeit"))
        assertEquals(ApCategory.COMBI, AiLookup.category("Kombinatorik"))
        assertEquals(ApCategory.GROWTH, AiLookup.category("Halbwertszeit"))
        assertEquals(ApCategory.SPEED, AiLookup.category("Geschwindigkeit"))
        assertEquals(ApCategory.WORK, AiLookup.category("Gemeinsam arbeiten"))
        assertEquals(ApCategory.EQUATIONS, AiLookup.category("Gleichungen aus dem Text"))
        assertEquals(ApCategory.RELATIONS, AiLookup.category("Beziehungsketten"))
        assertEquals(ApCategory.FRACTIONS, AiLookup.category("Anteile und Restmengen"))
        for (c in ApCategory.entries) assertEquals(c, AiLookup.category(c.title))
        assertNull(AiLookup.category("Quantenphysik"))
    }

    /** Antwort so, wie eine Person sie im Chat schreiben würde. */
    private fun chatAnswer(e: Expected, choices: List<String>): String = when (e) {
        is Expected.Choice -> if (e.index < 5 && choices[e.index].startsWith("(")) "Ich nehme " + "ABCDE"[e.index] else "Ich sage " + choices[e.index].replace(Regex("^\\([A-E]\\)\\s*"), "")
        is Expected.Remainder -> "${e.quotient} Rest ${e.remainder}"
        else -> "Ich glaube, es sind " + Fmt.spoken(e, choices)
    }

    @Test
    fun quizTasksAreCheckedLikeInTheApp() {
        var quiz = emptyList<AiQuizTask>()
        for (c in ApCategory.entries) {
            for (d in 1..3) {
                val created = run(AiToolbox.CREATE_TASK, """{"topic": "${c.title}", "difficulty": $d}""", ctx(quiz = quiz, seed = c.ordinal * 7 + d))
                assertTrue(created.label, created.ok)
                quiz = created.quiz!!
                val task = quiz.last()
                assertEquals("ap:${c.name}", task.source)
                assertTrue(created.result.str("aufgabe").length > 30)
                val problem = AiQuiz.problem(task)!!
                assertEquals(problem.key, AiQuiz.problem(task)!!.key)
                val right = run(AiToolbox.CHECK_ANSWER, """{"task_id": "${task.id}", "answer": "${chatAnswer(problem.expected, problem.choices)}"}""", ctx(quiz = quiz))
                assertTrue("${task.id} ${problem.prompt.caption}: ${right.result}", right.result["richtig"]!!.jsonPrimitive.boolean)
                assertTrue(right.result["loesungsweg"]!!.jsonArray.isNotEmpty())
                quiz = right.quiz!!
                assertTrue(quiz.last().answered)
            }
        }
        assertEquals(AiQuiz.KEPT, quiz.size)
    }

    @Test
    fun everyLevelCanBePractisedInTheChat() {
        var checked = 0
        for (w in Curriculum.worlds) {
            for (s in w.skills) {
                repeat(4) { k ->
                    val task = AiQuizTask("A1", "skill:${s.id}", 2, s.id.hashCode().toLong() * 31 + k)
                    val problem = AiQuiz.problem(task)!!
                    if (problem.prompt.hideAfterMs != null) return@repeat
                    val answer = chatAnswer(problem.expected, problem.choices)
                    val input = AiQuiz.normalizeAnswer(answer, problem)
                    assertNotNull("${s.id}: $answer", input)
                    val verdict = com.helddertierwelt.mentalmath.domain.climb.math.AnswerCheck.check(problem.expected, input!!)
                    assertTrue("${s.id}: „$answer“ → „$input“ (${problem.expected})", verdict.correct)
                    checked++
                }
            }
        }
        assertTrue(checked > 300)
    }

    @Test
    fun wrongAnswersGetTheSolutionAndTypicalMistake() {
        // Eine Aufgabe mit erklärtem typischem Fehler suchen.
        var found: Pair<AiQuizTask, String>? = null
        for (seed in 1L..400L) {
            val task = AiQuizTask("A1", "ap:PERCENT", 2, seed)
            val problem = AiQuiz.problem(task)!!
            val mistake = problem.mistakes.firstOrNull { it.answer is Expected.Exact } ?: continue
            found = task to Fmt.expected(mistake.answer)
            break
        }
        val (task, wrong) = found!!
        val out = run(AiToolbox.CHECK_ANSWER, """{"task_id": "a1", "answer": "$wrong"}""", ctx(quiz = listOf(task)))
        assertFalse(out.result["richtig"]!!.jsonPrimitive.boolean)
        assertNotNull(out.result["typischer_denkfehler"])
        assertTrue(out.label.endsWith("falsch"))
        val noNumber = run(AiToolbox.CHECK_ANSWER, """{"task_id": "A1", "answer": "keine Ahnung"}""", ctx(quiz = listOf(task)))
        assertFalse(noNumber.ok)
        val unknownTask = run(AiToolbox.CHECK_ANSWER, """{"task_id": "A9", "answer": "5"}""", ctx(quiz = listOf(task)))
        assertFalse(unknownTask.ok)
    }

    @Test
    fun hintsRevealOneStepAtATime() {
        val task = AiQuizTask("A1", "ap:PERCENT", 2, 5)
        val problem = AiQuiz.problem(task)!!
        var quiz = listOf(task)
        val final = Fmt.value(problem.expected, problem.choices)
        for (k in 0 until problem.steps.size - 1) {
            val h = run(AiToolbox.GET_HINT, """{"task_id": "A1"}""", ctx(quiz = quiz))
            assertTrue(h.ok)
            assertEquals(problem.steps[k].label, h.result.str("rechne"))
            assertFalse(h.result.toString().contains("\"$final\""))
            quiz = h.quiz!!
        }
        val last = run(AiToolbox.GET_HINT, """{"task_id": "A1"}""", ctx(quiz = quiz))
        assertNull(last.quiz)
        assertTrue(last.result.str("tipp").contains("Lass die Person rechnen"))
    }

    @Test
    fun createTaskUsesTheDiscussedTaskAndLevels() {
        val focusProblem = ApTasks.typed(ApTasks.taskOfType("MIX/dil", Random(3))!!)
        val focus = AiTaskContext(focusProblem, "12", false, null, "TMSnat-Simulation")
        val similar = run(AiToolbox.CREATE_TASK, """{"topic": "ähnlich"}""", ctx(focus = focus))
        assertEquals("type:MIX/dil", similar.quiz!!.last().source)
        val level = run(AiToolbox.CREATE_TASK, """{"topic": "mal 11"}""")
        assertEquals("skill:w2.times11a", level.quiz!!.last().source)
        val memory = run(AiToolbox.CREATE_TASK, """{"topic": "Lange Zahlen merken"}""")
        assertFalse(memory.ok)
        val nothing = run(AiToolbox.CREATE_TASK, """{"topic": "Quantenphysik"}""")
        assertFalse(nothing.ok)
        // Fortlaufende Kennungen.
        val second = run(AiToolbox.CREATE_TASK, """{"topic": "Prozente"}""", ctx(quiz = level.quiz!!))
        assertEquals("A2", second.result.str("id"))
    }

    @Test
    fun posedTasksBecomeCardsWithEverythingTheyNeed() {
        // TMSnat-Kategorie (eintippen), Level mit A–E-Antworten und ein Wochentag-Level.
        val created = run(AiToolbox.CREATE_TASK, """{"topic": "Prozente"}""")
        val task = created.quiz!!.last()
        val card = AiQuiz.card(task)!!
        val problem = AiQuiz.problem(task)!!
        assertEquals(task.id, card.id)
        assertEquals("Prozente in Textaufgaben", card.title)
        assertEquals(created.result.str("aufgabe"), card.text)
        assertEquals(problem.choices, card.choices)
        assertFalse(card.answered)
        assertTrue(created.result.str("hinweis").contains("Karte"))
        // Die Notiz fürs Gedächtnis enthält die Aufgabe, aber nicht die Lösung.
        val note = AiQuiz.note(card)
        assertTrue(note.startsWith("(Aufgabe ${task.id}"))
        assertTrue(note.contains(card.text.lines().last()))
        assertFalse(note.contains("\n"))
        val withChoices = Curriculum.worlds.flatMap { it.skills }.firstNotNullOfOrNull { s ->
            (1L..5L).map { AiQuizTask("A3", "skill:${s.id}", 2, it) }.firstOrNull { AiQuiz.problem(it)?.choices?.isNotEmpty() == true }
        }!!
        val choiceCard = AiQuiz.card(withChoices)!!
        assertTrue(choiceCard.choices.size >= 2)
        assertTrue(AiQuiz.note(choiceCard).contains("Antwortmöglichkeiten: " + choiceCard.choices.first()))
        // Nach der Antwort ist die Karte als beantwortet markiert.
        val answer = chatAnswer(problem.expected, problem.choices)
        val checked = run(AiToolbox.CHECK_ANSWER, """{"task_id": "${task.id}", "answer": "$answer"}""", ctx(quiz = created.quiz!!))
        assertTrue(AiQuiz.card(checked.quiz!!.last())!!.answered)
    }

    @Test
    fun checkAnswerTellsTheModelWhatToSay() {
        val task = AiQuizTask("A1", "ap:PERCENT", 2, 11)
        val problem = AiQuiz.problem(task)!!
        val right = run(AiToolbox.CHECK_ANSWER, """{"task_id": "A1", "answer": "${chatAnswer(problem.expected, problem.choices)}"}""", ctx(quiz = listOf(task)))
        assertTrue(right.result.str("anleitung").contains("erfinde keinen Fehler"))
        val wrongValue = (problem.expected as Expected.Exact).value.add(java.math.BigDecimal.ONE)
        val wrong = run(AiToolbox.CHECK_ANSWER, """{"task_id": "A1", "answer": "${Fmt.expected(Expected.Exact(wrongValue))}"}""", ctx(quiz = listOf(task)))
        assertFalse(wrong.result["richtig"]!!.jsonPrimitive.boolean)
        assertTrue(wrong.result.str("anleitung").startsWith("Die Antwort ist falsch"))
    }

    @Test
    fun proposalsBecomeActions() {
        val practice = run(AiToolbox.PREPARE_PRACTICE, """{"categories": ["Prozente", "Dreisatz"]}""")
        assertEquals(AiAction.ApPractice(listOf("PERCENT", "RATIO")), practice.action)
        val fromText = run(AiToolbox.PREPARE_PRACTICE, """{"categories": "Prozente und Dreisatz"}""")
        assertEquals(practice.action, fromText.action)
        assertEquals(AiAction.ApPractice(emptyList()), run(AiToolbox.PREPARE_PRACTICE, "{}").action)
        assertFalse(run(AiToolbox.PREPARE_PRACTICE, """{"categories": ["Quantenphysik"]}""").ok)
        assertEquals("Gezielt üben", AiActions.label(practice.action!!).title)

        assertEquals(AiAction.ApSimulation(16, 1_500), run(AiToolbox.PREPARE_SIMULATION, """{"variant": "tmsnat"}""").action)
        assertEquals(AiAction.ApSimulation(8, 750), run(AiToolbox.PREPARE_SIMULATION, """{"variant": "kurztest"}""").action)
        assertEquals(AiAction.ApSimulation(16, 1_200), run(AiToolbox.PREPARE_SIMULATION, """{"variant": "tempo"}""").action)
        assertEquals("Kurztest", AiActions.label(AiAction.ApSimulation(8, 750)).title)

        assertNull(run(AiToolbox.PREPARE_ERROR_TRAINING, "{}").action)
        assertEquals(AiAction.ApReview, run(AiToolbox.PREPARE_ERROR_TRAINING, "{}", ctx(apProgress())).action)

        assertFalse(run(AiToolbox.PREPARE_SIMILAR, "{}").ok)
        val focusProblem = ApTasks.typed(ApTasks.taskOfType("PERCENT/base", Random(3))!!)
        val focus = AiTaskContext(focusProblem, "12", false, null, "Üben")
        assertEquals(AiAction.ApTypes("PERCENT/base", 5), run(AiToolbox.PREPARE_SIMILAR, "{}", ctx(focus = focus)).action)
        assertEquals(AiAction.ApTypes("PERCENT/base", 10), run(AiToolbox.PREPARE_SIMILAR, """{"count": 50}""", ctx(focus = focus)).action)
        val skillFocus = AiTaskContext(Curriculum.skill("w4.mul21")!!.generate(Random(1)), "100", false, null, "Üben")
        assertEquals(AiAction.Level("w4.mul21", AiAction.MODE_PRACTICE), run(AiToolbox.PREPARE_SIMILAR, "{}", ctx(focus = skillFocus)).action)

        // Gesperrtes Level: nur öffnen; offenes Level: üben.
        val locked = run(AiToolbox.OPEN_LEVEL, """{"level": "Additionsmethode", "mode": "ueben"}""")
        assertEquals(AiAction.Level("w6.addmethod", AiAction.MODE_OPEN), locked.action)
        assertTrue(locked.result["gesperrt"]!!.jsonPrimitive.boolean)
        val open = run(AiToolbox.OPEN_LEVEL, """{"level": "Ergänzen zur 10", "mode": "üben"}""")
        assertEquals(AiAction.Level("w1.complement", AiAction.MODE_PRACTICE), open.action)
        val learn = run(AiToolbox.OPEN_LEVEL, """{"level": "Additionsmethode", "mode": "lernen"}""")
        assertEquals(AiAction.Level("w6.addmethod", AiAction.MODE_LEARN), learn.action)
        val ap = run(AiToolbox.OPEN_LEVEL, """{"level": "Wahrscheinlichkeiten", "mode": "pruefung"}""")
        assertEquals(AiAction.Level("w13.prob", AiAction.MODE_TEST), ap.action)
    }

    @Test
    fun settingsAreOnlyPrepared() {
        assertEquals(AiAction.Setting("reminder", "18:30"), run(AiToolbox.CHANGE_SETTING, """{"setting": "reminder", "value": "18:30"}""").action)
        assertEquals(AiAction.Setting("reminder", "7:00"), run(AiToolbox.CHANGE_SETTING, """{"setting": "reminder", "value": "7 Uhr"}""").action)
        assertFalse(run(AiToolbox.CHANGE_SETTING, """{"setting": "reminder", "value": "aus"}""").ok)
        val on = ctx(settings = PlusSettings(reminderOn = true))
        assertEquals(AiAction.Setting("reminder", "off"), run(AiToolbox.CHANGE_SETTING, """{"setting": "reminder", "value": "aus"}""", on).action)
        assertFalse(run(AiToolbox.CHANGE_SETTING, """{"setting": "reminder", "value": "25:00"}""").ok)
        assertEquals(AiAction.Setting("trick_tips", "off"), run(AiToolbox.CHANGE_SETTING, """{"setting": "trick_tips", "value": "aus"}""").action)
        assertFalse(run(AiToolbox.CHANGE_SETTING, """{"setting": "trick_tips", "value": "an"}""").ok)
        assertEquals(AiAction.Setting("adaptive", "on"), run(AiToolbox.CHANGE_SETTING, """{"setting": "adaptive", "value": "an"}""").action)
        assertEquals(AiAction.Setting("theme", Palette.OZEAN.name), run(AiToolbox.CHANGE_SETTING, """{"setting": "theme", "value": "Ozean"}""").action)
        assertEquals(AiAction.Setting("theme", Palette.SCHWARZ.name), run(AiToolbox.CHANGE_SETTING, """{"setting": "theme", "value": "tiefschwarz"}""").action)
        assertFalse(run(AiToolbox.CHANGE_SETTING, """{"setting": "theme", "value": "Grau"}""").ok)
        assertFalse(run(AiToolbox.CHANGE_SETTING, """{"setting": "volume", "value": "laut"}""").ok)
        assertEquals("Jeden Tag um 18:30 Uhr", AiActions.label(AiAction.Setting("reminder", "18:30")).detail)
    }

    // --- Texte für das Modell ----------------------------------------------------------------

    @Test
    fun systemPromptHasRulesDateAndStatus() {
        val prompt = AiPrompt.system(ctx(apProgress()))
        assertTrue(prompt.contains("Mental Math Plus"))
        assertTrue(prompt.contains("Heute ist Mittwoch, 28. Oktober 2026."))
        assertTrue(prompt.contains("Noch 192 Tage bis zum ersten TMSnat"))
        assertTrue(prompt.contains("zuletzt 11 von 16 Punkten"))
        assertTrue(prompt.contains("Fehlertraining: 1 Aufgabentypen fällig"))
        for (spec in AiToolbox.specs) {
            if (spec.name != AiToolbox.GET_HINT) assertTrue(spec.name, prompt.contains(spec.name) || spec.name == AiToolbox.PREPARE_SIMILAR)
        }
        // Kurz genug für einen schnellen Start (grob 3,3 Zeichen pro Token).
        assertTrue(prompt.length < 4_500)
        assertTrue(AiPrompt.system(ctx()).contains("noch keine Aufgaben gelöst"))
    }

    @Test
    fun taskQuestionContainsEverythingTheModelNeeds() {
        val problem = ApTasks.choice(ApTasks.taskOfType("PERCENT/base", Random(4))!!, 5, Random(4))
        val wrongIndex = (0 until 5).first { it != (problem.expected as Expected.Choice).index }
        val result = ProblemResult(problem, problem.choices[wrongIndex], false, 95_000, note = "Erklärung")
        val t = AiTaskContext.of(result, "TMSnat-Simulation")
        val q = AiPrompt.taskQuestion(t, "Wo lag mein Fehler?")
        assertTrue(q.contains("TMSnat-Simulation"))
        assertTrue(q.contains(problem.prompt.caption!!))
        assertTrue(q.contains("Meine Antwort: ${problem.choices[wrongIndex]} – falsch."))
        assertTrue(q.contains("Richtig ist: ${problem.choices[(problem.expected as Expected.Choice).index]}"))
        assertTrue(q.contains("Von der App erkannter Denkfehler: Erklärung"))
        assertTrue(q.contains("Meine Zeit: 95 Sekunden"))
        assertTrue(q.contains("Preis vorher (Grundwert)"))
        assertTrue(q.endsWith("Meine Frage: Wo lag mein Fehler?"))
        assertTrue(AiPrompt.suggestions(ctx(focus = t)).first().contains("Fehler"))
        assertTrue(t.card.length <= 141)
    }

    @Test
    fun levelTopicPointsTheModelToTheMethod() {
        val f = AiLookup.locate(Curriculum.worlds, "w6.addmethod")!!
        assertEquals("Welt 6, Level 1: Additionsmethode", f.name)
        val note = AiPrompt.topicNote(f)
        assertTrue(note.contains("Welt 6, Level 1: Additionsmethode"))
        assertTrue(note.contains("w6.addmethod"))
        // Das genannte Suchwort findet genau dieses Level.
        val hit = run(AiToolbox.SEARCH_METHODS, """{"query": "w6.addmethod"}""").result["treffer"]!!.jsonArray[0].jsonObject
        assertEquals("w6.addmethod", hit.str("id"))
        assertEquals(4, AiPrompt.topicSuggestions(f).size)
    }

    @Test
    fun progressNoteOnlyWhenSomethingChanged() {
        val a = apProgress()
        assertNull(AiPrompt.progressNote(a, a))
        val b = a.copy(totalSolved = a.totalSolved + 10, totalCorrect = a.totalCorrect + 8, apRuns = a.apRuns + ApRun(now, 13, 16, 1_300_000, 1_500, 5))
        val note = AiPrompt.progressNote(a, b)!!
        assertTrue(note.contains("10 Aufgaben gelöst, davon 8 richtig"))
        assertTrue(note.contains("13 von 16"))
    }

    @Test
    fun datesAreGerman() {
        assertEquals("Mittwoch, 28. Oktober 2026", AiLookup.germanDate(today))
        assertEquals("Donnerstag, 1. Januar 1970", AiLookup.germanDate(0))
        assertEquals("Samstag, 8. Mai 2027", AiLookup.germanDate(AiLookup.TMSNAT_DAY))
        assertEquals("Samstag, 29. Februar 2020", AiLookup.germanDate(AiLookup.epochDay(2020, 2, 29)))
        for (d in listOf(-1000L, 0L, 365L, 18_000L, 20_000L, 30_000L)) {
            val (y, m, day) = AiLookup.civil(d)
            assertEquals(d, AiLookup.epochDay(y, m, day))
        }
    }

    // --- Anzeige -----------------------------------------------------------------------------

    @Test
    fun latexAndTokensAreCleaned() {
        assertEquals("Das sind 3/4 der Menge.", AiText.clean("Das sind $\\frac{3}{4}$ der Menge."))
        assertEquals("96 : 0,8 = 120", AiText.clean("\\(96 \\div 0{,}8 = 120\\)"))
        assertEquals("(a + b)/2", AiText.clean("\\frac{a + b}{2}"))
        assertEquals("x² und 10³", AiText.clean("x^2 und 10^{3}"))
        assertEquals("Fertig.", AiText.clean("Fertig.<end_of_turn>"))
        assertEquals("Fertig.", AiText.clean("<|turn>Fertig.<turn|>"))
        assertEquals("( 3 + 4 ) · 2 ≤ 20", AiText.clean("\\left( 3 + 4 \\right) \\cdot 2 \\leq 20"))
        assertEquals("250 ml", AiText.clean("$250 \\text{ ml}$"))
        assertEquals("**120 €**", AiText.clean("\\boxed{120 €}"))
        assertEquals("√2 ≈ 1,41", AiText.clean("$\\sqrt{2} \\approx 1{,}41$"))
        assertEquals("Das kostet 5 $ oder 6 $.", AiText.clean("Das kostet 5 $ oder 6 $."))
        assertEquals("3 < 5 und 7 > 2", AiText.clean("3 < 5 und 7 > 2"))
        assertEquals("80%", AiText.clean("$80\\%$"))
    }

    @Test
    fun markdownIsParsed() {
        val blocks = AiMarkdown.parse(
            """
            ## Lösung
            Erst **80 %** ausrechnen, dann *zurück*.
            - Schritt eins
              - Unterpunkt
            1. Erstens
            2) Zweitens

            | Größe | Wert |
            |---|---|
            | Preis | 96 € |
            ```
            96 : 0,8
            ```
            > Merke: 100 % ist der alte Preis.
            ---
            3 * 4 = 12
            """.trimIndent(),
        )
        assertTrue(blocks[0] is MdBlock.Heading)
        val p = blocks[1] as MdBlock.Paragraph
        assertEquals("80 %", p.spans.first { it.bold }.text)
        assertEquals("zurück", p.spans.first { it.italic }.text)
        assertEquals(0, (blocks[2] as MdBlock.Bullet).level)
        assertEquals(1, (blocks[3] as MdBlock.Bullet).level)
        assertEquals("1.", (blocks[4] as MdBlock.Numbered).number)
        assertEquals("2.", (blocks[5] as MdBlock.Numbered).number)
        val table = blocks[6] as MdBlock.Table
        assertEquals("Größe", table.header[0][0].text)
        assertEquals("96 €", table.rows[0][1][0].text)
        assertEquals("96 : 0,8", (blocks[7] as MdBlock.Code).text)
        assertTrue(blocks[8] is MdBlock.Quote)
        assertEquals(MdBlock.Rule, blocks[9])
        assertEquals("3 * 4 = 12", (blocks[10] as MdBlock.Paragraph).spans.joinToString("") { it.text })
        // Nicht geschlossene Sternchen während des Schreibens: Text bleibt lesbar.
        assertEquals("Das ist wich", AiMarkdown.inline("Das ist **wich").joinToString("") { it.text })
        assertEquals("Fett: ok", AiText.plain("**Fett:** ok"))
    }

    // --- Gesprächsgedächtnis -----------------------------------------------------------------

    private fun msg(id: Long, role: AiRole, text: String, status: AiStatus = AiStatus.DONE) = AiMessage(id, role, text, status = status)

    @Test
    fun historyKeepsTheNewestMessagesWithinBudget() {
        val list = (1..20).map { i -> msg(i.toLong(), if (i % 2 == 1) AiRole.USER else AiRole.ASSISTANT, "x".repeat(330)) }
        val recent = AiHistory.recent(list, budgetTokens = 520)
        assertTrue(recent.size in 3..5)
        assertEquals(AiRole.USER, recent.first().role)
        assertEquals(20L, recent.last().id)
        // Abgebrochene Antworten fallen weg, gleiche Rollen werden zusammengelegt.
        val withError = listOf(
            msg(1, AiRole.USER, "Frage 1"),
            msg(2, AiRole.ASSISTANT, "kaputt", AiStatus.ERROR),
            msg(3, AiRole.USER, "Frage 2"),
            msg(4, AiRole.ASSISTANT, "Antwort"),
        )
        val merged = AiHistory.recent(withError, 1_000)
        assertEquals(2, merged.size)
        assertEquals("Frage 1\n\nFrage 2", merged[0].modelText)
        // Eine Frage ohne Antwort am Ende fällt weg (die neue Frage folgt direkt).
        val open = AiHistory.recent(withError + msg(5, AiRole.USER, "Frage 3"), 1_000)
        assertEquals(AiRole.ASSISTANT, open.last().role)
        assertTrue(AiHistory.recent(listOf(msg(1, AiRole.USER, "Hallo")), 1_000).isEmpty())
    }

    // --- Ablauf mit Werkzeugen ---------------------------------------------------------------

    /** Attrappe des Modells: liefert nacheinander die vorgegebenen Antworten. */
    private class ScriptedSession(private val replies: List<List<LlmChunk>>) : LlmSession {
        val inputs = mutableListOf<LlmInput>()
        override fun send(input: LlmInput, thinking: Boolean): Flow<LlmChunk> {
            inputs += input
            return flowOf(*replies.getOrElse(inputs.size - 1) { listOf(LlmChunk.Text("?")) }.toTypedArray())
        }

        override fun cancel() {}
    }

    @Test
    fun agentRunsToolsAndReturnsTheirResults() = runBlocking {
        val session = ScriptedSession(
            listOf(
                listOf(LlmChunk.Thought("Ich rechne nach."), LlmChunk.ToolCalls(listOf(LlmToolCall("calculate", """{"expression": "96 : 0,8"}""")))),
                listOf(LlmChunk.Text("Das sind "), LlmChunk.Text("120 €.")),
            ),
        )
        val events = AiAgent.answer(session, "Wie viel ist 96 durch 0,8?", thinking = true) { ctx() }.toList()
        assertEquals(AgentEvent.Thought("Ich rechne nach."), events[0])
        assertEquals(AgentEvent.ToolStarted("calculate"), events[1])
        val finished = events[2] as AgentEvent.ToolFinished
        assertEquals("120", finished.outcome.result.str("ergebnis"))
        assertEquals("Das sind 120 €.", events.filterIsInstance<AgentEvent.Text>().joinToString("") { it.text })
        val toolInput = session.inputs[1] as LlmInput.ToolResults
        assertEquals("calculate", toolInput.results[0].first)
        assertTrue(toolInput.results[0].second.contains("\"ergebnis\":\"120\""))
    }

    @Test
    fun textAroundToolCallsGetsItsOwnParagraph() = runBlocking {
        val session = ScriptedSession(
            listOf(
                listOf(LlmChunk.Text("Ich rechne nach."), LlmChunk.ToolCalls(listOf(LlmToolCall("calculate", """{"expression": "17 * 23"}""")))),
                listOf(LlmChunk.Text("Das sind "), LlmChunk.Text("391.")),
            ),
        )
        val text = AiAgent.answer(session, "17 mal 23?", thinking = false) { ctx() }.toList()
            .filterIsInstance<AgentEvent.Text>().joinToString("") { it.text }
        assertEquals("Ich rechne nach.\n\nDas sind 391.", text)
    }

    @Test
    fun agentStopsEndlessToolLoops() = runBlocking {
        val loop = List(10) { listOf<LlmChunk>(LlmChunk.ToolCalls(listOf(LlmToolCall("get_tmsnat_status", "{}")))) }
        val session = ScriptedSession(loop)
        val events = AiAgent.answer(session, "Hallo", thinking = false) { ctx() }.toList()
        assertEquals(AiAgent.MAX_ROUNDS, events.count { it is AgentEvent.ToolFinished })
        assertTrue(events.filterIsInstance<AgentEvent.Text>().last().text.contains("verheddert"))
        // Ab der dritten gleichen Abfrage kommt nur noch ein Hinweis.
        val repeated = events.filterIsInstance<AgentEvent.ToolFinished>().map { it.outcome.ok }
        assertEquals(listOf(true, true, false, false, false, false), repeated)
    }

    @Test
    fun agentUsesTheLatestContextForEachTool() = runBlocking {
        val session = ScriptedSession(
            listOf(
                listOf(LlmChunk.ToolCalls(listOf(LlmToolCall("create_task", """{"topic": "Prozente"}""")))),
                listOf(LlmChunk.ToolCalls(listOf(LlmToolCall("get_hint", """{"task_id": "A1"}""")))),
                listOf(LlmChunk.Text("Hier ist ein Tipp.")),
            ),
        )
        var quiz = emptyList<AiQuizTask>()
        val events = mutableListOf<AgentEvent>()
        AiAgent.answer(session, "Stell mir eine Aufgabe", thinking = false) { ctx(quiz = quiz) }.collect { e ->
            events += e
            if (e is AgentEvent.ToolFinished) e.outcome.quiz?.let { quiz = it }
        }
        val hint = events.filterIsInstance<AgentEvent.ToolFinished>()[1].outcome
        assertTrue(hint.result.toString(), hint.ok)
        assertEquals(1, quiz.single().hints)
    }

    @Test
    fun actionLabelsCoverEveryAction() {
        val actions = listOf(
            AiAction.ApPractice(emptyList()),
            AiAction.ApPractice(listOf("MIX")),
            AiAction.ApSimulation(16, 1_500),
            AiAction.ApReview,
            AiAction.ApTypes("PERCENT/base", 5),
            AiAction.Level("w6.addmethod", AiAction.MODE_OPEN),
            AiAction.Level("w6.addmethod", AiAction.MODE_LEARN),
            AiAction.Level("w6.addmethod", AiAction.MODE_PRACTICE),
            AiAction.Level("w6.addmethod", AiAction.MODE_TEST),
            AiAction.Setting("reminder", "off"),
            AiAction.Setting("trick_tips", "on"),
            AiAction.Setting("adaptive", "off"),
            AiAction.Setting("theme", "WALD"),
        )
        for (a in actions) {
            val l = AiActions.label(a)
            assertTrue(a.toString(), l.title.isNotBlank() && l.detail.isNotBlank() && l.button.isNotBlank())
        }
        assertEquals("Welt 6 · Additionsmethode", AiActions.label(actions[5]).detail)
        assertEquals("Mischen und Verdünnen", AiActions.label(actions[1]).detail)
        // Vorschläge lassen sich speichern und laden.
        val json = Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(AiAction.serializer()), actions)
        assertEquals(actions, Json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(AiAction.serializer()), json))
        assertTrue(json.contains("\"type\":\"practice\""))
        assertFalse(JsonArray(emptyList()).isNotEmpty())
    }
}
