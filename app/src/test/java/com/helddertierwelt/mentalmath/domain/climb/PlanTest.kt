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
import com.helddertierwelt.mentalmath.domain.climb.engine.DailyPlan
import com.helddertierwelt.mentalmath.domain.climb.engine.Drills
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionLogic
import com.helddertierwelt.mentalmath.domain.climb.engine.Memory
import com.helddertierwelt.mentalmath.domain.climb.engine.ProblemResult
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.SkillProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlanTest {
    private fun planOf(p: ClimbProgress, goals: List<String>, minutes: Int, follow: Boolean) =
        DailyPlan.newPlan(worlds, p, "t", "Test", goals.flatMap { DailyPlan.preset(worlds, it) }, now / Progression.DAY_MS - 1).copy(minutes = minutes, followPath = follow)

    private fun buildFor(p: ClimbProgress, goals: List<String>, minutes: Int, follow: Boolean) =
        DailyPlan.build(worlds, p, listOf(planOf(p, goals, minutes, follow)), now, now / Progression.DAY_MS)

    private val worlds = Curriculum.worlds
    private val now = 1_800_000_000_000L
    private val day = Progression.DAY_MS
    private val today = now / day

    private fun results(skillId: String, correct: Int, total: Int, ms: Long, note: String? = null): List<ProblemResult> {
        val skill = Curriculum.skill(skillId)!!
        val random = Random(1)
        return (0 until total).map { i ->
            ProblemResult(problem = skill.generate(random), answer = "1", correct = i < correct, timeMs = ms, note = if (i < correct) null else note)
        }
    }

    @Test
    fun recallFallsWithTimeAndOldProgressIsMigrated() {
        val sp = SkillProgress(stars = 1, stability = 10.0, memoryAt = now)
        assertEquals(1.0, Memory.recall(sp, now)!!, 1e-9)
        assertEquals(0.9, Memory.recall(sp, now + 10 * day)!!, 1e-9)
        assertNull(Memory.recall(SkillProgress(), now))
        // Alter Stand ohne Stabilität: aus dem Wiederholungsabstand geschätzt, beim Termin ≈ 85 %.
        val old = SkillProgress(stars = 1, intervalDays = 7, nextReviewAt = now + 7 * day)
        assertEquals(Memory.TARGET, Memory.recall(old, now + 7 * day)!!, 0.01)
    }

    @Test
    fun stabilityGrowsAfterAPauseAndShrinksAfterMistakes() {
        val id = "w1.times"
        val skill = Curriculum.skill(id)!!
        val fastMs = (skill.goals.star1 * 500).toLong()
        val base = ClimbProgress().with(id) { it.copy(stars = 1, stability = 5.0, memoryAt = now - 10 * day) }
        val good = Memory.update(worlds, base, results(id, 6, 6, fastMs), now)
        assertTrue(good.of(id).stability > 10.0)
        assertTrue(good.of(id).nextReviewAt > now + 10 * day)
        // Gleich danach noch einmal: kaum Zuwachs.
        val again = Memory.update(worlds, good, results(id, 6, 6, fastMs), now + 1000)
        assertTrue(again.of(id).stability < good.of(id).stability * 1.01)
        val bad = Memory.update(worlds, base, results(id, 1, 6, fastMs, note = "Stelle vergessen"), now)
        assertTrue(bad.of(id).stability < 5.0)
        assertEquals(5, bad.errors.size)
        assertEquals("Stelle vergessen", bad.errors.first().note)
        // Geführtes Üben: nur Fehler, keine Stabilität.
        val guided = Memory.update(worlds, base, results(id, 1, 6, fastMs), now, memory = false)
        assertEquals(5.0, guided.of(id).stability, 1e-9)
    }

    @Test
    fun errorLogIsCappedAndPatternsAreFound() {
        var p = ClimbProgress()
        repeat(80) { p = Memory.update(worlds, p, results("w1.times", 0, 5, 3000, note = "Übertrag"), now) }
        assertEquals(Memory.ERRORS_KEPT, p.errors.size)
        val pattern = Memory.patterns(p, now).first()
        assertEquals("w1.times", pattern.skillId)
        assertEquals("Übertrag", pattern.note)
    }

    @Test
    fun freshPlanStartsWithTheFirstLevel() {
        val plan = buildFor(ClimbProgress(), listOf("profi"), 20, true)
        val first = plan.blocks.first()
        assertEquals(DailyPlan.NEW, first.kind)
        assertEquals(worlds.first().skills.first().id, first.skillIds.single())
        assertNotNull(plan.next)
    }

    @Test
    fun planWarmsUpDueLevelsAndTargetsWeaknesses() {
        val ids = worlds.first().skills.map { it.id }
        var p = ids.fold(ClimbProgress()) { acc, id ->
            acc.with(id) { it.copy(stars = 2, practiced = 10, stability = 3.0, memoryAt = now - 20 * day, recent = List(10) { 3000L }) }
        }
        val weakId = ids[1]
        p = Memory.update(worlds, p, results(weakId, 0, 4, 3000, note = "Vorzeichen"), now - day).let { q ->
            Memory.update(worlds, q, results(weakId, 0, 4, 3000, note = "Vorzeichen"), now - day)
        }
        val plan = buildFor(p, listOf("profi"), 30, true)
        val kinds = plan.blocks.map { it.kind }
        assertTrue(kinds.toString(), DailyPlan.WARMUP in kinds)
        val weak = plan.blocks.first { it.kind == DailyPlan.WEAK }
        assertEquals(weakId, weak.skillIds.single())
        assertTrue(weak.reason, weak.reason.contains("Vorzeichen"))
        assertTrue(plan.blocks.map { it.id }.toSet().size == plan.blocks.size)
        // Die Schwachstelle steckt nicht schon im Aufwärmen.
        assertTrue(weakId !in plan.blocks.first { it.kind == DailyPlan.WARMUP }.skillIds)
    }

    @Test
    fun planIsKeptForTheDayAndRebuiltOnChanges() {
        val plan0 = planOf(ClimbProgress(), listOf("profi"), 20, true)
        val p0 = ClimbProgress(plans = listOf(plan0))
        val plan = DailyPlan.current(worlds, p0, now, today)
        val first = plan.blocks.first().id
        val saved = p0.copy(plan = DailyPlan.markDone(plan, first))
        assertEquals(listOf(first), DailyPlan.current(worlds, saved, now, today).done)
        // Geänderte Minuten: neu geplant, Erledigtes bleibt abgehakt.
        val changed = saved.copy(plans = listOf(plan0.copy(minutes = 10)))
        assertEquals(listOf(first), DailyPlan.current(worlds, changed, now, today).done)
        assertTrue(DailyPlan.current(worlds, saved, now + day, today + 1).done.isEmpty())
        // Neues Level gilt als erledigt, sobald eine Einheit dieses Levels fertig ist.
        val newBlock = plan.blocks.first { it.kind == DailyPlan.NEW }
        val after = DailyPlan.afterSession(plan, today, null, newBlock.skillIds.single())!!
        assertTrue(newBlock.id in after.done)
        assertEquals(plan, DailyPlan.afterSession(plan, today + 1, newBlock.id, null))
        val done = plan.blocks.fold(plan) { acc, b -> DailyPlan.markDone(acc, b.id) }
        assertTrue(done.finished)
    }

    @Test
    fun freeChoiceFollowsTheSelection() {
        val tms = buildFor(ClimbProgress(), listOf("tmsnat"), 20, false)
        val ids = tms.blocks.filter { it.kind == DailyPlan.NEW }.map { it.skillIds.single() }
        assertTrue(ids.toString(), ids.isNotEmpty() && ids.all { it.startsWith("w13.") || it.startsWith("w14.") })
        val abi = buildFor(ClimbProgress(), listOf("abi"), 30, false)
        assertTrue(abi.blocks.first { it.kind == DailyPlan.NEW }.skillIds.single().let { it.startsWith("w17.") || it.startsWith("w16.") })
    }

    @Test
    fun severalPlansShareTheDayAndRespectWeekdays() {
        val p = ClimbProgress()
        val a = DailyPlan.newPlan(worlds, p, "a", "Kopfrechnen", DailyPlan.preset(worlds, "profi"), today - 3)
        val b = DailyPlan.newPlan(worlds, p, "b", "TMSnat", DailyPlan.preset(worlds, "tmsnat"), today - 3).copy(minutes = 10)
        val both = DailyPlan.build(worlds, p, listOf(a, b), now, today)
        assertEquals(30, both.minutes)
        assertTrue(both.blocks.any { it.planId == "a" && it.kind == DailyPlan.NEW })
        assertTrue(both.blocks.any { it.planId == "b" && it.kind == DailyPlan.NEW })
        // Plan b nicht an diesem Wochentag, Plan a pausiert → nichts zu tun.
        val wd = DailyPlan.weekday(today)
        val none = DailyPlan.build(worlds, p, listOf(a.copy(paused = true), b.copy(weekdays = (1..7).filter { it != wd })), now, today)
        assertTrue(none.blocks.isEmpty())
        assertEquals(4, DailyPlan.weekday(0)) // 1.1.1970 war ein Donnerstag
        // Ende vorbei → nicht mehr aktiv.
        assertTrue(DailyPlan.active(listOf(a.copy(endDay = today - 1)), today).isEmpty())
    }

    @Test
    fun phasesFollowTheEndDate() {
        val p = ClimbProgress()
        val tms = DailyPlan.newPlan(worlds, p, "t", "TMSnat", DailyPlan.preset(worlds, "tmsnat"), today - 90).copy(endDay = today + 2)
        assertEquals(DailyPlan.Phase.FINAL, DailyPlan.phase(tms, p, today))
        val final = DailyPlan.build(worlds, p, listOf(tms), now, today)
        assertTrue(final.blocks.none { it.kind == DailyPlan.NEW })
        assertEquals(DailyPlan.TARGET_SIM, final.blocks.first { it.kind == DailyPlan.APPLY }.target)
        val early = tms.copy(startDay = today, endDay = today + 100)
        assertEquals(DailyPlan.Phase.BUILD, DailyPlan.phase(early, p, today))
        assertEquals(DailyPlan.Phase.CONSOLIDATE, DailyPlan.phase(early, p, today + 80))
        // Viele Levels bis zum Enddatum: zwei neue pro Tag.
        val tight = tms.copy(startDay = today, endDay = today + 14)
        val status = DailyPlan.status(tight, p, today)
        assertTrue(status.perDay!! > 1.2)
        assertEquals(2, DailyPlan.build(worlds, p, listOf(tight), now, today).blocks.count { it.kind == DailyPlan.NEW })
    }

    @Test
    fun examDateBringsLevelsBackInTime() {
        // Level gut gelernt (heute noch 92 %), würde bis zum Enddatum in 30 Tagen aber unter 90 % fallen.
        val id = worlds.first().skills.first().id
        val p = ClimbProgress().with(id) { it.copy(stars = 2, practiced = 10, stability = 20.0, memoryAt = now - 5 * day, recent = List(10) { 2000L }) }
        val open = DailyPlan.newPlan(worlds, p, "o", "Offen", listOf(id), today - 10)
        assertTrue(DailyPlan.build(worlds, p, listOf(open), now, today).blocks.none { it.kind == DailyPlan.WARMUP })
        val exam = open.copy(endDay = today + 30)
        val warm = DailyPlan.build(worlds, p, listOf(exam), now, today).blocks.first { it.kind == DailyPlan.WARMUP }
        assertEquals(listOf(id), warm.skillIds)
    }

    @Test
    fun accuracyRuleAdjustsTheAmountOfNewLevels() {
        val skills = worlds.first().skills
        fun withRecent(ok: Int) = skills.take(3).fold(ClimbProgress(unlockAll = true)) { acc, sk ->
            acc.with(sk.id) { it.copy(stars = 1, practiced = 10, stability = 30.0, memoryAt = now, recent = List(10) { i -> if (i < ok) 2000L else -2000L }) }
        }
        fun news(p: ClimbProgress) = DailyPlan.build(worlds, p, listOf(DailyPlan.newPlan(worlds, p, "x", "x", skills.map { it.id }, today).copy(minutes = 30)), now, today)
            .blocks.count { it.kind == DailyPlan.NEW }
        assertEquals(2, news(withRecent(10)))
        assertEquals(1, news(withRecent(8)))
        assertEquals(0, news(withRecent(6)))
    }

    @Test
    fun oldSinglePlanIsMigrated() {
        val m = DailyPlan.migrate(worlds, ClimbProgress(), true, listOf("tmsnat"), 30, false, today)
        assertEquals(1, m.plans.size)
        assertEquals(30, m.plans.single().minutes)
        assertTrue(m.plans.single().skillIds.all { it.startsWith("w13.") || it.startsWith("w14.") })
        assertTrue(DailyPlan.migrate(worlds, ClimbProgress(), false, listOf("profi"), 20, true, today).let { it.plansMigrated && it.plans.isEmpty() })
    }

    private fun masteredPath(n: Int): ClimbProgress {
        val skills = worlds.first().skills.take(n)
        return skills.fold(ClimbProgress()) { acc, sk ->
            acc.with(sk.id) { it.copy(stars = 1, practiced = 10, stability = 30.0, memoryAt = now, recent = List(10) { 2000L }) }
        }
    }

    @Test
    fun checkTestsThePrerequisitesAndMarksWeakOnes() {
        val skills = worlds.first().skills
        val target = skills[3]
        var p = masteredPath(3).with(target.id) { it.copy(failStreak = 1, testsTaken = 1) }
        assertTrue(Drills.checkUseful(worlds, p, target.id))
        val session = Drills.check(worlds, p, target.id, Random(3))!!
        assertEquals(Drills.CHECK_SIZE, session.problems.size)
        val ids = session.problems.map { it.problem.skillId }.toSet()
        assertEquals(setOf(skills[2].id, skills[1].id), ids)
        // Alle Aufgaben der direkten Grundlage falsch.
        val results = session.problems.map { ProblemResult(it.problem, "1", correct = it.problem.skillId != skills[2].id, timeMs = 2000) }
        val (after, outcome) = Drills.afterCheck(worlds, p, session.copy(results = results))
        assertEquals(listOf(skills[2].id), outcome!!.weak.map { it.id })
        assertTrue(after.of(skills[2].id).refreshPending)
        assertTrue(!after.of(skills[1].id).refreshPending)
        // Der Plan bekommt einen Grundlagen-Check und danach das Auffrischen.
        val plan = buildFor(p, listOf("profi"), 20, true)
        val check = plan.blocks.first { it.kind == DailyPlan.CHECK }
        assertEquals(target.id, check.skillIds.single())
        val withFollowUp = DailyPlan.afterCheck(plan, today, outcome)!!
        val idx = withFollowUp.blocks.indexOfFirst { it.kind == DailyPlan.CHECK }
        assertEquals(skills[2].id, withFollowUp.blocks[idx + 1].skillIds.single())
        p = after
        assertTrue(Progression.blockers(worlds, p, target.id).isNotEmpty())
    }

    @Test
    fun patternDrillFindsMatchingProblems() {
        val random = Random(11)
        val (skill, mistake) = worlds.flatMap { it.skills }.firstNotNullOf { sk ->
            sk.generate(random).mistakes.firstOrNull()?.let { sk to it }
        }
        val note = SessionLogic.MISTAKE_PREFIX + mistake.why
        val problems = Drills.patternProblems(skill, note, Random(5))
        assertNotNull("${skill.id}: keine Aufgaben zum Fehlermuster", problems)
        assertTrue(problems!!.all { Drills.matches(it, note) })
        // Im Plan: Schwachstelle mit Fehlermuster wird zum Fehlermuster-Training.
        var p = ClimbProgress().with(skill.id) { it.copy(practiced = 10, recent = List(10) { 3000L }) }
        repeat(3) { p = Memory.update(worlds, p, listOf(ProblemResult(skill.generate(random), "1", false, 3000, note = note)), now - day) }
        val plan = buildFor(p, listOf("profi", "tmsnat", "abi"), 30, true)
        val block = plan.blocks.first { it.kind == DailyPlan.WEAK }
        assertEquals(DailyPlan.TARGET_PATTERN, block.target)
        assertEquals(note, block.note)
        assertTrue(block.reason, !block.reason.contains(SessionLogic.MISTAKE_PREFIX))
    }

    @Test
    fun speedTrainingUsesATightTimeLimit() {
        val skill = worlds.first().skills.first()
        val slow = (skill.goals.star2 * 1500).toLong()
        val p = ClimbProgress().with(skill.id) { it.copy(stars = 1, recent = List(10) { slow }) }
        assertTrue(Drills.speedUseful(skill, p))
        assertTrue(!Drills.speedUseful(skill, ClimbProgress().with(skill.id) { it.copy(stars = 1, recent = List(10) { 500L }) }))
        val s = Drills.speedSession(skill, Random(1))
        assertEquals((skill.goals.star2 * Drills.SPEED_FACTOR * 1000).toLong(), s.timeLimitMs)
        val mix = Drills.recentlyLearned(worlds, masteredPath(4))
        assertEquals(4, mix.size)
    }

    @Test
    fun patternDrillWorksForMostLevelsWithTypicalMistakes() {
        var with = 0
        var ok = 0
        val missing = mutableListOf<String>()
        for (sk in worlds.flatMap { it.skills }) {
            val r = Random(sk.id.hashCode())
            val m = (0 until 20).firstNotNullOfOrNull { sk.generate(r).mistakes.firstOrNull() } ?: continue
            with++
            if (Drills.patternProblems(sk, SessionLogic.MISTAKE_PREFIX + m.why, Random(1)) != null) ok++ else missing += sk.id
        }
        println("Fehlermuster-Training: $ok von $with Levels mit typischen Fehlern; ohne: ${missing.take(15)}")
        assertTrue(ok >= with * 0.8)
    }
}
