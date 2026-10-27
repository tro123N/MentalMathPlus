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
import com.helddertierwelt.mentalmath.domain.climb.engine.Placement
import com.helddertierwelt.mentalmath.domain.climb.engine.ProblemResult
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.engine.Reminder
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionFinisher
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionLogic
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.domain.climb.engine.Stats
import com.helddertierwelt.mentalmath.domain.climb.model.Backup
import com.helddertierwelt.mentalmath.domain.climb.model.BackupCodec
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.DayStat
import com.helddertierwelt.mentalmath.domain.climb.model.Palette
import com.helddertierwelt.mentalmath.domain.climb.model.PlusSettings
import com.helddertierwelt.mentalmath.domain.climb.model.addDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Einstufungstest, Statistik, Erinnerung und Sicherung. */
class PlusFeaturesTest {

    private val worlds = Curriculum.worlds
    private val now = 1_800_000_000_000L
    private val today = now / Progression.DAY_MS

    /** Spielt einen Einstufungstest durch: [wrong] Fehler, jede Aufgabe mit [paceFactor] × Ein-Stern-Zeit. */
    private fun playPlacement(worldIndex: Int, wrong: Int, paceFactor: Double, before: ClimbProgress = ClimbProgress()) =
        run {
            val world = worlds[worldIndex]
            var s = Placement.create(world, Random(7))
            assertEquals(Placement.problemCount(world), s.total)
            var mistakes = 0
            while (s.phase != Phase.FINISHED) {
                val planned = s.current!!
                val skill = Curriculum.skill(planned.problem.skillId)!!
                assertEquals((skill.goals.timeLimit * 1000).toLong(), s.currentLimitMs)
                val ms = (skill.goals.star1 * 1000 * paceFactor).toLong()
                s = if (mistakes < wrong) {
                    mistakes++
                    SessionLogic.timeout(s, s.currentLimitMs!!).first
                } else {
                    s.copy(results = s.results + ProblemResult(planned.problem, "x", true, ms), phase = Phase.FLASH)
                }
                s = SessionLogic.advance(s)
            }
            SessionFinisher.finish(worlds, before, s, now, today)
        }

    @Test
    fun placementPassesAndSkipsTheWorld() {
        val (p, summary) = playPlacement(worldIndex = 2, wrong = 1, paceFactor = 0.8)
        val outcome = summary.placement!!
        assertEquals(SessionType.PLACEMENT, summary.type)
        assertTrue(outcome.passed)
        assertEquals(worlds[2].skills.size, outcome.newlyMastered)
        worlds[2].skills.forEach { skill ->
            val sp = p.of(skill.id)
            assertEquals(1, sp.stars)
            assertTrue(sp.placed)
            assertEquals(now + Progression.PLACEMENT_INTERVAL * Progression.DAY_MS, sp.nextReviewAt)
        }
        // Welt 3 ist damit offen, ebenso Welt 4; Welt 2 bleibt der nächste Schritt.
        assertTrue(Progression.worldUnlocked(worlds, p, 2))
        assertTrue(Progression.worldUnlocked(worlds, p, 3))
        assertTrue(Progression.skillUnlocked(worlds, p, worlds[3].skills[0].id))
        assertEquals(worlds[0].skills[0].id, Progression.nextSkill(worlds, p)?.id)
        assertEquals(1, p.streakDays)
        assertEquals(today, p.days.last().day)
    }

    @Test
    fun placementFailsWhenTooSlowOrTooManyMistakes() {
        val slow = playPlacement(worldIndex = 2, wrong = 0, paceFactor = 1.2).second.placement!!
        assertFalse(slow.passed)
        assertTrue(slow.accuracyOk)
        assertFalse(slow.paceOk)
        val sloppy = playPlacement(worldIndex = 2, wrong = 3, paceFactor = 0.5)
        assertFalse(sloppy.second.placement!!.passed)
        assertFalse(sloppy.second.placement!!.accuracyOk)
        assertFalse(sloppy.first.of(worlds[2].skills[0].id).mastered)
    }

