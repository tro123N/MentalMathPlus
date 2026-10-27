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

import kotlin.math.max
import kotlin.math.min

/** Stand der anpassenden Schwierigkeit im freien Training. */
data class AdaptiveState(
    /** Aktuelle Stufe 1 (leicht) bis 3 (schwer). */
    val level: Int,
    /** Positive Zahl: gute Antworten in Folge; negative Zahl: Fehler in Folge. */
    val streak: Int = 0,
    /** Wie oft die Stufe schon gewechselt hat. */
    val changes: Int = 0,
)

/**
 * Anpassende Schwierigkeit: Wer mehrmals hintereinander richtig und zügig antwortet, bekommt
 * schwerere Aufgaben; nach Fehlern in Folge werden sie wieder leichter.
 */
object Adaptive {

    /** So viele gute Antworten in Folge heben die Stufe. */
    const val UP_AFTER = 4

    /** So viele Fehler in Folge senken die Stufe. */
    const val DOWN_AFTER = 2

    /** Zügig heißt: höchstens so viele ms (für Rechnungen; Textaufgaben bekommen doppelt so viel Zeit). */
    fun fastMs(level: Int, topicId: String): Long {
        val base = 6_000L + 4_000L * (level - 1)
        val slowTopic = topicId.endsWith("word") || topicId.endsWith("geometry") || topicId.endsWith("four_ops") || topicId.endsWith("algebra")
        return if (slowTopic) base * 2 else base
    }

    fun update(state: AdaptiveState, correct: Boolean, timeMs: Long, topicId: String): AdaptiveState {
        val fast = timeMs <= fastMs(state.level, topicId)
        return when {
            correct && fast -> {
                val streak = max(state.streak, 0) + 1
                if (streak >= UP_AFTER && state.level < 3) {
                    AdaptiveState(state.level + 1, 0, state.changes + 1)
                } else {
                    state.copy(streak = streak)
                }
            }

            !correct -> {
                val streak = min(state.streak, 0) - 1
                if (streak <= -DOWN_AFTER && state.level > 1) {
                    AdaptiveState(state.level - 1, 0, state.changes + 1)
                } else {
                    state.copy(streak = streak)
                }
            }

            // Richtig, aber langsam: Stufe passt, Zähler zurücksetzen.
            else -> state.copy(streak = 0)
        }
    }
}
