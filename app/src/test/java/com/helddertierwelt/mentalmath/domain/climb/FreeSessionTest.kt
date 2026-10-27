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
import com.helddertierwelt.mentalmath.domain.climb.engine.FreeMode
import com.helddertierwelt.mentalmath.domain.climb.engine.FreeRules
import com.helddertierwelt.mentalmath.domain.climb.engine.Phase
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionFinisher
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionLogic
import com.helddertierwelt.mentalmath.domain.climb.free.FreeTopic
import com.helddertierwelt.mentalmath.domain.climb.free.FreeTopics
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FreeSessionTest {

    private fun problems(n: Int) = List(n) { FreeTopics.generate(FreeTopic.BASIC, 1, Random(it.toLong())) }

    @Test
    fun survivalEndsAfterLives() {
        var s = SessionLogic.createFree(problems(50), FreeRules(FreeMode.UEBERLEBEN, 3, 1), "Test", null)
        repeat(3) {
            s = SessionLogic.type(s, '0').first
            s = SessionLogic.type(s, '0').first
            s = SessionLogic.submit(s.copy(input = "999999"), 1000).first
            assertEquals(Phase.FLASH, s.phase)
            s = SessionLogic.advance(s)
        }
        assertEquals(Phase.FINISHED, s.phase)
        assertEquals(3, s.results.size)
        val (p, summary) = SessionFinisher.finish(Curriculum.worlds, ClimbProgress(), s, 0, 100)
        assertEquals(0, summary.freeScore)
        assertEquals(3, p.free["free.basic"]?.solved)
    }

    @Test
    fun timedEndsWhenTimeIsUsed() {
        var s = SessionLogic.createFree(problems(50), FreeRules(FreeMode.ZEIT, 10, 1), "Test", null)
        while (s.phase != Phase.FINISHED) {
            val answer = (s.expected as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Exact).value.toPlainString()
            s = SessionLogic.submit(s.copy(input = answer), 3000).first
            s = SessionLogic.advance(s)
        }
        assertEquals(4, s.results.size)
        val (p, summary) = SessionFinisher.finish(Curriculum.worlds, ClimbProgress(), s, 0, 100)
        assertEquals(4, summary.freeScore)
        assertEquals(4, p.freeBest["ZEIT_10_1"])
        assertTrue(SessionLogic.finishNow(s).phase == Phase.FINISHED)
    }
}
