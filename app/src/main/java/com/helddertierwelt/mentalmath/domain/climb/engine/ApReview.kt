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

import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
import com.helddertierwelt.mentalmath.domain.climb.model.ApMistakeEntry
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import kotlin.random.Random

/**
 * Fehlertraining im TMSnat-Training: Jeder Aufgabentyp, der falsch gelöst wurde, kommt wieder –
 * sofort, dann nach 1, 3 und 7 Tagen. Nach der vierten richtigen Wiederholung gilt er als gelernt.
 */
object ApReview {

    /** Abstände in Tagen nach der 1., 2. und 3. richtigen Wiederholung. */
    val INTERVAL_DAYS = listOf(1, 3, 7)

    /** So viele Aufgabentypen kommen höchstens in einer Einheit Fehlertraining. */
    const val SESSION_MAX = 12

    /** Fällige Aufgabentypen, die ältesten zuerst. */
    fun due(p: ClimbProgress, now: Long): List<ApMistakeEntry> = p.apMistakes.filter { it.due <= now }.sortedBy { it.due }

    /** Der nächste Zeitpunkt, an dem ein Typ fällig wird (null: nichts in der Wiederholung). */
    fun nextDue(p: ClimbProgress, now: Long): Long? = p.apMistakes.filter { it.due > now }.minOfOrNull { it.due }

    /**
     * Ergebnisse einer Einheit einrechnen: Falsch → Typ kommt (wieder) ins Fehlertraining.
     * Richtig bei einem fälligen Typ → eine Stufe weiter; nach der letzten Stufe gelernt.
     * Nicht bearbeitete Aufgaben einer Simulation zählen nicht als Denkfehler.
     */
    fun update(p: ClimbProgress, results: List<ProblemResult>, now: Long): ClimbProgress {
        var list = p.apMistakes
        var changed = false
        for (r in results) {
            if (r.unanswered) continue
            val type = ApTasks.typeOf(r.problem) ?: continue
            val i = list.indexOfFirst { it.type == type }
            if (!r.correct) {
                val entry = if (i >= 0) list[i].copy(stage = 0, due = now, misses = list[i].misses + 1) else ApMistakeEntry(type, 0, now, 1)
                list = if (i >= 0) list.toMutableList().also { it[i] = entry } else list + entry
                changed = true
            } else if (i >= 0 && list[i].due <= now) {
                val e = list[i]
                val next = e.stage + 1
                list = if (next > INTERVAL_DAYS.size) {
                    list.filterIndexed { j, _ -> j != i }
                } else {
                    list.toMutableList().also { it[i] = e.copy(stage = next, due = now + INTERVAL_DAYS[next - 1] * Progression.DAY_MS) }
                }
                changed = true
            }
        }
        return if (changed) p.copy(apMistakes = list) else p
    }

    /** Aufgaben für eine Einheit Fehlertraining: zu jedem fälligen Typ eine neue Aufgabe mit anderen Zahlen. */
    fun problems(p: ClimbProgress, now: Long, r: Random, options: Int = 5): List<Problem> =
        due(p, now).take(SESSION_MAX)
            .mapNotNull { e -> ApTasks.taskOfType(e.type, r)?.let { ApTasks.choice(it, options, r) } }
            .shuffled(r)
}
