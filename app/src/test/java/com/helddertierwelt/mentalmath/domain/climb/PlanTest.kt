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
        val plan = DailyPlan.build(worlds, ClimbProgress(), listOf("profi"), 20, true, now, today)
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
        val plan = DailyPlan.build(worlds, p, listOf("profi"), 30, true, now, today)
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
        val p0 = ClimbProgress()
        val plan = DailyPlan.current(worlds, p0, listOf("profi"), 20, true, now, today)
        val first = plan.blocks.first().id
        val saved = p0.copy(plan = DailyPlan.markDone(plan, first))
        val same = DailyPlan.current(worlds, saved, listOf("profi"), 20, true, now, today)
        assertEquals(listOf(first), same.done)
        assertTrue(DailyPlan.current(worlds, saved, listOf("profi"), 10, true, now, today).done.isEmpty())
        assertTrue(DailyPlan.current(worlds, saved, listOf("profi"), 20, true, now + day, today + 1).done.isEmpty())
        // Neues Level gilt als erledigt, sobald eine Einheit dieses Levels fertig ist.
        val newBlock = plan.blocks.first { it.kind == DailyPlan.NEW }
        val after = DailyPlan.afterSession(plan, today, null, newBlock.skillIds.single())!!
        assertTrue(newBlock.id in after.done)
        assertEquals(plan, DailyPlan.afterSession(plan, today + 1, newBlock.id, null))
        val done = plan.blocks.fold(plan) { acc, b -> DailyPlan.markDone(acc, b.id) }
        assertTrue(done.finished)
    }

    @Test
    fun freeChoiceFollowsTheGoals() {
        val tms = DailyPlan.build(worlds, ClimbProgress(), listOf("tmsnat"), 20, false, now, today)
        val newBlock = tms.blocks.first { it.kind == DailyPlan.NEW }
        assertTrue(newBlock.skillIds.single(), newBlock.skillIds.single().startsWith("w13.") || newBlock.skillIds.single().startsWith("w14."))
        assertTrue(tms.blocks.any { it.kind == DailyPlan.APPLY && it.target != null })
        // Zwei Ziele, 30 Minuten: für beide wird angewendet.
        val both = DailyPlan.build(worlds, ClimbProgress(), listOf("profi", "tmsnat"), 30, true, now, today)
        assertEquals(2, both.blocks.count { it.kind == DailyPlan.APPLY })
        val abi = DailyPlan.build(worlds, ClimbProgress(), listOf("abi"), 30, false, now, today)
        assertTrue(abi.blocks.first { it.kind == DailyPlan.NEW }.skillIds.single().let { it.startsWith("w17.") || it.startsWith("w16.") })
    }
}
