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

package com.helddertierwelt.mentalmath.domain.climb.engine

import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.DayStat
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.TestLog
import com.helddertierwelt.mentalmath.domain.climb.model.World
import com.helddertierwelt.mentalmath.domain.climb.model.addDay
import kotlin.math.max
import kotlin.math.min

/**
 * Regeln des Kletterwegs: Freischalten, Zurückschicken zur Grundlage,
 * Wiederholungsabstände und Einrosten.
 */
object Progression {

    /** Wiederholungsabstände in Tagen. */
    val INTERVALS = listOf(1, 3, 7, 14, 30, 60, 120)
    const val DAY_MS = 86_400_000L

    /** So viele nicht bestandene Prüfungen in Folge schicken zur Grundlage zurück. */
    const val FAILS_BEFORE_REFRESH = 2

    /** Länge einer Auffrischungsprüfung. */
    const val REFRESH_SIZE = 6

    /** Aufgaben pro Level in der Wiederholung. */
    const val REVIEW_PER_SKILL = 3

    /** Höchstens so viele Levels in einer Wiederholung. */
    const val REVIEW_MAX_SKILLS = 5

    data class Position(val world: Int, val skill: Int)

    fun locate(worlds: List<World>, skillId: String): Position? {
        worlds.forEachIndexed { wi, w ->
            val si = w.skills.indexOfFirst { it.id == skillId }
            if (si >= 0) return Position(wi, si)
        }
        return null
    }

    fun skill(worlds: List<World>, skillId: String): Skill? =
        locate(worlds, skillId)?.let { worlds[it.world].skills[it.skill] }

    /**
     * Eine Welt ist offen, wenn die Welt davor geschafft ist – oder wenn in ihr schon ein Level
     * geschafft wurde (z. B. über einen Einstufungstest).
     */
    fun worldUnlocked(worlds: List<World>, progress: ClimbProgress, worldIndex: Int): Boolean =
        worldIndex <= 0 ||
            worlds.getOrNull(worldIndex)?.independent == true ||
            worlds[worldIndex - 1].skills.all { progress.of(it.id).mastered } ||
            worlds.getOrNull(worldIndex)?.skills?.any { progress.of(it.id).mastered } == true

    fun skillUnlocked(worlds: List<World>, progress: ClimbProgress, skillId: String): Boolean {
        val pos = locate(worlds, skillId) ?: return false
        if (worlds[pos.world].independent) return true
        if (!worldUnlocked(worlds, progress, pos.world)) return false
        if (pos.skill == 0) return true
        return progress.of(worlds[pos.world].skills[pos.skill - 1].id).mastered
    }

    /** Das Level, auf dem dieses Level direkt aufbaut. */
    fun prerequisite(worlds: List<World>, skillId: String): Skill? {
        val pos = locate(worlds, skillId) ?: return null
        if (worlds[pos.world].independent) return null
        return when {
            pos.skill > 0 -> worlds[pos.world].skills[pos.skill - 1]
            pos.world > 0 -> worlds[pos.world - 1].skills.lastOrNull()
            else -> null
        }
    }

    /** Levels, die erst aufgefrischt werden müssen, bevor die Prüfung dieses Levels möglich ist. */
    fun blockers(worlds: List<World>, progress: ClimbProgress, skillId: String): List<Skill> {
        val pos = locate(worlds, skillId) ?: return emptyList()
        if (worlds[pos.world].independent) return emptyList()
        val earlier = worlds[pos.world].skills.take(pos.skill).toMutableList()
        if (pos.skill == 0) prerequisite(worlds, skillId)?.let { earlier += it }
        return earlier.filter { progress.of(it.id).needsAttention }
    }

    /** Das nächste Level, an dem weitergeklettert werden sollte. */
    fun nextSkill(worlds: List<World>, progress: ClimbProgress): Skill? {
        worlds.forEachIndexed { wi, w ->
            if (w.independent) return@forEachIndexed
            if (!worldUnlocked(worlds, progress, wi)) return null
            w.skills.forEach { s -> if (!progress.of(s.id).mastered) return s }
        }
        return null
    }

