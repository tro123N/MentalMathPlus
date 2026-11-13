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

package com.helddertierwelt.mentalmath.domain.climb.model

import kotlinx.serialization.Serializable

/** Gespeicherter Fortschritt eines Levels. */
@Serializable
data class SkillProgress(
    /** Beste erreichte Sterne in einer Prüfung (0 bis 3). */
    val stars: Int = 0,
    /** Beste Durchschnittszeit pro Aufgabe in einer bestandenen Prüfung (ms), 0 = keine. */
    val bestAvgMs: Long = 0,
    val testsTaken: Int = 0,
    val testsPassed: Int = 0,
    /** Nicht bestandene Prüfungen in Folge. */
    val failStreak: Int = 0,
    /** Geübte Aufgaben insgesamt (Üben, geführt, Wiederholung). */
    val practiced: Int = 0,
    val practicedCorrect: Int = 0,
    val lastPracticedAt: Long = 0,
    /** Aktueller Wiederholungsabstand in Tagen (0 = noch nicht eingeplant). */
    val intervalDays: Int = 0,
    /** Zeitpunkt (ms), ab dem das Level wieder zur Auffrischung fällig ist. */
    val nextReviewAt: Long = 0,
    /** Das Level ist „eingerostet“ und muss aufgefrischt werden. */
    val rusty: Boolean = false,
    /** Ein darauf aufbauendes Level ist mehrfach nicht bestanden worden; dieses Level zuerst auffrischen. */
    val refreshPending: Boolean = false,
    /** Letzte Ergebnisse: positive Zahl = richtig in so vielen ms, negative Zahl = falsch. */
    val recent: List<Long> = emptyList(),
    /** Tagesstatistik dieses Levels (ohne geführtes Üben), die letzten [ClimbProgress.SKILL_DAYS] Übungstage. */
    val days: List<DayStat> = emptyList(),
    /** Die letzten Prüfungen dieses Levels. */
    val tests: List<TestLog> = emptyList(),
    /** Das Level wurde über einen Einstufungstest übersprungen. */
    val placed: Boolean = false,
    /** Gedächtnismodell: nach so vielen Tagen ist die Sicherheit auf 90 % gesunken (0 = noch unbekannt). */
    val stability: Double = 0.0,
    /** Zeitpunkt (ms), auf den sich [stability] bezieht – zuletzt geübt. */
    val memoryAt: Long = 0,
    /** Gedächtnismodell: wie schwer dieses Level für die Person ist (1 leicht bis 10 schwer, 0 = noch unbekannt). */
    val difficulty: Double = 0.0,
) {
    val mastered: Boolean get() = stars >= 1
    val needsAttention: Boolean get() = rusty || refreshPending
}

/** Gelöste Aufgaben an einem Tag. */
@Serializable
data class DayStat(
    /** Tag seit 1.1.1970 in Ortszeit. */
    val day: Long,
    val solved: Int = 0,
    val correct: Int = 0,
    /** Summe der Rechenzeiten in ms. */
    val totalMs: Long = 0,
) {
    operator fun plus(other: DayStat): DayStat =
        copy(solved = solved + other.solved, correct = correct + other.correct, totalMs = totalMs + other.totalMs)
}

/** Eine abgelegte Prüfung. */
@Serializable
data class TestLog(
    /** Zeitpunkt in ms. */
    val at: Long,
    val avgMs: Long,
    val correct: Int,
    val total: Int,
    val stars: Int,
)

/** Eine Testsimulation „Arithmetisches Problemlösen“. */
@Serializable
data class ApRun(
    /** Zeitpunkt in ms. */
    val at: Long,
    val score: Int,
    val total: Int,
    /** Verbrauchte Zeit in ms. */
    val usedMs: Long,
    /** Zeitlimit in Sekunden. */
    val seconds: Int,
    /** Anzahl der Antwortmöglichkeiten (im TMSnat 5, A–E). */
    val options: Int,
)

/** Ein Aufgabentyp im Fehlertraining (TMSnat-Training). */
@Serializable
data class ApMistakeEntry(
    /** Aufgabentyp, z. B. „PERCENT/base“ (Kategorie und Art der Aufgabe). */
    val type: String,
    /** 0 = gerade falsch gelöst; jede richtige Wiederholung erhöht die Stufe. */
    val stage: Int = 0,
    /** Ab diesem Zeitpunkt (ms) ist der Typ wieder dran. */
    val due: Long = 0,
    /** So oft war dieser Typ insgesamt falsch. */
    val misses: Int = 1,
)

/** Statistik einer Aufgabenart im freien Training. */
@Serializable
data class FreeStat(
    val solved: Int = 0,
    val correct: Int = 0,
    val totalMs: Long = 0,
)

