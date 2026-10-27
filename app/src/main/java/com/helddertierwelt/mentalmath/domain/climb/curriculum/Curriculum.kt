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

package com.helddertierwelt.mentalmath.domain.climb.curriculum

import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World

/** Der gesamte Kletterweg in der Reihenfolge, in der er gegangen wird. */
object Curriculum {

    val worlds: List<World> by lazy {
        listOf(
            World1.world,
            World2.world,
            World3.world,
            World4.world,
            World5.world,
            World6.world,
        )
    }

    val skills: List<Skill> by lazy { worlds.flatMap { it.skills } }

    fun skill(id: String): Skill? = skills.firstOrNull { it.id == id }

    fun worldOf(skillId: String): World? = worlds.firstOrNull { w -> w.skills.any { it.id == skillId } }
}