    /** Tagesstatistik einer Liste von Ergebnissen. */
    fun dayStat(day: Long, results: List<ProblemResult>): DayStat =
        DayStat(day = day, solved = results.size, correct = results.count { it.correct }, totalMs = results.sumOf { it.timeMs })

    /** Ergebnisse in die Tagesstatistik aller Bereiche übernehmen. */
    fun recordDay(progress: ClimbProgress, results: List<ProblemResult>, today: Long): ClimbProgress =
        if (results.isEmpty()) progress else progress.copy(days = progress.days.addDay(dayStat(today, results), ClimbProgress.DAYS_KEPT))

    /**
     * Alle Ergebnisse einer Einheit in die Statistik übernehmen.
     * [skillDays]: auch die Tagesstatistik der Levels fortschreiben (nicht beim geführten Üben,
     * dessen Zeiten mehrere Zwischenschritte enthalten).
     */
    fun record(
        progress: ClimbProgress,
        results: List<ProblemResult>,
        now: Long,
        today: Long = now / DAY_MS,
        skillDays: Boolean = true,
    ): ClimbProgress {
        var p = progress
        results.groupBy { it.problem.skillId }.forEach { (skillId, list) ->
            p = p.with(skillId) { sp ->
                val newRecent = (sp.recent + list.map { if (it.correct) max(1L, it.timeMs) else -max(1L, it.timeMs) })
                    .takeLast(20)
                sp.copy(
                    practiced = sp.practiced + list.size,
                    practicedCorrect = sp.practicedCorrect + list.count { it.correct },
                    lastPracticedAt = now,
                    recent = if (skillDays) newRecent else sp.recent,
                    days = if (skillDays) sp.days.addDay(dayStat(today, list), ClimbProgress.SKILL_DAYS) else sp.days,
                )
            }
        }
        p = recordDay(p, results, today)
        return p.copy(
            totalSolved = p.totalSolved + results.size,
            totalCorrect = p.totalCorrect + results.count { it.correct },
        )
    }

    fun afterTest(
        worlds: List<World>,
        progress: ClimbProgress,
        skillId: String,
        outcome: Outcome,
        now: Long,
    ): ClimbProgress {
        var p = progress.with(skillId) { sp ->
            if (outcome.passed) {
                val interval = if (sp.intervalDays <= 0) INTERVALS.first() else sp.intervalDays
                sp.copy(
                    stars = max(sp.stars, outcome.stars),
                    bestAvgMs = if (sp.bestAvgMs == 0L) outcome.avgMs else min(sp.bestAvgMs, outcome.avgMs),
                    testsTaken = sp.testsTaken + 1,
                    testsPassed = sp.testsPassed + 1,
                    failStreak = 0,
                    rusty = false,
                    refreshPending = false,
                    intervalDays = interval,
                    nextReviewAt = now + interval * DAY_MS,
                )
            } else {
                sp.copy(testsTaken = sp.testsTaken + 1, failStreak = sp.failStreak + 1)
            }
        }
        p = p.with(skillId) { sp ->
            val log = TestLog(at = now, avgMs = outcome.avgMs, correct = outcome.correct, total = outcome.total, stars = outcome.stars)
            sp.copy(tests = (sp.tests + log).takeLast(ClimbProgress.TESTS_KEPT))
        }
        if (!outcome.passed && p.of(skillId).failStreak >= FAILS_BEFORE_REFRESH) {
            val pre = prerequisite(worlds, skillId)
            if (pre != null && p.of(pre.id).mastered) {
                p = p.with(pre.id) { it.copy(refreshPending = true) }
                p = p.with(skillId) { it.copy(failStreak = 0) }
            }
        }
        return p
    }

    /** Auffrischung bestanden: Level ist wieder frei von Rost. */
    fun afterRefresh(progress: ClimbProgress, skillId: String, outcome: Outcome, now: Long): ClimbProgress {
        if (!outcome.passed) return progress
        return progress.with(skillId) { sp ->
            val interval = max(1, sp.intervalDays)
            sp.copy(
                rusty = false,
                refreshPending = false,
                intervalDays = interval,
                nextReviewAt = now + interval * DAY_MS,
            )
        }
    }

