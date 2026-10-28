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
import com.helddertierwelt.mentalmath.domain.climb.engine.ApReview
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress

/** Texte für das Modell: Rolle und Regeln (Systemanweisung), Aufgaben und Vorschläge. */
object AiPrompt {

    /** Die Rolle und die Regeln des KI-Coachs. */
    private val rules = """
        Du bist der KI-Coach der App „Mental Math Plus“. Du läufst offline direkt auf dem Handy der Person. Du hilfst beim Kopfrechnen und bei der Vorbereitung auf den Untertest „Arithmetisches Problemlösen“ des TMSnat (Medizinertest ab 2027; der Untertest stammt aus dem HAM-Nat).

        Die App:
        - Kletterweg: Welten 1 bis 12 mit Levels zu Rechentricks (Lernen, Üben, Prüfung mit bis zu drei Sternen). Die Methoden folgen dem Buch „Mathe-Magie“; die App erklärt sie in eigenen Worten.
        - TMSnat-Training (Welt 13): Textaufgaben in 12 Kategorien; Testsimulation mit 16 Aufgaben in 25 Minuten, Antworten A bis E, ohne Taschenrechner, ohne Abzug für falsche Antworten; Fehlertraining (falsch gelöste Aufgabentypen kommen nach 1, 3 und 7 Tagen wieder); Punkt-Prognose.
        - Freies Training und der klassische Modus.

        Deine Regeln:
        1. Antworte auf Deutsch, freundlich, in einfachen Worten und knapp. Sprich die Person mit „du“ an.
        2. Rechne nie im Kopf: Prüfe jede Zahl, die du ausrechnest, vorher mit dem Werkzeug calculate.
        3. Zahlen zum Lernstand nennst du nur aus get_tmsnat_status oder get_climb_status. Erfinde nichts.
        4. Bevor du einen Rechentrick oder eine Methode der App erklärst, hol dir die Erklärung mit search_methods.
        5. Passt eine Übung, bereite sie mit prepare_practice, prepare_simulation, prepare_error_training, prepare_similar_tasks oder open_level vor. Die Person startet sie selbst über den Knopf unter deiner Antwort. Einstellungen bereitest du mit change_setting vor.
        6. Aufgaben im Chat: Erzeuge sie mit create_task und stelle sie wörtlich, ohne die Lösung zu verraten. Antwortet die Person, prüfe mit check_answer. Bittet sie um Hilfe, gib mit get_hint einen Schritt als Tipp.
        7. Geht es um eine Aufgabe aus der App, ist ihr Lösungsweg richtig. Erkläre ihn Schritt für Schritt und zeige, wo der Denkfehler lag und woran man ihn künftig erkennt.
        8. Schreibe ohne LaTeX: × für mal, : für geteilt, Brüche wie 3/4, Kommazahlen mit Komma (2,5).
        9. Gib keine Texte aus Büchern wieder. Wenn du etwas nicht weißt, sag es ehrlich.
    """.trimIndent()

    /** Systemanweisung mit dem heutigen Datum und einem kurzen Lernstand. */
    fun system(ctx: AiContext): String = rules + "\n\n" + situation(ctx)

    /** Datum, Tage bis zum Test und ein kurzer Lernstand. */
    fun situation(ctx: AiContext): String {
        val p = ctx.progress
        val lines = mutableListOf<String>()
        val days = AiLookup.TMSNAT_DAY - ctx.today
        lines += "Heute ist ${AiLookup.germanDate(ctx.today)}." + when {
            days > 1 -> " Noch $days Tage bis zum ersten TMSnat (8./9. Mai 2027)."
            days in 0..1 -> " Der erste TMSnat (8./9. Mai 2027) steht unmittelbar bevor."
            else -> ""
        }
        lines += "Kurzer Stand: " + summary(p, ctx)
        ctx.focus?.let { lines += "Die Person hat das Gespräch von einer Aufgabe aus geöffnet (${it.source})." }
        return lines.joinToString("\n")
    }

    /** Ein bis zwei Sätze zum Lernstand – genug für eine Begrüßung, Details liefern die Werkzeuge. */
    fun summary(p: ClimbProgress, ctx: AiContext): String {
        val worlds = ctx.worlds
        val parts = mutableListOf<String>()
        val streak = Progression.visibleStreak(p, ctx.today)
        parts += if (p.totalSolved == 0L) "noch keine Aufgaben gelöst" else "${Fmt.int(p.totalSolved)} Aufgaben gelöst" + if (streak > 1) ", Serie $streak Tage" else ""
        val main = worlds.filter { !it.independent }.flatMap { it.skills }
        val mastered = main.count { p.of(it.id).mastered }
        parts += "Kletterweg $mastered von ${main.size} Levels geschafft" +
            (Progression.nextSkill(worlds, p)?.let { next -> AiLookup.locate(worlds, next.id)?.let { ", als Nächstes ${it.name}" } } ?: "")
        val due = Progression.dueSkills(worlds, p, ctx.now).size
        if (due > 0) parts += "$due Levels zur Wiederholung fällig"
        val runs = p.apRuns
        val ap = ApCategory.entries.sumOf { p.of(it.skillId).recent.size }
        parts += when {
            runs.isNotEmpty() -> "TMSnat: ${runs.size} Simulation(en), zuletzt ${runs.last().score} von ${runs.last().total} Punkten"
            ap > 0 -> "TMSnat: $ap Aufgaben geübt, noch keine Simulation"
            else -> "TMSnat-Training noch nicht begonnen"
        }
        val reviewDue = ApReview.due(p, ctx.now).size
        if (reviewDue > 0) parts += "Fehlertraining: $reviewDue Aufgabentypen fällig"
        return parts.joinToString("; ") + "."
    }