/** Gesamter Fortschritt im Kletterweg. */
@Serializable
data class ClimbProgress(
    val version: Int = 1,
    val skills: Map<String, SkillProgress> = emptyMap(),
    /** Tage in Folge mit mindestens einer abgeschlossenen Übung. */
    val streakDays: Int = 0,
    /** Letzter aktiver Tag (Epoch-Tag), -1 = noch nie. */
    val lastActiveDay: Long = -1,
    val totalSolved: Long = 0,
    val totalCorrect: Long = 0,
    /** Statistik des freien Trainings je Aufgabenart (Schlüssel z. B. „free.percent“). */
    val free: Map<String, FreeStat> = emptyMap(),
    /** Bestwerte des freien Trainings je Modus (Schlüssel z. B. „ZEIT_60“). */
    val freeBest: Map<String, Int> = emptyMap(),
    /** Gelöste Aufgaben pro Tag (alle Bereiche), die letzten [DAYS_KEPT] Übungstage. */
    val days: List<DayStat> = emptyList(),
    /** Längste Serie von Tagen in Folge. */
    val bestStreak: Int = 0,
    /** Testsimulationen „Arithmetisches Problemlösen“, die neuesten zuletzt. */
    val apRuns: List<ApRun> = emptyList(),
    /** Fehlertraining: Aufgabentypen des TMSnat-Trainings, die zuletzt falsch gelöst wurden. */
    val apMistakes: List<ApMistakeEntry> = emptyList(),
    /** Alle Levels sind offen (Schalter in den Einstellungen) – Sterne gibt es weiter nur durch Prüfungen. */
    val unlockAll: Boolean = false,
    /** Fehlerprotokoll: die letzten falsch gelösten Aufgaben mit erkanntem Denkfehler. */
    val errors: List<ErrorEntry> = emptyList(),
    /** Trainingsplan des Tages (wird einmal pro Tag erstellt). */
    val plan: DayPlan? = null,
    /** Trainingspläne der Person (mehrere gleichzeitig möglich). */
    val plans: List<TrainingPlan> = emptyList(),
    /** Der alte Einzelplan (Version 2.5) wurde in [plans] übernommen. */
    val plansMigrated: Boolean = false,
) {
    fun of(skillId: String): SkillProgress = skills[skillId] ?: SkillProgress()

    fun with(skillId: String, update: (SkillProgress) -> SkillProgress): ClimbProgress =
        copy(skills = skills + (skillId to update(of(skillId))))

    companion object {
        /** So viele Übungstage werden insgesamt gespeichert. */
        const val DAYS_KEPT = 400

        /** So viele Übungstage werden je Level gespeichert. */
        const val SKILL_DAYS = 60

        /** So viele Prüfungen werden je Level gespeichert. */
        const val TESTS_KEPT = 30

        /** So viele Testsimulationen werden gespeichert. */
        const val AP_RUNS_KEPT = 100
    }
}

/** Einen Tageseintrag in eine nach Tagen sortierte Liste einrechnen und auf [keep] Einträge kürzen. */
fun List<DayStat>.addDay(entry: DayStat, keep: Int): List<DayStat> {
    val last = lastOrNull()
    val merged = when {
        last == null || last.day < entry.day -> this + entry
        last.day == entry.day -> dropLast(1) + (last + entry)
        else -> {
            // Älterer Tag (z. B. nach Zeitumstellung): an der richtigen Stelle einrechnen.
            val existing = indexOfFirst { it.day == entry.day }
            if (existing >= 0) mapIndexed { i, d -> if (i == existing) d + entry else d } else (this + entry).sortedBy { it.day }
        }
    }
    return if (merged.size > keep) merged.takeLast(keep) else merged
}

/** Eine falsch gelöste Aufgabe im Fehlerprotokoll. */
@Serializable
data class ErrorEntry(
    val skillId: String,
    val at: Long,
    /** Erkannter typischer Denkfehler (falls die Antwort zu einem bekannten Fehler passt). */
    val note: String? = null,
)

/** Ein Abschnitt des Tagesplans. */
@Serializable
data class PlanBlock(
    val id: String,
    /** WARMUP, CHECK, NEW, WEAK, SPEED oder APPLY. */
    val kind: String,
    val title: String,
    /** Warum dieser Abschnitt heute dran ist (für die Person). */
    val reason: String,
    val skillIds: List<String> = emptyList(),
    /** Anzahl Aufgaben. */
    val count: Int = 0,
    /** Wie der Abschnitt gestartet wird (ap, units, pattern, speed, mix, check …). */
    val target: String? = null,
    /** Fehlermuster-Training: der Denkfehler, um den es geht. */
    val note: String? = null,
    /** Zu welchem Trainingsplan der Abschnitt gehört (null: für alle, z. B. Aufwärmen). */
    val planId: String? = null,
)

/** Ein Trainingsplan, den die Person anlegt: was, wie viel, an welchen Tagen und bis wann. */
@Serializable
data class TrainingPlan(
    val id: String,
    val name: String,
    /** Ausgewählte Levels (Reihenfolge wie im Kletterweg). */
    val skillIds: List<String>,
    /** Minuten an jedem Übungstag. */
    val minutes: Int = 20,
    /** Übungstage, 1 = Montag … 7 = Sonntag. */
    val weekdays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    /** Erster Tag (Epoch-Tag). */
    val startDay: Long,
    /** Letzter Tag, z. B. der Prüfungstag (Epoch-Tag); null = ohne Ende. */
    val endDay: Long? = null,
    /** Neue Levels in der Reihenfolge des Kletterwegs (sonst abwechselnd aus den gewählten Welten). */
    val followPath: Boolean = true,
    val paused: Boolean = false,
    /** Geschaffte Levels der Auswahl beim Start (für die Fortschrittsanzeige). */
    val masteredAtStart: Int = 0,
)

/** Der Trainingsplan eines Tages. */
@Serializable
data class DayPlan(
    val day: Long,
    val minutes: Int,
    val blocks: List<PlanBlock>,
    /** IDs der erledigten Abschnitte. */
    val done: List<String> = emptyList(),
    /** Einstellungen, mit denen der Plan erstellt wurde (ändern sie sich, wird neu geplant). */
    val settingsKey: String = "",
    /** Hinweise zur Planung (z. B. warum heute nichts Neues kommt). */
    val notes: List<String> = emptyList(),
) {
    val next: PlanBlock? get() = blocks.firstOrNull { it.id !in done }
    val finished: Boolean get() = blocks.isNotEmpty() && blocks.all { it.id in done }
}