    /** Wiederholung auswerten: Abstände verlängern oder Level als eingerostet markieren. */
    fun afterReview(
        worlds: List<World>,
        progress: ClimbProgress,
        results: List<ProblemResult>,
        now: Long,
    ): ClimbProgress {
        var p = progress
        results.groupBy { it.problem.skillId }.forEach { (skillId, list) ->
            val skill = skill(worlds, skillId) ?: return@forEach
            val accuracy = list.count { it.correct }.toDouble() / list.size
            val avgSec = list.sumOf { it.timeMs } / list.size / 1000.0
            p = p.with(skillId) { sp ->
                when {
                    accuracy >= 0.999 && avgSec <= skill.goals.star1 * 1.25 -> {
                        val next = nextInterval(sp.intervalDays)
                        sp.copy(intervalDays = next, nextReviewAt = now + next * DAY_MS)
                    }

                    accuracy >= 0.5 -> sp.copy(intervalDays = 1, nextReviewAt = now + DAY_MS)
                    else -> sp.copy(rusty = true, intervalDays = 1, nextReviewAt = now + DAY_MS)
                }
            }
        }
        return p
    }

    fun nextInterval(current: Int): Int {
        val index = INTERVALS.indexOfFirst { it > current }
        return if (index < 0) INTERVALS.last() else INTERVALS[index]
    }

    /** Levels, die zur Wiederholung fällig sind (eingerostete zuerst, dann die überfälligsten). */
    fun dueSkills(worlds: List<World>, progress: ClimbProgress, now: Long): List<Skill> =
        worlds.flatMap { it.skills }
            .filter { s ->
                val sp = progress.of(s.id)
                sp.mastered && !sp.needsAttention && sp.nextReviewAt in 1..now
            }
            .sortedBy { progress.of(it.id).nextReviewAt }

    /** Levels, die aufgefrischt werden müssen. */
    fun needsRefresh(worlds: List<World>, progress: ClimbProgress): List<Skill> =
        worlds.flatMap { it.skills }.filter { progress.of(it.id).let { sp -> sp.mastered && sp.needsAttention } }

    fun touchStreak(progress: ClimbProgress, today: Long): ClimbProgress {
        val p = when (progress.lastActiveDay) {
            today -> progress
            today - 1 -> progress.copy(streakDays = progress.streakDays + 1, lastActiveDay = today)
            else -> progress.copy(streakDays = 1, lastActiveDay = today)
        }
        return if (p.streakDays > p.bestStreak) p.copy(bestStreak = p.streakDays) else p
    }

    /**
     * Einstufungstest bestanden: Alle noch offenen Levels der Welt gelten als geschafft (ein Stern).
     * Sie kommen bald in die Wiederholung, damit sich zeigt, ob sie wirklich sitzen.
     */
    fun applyPlacement(world: World, progress: ClimbProgress, now: Long): ClimbProgress {
        var p = progress
        world.skills.forEach { skill ->
            if (!p.of(skill.id).mastered) {
                p = p.with(skill.id) { sp ->
                    sp.copy(
                        stars = 1,
                        placed = true,
                        rusty = false,
                        refreshPending = false,
                        failStreak = 0,
                        intervalDays = PLACEMENT_INTERVAL,
                        nextReviewAt = now + PLACEMENT_INTERVAL * DAY_MS,
                    )
                }
            }
        }
        return p
    }

    /** Übersprungene Levels kommen nach so vielen Tagen in die Wiederholung. */
    const val PLACEMENT_INTERVAL = 3

    /** Tage in Folge, wie sie heute angezeigt werden sollen (0, wenn die Serie gerissen ist). */
    fun visibleStreak(progress: ClimbProgress, today: Long): Int =
        if (progress.lastActiveDay >= today - 1) progress.streakDays else 0
}
