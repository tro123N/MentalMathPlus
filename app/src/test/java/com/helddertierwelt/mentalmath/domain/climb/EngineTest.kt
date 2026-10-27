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
import com.helddertierwelt.mentalmath.domain.climb.engine.Evaluation
import com.helddertierwelt.mentalmath.domain.climb.engine.Outcome
import com.helddertierwelt.mentalmath.domain.climb.engine.ProblemResult
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.math.AnswerCheck
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.Goals
import com.helddertierwelt.mentalmath.domain.climb.model.SkillProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import kotlin.random.Random

class EngineTest {

    private val worlds = Curriculum.worlds
    private val now = 1_800_000_000_000L
    private val day = Progression.DAY_MS

    private fun results(skillId: String, correct: Int, total: Int, ms: Long): List<ProblemResult> {
        val skill = Curriculum.skill(skillId)!!
        val random = Random(1)
        return (0 until total).map { i ->
            ProblemResult(problem = skill.generate(random), answer = "", correct = i < correct, timeMs = ms)
        }
    }

    private fun mastered(vararg ids: String): ClimbProgress =
        ids.fold(ClimbProgress()) { p, id -> p.with(id) { it.copy(stars = 1, intervalDays = 1, nextReviewAt = now + day) } }

    @Test
    fun starsDependOnAccuracyAndTime() {
        val goals = Goals(testSize = 10, star1 = 10.0, star2 = 6.0, star3 = 3.0)
        assertEquals(3, Evaluation.outcome(goals, results("w1.times", 10, 10, 2_500)).stars)
        assertEquals(2, Evaluation.outcome(goals, results("w1.times", 9, 10, 5_000)).stars)
        assertEquals(1, Evaluation.outcome(goals, results("w1.times", 10, 10, 9_000)).stars)
        assertEquals(0, Evaluation.outcome(goals, results("w1.times", 10, 10, 11_000)).stars)
        assertEquals(0, Evaluation.outcome(goals, results("w1.times", 8, 10, 1_000)).stars)
        assertEquals(1, Evaluation.allowedMistakes(goals))
        assertEquals(1, Evaluation.allowedMistakes(Goals(testSize = 6, minAccuracy = 0.83, star1 = 3.0, star2 = 2.0, star3 = 1.0)))
    }

    @Test
    fun unlockingFollowsThePath() {
        val first = worlds[0].skills[0].id
        val second = worlds[0].skills[1].id
        val empty = ClimbProgress()
        assertTrue(Progression.skillUnlocked(worlds, empty, first))
        assertFalse(Progression.skillUnlocked(worlds, empty, second))
        assertTrue(Progression.skillUnlocked(worlds, mastered(first), second))
        assertFalse(Progression.worldUnlocked(worlds, empty, 1))
        val allOfWorld1 = mastered(*worlds[0].skills.map { it.id }.toTypedArray())
        assertTrue(Progression.worldUnlocked(worlds, allOfWorld1, 1))
        assertTrue(Progression.skillUnlocked(worlds, allOfWorld1, worlds[1].skills[0].id))
        assertEquals(worlds[1].skills[0].id, Progression.nextSkill(worlds, allOfWorld1)?.id)
    }

    @Test
    fun twoFailedTestsSendYouBackToThePrerequisite() {
        val pre = worlds[0].skills[0].id
        val skill = worlds[0].skills[1].id
        val fail = Outcome(total = 10, correct = 5, accuracy = 0.5, avgMs = 9_000, stars = 0)
        var p = mastered(pre)
        p = Progression.afterTest(worlds, p, skill, fail, now)
        assertTrue(Progression.blockers(worlds, p, skill).isEmpty())
        p = Progression.afterTest(worlds, p, skill, fail, now)
        assertTrue(p.of(pre).refreshPending)
        assertEquals(listOf(pre), Progression.blockers(worlds, p, skill).map { it.id })

        val pass = Outcome(total = 6, correct = 6, accuracy = 1.0, avgMs = 1_000, stars = 3)
        p = Progression.afterRefresh(p, pre, pass, now)
        assertFalse(p.of(pre).refreshPending)
        assertTrue(Progression.blockers(worlds, p, skill).isEmpty())
    }

    @Test
    fun passingATestSchedulesAReview() {
        val skill = worlds[0].skills[0].id
        val pass = Outcome(total = 12, correct = 12, accuracy = 1.0, avgMs = 1_500, stars = 2)
        val p = Progression.afterTest(worlds, ClimbProgress(), skill, pass, now)
        assertEquals(2, p.of(skill).stars)
        assertEquals(now + day, p.of(skill).nextReviewAt)
        assertTrue(Progression.dueSkills(worlds, p, now).isEmpty())
        assertEquals(listOf(skill), Progression.dueSkills(worlds, p, now + day).map { it.id })
    }

