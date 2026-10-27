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
    /** Anzahl der Antwortmöglichkeiten (4 = HAM-Nat, 5 = TMSnat). */
    val options: Int,
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
