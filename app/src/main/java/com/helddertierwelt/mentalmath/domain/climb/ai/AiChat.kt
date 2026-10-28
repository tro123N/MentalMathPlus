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
import com.helddertierwelt.mentalmath.domain.climb.engine.ProblemResult
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.Palette
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wer spricht. */
@Serializable
enum class AiRole { USER, ASSISTANT }

/** Zustand einer Antwort der KI. */
@Serializable
enum class AiStatus {
    /** Fertig. */
    DONE,

    /** Wird gerade geschrieben. */
    STREAMING,

    /** Von der Person abgebrochen. */
    STOPPED,

    /** Fehler beim Antworten. */
    ERROR,
}

/** Ein Werkzeug, das die KI bei einer Antwort benutzt hat (für die kleinen Hinweise unter der Antwort). */
@Serializable
data class AiToolUse(val name: String, val label: String, val ok: Boolean = true)

/** Eine Nachricht im Gespräch mit dem KI-Coach. */
@Serializable
data class AiMessage(
    val id: Long,
    val role: AiRole,
    /** Was angezeigt wird. */
    val text: String,
    /** Was das Modell bekommt, wenn es vom angezeigten Text abweicht (z. B. mit der ganzen Aufgabe). */
    val prompt: String? = null,
    /** Gedanken der KI (Denkmodus), werden eingeklappt angezeigt. */
    val thought: String = "",
    /** Kurzbeschreibung der Aufgabe, um die es geht (wird als kleine Karte gezeigt). */
    val task: String? = null,
    /** Vorschläge der KI, die die Person mit einem Knopf ausführt. */
    val actions: List<AiAction> = emptyList(),
    /** Schon ausgeführte bzw. übernommene Vorschläge (Index in [actions]). */
    val done: List<Int> = emptyList(),
    val tools: List<AiToolUse> = emptyList(),
    val status: AiStatus = AiStatus.DONE,
    /** Fehlermeldung bei [AiStatus.ERROR]. */
    val error: String? = null,
) {
    /** Text für das Gedächtnis des Modells. */
    val modelText: String get() = prompt ?: text
}

/**
 * Ein Vorschlag der KI, den die Person per Knopf ausführt. Die KI startet nie selbst eine Übung
 * oder ändert Einstellungen – sie bereitet nur vor.
 */
@Serializable
sealed interface AiAction {

    /** TMSnat-Training: üben in bestimmten Kategorien (leer = alle gemischt). */
    @Serializable
    @SerialName("practice")
    data class ApPractice(val categories: List<String>) : AiAction

    /** Testsimulation mit [count] Aufgaben in [seconds] Sekunden. */
    @Serializable
    @SerialName("simulation")
    data class ApSimulation(val count: Int, val seconds: Int) : AiAction

    /** Fehlertraining. */
    @Serializable
    @SerialName("review")
    data object ApReview : AiAction

    /** Mehrere Aufgaben eines Aufgabentyps (z. B. „PERCENT/base“) mit neuen Zahlen. */
    @Serializable
    @SerialName("types")
    data class ApTypes(@SerialName("taskType") val type: String, val count: Int) : AiAction

    /** Ein Level des Kletterwegs öffnen, lernen, üben oder prüfen. */
    @Serializable
    @SerialName("level")
    data class Level(val skillId: String, val mode: String = MODE_OPEN) : AiAction

    /** Eine Einstellung ändern: [key] ist z. B. „reminder“, [value] z. B. „18:30“. */
    @Serializable
    @SerialName("setting")
    data class Setting(val key: String, val value: String) : AiAction

    companion object {
        const val MODE_OPEN = "open"
        const val MODE_LEARN = "learn"
        const val MODE_PRACTICE = "practice"
        const val MODE_TEST = "test"

        const val SETTING_REMINDER = "reminder"
        const val SETTING_TRICK_TIPS = "trick_tips"
        const val SETTING_ADAPTIVE = "adaptive"
        const val SETTING_THEME = "theme"
    }
}

