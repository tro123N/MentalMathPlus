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
import com.helddertierwelt.mentalmath.domain.climb.model.DayPlan
import com.helddertierwelt.mentalmath.domain.climb.model.PlanBlock
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.math.roundToInt

/**
 * Trainingsplan „Heute“: stellt aus Gedächtnismodell, Fehlerprotokoll und Ziel eine Einheit von
 * etwa der gewünschten Länge zusammen – Aufwärmen (fällige Wiederholungen), Neues, Schwachstelle,
 * Tempo und Anwenden. Der Plan wird einmal am Tag erstellt und neu gebaut, wenn sich die
 * Einstellungen ändern.
 */
object DailyPlan {

    const val WARMUP = "WARMUP"
    const val NEW = "NEW"
    const val WEAK = "WEAK"
    const val SPEED = "SPEED"
    const val APPLY = "APPLY"

    /** Ziele des Plans. */
    const val GOAL_PROFI = "profi"
    const val GOAL_TMSNAT = "tmsnat"
    const val GOAL_ABI = "abi"
    val GOALS = listOf(GOAL_PROFI, GOAL_TMSNAT, GOAL_ABI)

    fun goalName(goal: String): String = when (goal) {
        GOAL_TMSNAT -> "TMSnat"
        GOAL_ABI -> "Abitur / LK"
        else -> "Kopfrechnen"
    }

    /** Aktionsschlüssel in [PlanBlock.target]: TMSnat-Übung, Einheiten. */
    const val TARGET_AP = "ap"
    const val TARGET_UNITS = "units"

    /** Geschätzte Sekunden pro Aufgabe (mit Lesen und Rückmeldung). */
    const val SECONDS_PER_TASK = 18

    const val WARMUP_MAX_SKILLS = 4

    /** Ab diesem Punktwert gilt ein Level als Schwachstelle. */
    const val WEAK_THRESHOLD = 4.0

    fun settingsKey(goals: List<String>, minutes: Int, followPath: Boolean): String =
        goals.sorted().joinToString("+") + "|" + minutes + "|" + followPath

    /** Welten, die zu den Zielen gehören. */
    fun goalWorlds(worlds: List<World>, goals: List<String>): List<World> {
        val ids = buildSet {
            if (GOAL_PROFI in goals || goals.isEmpty()) {
                worlds.filter { !it.independent }.forEach { add(it.id) }
                add("w15")
            }
            if (GOAL_TMSNAT in goals) { add("w13"); add("w14") }
            if (GOAL_ABI in goals) { add("w17"); add("w16") }
        }
        return worlds.filter { it.id in ids }
    }

    /** Der heutige Plan: der gespeicherte, wenn er von heute ist und zu den Einstellungen passt, sonst ein neuer. */
    fun current(
        worlds: List<World>,
        progress: ClimbProgress,
        goals: List<String>,
        minutes: Int,
        followPath: Boolean,
        now: Long,
        today: Long,
    ): DayPlan {
        val key = settingsKey(goals, minutes, followPath)
        val saved = progress.plan
        if (saved != null && saved.day == today && saved.settingsKey == key) return saved
        return build(worlds, progress, goals, minutes, followPath, now, today)
    }

    /** Einen Baustein als erledigt markieren. */
    fun markDone(plan: DayPlan, blockId: String): DayPlan =
        if (blockId in plan.done || plan.blocks.none { it.id == blockId }) plan else plan.copy(done = plan.done + blockId)

    /**
     * Nach einer Einheit: den gestarteten Baustein [blockId] abhaken; ein Baustein „Neu“ gilt auch
     * als erledigt, sobald eine Einheit dieses Levels ([skillId]) abgeschlossen wurde.
     */
    fun afterSession(plan: DayPlan?, today: Long, blockId: String?, skillId: String?): DayPlan? {
        if (plan == null || plan.day != today) return plan
        var p: DayPlan = plan
        if (blockId != null) p = markDone(p, blockId)
        if (skillId != null) plan.blocks.filter { it.kind == NEW && skillId in it.skillIds }.forEach { p = markDone(p, it.id) }
        return p
    }

    /** Geschätzte Minuten eines Plans. */
    fun minutesOf(blocks: List<PlanBlock>): Int =
        (blocks.sumOf { it.count } * SECONDS_PER_TASK / 60.0).roundToInt().coerceAtLeast(1)

    /** Punktwert „braucht Übung“ eines Levels, dazu der wichtigste Grund. */
    data class Weakness(val skill: Skill, val score: Double, val reason: String)

