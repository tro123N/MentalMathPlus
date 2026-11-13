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
package com.helddertierwelt.mentalmath.domain.climb

import com.helddertierwelt.mentalmath.domain.climb.curriculum.Curriculum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Herkunft der Levels: Buch oder Ergänzung, und die Reihenfolge der Kapitel im Kletterweg. */
class BookTest {
    private val worlds = Curriculum.worlds

    @Test
    fun supplementWorldsContainNoBookLevels() {
        worlds.filter { it.independent }.forEach { w -> assertTrue(w.id, w.supplement) }
        worlds.filter { !it.independent }.forEach { w -> assertTrue(w.id, w.recommendedAfter == null) }
    }

    @Test
    fun mainPathFollowsTheChapters() {
        // Hauptweg: Kapitel steigen an; bewusste Ausnahmen (vorgezogen oder als Vorstufe wiederholt) stehen hier.
        val allowed = setOf("w5.percent", "w6.close", "w12.newyear", "w1.times", "w1.divfacts", "w1.zeros")
        var last = 0
        val outOfOrder = mutableListOf<String>()
        worlds.filter { !it.independent }.flatMap { it.skills }.forEach { s ->
            val ch = s.book.chapter ?: return@forEach
            if (s.id in allowed) return@forEach
            if (ch < last) outOfOrder += s.id
            last = maxOf(last, ch)
        }
        assertEquals(emptyList<String>(), outOfOrder)
        // Alle Kapitel 0–9 kommen vor.
        assertEquals((0..9).toList(), worlds.flatMap { it.chapters }.distinct().sorted())
    }
}
