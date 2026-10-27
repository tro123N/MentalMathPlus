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

import com.helddertierwelt.mentalmath.domain.climb.curriculum.World12
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Prüft die Wochentagsmethode und das magische Quadrat gegen exakte Rechnungen. */
class World12Test {

    @Test
    fun weekdayMethodMatchesCalendar() {
        var date = LocalDate.of(1800, 1, 1)
        val end = LocalDate.of(2199, 12, 31)
        while (!date.isAfter(end)) {
            val expected = date.dayOfWeek.value % 7 // Montag 1 … Sonntag 0
            assertEquals("$date", expected, World12.weekdayCode(date.dayOfMonth, date.monthValue, date.year))
            date = date.plusDays(1)
        }
    }

    @Test
    fun baseSquareIsMagic() {
        val b = World12.base
        val groups = mutableListOf<List<Long>>()
        groups += b
        groups += (0..3).map { c -> (0..3).map { b[it][c] } }
        groups += (0..3).map { b[it][it] }
        groups += (0..3).map { b[it][3 - it] }
        for ((r, c) in listOf(0 to 0, 0 to 2, 2 to 0, 2 to 2, 1 to 1)) groups += listOf(b[r][c], b[r][c + 1], b[r + 1][c], b[r + 1][c + 1])
        groups += listOf(b[0][0], b[0][3], b[3][0], b[3][3])
        groups.forEach {
            assertEquals(34L, it.sum())
            assertEquals(1, it.count { v -> v >= 13 })
        }
    }

    @Test
    fun magicSquaresHitAnyTotal() {
        for (total in 35L..200L) {
            val cells = (0..3).map { r -> (0..3).map { c -> World12.magicSquare(total, r, c).steps.last().expected } }
            val values = cells.map { row -> row.map { (it as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Exact).value.toLong() } }
            values.forEach { assertEquals(total, it.sum()) }
            (0..3).forEach { c -> assertEquals(total, values.sumOf { it[c] }) }
        }
    }
}
