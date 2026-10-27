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
import com.helddertierwelt.mentalmath.domain.climb.engine.Phase
import com.helddertierwelt.mentalmath.domain.climb.engine.Session
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionEvent
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionFinisher
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionLogic
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SessionTest {

    private val worlds = Curriculum.worlds

    private fun raw(e: Expected): String = when (e) {
        is Expected.Exact -> e.value.stripTrailingZeros().toPlainString().replace('.', ',')
        is Expected.Approx -> e.value.stripTrailingZeros().toPlainString().replace('.', ',')
        is Expected.Remainder -> "${e.quotient}R${e.remainder}"
        is Expected.Frac -> "${e.numerator}/${e.denominator}"
        is Expected.Choice -> e.index.toString()
        is Expected.Digits -> e.digits
    }

    private fun typeText(s: Session, text: String): Session = text.fold(s) { acc, c -> SessionLogic.type(acc, c).first }

    private fun answerCorrectly(s: Session): Pair<Session, SessionEvent> {
        val typed = typeText(s, raw(s.expected!!))
        return SessionLogic.submit(typed, 2_000)
    }

    @Test
    fun practiceWithAllCorrectFinishes() {
        val skill = Curriculum.skill("w4.mul21")!!
        var s = SessionLogic.create(SessionType.PRACTICE, skill, Random(3))
        assertEquals(skill.practiceCount, s.total)
        while (s.phase != Phase.FINISHED) {
            val (next, event) = answerCorrectly(s)
            assertEquals(SessionEvent.CORRECT, event)
            assertEquals(Phase.FLASH, next.phase)
            s = SessionLogic.advance(next)
        }
        assertEquals(skill.practiceCount, s.results.size)
        assertTrue(s.results.all { it.correct })
    }

    @Test
    fun wrongAnswerInPracticeShowsSolutionAndComesBack() {
        val skill = Curriculum.skill("w4.mul21")!!
        var s = SessionLogic.create(SessionType.PRACTICE, skill, Random(5))
        val firstKey = s.problem!!.key
        s = typeText(s, "1")
        val (afterWrong, event) = SessionLogic.submit(s, 1_000)
        assertEquals(SessionEvent.WRONG, event)
        assertEquals(Phase.REVEAL, afterWrong.phase)
        assertEquals(skill.practiceCount + 1, afterWrong.total)
        assertEquals(firstKey, afterWrong.problems[1 + SessionLogic.RETRY_DISTANCE].problem.key)
        assertTrue(afterWrong.problems[1 + SessionLogic.RETRY_DISTANCE].retry)
        s = SessionLogic.advance(afterWrong)
        assertEquals(1, s.index)
        assertEquals(Phase.ANSWERING, s.phase)
    }

    @Test
    fun guidedModeWalksThroughSteps() {
        val skill = Curriculum.skill("w4.mul31")!!
        var s = SessionLogic.create(SessionType.GUIDED, skill, Random(7))
        val steps = s.problem!!.steps.size
        assertTrue(steps >= 3)
        // Erster Schritt falsch, dann richtig.
        s = typeText(s, "1")
        val (wrong, e1) = SessionLogic.submit(s, 500)
        assertEquals(SessionEvent.STEP_WRONG, e1)
        assertEquals(Phase.ANSWERING, wrong.phase)
        assertTrue(wrong.showHint)
        s = wrong
        repeat(steps) { i ->
            val (next, event) = answerCorrectly(s)
            if (i < steps - 1) {
                assertEquals(SessionEvent.STEP_CORRECT, event)
                assertEquals(i + 1, next.stepIndex)
            } else {
                assertEquals(SessionEvent.CORRECT, event)
                assertEquals(Phase.FLASH, next.phase)
            }
            s = next
        }
        assertEquals(1, s.results.size)
        assertTrue(s.results[0].correct)
        assertEquals(1, s.results[0].hintsUsed)
        s = SessionLogic.advance(s)
        assertEquals(1, s.index)
        assertEquals(0, s.stepIndex)
    }

    @Test
    fun guidedRevealAfterTwoMistakes() {
        val skill = Curriculum.skill("w4.mul21")!!
        var s = SessionLogic.create(SessionType.GUIDED, skill, Random(9))
        s = SessionLogic.submit(typeText(s, "1"), 100).first
        val (revealed, _) = SessionLogic.submit(typeText(s, "2"), 100)
        assertEquals(Phase.REVEAL, revealed.phase)
        s = SessionLogic.advance(revealed)
        assertEquals(Phase.ANSWERING, s.phase)
        assertEquals(1, s.stepIndex)
        while (s.index == 0 && s.phase != Phase.FINISHED) {
            s = if (s.phase == Phase.ANSWERING) answerCorrectly(s).first else SessionLogic.advance(s)
        }
        assertFalse("Aufgabe mit aufgedecktem Schritt zählt nicht als richtig", s.results[0].correct)
    }

    @Test
    fun examTimeoutCountsAsWrong() {
        val skill = Curriculum.skill("w1.times")!!
        var s = SessionLogic.create(SessionType.TEST, skill, Random(11))
        assertNotNull(s.timeLimitMs)
        val (timedOut, event) = SessionLogic.timeout(s, s.timeLimitMs!!)
        assertEquals(SessionEvent.TIMEOUT, event)
        assertTrue(timedOut.results.last().timedOut)
        assertEquals(skill.goals.testSize, timedOut.total)
        s = SessionLogic.advance(timedOut)
        assertEquals(1, s.index)
    }

    @Test
    fun keysAreFilteredByInputKind() {
        val decimalSkill = Curriculum.skill("w5.dec21")!!
        var s = SessionLogic.create(SessionType.PRACTICE, decimalSkill, Random(1))
        s = typeText(s, "6,0,2")
        assertEquals("6,02", s.input)
        val intSkill = Curriculum.skill("w4.mul21")!!
        var t = SessionLogic.create(SessionType.PRACTICE, intSkill, Random(1))
        t = typeText(t, "0,5R/7")
        assertEquals("57", t.input)
        t = SessionLogic.backspace(t).first
        assertEquals("5", t.input)
    }

    @Test
    fun passingATestUnlocksTheNextLevel() {
        val skill = worlds[0].skills[0]
        var s = SessionLogic.create(SessionType.TEST, skill, Random(13))
        while (s.phase != Phase.FINISHED) {
            val typed = typeText(s, raw(s.expected!!))
            s = SessionLogic.advance(SessionLogic.submit(typed, 900).first)
        }
        val (p, summary) = SessionFinisher.finish(worlds, ClimbProgress(), s, now = 1_000_000L, today = 10)
        assertEquals(3, summary.starsAfter)
        assertTrue(summary.outcome!!.passed)
        assertEquals(worlds[0].skills[1].id, summary.unlocked?.id)
        assertEquals(1, p.streakDays)
        assertEquals(skill.goals.testSize.toLong(), p.totalSolved)
    }

    @Test
    fun reviewCoversDueSkills() {
        val skills = listOf(Curriculum.skill("w1.times")!!, Curriculum.skill("w2.square5")!!)
        val s = SessionLogic.createReview(skills, Random(2), "Wiederholung")
        assertEquals(6, s.total)
        assertEquals(setOf("w1.times", "w2.square5"), s.problems.map { it.problem.skillId }.toSet())
    }
}