    @Test
    fun placementKeepsBetterStars() {
        val skill = worlds[1].skills[0].id
        val before = ClimbProgress().with(skill) { it.copy(stars = 3, intervalDays = 30, nextReviewAt = now + 99) }
        val (p, summary) = playPlacement(worldIndex = 1, wrong = 0, paceFactor = 0.5, before = before)
        assertEquals(worlds[1].skills.size - 1, summary.placement!!.newlyMastered)
        assertEquals(3, p.of(skill).stars)
        assertFalse(p.of(skill).placed)
        assertEquals(now + 99, p.of(skill).nextReviewAt)
        assertEquals(listOf(worlds[0].id), Placement.candidates(worlds, p).map { it.id }.take(1))
        assertEquals(worlds[0].id, Placement.recommended(worlds, p)?.id)
    }

    @Test
    fun dayStatsMergeAndStayShort() {
        var days = emptyList<DayStat>()
        days = days.addDay(DayStat(10, 2, 1, 100), 3)
        days = days.addDay(DayStat(10, 3, 3, 50), 3)
        days = days.addDay(DayStat(12, 1, 1, 10), 3)
        days = days.addDay(DayStat(11, 4, 2, 40), 3)
        assertEquals(listOf(10L, 11L, 12L), days.map { it.day })
        assertEquals(5, days[0].solved)
        days = days.addDay(DayStat(13, 1, 0, 5), 3)
        assertEquals(listOf(11L, 12L, 13L), days.map { it.day })
        val series = Stats.lastDays(days, 14, 5)
        assertEquals(listOf(10L, 11L, 12L, 13L, 14L), series.map { it.day })
        assertEquals(listOf(0, 4, 1, 1, 0), series.map { it.solved })
    }

    @Test
    fun formRanksStrengthsAndWeaknesses() {
        val good = worlds[0].skills[0]
        val bad = worlds[0].skills[1]
        val few = worlds[0].skills[2]
        val fast = (good.goals.star3 * 1000).toLong()
        val slow = (bad.goals.star1 * 2000).toLong()
        val p = ClimbProgress()
            .with(good.id) { it.copy(stars = 3, recent = List(10) { fast }) }
            .with(bad.id) { it.copy(stars = 1, recent = List(6) { slow } + List(4) { -slow }) }
            .with(few.id) { it.copy(stars = 1, recent = listOf(1000L, 1000L)) }
        val goodForm = Stats.form(good, p.of(good.id))!!
        assertEquals(100, goodForm.score)
        val badForm = Stats.form(bad, p.of(bad.id))!!
        assertEquals(36, badForm.score)
        assertNull(Stats.form(few, p.of(few.id)))
        assertEquals(listOf(good.id), Stats.strengths(worlds, p).map { it.skill.id })
        assertEquals(listOf(bad.id), Stats.weaknesses(worlds, p).map { it.skill.id })
        val summary = Stats.worldSummary(worlds[0], p)
        assertEquals(3, summary.mastered)
        assertEquals(68, summary.form)
    }

    @Test
    fun recordingFillsHistoryButNotForGuidedTimes() {
        val skill = worlds[0].skills[0]
        val random = Random(3)
        val results = List(4) { ProblemResult(skill.generate(random), "", it != 0, 2_000) }
        var p = Progression.record(ClimbProgress(), results, now, today, skillDays = false)
        assertTrue(p.of(skill.id).recent.isEmpty())
        assertTrue(p.of(skill.id).days.isEmpty())
        assertEquals(4, p.days.single().solved)
        p = Progression.record(p, results, now, today)
        assertEquals(4, p.of(skill.id).recent.size)
        assertEquals(DayStat(today, 4, 3, 8_000), p.of(skill.id).days.single())
        assertEquals(DayStat(today, 8, 6, 16_000), p.days.single())
        p = Progression.touchStreak(p, today)
        p = Progression.touchStreak(p, today + 1)
        p = Progression.touchStreak(p, today + 3)
        assertEquals(1, p.streakDays)
        assertEquals(2, p.bestStreak)
    }