    fun weaknesses(worlds: List<World>, progress: ClimbProgress, now: Long, among: List<World> = worlds): List<Weakness> {
        val errors = Memory.recentErrors(progress, now)
        val patterns = Memory.patterns(progress, now).groupBy { it.skillId }
        return among.flatMap { it.skills }.mapNotNull { skill ->
            val sp = progress.of(skill.id)
            if (sp.practiced == 0 && sp.testsTaken == 0) return@mapNotNull null
            val form = Stats.form(skill, sp)
            val err = errors[skill.id] ?: 0
            val pattern = patterns[skill.id]?.firstOrNull()
            val score = err * 1.5 +
                (form?.let { (100 - it.score) / 12.0 } ?: 0.0) +
                (if (sp.needsAttention) 8.0 else 0.0) +
                (if (sp.failStreak > 0) 3.0 * sp.failStreak else 0.0) +
                (pattern?.let { 2.0 * it.count } ?: 0.0)
            val reason = when {
                sp.rusty -> "eingerostet – kurz auffrischen"
                sp.refreshPending -> "Grundlage für ein Level, das noch hakt"
                pattern != null -> "Fehlermuster (${pattern.count}×): ${pattern.note}"
                sp.failStreak > 0 -> "Prüfung zuletzt nicht bestanden"
                err > 0 -> "$err Fehler in den letzten 14 Tagen"
                form != null -> "Sicherheit erst ${form.score} %"
                else -> "braucht Übung"
            }
            Weakness(skill, score, reason)
        }.filter { it.score >= WEAK_THRESHOLD }.sortedByDescending { it.score }
    }

    /** Das neue Level für heute. */
    fun newSkill(worlds: List<World>, progress: ClimbProgress, goals: List<String>, followPath: Boolean, today: Long): Skill? {
        if (followPath) return Progression.nextSkill(worlds, progress)
        val candidates = goalWorlds(worlds, goals).mapIndexedNotNull { _, w ->
            val wi = worlds.indexOf(w)
            if (!w.independent && !Progression.worldUnlocked(worlds, progress, wi)) return@mapIndexedNotNull null
            w.skills.firstOrNull { !progress.of(it.id).mastered && Progression.skillUnlocked(worlds, progress, it.id) }
        }.distinctBy { it.id }
        if (candidates.isEmpty()) return null
        return candidates[(today % candidates.size).toInt()]
    }

