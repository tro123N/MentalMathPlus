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

package com.helddertierwelt.mentalmath.domain.climb.engine

/** Zeitanalyse einer Testsimulation: Wie wurde die Zeit auf die Aufgaben verteilt? */
object ApTime {

    /** Ab diesem Vielfachen der Zielzeit gilt eine Aufgabe als Zeitfresser. */
    const val SLOW_FACTOR = 1.6

    data class Task(
        /** Nummer in der ursprünglichen Reihenfolge (1 bis n). */
        val number: Int,
        /** Gesamte Zeit, auch aus übersprungenen Versuchen. */
        val ms: Long,
        val correct: Boolean,
        val answered: Boolean,
    )

    data class Report(
        /** Alle Aufgaben in der ursprünglichen Reihenfolge. */
        val tasks: List<Task>,
        /** Zielzeit pro Aufgabe (Gesamtzeit geteilt durch die Zahl der Aufgaben). */
        val targetMs: Long,
        /** Zeit für die erste und die zweite Hälfte der Aufgaben (nach Nummer). */
        val firstHalfMs: Long,
        val secondHalfMs: Long,
        /** Aufgaben, die deutlich länger als die Zielzeit gedauert haben, die längsten zuerst. */
        val slow: List<Task>,
        /** Zeit, die auf falsch gelöste Aufgaben entfiel. */
        val wrongMs: Long,
        val wrongCount: Int,
        val unanswered: Int,
        /** Durchschnitt pro bearbeiteter Aufgabe. */
        val avgAnsweredMs: Long,
    )

    fun analyze(results: List<ProblemResult>, limitSeconds: Int): Report {
        val tasks = results.mapIndexed { i, r ->
            Task(number = if (r.number > 0) r.number else i + 1, ms = r.totalMs, correct = r.correct, answered = !r.unanswered)
        }.sortedBy { it.number }
        val n = tasks.size.coerceAtLeast(1)
        val target = limitSeconds * 1000L / n
        val half = (tasks.size + 1) / 2
        val answered = tasks.filter { it.answered }
        val wrong = answered.filter { !it.correct }
        return Report(
            tasks = tasks,
            targetMs = target,
            firstHalfMs = tasks.take(half).sumOf { it.ms },
            secondHalfMs = tasks.drop(half).sumOf { it.ms },
            slow = tasks.filter { it.ms > target * SLOW_FACTOR }.sortedByDescending { it.ms },
            wrongMs = wrong.sumOf { it.ms },
            wrongCount = wrong.size,
            unanswered = tasks.count { !it.answered },
            avgAnsweredMs = if (answered.isEmpty()) 0 else answered.sumOf { it.ms } / answered.size,
        )
    }
}
