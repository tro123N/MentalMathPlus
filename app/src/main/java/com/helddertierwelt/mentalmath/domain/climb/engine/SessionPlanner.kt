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

import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import kotlin.random.Random

/** Stellt die Aufgaben einer Übungseinheit zusammen. */
object SessionPlanner {

    /** [count] möglichst unterschiedliche Aufgaben eines Levels. */
    fun problems(skill: Skill, count: Int, random: Random, avoid: Set<String> = emptySet()): List<Problem> {
        val result = mutableListOf<Problem>()
        val keys = HashSet<String>(avoid)
        var attempts = 0
        while (result.size < count && attempts < count * 40) {
            attempts++
            val p = skill.generate(random)
            if (keys.add(p.key)) result += p
        }
        while (result.size < count) result += skill.generate(random)
        return result
    }

    /** Gemischte Wiederholung über mehrere Levels. */
    fun review(skills: List<Skill>, perSkill: Int, random: Random): List<Problem> =
        skills.flatMap { problems(it, perSkill, random) }.shuffled(random)
}