    fun build(
        worlds: List<World>,
        progress: ClimbProgress,
        goals: List<String>,
        minutes: Int,
        followPath: Boolean,
        now: Long,
        today: Long,
    ): DayPlan {
        val budget = (minutes * 60 / SECONDS_PER_TASK).coerceAtLeast(10)
        val blocks = mutableListOf<PlanBlock>()
        var used = 0
        fun fits(count: Int) = blocks.isEmpty() || used + count <= budget + 4
        fun add(block: PlanBlock) { blocks += block; used += block.count }
        val all = worlds.flatMap { it.skills }

        // Neues Level und größte Schwachstelle vorab bestimmen, damit das Aufwärmen sie auslässt.
        val fresh = newSkill(worlds, progress, goals, followPath, today)
        val weak = weaknesses(worlds, progress, now).firstOrNull { it.skill.id != fresh?.id }

        // 1. Aufwärmen: fällige oder am ehesten vergessene Levels.
        val mastered = all.filter { it.id != weak?.skill?.id && progress.of(it.id).let { sp -> sp.mastered && !sp.needsAttention } }
        val byRecall = mastered.map { it to (Memory.recall(progress.of(it.id), now) ?: 1.0) }.sortedBy { it.second }
        val due = byRecall.filter { (s, r) -> r < Memory.TARGET || progress.of(s.id).nextReviewAt in 1..now }
        val warm = (due.ifEmpty { byRecall.filter { it.second < 0.95 }.take(2) }).take(WARMUP_MAX_SKILLS)
        if (warm.isNotEmpty()) {
            val reason = if (due.isNotEmpty()) {
                val pct = (warm.first().second * 100).roundToInt()
                "${due.size} ${if (due.size == 1) "Level ist" else "Levels sind"} fällig – geschätzte Sicherheit zuletzt $pct %"
            } else {
                "kurz auffrischen, bevor etwas verblasst"
            }
            add(PlanBlock("$today-$WARMUP", WARMUP, "Aufwärmen", reason, warm.map { it.first.id }, warm.size * Progression.REVIEW_PER_SKILL))
        }

        // 2. Neues Level.
        if (fresh != null && fits(fresh.practiceCount)) {
            val reason = if (followPath) "nächstes Level auf dem Kletterweg" else "nächster Schritt für dein Ziel"
            add(PlanBlock("$today-$NEW", NEW, "Neu: ${fresh.title}", reason, listOf(fresh.id), fresh.practiceCount))
        }

        // 3. Schwachstelle (eingerostete Levels zuerst).
        if (weak != null && fits(weak.skill.practiceCount)) {
            val count = if (progress.of(weak.skill.id).let { it.mastered && it.needsAttention }) Progression.REFRESH_SIZE else weak.skill.practiceCount
            add(PlanBlock("$today-$WEAK", WEAK, "Schwachstelle: ${weak.skill.title}", weak.reason, listOf(weak.skill.id), count))
        }

        // 4. Tempo: sicher, aber noch langsam.
        val usedIds = blocks.flatMap { it.skillIds }.toSet()
        val speed = goalWorlds(worlds, goals).flatMap { it.skills }
            .filter { it.id !in usedIds && progress.of(it.id).let { sp -> sp.mastered && !sp.needsAttention } }
            .mapNotNull { s -> Stats.form(s, progress.of(s.id))?.let { s to it } }
            .filter { (s, f) -> f.accuracy >= 0.9 && f.medianSec != null && f.medianSec > s.goals.star2 }
            .maxByOrNull { (s, f) -> f.medianSec!! / s.goals.star2 }
        if (speed != null && fits(speed.first.practiceCount)) {
            val (s, f) = speed
            val reason = "sicher, aber ${fmt(f.medianSec!!)} s pro Aufgabe – Ziel unter ${fmt(s.goals.star2)} s"
            add(PlanBlock("$today-$SPEED", SPEED, "Tempo: ${s.title}", reason, listOf(s.id), s.practiceCount))
        }

        // 5. Anwenden: gemischte Aufgaben passend zum Ziel – bei mehreren Zielen täglich ein anderes
        // zuerst, die übrigen, solange die Zeit reicht.
        val chosen = goals.ifEmpty { listOf(GOAL_PROFI) }
        for (i in chosen.indices) {
            val goal = chosen[((today + i) % chosen.size).toInt()]
            val apply = applyBlock(worlds, progress, goal, today) ?: continue
            if (i == 0 || used + apply.count <= budget) { if (fits(apply.count)) add(apply) }
        }

        // Zu wenig gefüllt? Eine zweite Schwachstelle.
        if (used < budget * 0.7) {
            val ids = blocks.flatMap { it.skillIds }.toSet()
            weaknesses(worlds, progress, now).firstOrNull { it.skill.id !in ids }?.let { w ->
                add(PlanBlock("$today-${WEAK}2", WEAK, "Schwachstelle: ${w.skill.title}", w.reason, listOf(w.skill.id), w.skill.practiceCount))
            }
        }
        return DayPlan(day = today, minutes = minutes, blocks = blocks, settingsKey = settingsKey(goals, minutes, followPath))
    }

    private fun applyBlock(worlds: List<World>, progress: ClimbProgress, goal: String, today: Long): PlanBlock? {
        val id = "$today-$APPLY-$goal"
        return when (goal) {
            GOAL_TMSNAT -> if (today % 2 == 0L) {
                PlanBlock(id, APPLY, "Anwenden: TMSnat-Aufgaben", "gemischte Textaufgaben wie im Test", count = 8, target = TARGET_AP)
            } else {
                PlanBlock(id, APPLY, "Anwenden: Einheiten umrechnen", "gemischte Umrechnungen wie im Test", count = 10, target = TARGET_UNITS)
            }

            GOAL_ABI -> mixedBlock(worlds, progress, id, "w17.mixed", "Anwenden: Oberstufe gemischt", "Aufgaben aus allen LK-Themen")
            else -> mixedBlock(worlds, progress, id, "w15.mixed", "Anwenden: Rechentricks gemischt", "Tricks erkennen, wenn sie nicht angesagt sind")
        }
    }

    private fun mixedBlock(worlds: List<World>, progress: ClimbProgress, id: String, skillId: String, title: String, reason: String): PlanBlock? {
        val skill = Progression.skill(worlds, skillId) ?: return null
        if (!Progression.skillUnlocked(worlds, progress, skillId)) return null
        return PlanBlock(id, APPLY, title, reason, listOf(skill.id), skill.practiceCount)
    }

    private fun fmt(sec: Double): String = String.format(java.util.Locale.GERMANY, "%.1f", sec)
}