    @Test
    fun testsAreLogged() {
        val skill = worlds[0].skills[0]
        val outcome = com.helddertierwelt.mentalmath.domain.climb.engine.Outcome(10, 10, 1.0, 1500, 3)
        var p = Progression.afterTest(worlds, ClimbProgress(), skill.id, outcome, now)
        p = Progression.afterTest(worlds, p, skill.id, outcome.copy(avgMs = 1200), now + 1)
        assertEquals(listOf(1500L, 1200L), p.of(skill.id).tests.map { it.avgMs })
        assertEquals(-20, Stats.testTrendPercent(p.of(skill.id)))
    }

    @Test
    fun reminderTextFollowsTheProgress() {
        val first = worlds[0].skills[0]
        val empty = ClimbProgress()
        assertTrue(Reminder.shouldNotify(empty, today, onlyIfIdle = true))
        assertTrue(Reminder.message(worlds, empty, now, today).text.contains(first.title))
        val activeToday = empty.copy(lastActiveDay = today, streakDays = 3)
        assertFalse(Reminder.shouldNotify(activeToday, today, onlyIfIdle = true))
        assertTrue(Reminder.shouldNotify(activeToday, today, onlyIfIdle = false))
        val atRisk = empty.copy(lastActiveDay = today - 1, streakDays = 5)
            .with(first.id) { it.copy(stars = 2, nextReviewAt = now - 1, intervalDays = 3) }
        val message = Reminder.message(worlds, atRisk, now, today)
        assertEquals("Deine Serie: 5 Tage", message.title)
        assertTrue(message.text.contains("Wiederholung"))
        assertTrue(message.text.contains("Serie hält"))
        val rusty = atRisk.with(first.id) { it.copy(rusty = true) }
        assertTrue(Reminder.message(worlds, rusty, now, today).text.contains("eingerostet"))
    }

    @Test
    fun backupRoundTripAndValidation() {
        val skill = worlds[0].skills[0]
        val progress = ClimbProgress(totalSolved = 42).with(skill.id) { it.copy(stars = 2, recent = listOf(1200, -300)) }
        val settings = PlusSettings(palette = Palette.WALD.name, reminderOn = true, reminderHour = 7, reminderMinute = 5)
        val text = BackupCodec.encode(BackupCodec.create(progress, settings, now))
        val back = BackupCodec.decode(text).getOrThrow()
        assertEquals(progress, back.progress)
        assertEquals(settings, back.settings)
        assertEquals(Backup.APP, back.app)
        assertEquals("7:05", back.settings!!.reminderTime)
        assertEquals(Palette.WALD, back.settings!!.paletteValue)
        assertEquals(Palette.STANDARD, PlusSettings(palette = "GIBTSNICHT").paletteValue)

        assertTrue(BackupCodec.decode("Hallo").isFailure)
        assertTrue(BackupCodec.decode("{\"foo\": 1}").isFailure)
        val other = BackupCodec.decode(text.replace(Backup.APP, "AndereApp"))
        assertEquals(BackupCodec.NOT_A_BACKUP, other.exceptionOrNull()?.message)
        val newer = BackupCodec.decode(text.replace("\"format\": 1", "\"format\": 99"))
        assertEquals(BackupCodec.TOO_NEW, newer.exceptionOrNull()?.message)
        // Die reine Fortschrittsdatei wird ebenfalls angenommen.
        val raw = kotlinx.serialization.json.Json.encodeToString(ClimbProgress.serializer(), progress)
        val fromRaw = BackupCodec.decode(raw).getOrThrow()
        assertEquals(progress, fromRaw.progress)
        assertNotNull(fromRaw)
    }
}
