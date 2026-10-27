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

import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.math.floor
import kotlin.random.Random

/** Auswertung eines Einstufungstests. */
data class PlacementOutcome(
    val worldId: String,
    val total: Int,
    val correct: Int,
    /** Höchstens so viele Fehler waren erlaubt. */
    val allowedMistakes: Int,
    /**
     * Tempo im Verhältnis zum Ein-Stern-Ziel der jeweiligen Levels, gemittelt über alle Aufgaben.
     * 1,0 = genau so schnell wie für einen Stern nötig; kleiner ist schneller.
     */
    val pace: Double,
    val passed: Boolean,
    /** So viele Levels wurden durch den Test neu als geschafft markiert. */
    val newlyMastered: Int = 0,
) {
    val accuracyOk: Boolean get() = total - correct <= allowedMistakes
    val paceOk: Boolean get() = pace <= 1.0 + 1e-9
}

/**
 * Einstufungstest: Aus jedem Level einer Welt kommen [PER_SKILL] Aufgaben, jede mit dem
 * Zeitlimit ihres Levels. Wer fast alles richtig hat und im Schnitt das Tempo für einen
 * Stern schafft, überspringt die Welt.
 */
object Placement {

    /** Aufgaben je Level. */
    const val PER_SKILL = 2

    /** Erlaubter Fehleranteil. */
    const val MISTAKE_SHARE = 0.1

    fun problemCount(world: World): Int = world.skills.size * PER_SKILL

    fun allowedMistakes(total: Int): Int = floor(total * MISTAKE_SHARE + 1e-9).toInt()

    /** Ungefähre Dauer in Sekunden, wenn man das Ein-Stern-Tempo schafft. */
    fun estimatedSeconds(world: World): Double = world.skills.sumOf { it.goals.star1 * PER_SKILL }

    /** Welten, für die ein Einstufungstest noch etwas bringt: nicht vollständig geschafft. */
    fun candidates(worlds: List<World>, progress: ClimbProgress): List<World> =
        worlds.filter { w -> !w.independent && w.skills.any { !progress.of(it.id).mastered } }

    /** Die Welt, mit der man anfangen sollte: die erste, die noch nicht geschafft ist. */
    fun recommended(worlds: List<World>, progress: ClimbProgress): World? = candidates(worlds, progress).firstOrNull()

    fun create(world: World, random: Random): Session {
        val problems = world.skills.flatMap { skill ->
            val limit = (skill.goals.timeLimit * 1000).toLong()
            SessionPlanner.problems(skill, PER_SKILL, random).map { Planned(it, limitMs = limit) }
        }
        return Session(
            type = SessionType.PLACEMENT,
            skillId = null,
            title = "Welt ${world.number} · ${world.title}",
            problems = problems,
            worldId = world.id,
        )
    }

    fun evaluate(world: World, results: List<ProblemResult>): PlacementOutcome {
        val star1 = world.skills.associate { it.id to it.goals.star1 * 1000.0 }
        val total = results.size
        val correct = results.count { it.correct }
        val pace = if (total == 0) {
            Double.MAX_VALUE
        } else {
            results.sumOf { r -> r.timeMs / (star1[r.problem.skillId] ?: 1.0) } / total
        }
        val allowed = allowedMistakes(total)
        val complete = total >= problemCount(world)
        val passed = complete && total - correct <= allowed && pace <= 1.0 + 1e-9
        return PlacementOutcome(
            worldId = world.id,
            total = total,
            correct = correct,
            allowedMistakes = allowed,
            pace = pace,
            passed = passed,
        )
    }
}