/** Beschriftung eines Vorschlags für die Oberfläche. */
data class AiActionLabel(val title: String, val detail: String, val button: String, val doneText: String)

object AiActions {

    fun categories(names: List<String>): List<ApCategory> = names.mapNotNull { n -> ApCategory.entries.firstOrNull { it.name == n } }

    fun label(action: AiAction): AiActionLabel = when (action) {
        is AiAction.ApPractice -> {
            val cats = categories(action.categories)
            if (cats.isEmpty() || cats.size == ApCategory.entries.size) {
                AiActionLabel("Gemischt üben", "Alle 12 Kategorien, mit Lösungsweg nach Fehlern", "Starten", "Gestartet")
            } else {
                AiActionLabel("Gezielt üben", cats.joinToString(", ") { it.title }, "Starten", "Gestartet")
            }
        }
        is AiAction.ApSimulation -> AiActionLabel(
            ApTasks.simulationName(action.count, action.seconds),
            "${action.count} Aufgaben in ${minutes(action.seconds)}, Antworten A–E",
            "Starten",
            "Gestartet",
        )
        AiAction.ApReview -> AiActionLabel("Fehlertraining", "Aufgabentypen, die zuletzt falsch waren – mit neuen Zahlen", "Starten", "Gestartet")
        is AiAction.ApTypes -> AiActionLabel(
            "${action.count} ähnliche Aufgaben",
            ApTasks.typeName(action.type) + " · " + (ApCategory.entries.firstOrNull { it.name == action.type.substringBefore('/') }?.title ?: ""),
            "Starten",
            "Gestartet",
        )
        is AiAction.Level -> {
            val skill = Curriculum.skill(action.skillId)
            val world = Curriculum.worldOf(action.skillId)
            val name = skill?.title ?: action.skillId
            val where = if (world != null) "Welt ${world.number} · $name" else name
            when (action.mode) {
                AiAction.MODE_LEARN -> AiActionLabel("Lernen: $name", where, "Öffnen", "Geöffnet")
                AiAction.MODE_PRACTICE -> AiActionLabel("Üben: $name", where, "Starten", "Gestartet")
                AiAction.MODE_TEST -> AiActionLabel("Prüfung: $name", where, "Starten", "Gestartet")
                else -> AiActionLabel("Level öffnen", where, "Öffnen", "Geöffnet")
            }
        }
        is AiAction.Setting -> when (action.key) {
            AiAction.SETTING_REMINDER -> if (action.value == "off") {
                AiActionLabel("Erinnerung ausschalten", "Keine tägliche Erinnerung mehr", "Übernehmen", "Übernommen")
            } else {
                AiActionLabel("Tägliche Erinnerung", "Jeden Tag um ${action.value} Uhr", "Übernehmen", "Übernommen")
            }
            AiAction.SETTING_TRICK_TIPS -> AiActionLabel(
                if (action.value == "on") "Trick-Tipps einschalten" else "Trick-Tipps ausschalten",
                "Hinweis auf den passenden Trick bei jeder Aufgabe",
                "Übernehmen",
                "Übernommen",
            )
            AiAction.SETTING_ADAPTIVE -> AiActionLabel(
                if (action.value == "on") "Anpassende Schwierigkeit an" else "Anpassende Schwierigkeit aus",
                "Die Stufe passt sich deinen Ergebnissen an",
                "Übernehmen",
                "Übernommen",
            )
            AiAction.SETTING_THEME -> AiActionLabel(
                "Farbthema „${Palette.of(action.value).title}“",
                Palette.of(action.value).subtitle,
                "Übernehmen",
                "Übernommen",
            )
            else -> AiActionLabel("Einstellung", action.value, "Übernehmen", "Übernommen")
        }
    }

    private fun minutes(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (s == 0) "$m Minuten" else "$m:${s.toString().padStart(2, '0')} Minuten"
    }
}

