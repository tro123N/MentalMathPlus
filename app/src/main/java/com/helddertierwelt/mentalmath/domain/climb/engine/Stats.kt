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
import com.helddertierwelt.mentalmath.domain.climb.model.SkillProgress
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.math.abs
import kotlin.math.roundToInt

/** Wie sicher ein Level gerade sitzt, berechnet aus den letzten Ergebnissen. */
data class Form(
    val skill: Skill,
    /** 0 bis 100. */
    val score: Int,
    /** Anteil richtiger Antworten der letzten Aufgaben (0 bis 1). */
    val accuracy: Double,
    /** Mittlere Zeit (Median) der richtigen Antworten in Sekunden, null ohne richtige Antwort. */
    val medianSec: Double?,
    /** Anzahl der ausgewerteten Aufgaben. */
    val sample: Int,
    /** Das Level ist eingerostet oder muss aufgefrischt werden. */
    val needsAttention: Boolean,
) {
    val label: String
        get() = when {
            score >= 85 -> "sehr sicher"
            score >= 70 -> "sicher"
            score >= 50 -> "wackelig"
            else -> "unsicher"
        }
}

/** Kennzahlen einer Welt. */
data class WorldSummary(
    val world: World,
    val mastered: Int,
    val stars: Int,
    /** Mittlere Form der Levels mit genug Ergebnissen, null wenn es noch keine gibt. */
    val form: Int?,
    val attention: Int,
)

/** Auswertungen für die Statistik-Seite. */
object Stats {

    /** So viele Ergebnisse braucht ein Level mindestens für eine Einschätzung. */
    const val MIN_SAMPLE = 4

    fun form(skill: Skill, sp: SkillProgress): Form? {
        val recent = sp.recent
        if (recent.size < MIN_SAMPLE) return null
        val correctTimes = recent.filter { it > 0 }.map { it / 1000.0 }.sorted()
        val accuracy = correctTimes.size.toDouble() / recent.size
        val median = if (correctTimes.isEmpty()) {
            null
        } else if (correctTimes.size % 2 == 1) {
            correctTimes[correctTimes.size / 2]
        } else {
            (correctTimes[correctTimes.size / 2 - 1] + correctTimes[correctTimes.size / 2]) / 2
        }
        val goals = skill.goals
        val slow = goals.star1 * 1.5
        val fast = goals.star3
        val speed = if (median == null) 0.0 else ((slow - median) / (slow - fast)).coerceIn(0.0, 1.0)
        val score = (100 * (0.6 * accuracy + 0.4 * speed)).roundToInt().coerceIn(0, 100)
        return Form(skill, score, accuracy, median, recent.size, sp.needsAttention)
    }

    /** Form aller Levels mit genug Ergebnissen. */
    fun forms(worlds: List<World>, progress: ClimbProgress): List<Form> =
        worlds.flatMap { it.skills }.mapNotNull { form(it, progress.of(it.id)) }

    /** Die sichersten Levels. */
    fun strengths(worlds: List<World>, progress: ClimbProgress, count: Int = 3): List<Form> =
        forms(worlds, progress)
            .filter { !it.needsAttention && it.score >= 60 }
            .sortedWith(compareByDescending<Form> { it.score }.thenByDescending { it.sample })
            .take(count)

    /** Die Levels, die am meisten Übung brauchen: eingerostete zuerst, dann die schwächsten. */
    fun weaknesses(worlds: List<World>, progress: ClimbProgress, count: Int = 3): List<Form> {
        val strong = strengths(worlds, progress, count).map { it.skill.id }.toSet()
        return forms(worlds, progress)
            .filter { it.skill.id !in strong && (it.needsAttention || it.score < 85) }
            .sortedWith(compareByDescending<Form> { it.needsAttention }.thenBy { it.score })
            .take(count)
    }

    fun worldSummary(world: World, progress: ClimbProgress): WorldSummary {
        val forms = world.skills.mapNotNull { form(it, progress.of(it.id)) }
        return WorldSummary(
            world = world,
            mastered = world.skills.count { progress.of(it.id).mastered },
            stars = world.skills.sumOf { progress.of(it.id).stars },
            form = if (forms.isEmpty()) null else forms.map { it.score }.average().roundToInt(),
            attention = world.skills.count { progress.of(it.id).let { sp -> sp.mastered && sp.needsAttention } },
        )
    }

    /** Die letzten [count] Tage bis einschließlich [today], Tage ohne Übung mit 0. */
    fun lastDays(days: List<DayStat>, today: Long, count: Int): List<DayStat> {
        val byDay = days.associateBy { it.day }
        return (today - count + 1..today).map { d -> byDay[d] ?: DayStat(d) }
    }

    /** Anzahl der Tage, an denen geübt wurde. */
    fun activeDays(progress: ClimbProgress): Int = progress.days.count { it.solved > 0 }

    /** Gelöste Aufgaben in den letzten [count] Tagen. */
    fun solvedInLast(progress: ClimbProgress, today: Long, count: Int): Int =
        progress.days.filter { it.day > today - count && it.day <= today }.sumOf { it.solved }

    /** Durchschnittszeit der richtigen Antworten (ms) eines Eintrags aus [SkillProgress.recent]. */
    fun recentAvgMs(sp: SkillProgress): Long? {
        val times = sp.recent.filter { it > 0 }
        return if (times.isEmpty()) null else times.sum() / times.size
    }

    /** Fehleranteil der letzten Aufgaben in Prozent. */
    fun recentMistakePercent(sp: SkillProgress): Int? =
        if (sp.recent.isEmpty()) null else (100.0 * sp.recent.count { it < 0 } / sp.recent.size).roundToInt()

    /** Veränderung der Prüfungszeit zwischen der ersten und der letzten Prüfung in Prozent (negativ = schneller). */
    fun testTrendPercent(sp: SkillProgress): Int? {
        val tests = sp.tests.filter { it.avgMs > 0 }
        if (tests.size < 2) return null
        val first = tests.first().avgMs.toDouble()
        val last = tests.last().avgMs.toDouble()
        val change = (last - first) / first * 100
        return if (abs(change) < 0.5) 0 else change.roundToInt()
    }
}
