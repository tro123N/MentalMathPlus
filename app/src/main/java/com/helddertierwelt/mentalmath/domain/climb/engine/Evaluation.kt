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

import com.helddertierwelt.mentalmath.domain.climb.model.Goals
import com.helddertierwelt.mentalmath.domain.climb.model.Problem

/** Art einer Übungseinheit. */
enum class SessionType {
    /** Zwischenschritte eintippen, ohne Zeitdruck. */
    GUIDED,

    /** Freies Üben mit Lösungsweg nach Fehlern. */
    PRACTICE,

    /** Prüfung auf Zeit; entscheidet über Sterne und Freischalten. */
    TEST,

    /** Kurze Auffrischungsprüfung für eingerostete Levels. */
    REFRESH,

    /** Gemischte Wiederholung fälliger Levels. */
    REVIEW,

    /** Freies Training mit eigenen Aufgabenarten und Modi. */
    FREE,

    /** Einstufungstest: eine ganze Welt auf einmal prüfen und bei Erfolg überspringen. */
    PLACEMENT,
}

/** Ergebnis einer einzelnen Aufgabe. */
data class ProblemResult(
    val problem: Problem,
    /** Eingabe des Nutzers in Anzeigeform, leer bei Zeitablauf. */
    val answer: String,
    val correct: Boolean,
    val timeMs: Long,
    val timedOut: Boolean = false,
    /** Hinweise, die im geführten Üben gebraucht wurden. */
    val hintsUsed: Int = 0,
    /** Wiederholung einer zuvor falsch gelösten Aufgabe in derselben Einheit. */
    val retry: Boolean = false,
)

/** Auswertung einer Prüfung. */
data class Outcome(
    val total: Int,
    val correct: Int,
    val accuracy: Double,
    val avgMs: Long,
    val stars: Int,
) {
    val passed: Boolean get() = stars >= 1
}

object Evaluation {

    fun outcome(goals: Goals, results: List<ProblemResult>): Outcome {
        val total = results.size
        val correct = results.count { it.correct }
        val accuracy = if (total == 0) 0.0 else correct.toDouble() / total
        val avgMs = if (total == 0) 0L else results.sumOf { it.timeMs } / total
        val avgSec = avgMs / 1000.0
        val stars = when {
            total == 0 -> 0
            accuracy + 1e-9 < goals.minAccuracy -> 0
            avgSec <= goals.star3 -> 3
            avgSec <= goals.star2 -> 2
            avgSec <= goals.star1 -> 1
            else -> 0
        }
        return Outcome(total, correct, accuracy, avgMs, stars)
    }

    /** Wie viele Fehler in einer Prüfung höchstens erlaubt sind. */
    fun allowedMistakes(goals: Goals): Int {
        val n = goals.testSize
        var mistakes = 0
        while (mistakes < n && (n - (mistakes + 1)).toDouble() / n + 1e-9 >= goals.minAccuracy) mistakes++
        return mistakes
    }
}