/** Eine Aufgabe, die die KI im Gespräch gestellt hat. Sie wird aus [seed] immer gleich neu erzeugt. */
@Serializable
data class AiQuizTask(
    /** Kurze Kennung, z. B. „A1“. */
    val id: String,
    /** Herkunft: „ap:PERCENT“ (Kategorie), „type:PERCENT/base“ (Aufgabentyp) oder „skill:w3.add2“ (Level). */
    val source: String,
    val difficulty: Int,
    val seed: Long,
    /** So viele Tipps wurden schon gegeben. */
    val hints: Int = 0,
    /** Die Person hat schon geantwortet: Der Lösungsweg ist bekannt. */
    val answered: Boolean = false,
)

/** Eine Aufgabe aus der App, über die gesprochen wird (z. B. nach einem Fehler im Üben). */
data class AiTaskContext(
    val problem: Problem,
    /** Antwort der Person in Anzeigeform, leer = keine Antwort. */
    val answer: String,
    val correct: Boolean,
    /** Erklärung eines erkannten typischen Fehlers. */
    val note: String?,
    /** Woher die Aufgabe stammt, z. B. „TMSnat-Simulation“. */
    val source: String,
    /** Zeit für die Aufgabe in ms (0 = unbekannt). */
    val timeMs: Long = 0,
) {
    /** Kurzer Text für die Karte über der Frage. */
    val card: String
        get() {
            val text = (problem.prompt.caption ?: problem.prompt.text).let { if (it.length > 140) it.take(137).trimEnd() + " …" else it }
            return text
        }

    companion object {
        fun of(result: ProblemResult, source: String): AiTaskContext =
            AiTaskContext(result.problem, result.answer, result.correct, result.note, source, result.totalMs)
    }
}

/** Das gespeicherte Gespräch. */
@Serializable
data class AiConversation(
    val messages: List<AiMessage> = emptyList(),
    val quiz: List<AiQuizTask> = emptyList(),
    /** Fortlaufende Nummer für Nachrichten. */
    val nextId: Long = 1,
) {
    companion object {
        /** So viele Nachrichten werden höchstens gespeichert. */
        const val KEPT = 80
    }
}

/** Hilfen für das Gedächtnis des Modells. */
object AiHistory {

    /** Grobe Schätzung: so viele Zeichen deutscher Text ergeben etwa ein Token. */
    const val CHARS_PER_TOKEN = 3.3

    fun tokens(text: String): Int = (text.length / CHARS_PER_TOKEN).toInt() + 4

    /**
     * Die letzten Nachrichten, die zusammen höchstens [budgetTokens] groß sind – für den Neustart
     * des Modells mit dem bisherigen Gespräch. Abgebrochene und fehlerhafte Antworten fallen weg;
     * das Ergebnis beginnt mit einer Nachricht der Person und endet mit einer Antwort der KI
     * (danach folgt die neue Frage).
     */
    fun recent(messages: List<AiMessage>, budgetTokens: Int): List<AiMessage> {
        val usable = messages.filter { it.status == AiStatus.DONE && it.modelText.isNotBlank() }
        val picked = ArrayDeque<AiMessage>()
        var used = 0
        for (m in usable.asReversed()) {
            val t = tokens(m.modelText)
            if (used + t > budgetTokens && picked.isNotEmpty()) break
            picked.addFirst(m)
            used += t
        }
        while (picked.isNotEmpty() && picked.first().role != AiRole.USER) picked.removeFirst()
        while (picked.isNotEmpty() && picked.last().role != AiRole.ASSISTANT) picked.removeLast()
        // Zwei Nachrichten derselben Rolle hintereinander zusammenfassen (z. B. nach einem Fehler).
        val merged = mutableListOf<AiMessage>()
        for (m in picked) {
            val last = merged.lastOrNull()
            if (last != null && last.role == m.role) {
                merged[merged.lastIndex] = last.copy(prompt = last.modelText + "\n\n" + m.modelText)
            } else {
                merged += m
            }
        }
        return merged
    }
}

/** Texte, die zur Anzeige von Zahlen gebraucht werden. */
internal fun Problem.answerText(): String = Fmt.spoken(expected, choices)