    @Test
    fun reviewsGrowIntervalsOrMakeSkillsRusty() {
        val id = "w1.times"
        val good = results(id, 3, 3, 1_000)
        val bad = results(id, 1, 3, 1_000)
        var p = mastered(id)
        p = Progression.afterReview(worlds, p, good, now)
        assertEquals(3, p.of(id).intervalDays)
        p = Progression.afterReview(worlds, p, bad, now)
        assertTrue(p.of(id).rusty)
        assertEquals(1, p.of(id).intervalDays)
        assertEquals(listOf(id), Progression.needsRefresh(worlds, p).map { it.id })
    }

    @Test
    fun streakCountsConsecutiveDays() {
        var p = ClimbProgress()
        p = Progression.touchStreak(p, 100)
        assertEquals(1, p.streakDays)
        p = Progression.touchStreak(p, 100)
        assertEquals(1, p.streakDays)
        p = Progression.touchStreak(p, 101)
        assertEquals(2, p.streakDays)
        assertEquals(2, Progression.visibleStreak(p, 102))
        assertEquals(0, Progression.visibleStreak(p, 103))
        p = Progression.touchStreak(p, 105)
        assertEquals(1, p.streakDays)
    }

    @Test
    fun recordKeepsRecentResults() {
        val id = "w1.times"
        var p = ClimbProgress()
        repeat(5) { p = Progression.record(p, results(id, 3, 5, 2_000), now) }
        assertEquals(20, p.of(id).recent.size)
        assertEquals(25, p.of(id).practiced)
        assertEquals(25L, p.totalSolved)
    }

    @Test
    fun formattingIsGerman() {
        assertEquals("1.225", Fmt.int(1225))
        assertEquals("12.345.678", Fmt.int(12_345_678))
        assertEquals("999", Fmt.int(999))
        assertEquals("${Fmt.MINUS}6", Fmt.int(-6))
        assertEquals("60,2", Fmt.dec(BigDecimal("60.20")))
        assertEquals("6,30 €", Fmt.money(BigDecimal("6.3")))
        assertEquals("0,07", Fmt.dec(BigDecimal("0.07")))
        assertEquals("1.234,5", Fmt.dec(BigDecimal("1234.5")))
        assertEquals("470", Fmt.dec(BigDecimal("4.7E+2")))
        assertEquals("25 R 4", Fmt.expected(Expected.Remainder(25, 4)))
    }

    @Test
    fun answerCheckUnderstandsAllInputs() {
        assertTrue(AnswerCheck.check(Expected.Exact(BigDecimal("60.2")), "60,2").correct)
        assertTrue(AnswerCheck.check(Expected.Exact(BigDecimal("60.2")), "60,20").correct)
        assertFalse(AnswerCheck.check(Expected.Exact(BigDecimal("60.2")), "602").correct)
        assertTrue(AnswerCheck.check(Expected.Exact(BigDecimal("0.5")), ",5").correct)
        assertTrue(AnswerCheck.check(Expected.Exact(BigDecimal("7")), "7,").correct)
        assertFalse(AnswerCheck.check(Expected.Exact(BigDecimal("7")), "").correct)
        assertTrue(AnswerCheck.check(Expected.Remainder(25, 4), "25R4").correct)
        assertTrue(AnswerCheck.check(Expected.Remainder(25, 0), "25").correct)
        assertFalse(AnswerCheck.check(Expected.Remainder(25, 4), "25R3").correct)
        assertTrue(AnswerCheck.check(Expected.Frac(3, 4), "6/8").correct)
        assertFalse(AnswerCheck.check(Expected.Frac(3, 4, requireReduced = true), "6/8").correct)
        assertTrue(AnswerCheck.check(Expected.Frac(3, 4, requireReduced = true), "3/4").correct)
        assertTrue(AnswerCheck.check(Expected.Frac(2, 1), "2").correct)
        assertTrue(AnswerCheck.check(Expected.Approx(BigDecimal(100), BigDecimal(5)), "104").correct)
        assertFalse(AnswerCheck.check(Expected.Approx(BigDecimal(100), BigDecimal(5)), "106").correct)
        assertTrue(AnswerCheck.check(Expected.Choice(2), "2").correct)
        assertTrue(AnswerCheck.check(Expected.Digits("0428"), "0428").correct)
        assertFalse(AnswerCheck.check(Expected.Digits("0428"), "428").correct)
    }

    @Test
    fun progressSurvivesJson() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val p = mastered("w1.times").with("w1.times") { it.copy(recent = listOf(1200, -900), rusty = true) }
        val text = json.encodeToString(ClimbProgress.serializer(), p)
        val back = json.decodeFromString(ClimbProgress.serializer(), text)
        assertEquals(p, back)
        val old = json.decodeFromString(ClimbProgress.serializer(), "{\"skills\":{\"w1.times\":{\"stars\":2,\"unknown\":5}}}")
        assertEquals(SkillProgress(stars = 2), old.of("w1.times"))
    }
}
