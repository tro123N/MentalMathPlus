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

import com.helddertierwelt.mentalmath.domain.climb.math.Phonetic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Prüft die eigenen Wortlisten gegen den phonetischen Code. */
class PhoneticTest {

    @Test
    fun pegsMatchTheirNumbers() {
        Phonetic.pegs.forEachIndexed { i, word ->
            assertEquals("Merkwort $word", i.toString(), Phonetic.code(word))
        }
    }

    @Test
    fun longWordsAreDecodable() {
        Phonetic.longWords.forEach { assertNotNull("Wort $it", Phonetic.code(it)) }
        assertEquals("735", Phonetic.code("Kamel"))
        assertEquals("196", Phonetic.code("Teppich"))
        assertEquals("74185", Phonetic.code("Kartoffel"))
        assertEquals("3014", Phonetic.code("Hamster"))
    }

    @Test
    fun nonsenseWordsDecodeBack() {
        listOf("115", "05", "738", "6060", "999", "12345").forEach { d ->
            assertEquals(d, Phonetic.code(Phonetic.nonsense(d)))
            assertEquals(d, Phonetic.code(Phonetic.wordFor(d)))
        }
    }
}
