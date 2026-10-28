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
import com.helddertierwelt.mentalmath.domain.climb.engine.Adaptive
import com.helddertierwelt.mentalmath.domain.climb.engine.AdaptiveState
import com.helddertierwelt.mentalmath.domain.climb.engine.FreeMode
import com.helddertierwelt.mentalmath.domain.climb.engine.FreeRules
import com.helddertierwelt.mentalmath.domain.climb.engine.Phase
import com.helddertierwelt.mentalmath.domain.climb.engine.TrickAdvisor
import com.helddertierwelt.mentalmath.domain.climb.free.FreeTopic
import com.helddertierwelt.mentalmath.domain.climb.free.FreeTopics
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.exact
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

    private fun prompt(text: String) = Problem("free.basic", text, Prompt(text), exact(1), InputKind.INTEGER, emptyList())

    @Test
    fun trickAdvisorFindsTheRightLevel() {
        val cases = mapOf(
            "43 × 11" to "w2.times11a",
            "47 × 11" to "w2.times11b",
            "78 × 11" to "w2.times11b",
            "65²" to "w2.square5",
            "43 × 47" to "w2.sametens",
            "96 × 97" to "w6.close",
            "47 × 6" to "w4.mul21",
            "49 × 7" to "w4.roundup",
            "347 × 6" to "w4.mul31",
            "58 + 36" to "w3.add2",
            "1.000 − 367" to "w3.complement",
            "15 % von 240" to "w5.percent",
            "7 × 8" to "w1.times",
            "17³" to "w6.cube",
        )
        cases.forEach { (text, id) -> assertEquals(text, id, TrickAdvisor.guessSkill(prompt(text))) }
        // Kletterweg-Aufgaben gehören zu ihrem Level.
        val skill = worlds[5].skills[0]
        val tip = TrickAdvisor.tip(skill.generate(Random(2)), worlds)
        assertEquals(skill.id, tip.skillId)
        assertEquals(skill.method, tip.title)
        // Jede Aufgabe des freien Trainings bekommt einen Tipp mit gültigem Level oder einen allgemeinen Hinweis.
        val ids = worlds.flatMap { w -> w.skills.map { it.id } }.toSet()
        val r = Random(9)
        FreeTopic.entries.forEach { t ->
            (1..3).forEach { level ->
                repeat(30) {
                    val t2 = TrickAdvisor.tip(FreeTopics.generate(t, level, r), worlds)
                    assertTrue("${t.name}: ${t2.skillId}", t2.skillId == null || t2.skillId in ids)
                    assertTrue(t2.text.isNotBlank())
                }
            }
        }
        TrickAdvisor.run { cases.values.forEach { assertTrue(it, it in ids) } }
    }

    @Test
    fun adaptiveDifficultyGoesUpAndDown() {
        var st = AdaptiveState(1)
        repeat(Adaptive.UP_AFTER) { st = Adaptive.update(st, true, 1_000, "free.basic") }
        assertEquals(2, st.level)
        st = Adaptive.update(st, true, 60_000, "free.basic")
        assertEquals(0, st.streak)
        repeat(Adaptive.DOWN_AFTER) { st = Adaptive.update(st, false, 1_000, "free.basic") }
        assertEquals(1, st.level)
        repeat(10) { st = Adaptive.update(st, false, 1_000, "free.basic") }
        assertEquals(1, st.level)
        repeat(20) { st = Adaptive.update(st, true, 1_000, "free.basic") }
        assertEquals(3, st.level)

        // In der Einheit: Nach dem Stufenwechsel werden die kommenden Aufgaben neu erzeugt.
        val r = Random(4)
        val gen = { level: Int, count: Int -> List(count) { FreeTopics.generate(FreeTopic.BASIC, level, r) } }
        var s = SessionLogic.createFree(gen(1, 20), FreeRules(FreeMode.ENDLOS, 0, 0), "x", null).copy(adaptive = AdaptiveState(1))
        var regenerated = 0
        repeat(Adaptive.UP_AFTER) {
            val p = s.current!!.problem
            s = s.copy(results = s.results + ProblemResult(p, "", true, 500), phase = Phase.FLASH)
            s = SessionLogic.adapt(s) { level, count -> regenerated = count; assertEquals(2, level); gen(level, count) }
            s = SessionLogic.advance(s)
        }
        assertEquals(2, s.adaptive!!.level)
        assertEquals(20 - Adaptive.UP_AFTER, regenerated)
        assertEquals(20, s.total)
    }

    @Test
    fun apSimulationSkipsTimesOutAndRecords() {
        val ap = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
        val problems = ap.simulation(16, 5, Random(8))
        val rules = FreeRules(FreeMode.SIMULATION, 1500, 5, FreeRules.TRACK_AP)
        var s = SessionLogic.createFree(problems, rules, "TMSnat", null)
        assertTrue(s.isSimulation)
        val first = s.current!!.problem
        s = SessionLogic.skip(s, 30_000)
        assertEquals(30_000, s.usedMs)
        assertEquals(first, s.problems.last().problem)
        assertEquals(16, SessionLogic.open(s))
        // Drei Aufgaben richtig beantworten.
        repeat(3) {
            val c = (s.expected as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Choice).index
            s = SessionLogic.choose(s, c, 60_000).first
            assertEquals(Phase.FLASH, s.phase)
            s = SessionLogic.advance(s)
        }
        assertEquals(13, SessionLogic.open(s))
        assertFalse(SessionLogic.freeOver(s))
        assertTrue(SessionLogic.freeOver(s, 1_500_000))
        val done = SessionLogic.finishNow(s)
        assertEquals(16, done.results.size)
        assertEquals(3, done.results.count { it.correct })
        assertEquals(13, done.results.count { it.timedOut })
        val (p, summary) = SessionFinisher.finish(worlds, ClimbProgress(), done, now, today)
        assertEquals(3, summary.freeScore)
        val run = summary.apRun!!
        assertEquals(3, run.score)
        assertEquals(16, run.total)
        assertEquals(5, run.options)
        assertEquals(listOf(run), p.apRuns)
        assertEquals(3L, p.totalSolved)
        assertEquals(3, p.freeBest["ap_SIMULATION_1500_5"])
        // Die beantworteten Aufgaben zählen für die Kategorien (Levels von Welt 13).
        val answeredSkills = done.results.filter { !it.timedOut }.map { it.problem.skillId }
        answeredSkills.forEach { assertTrue(it, p.of(it).recent.isNotEmpty()) }
        assertTrue(answeredSkills.all { it.startsWith("w13.") })
    }

    @Test
    fun apPracticeShowsSolutionAfterMistake() {
        val ap = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
        val problems = List(20) { ap.mixed(5, 2, Random(40 + it)) }
        val rules = FreeRules(FreeMode.ENDLOS, 0, 2, FreeRules.TRACK_AP)
        var s = SessionLogic.createFree(problems, rules, "Gemischt üben", null)
        assertTrue(s.showsSolution)
        assertFalse(s.isExam)
        // Falsch: Lösungsweg zeigen, keine Wiederholung derselben Aufgabe einplanen.
        val c = (s.expected as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Choice).index
        s = SessionLogic.choose(s, (c + 1) % s.problem!!.choices.size, 20_000).first
        assertEquals(Phase.REVEAL, s.phase)
        assertEquals(20, s.problems.size)
        s = SessionLogic.advance(s)
        assertEquals(Phase.ANSWERING, s.phase)
        assertEquals(1, s.index)
        // Richtig: kurze Rückmeldung wie bisher.
        val c2 = (s.expected as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Choice).index
        s = SessionLogic.choose(s, c2, 20_000).first
        assertEquals(Phase.FLASH, s.phase)
        // Testsimulation und normales freies Training bleiben ohne Lösungsweg.
        val sim = SessionLogic.createFree(problems, FreeRules(FreeMode.SIMULATION, 1500, 5, FreeRules.TRACK_AP), "TMSnat-Simulation", null)
        assertTrue(sim.isExam)
        assertFalse(sim.showsSolution)
        val free = SessionLogic.createFree(problems, FreeRules(FreeMode.ENDLOS, 0, 2), "Frei", null)
        assertTrue(free.isExam)
        assertEquals("TMSnat-Simulation", ap.simulationName(16, 1500))
        assertEquals("Kurztest", ap.simulationName(8, 750))
        assertEquals("Tempo-Test", ap.simulationName(16, 1200))
    }

    @Test
    fun typicalMistakesAreExplained() {
        val ap = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
        val r = Random(11)
        // Eine Aufgabe suchen, bei der eine falsche Antwort einen erklärten typischen Fehler hat.
        val problem = generateSequence { ap.mixed(5, 2, r) }.first { it.mistakes.isNotEmpty() }
        val mistake = problem.mistakes.first()
        val index = (mistake.answer as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Choice).index
        val rules = FreeRules(FreeMode.ENDLOS, 0, 2, FreeRules.TRACK_AP)
        var s = SessionLogic.createFree(listOf(problem), rules, "Gemischt üben", null)
        s = SessionLogic.choose(s, index, 30_000).first
        assertEquals(Phase.REVEAL, s.phase)
        assertEquals(SessionLogic.MISTAKE_PREFIX + mistake.why, s.feedback?.note)
        assertEquals(mistake.why, s.results.single().note)
        // Beim Eintippen (Level) wird derselbe Fehler erkannt.
        val typed = generateSequence { ap.typed(ap.task(com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory.UNITS, 2, r)) }
            .first { p -> p.mistakes.any { it.answer is com.helddertierwelt.mentalmath.domain.climb.model.Expected.Exact } }
        val m = typed.mistakes.first { it.answer is com.helddertierwelt.mentalmath.domain.climb.model.Expected.Exact }
        val text = (m.answer as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Exact).value.stripTrailingZeros().toPlainString().replace('.', ',')
        assertEquals(m.why, typed.mistakeFor(text))
        assertNull(typed.mistakeFor(""))
    }

    @Test
    fun everyTaskTypeCanBeRegenerated() {
        val ap = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
        val r = Random(21)
        // Jeder Generator liefert einen bekannten Typ …
        com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory.entries.forEach { c ->
            for (d in 1..3) repeat(40) {
                val type = ap.typeOf(ap.typed(ap.task(c, d, r)))!!
                assertTrue("Unbekannter Aufgabentyp $type", type in ap.types)
            }
        }
        // … und jeder Typ lässt sich mit neuen Zahlen wieder erzeugen.
        ap.types.forEach { type ->
            val t = ap.taskOfType(type, r)
            assertNotNull("Typ $type nicht erzeugbar", t)
            assertEquals(type, ap.typeOf(ap.typed(t!!)))
        }
        // Auch Aufgaben des gemischten Levels (Level-ID statt Kategorie) haben einen Typ.
        val mixed = ap.mixed(5, 2, r).copy(skillId = "w13.mixed")
        assertNotNull(ap.typeOf(mixed))
    }

    @Test
    fun apReviewBringsMistakesBack() {
        val ap = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
        val review = com.helddertierwelt.mentalmath.domain.climb.engine.ApReview
        val r = Random(4)
        val problem = ap.choice(ap.task(com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory.PERCENT, 2, r), 5, r)
        val type = ap.typeOf(problem)!!
        val day = Progression.DAY_MS
        fun result(correct: Boolean) = ProblemResult(problem, if (correct) "A" else "B", correct, 40_000)
        var p = review.update(ClimbProgress(), listOf(result(false)), now)
        assertEquals(listOf(type), review.due(p, now).map { it.type })
        assertEquals(0, p.apMistakes.single().stage)
        // Nicht bearbeitet zählt nicht; andere Aufgaben auch nicht.
        assertEquals(p, review.update(p, listOf(ProblemResult(problem, "", false, 0, timedOut = true)), now))
        // Fehlertraining: eine neue Aufgabe desselben Typs.
        val again = review.problems(p, now, r)
        assertEquals(listOf(type), again.map { ap.typeOf(it) })
        // Richtig → morgen wieder, dann nach 3 und 7 Tagen, danach gelernt.
        p = review.update(p, listOf(result(true)), now)
        assertTrue(review.due(p, now).isEmpty())
        assertEquals(now + day, p.apMistakes.single().due)
        p = review.update(p, listOf(result(true)), now + day)
        assertEquals(now + 4 * day, p.apMistakes.single().due)
        // Noch nicht fällig: eine richtige Antwort ändert nichts.
        assertEquals(p, review.update(p, listOf(result(true)), now + 2 * day))
        p = review.update(p, listOf(result(true)), now + 4 * day)
        assertEquals(3, p.apMistakes.single().stage)
        // Wieder falsch → zurück auf Anfang.
        val back = review.update(p, listOf(result(false)), now + 5 * day)
        assertEquals(0, back.apMistakes.single().stage)
        assertEquals(2, back.apMistakes.single().misses)
        p = review.update(p, listOf(result(true)), now + 11 * day)
        assertTrue("Nach vier richtigen Wiederholungen gelernt", p.apMistakes.isEmpty())
    }

    @Test
    fun apForecastFollowsTheResults() {
        val forecast = com.helddertierwelt.mentalmath.domain.climb.engine.ApForecast
        assertTrue(forecast.forecast(ClimbProgress()) is com.helddertierwelt.mentalmath.domain.climb.engine.ApForecast.Outcome.NeedMore)
        fun progress(sample: (Int) -> Long): ClimbProgress =
            com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory.entries.fold(ClimbProgress()) { acc, c ->
                acc.with(c.skillId) { it.copy(recent = List(10) { i -> sample(i) }) }
            }
        // Alles richtig und schnell: volle Punktzahl, die Zeit reicht immer.
        val perfect = (forecast.forecast(progress { 40_000L }, runs = 2_000) as com.helddertierwelt.mentalmath.domain.climb.engine.ApForecast.Outcome.Ready).forecast
        assertEquals(16, perfect.median)
        assertEquals(0.0, perfect.timeShort, 1e-9)
        assertNull(perfect.weakest)
        // Alles richtig, aber 150 s pro Aufgabe: nur 10 Aufgaben in 25 Minuten, der Rest wird geraten.
        val slow = (forecast.forecast(progress { 150_000L }, runs = 2_000) as com.helddertierwelt.mentalmath.domain.climb.engine.ApForecast.Outcome.Ready).forecast
        assertTrue("Prognose ${slow.median}", slow.median in 10..13)
        assertEquals(1.0, slow.timeShort, 1e-9)
        // Jede zweite falsch: etwa die Hälfte.
        val half = (forecast.forecast(progress { i -> if (i % 2 == 0) 60_000L else -60_000L }, runs = 2_000) as com.helddertierwelt.mentalmath.domain.climb.engine.ApForecast.Outcome.Ready).forecast
        assertTrue("Prognose ${half.median}", half.median in 6..10)
        assertTrue(half.low <= half.median && half.median <= half.high)
        assertEquals(100.0, half.distribution.sum(), 1e-6)
    }

    @Test
    fun apTimeAnalysisSplitsTheTime() {
        val ap = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
        val problems = ap.simulation(4, 5, Random(2))
        val results = listOf(
            ProblemResult(problems[0], "A", true, 60_000, number = 1),
            ProblemResult(problems[2], "B", false, 100_000, number = 3, extraMs = 200_000),
            ProblemResult(problems[1], "C", true, 80_000, number = 2),
            ProblemResult(problems[3], "", false, 0, timedOut = true, number = 4, extraMs = 30_000),
        )
        val report = com.helddertierwelt.mentalmath.domain.climb.engine.ApTime.analyze(results, 400)
        assertEquals(listOf(1, 2, 3, 4), report.tasks.map { it.number })
        assertEquals(100_000L, report.targetMs)
        assertEquals(140_000L, report.firstHalfMs)
        assertEquals(330_000L, report.secondHalfMs)
        assertEquals(listOf(3), report.slow.map { it.number })
        assertEquals(300_000L, report.wrongMs)
        assertEquals(1, report.wrongCount)
        assertEquals(1, report.unanswered)
        assertEquals(440_000L / 3, report.avgAnsweredMs)
    }

    @Test
    fun skippedTimeStaysWithTheTask() {
        val ap = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
        val rules = FreeRules(FreeMode.SIMULATION, 1500, 5, FreeRules.TRACK_AP)
        var s = SessionLogic.createFree(ap.simulation(3, 5, Random(9)), rules, "Kurztest", null)
        assertEquals(listOf(1, 2, 3), s.problems.map { it.number })
        s = SessionLogic.skip(s, 50_000)
        assertEquals(listOf(2, 3, 1), s.problems.map { it.number })
        assertEquals(50_000L, s.problems.last().spentMs)
        val c = (s.expected as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Choice).index
        s = SessionLogic.advance(SessionLogic.choose(s, c, 20_000).first)
        // Zeit läuft während Aufgabe 3 ab: Aufgabe 3 bekommt die laufende Zeit, Aufgabe 1 ihre frühere Zeit.
        val done = SessionLogic.finishNow(s, 10_000)
        val byNumber = done.results.associateBy { it.number }
        assertEquals(20_000L, byNumber.getValue(2).totalMs)
        assertEquals(10_000L, byNumber.getValue(3).totalMs)
        assertEquals(50_000L, byNumber.getValue(1).totalMs)
        assertEquals(80_000L, done.usedMs)
    }

    @Test
    fun apAnswerPositionsAreEven() {
        // Die richtige Antwort steht etwa gleich oft bei A, B, C, D und E – kein Muster zum Raten.
        val r = Random(3)
        val counts = IntArray(5)
        repeat(100) {
            com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks.simulation(16, 5, r).forEach {
                counts[(it.expected as com.helddertierwelt.mentalmath.domain.climb.model.Expected.Choice).index]++
            }
        }
        val total = counts.sum()
        counts.forEachIndexed { i, c ->
            assertTrue("Antwort ${"ABCDE"[i]}: $c von $total", c * 100 >= total * 14 && c * 100 <= total * 26)
        }
    }

    @Test
    fun independentWorldIsAlwaysOpen() {
        val w13 = worlds.indexOfFirst { it.independent }
        assertTrue(w13 >= 0)
        val empty = ClimbProgress()
        assertTrue(Progression.worldUnlocked(worlds, empty, w13))
        worlds[w13].skills.forEach { assertTrue(it.id, Progression.skillUnlocked(worlds, empty, it.id)) }
        // Der Hauptweg bleibt beim ersten Level.
        assertEquals(worlds[0].skills[0].id, Progression.nextSkill(worlds, empty)?.id)
        // Auch wenn alle Hauptwelten geschafft sind, schickt „Weiterklettern“ nicht in die Zusatzwelt.
        val allMain = worlds.filter { !it.independent }.flatMap { it.skills }
            .fold(ClimbProgress()) { acc, sk -> acc.with(sk.id) { it.copy(stars = 1) } }
        assertNull(Progression.nextSkill(worlds, allMain))
        assertTrue(Placement.candidates(worlds, empty).none { it.independent })
    }
}