    /** Der ganze Text einer Aufgabe aus der App für das Modell – mit Antwort der Person und Lösungsweg. */
    fun taskBlock(t: AiTaskContext): String {
        val p = t.problem
        val sb = StringBuilder()
        sb.append("Es geht um diese Aufgabe aus der App (").append(t.source).append("):\n")
        sb.append(AiLookup.taskText(p)).append('\n')
        if (p.choices.isNotEmpty()) sb.append("Antwortmöglichkeiten: ").append(p.choices.joinToString("  ")).append('\n')
        ApTasks.typeOf(p)?.let { type ->
            val cat = ApCategory.of(p)
            sb.append("Aufgabentyp: ").append(ApTasks.typeName(type)).append(cat?.let { " (${it.title})" } ?: "").append('\n')
        }
        when {
            t.answer.isEmpty() -> sb.append("Ich habe nicht geantwortet.\n")
            t.correct -> sb.append("Meine Antwort: ").append(t.answer).append(" – richtig.\n")
            else -> sb.append("Meine Antwort: ").append(t.answer).append(" – falsch.\n")
        }
        sb.append("Richtig ist: ").append(p.answerText()).append('\n')
        t.note?.let { sb.append("Von der App erkannter Denkfehler: ").append(it).append('\n') }
        if (t.timeMs > 0) sb.append("Meine Zeit: ").append(Calculator.german(java.math.BigDecimal(t.timeMs / 1000.0).setScale(0, java.math.RoundingMode.HALF_UP))).append(" Sekunden\n")
        p.method?.let { sb.append("Methode: ").append(it).append('\n') }
        sb.append("Lösungsweg der App:\n")
        AiLookup.solutionLines(p).forEach { sb.append(it).append('\n') }
        return sb.toString().trimEnd()
    }

    /** Frage zu einer Aufgabe: Aufgabe und Frage in einer Nachricht. */
    fun taskQuestion(t: AiTaskContext, question: String): String = taskBlock(t) + "\n\nMeine Frage: " + question.trim()

    /** Kurzer Hinweis, wenn seit der letzten Antwort weiter geübt wurde. */
    fun progressNote(before: ClimbProgress, after: ClimbProgress): String? {
        val solved = after.totalSolved - before.totalSolved
        val correct = after.totalCorrect - before.totalCorrect
        val runs = after.apRuns.size - before.apRuns.size
        if (solved <= 0 && runs <= 0) return null
        val parts = mutableListOf<String>()
        if (solved > 0) parts += "$solved Aufgaben gelöst, davon $correct richtig"
        if (runs > 0) after.apRuns.lastOrNull()?.let { parts += "neue Simulation: ${it.score} von ${it.total} Punkten" }
        return "(Hinweis der App: Seit der letzten Nachricht hat die Person weiter geübt – ${parts.joinToString(", ")}. Aktuelle Zahlen liefern die Werkzeuge.)"
    }

    /** Vorschläge zum Antippen, passend zur Lage. */
    fun suggestions(ctx: AiContext): List<String> {
        val focus = ctx.focus
        if (focus != null) {
            return if (focus.correct) {
                listOf("Wie geht das noch schneller?", "Erkläre den Lösungsweg", "Gib mir ähnliche Aufgaben")
            } else {
                listOf("Wo lag mein Fehler?", "Erkläre den Lösungsweg Schritt für Schritt", "Gib mir ähnliche Aufgaben", "Wie erkenne ich solche Aufgaben?")
            }
        }
        val p = ctx.progress
        val list = mutableListOf("Wie stehe ich gerade da?", "Was soll ich heute üben?")
        if (ApReview.due(p, ctx.now).isNotEmpty()) list += "Was steht im Fehlertraining an?"
        list += "Stell mir eine Prozentaufgabe"
        list += if (p.apRuns.isEmpty()) "Wie läuft der Test genau ab?" else "Wie teile ich mir die 25 Minuten ein?"
        list += "Erkläre mir den Dreisatz"
        if (!ctx.settings.reminderOn) list += "Erinnere mich täglich ans Üben"
        return list
    }
}
