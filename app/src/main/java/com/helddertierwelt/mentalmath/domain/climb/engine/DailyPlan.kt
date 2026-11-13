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
import com.helddertierwelt.mentalmath.domain.climb.model.TrainingPlan
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Trainingspläne und der Plan „Heute“.
 *
 * Die Person legt beliebig viele Pläne an (Auswahl an Levels, Minuten, Wochentage, Zeitraum). Jeden
 * Tag werden die aktiven Pläne zu einer Einheit zusammengesetzt. Grundlage sind gut belegte
 * Lernprinzipien:
 * - verteiltes Wiederholen nach Gedächtnismodell ([Memory]); hat ein Plan ein Enddatum, richten sich
 *   die Abstände nach ihm (Abstand ≈ 10–20 % der Zeit bis zur Prüfung, Cepeda u. a. 2008),
 * - Abrufen statt Nachlesen (fast nur kurze Abfragen),
 * - Mischen verschiedener Aufgabentypen (Interleaving, Rohrer u. a.),
 * - erst sicher, dann schnell (Mastery Learning; Tempo erst bei ≥ 90 % richtig),
 * - etwa 85 % richtige Antworten (Wilson u. a. 2019): Läuft es deutlich besser, kommt mehr Neues,
 *   läuft es schlechter, mehr Festigen,
 * - Phasen bis zum Enddatum: Aufbau → Festigen → Endspurt (keine neuen Levels, Simulationen,
 *   leichte Wiederholung).
 */
object DailyPlan {

    const val WARMUP = "WARMUP"
    const val CHECK = "CHECK"
    const val NEW = "NEW"
    const val WEAK = "WEAK"
    const val SPEED = "SPEED"
    const val APPLY = "APPLY"

    /** Vorlagen für neue Pläne. */
    const val GOAL_PROFI = "profi"
    const val GOAL_TMSNAT = "tmsnat"
    const val GOAL_ABI = "abi"
    const val GOAL_CUSTOM = "custom"
    val GOALS = listOf(GOAL_PROFI, GOAL_TMSNAT, GOAL_ABI)

    fun goalName(goal: String): String = when (goal) {
        GOAL_TMSNAT -> "TMSnat"
        GOAL_ABI -> "Abitur / LK"
        GOAL_CUSTOM -> "Eigene Auswahl"
        else -> "Kopfrechnen"
    }

    /** Aktionsschlüssel in [PlanBlock.target]. */
    const val TARGET_AP = "ap"
    const val TARGET_UNITS = "units"
    const val TARGET_PATTERN = "pattern"
    const val TARGET_SPEED = "speed"
    const val TARGET_MIX = "mix"
    const val TARGET_SIM = "sim"

    /** Geschätzte Sekunden pro Aufgabe (mit Lesen und Rückmeldung). */
    const val SECONDS_PER_TASK = 18

    const val WARMUP_MAX_SKILLS = 4

    /** Ab diesem Punktwert gilt ein Level als Schwachstelle. */
    const val WEAK_THRESHOLD = 4.0

    /** Zielquote richtiger Antworten (85-%-Regel). */
    const val TARGET_ACCURACY = 0.85

    // --- Pläne ---------------------------------------------------------------------------------

    /** Wochentag eines Epoch-Tags: 1 = Montag … 7 = Sonntag (1.1.1970 war ein Donnerstag). */
    fun weekday(day: Long): Int = (((day + 3) % 7 + 7) % 7).toInt() + 1

    val WEEKDAY_NAMES = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    /** Levels einer Vorlage. */
    fun preset(worlds: List<World>, goal: String): List<String> = when (goal) {
        GOAL_PROFI -> worlds.filter { !it.independent || it.id == "w15" }
        GOAL_TMSNAT -> worlds.filter { it.id == "w13" || it.id == "w14" }
        GOAL_ABI -> worlds.filter { it.id == "w16" || it.id == "w17" }
        else -> emptyList()
    }.flatMap { w -> w.skills.map { it.id } }

    /** Ausgewählte Levels in der Reihenfolge des Kletterwegs. */
    fun ordered(worlds: List<World>, skillIds: Collection<String>): List<String> {
        val set = skillIds.toSet()
        return worlds.flatMap { w -> w.skills.map { it.id } }.filter { it in set }
    }

    fun newPlan(worlds: List<World>, progress: ClimbProgress, id: String, name: String, skillIds: List<String>, today: Long): TrainingPlan {
        val ids = ordered(worlds, skillIds)
        return TrainingPlan(id = id, name = name, skillIds = ids, startDay = today, masteredAtStart = ids.count { progress.of(it).mastered })
    }

    /** Ist der Plan an diesem Tag dran? */
    fun activeOn(plan: TrainingPlan, day: Long): Boolean =
        !plan.paused && day >= plan.startDay && (plan.endDay == null || day <= plan.endDay) && weekday(day) in plan.weekdays

    fun active(plans: List<TrainingPlan>, day: Long): List<TrainingPlan> = plans.filter { activeOn(it, day) }

    /** Der alte Einzelplan (2.5) wird einmalig als Trainingsplan übernommen. */
    fun migrate(worlds: List<World>, progress: ClimbProgress, setUp: Boolean, goals: List<String>, minutes: Int, followPath: Boolean, today: Long): ClimbProgress {
        if (progress.plansMigrated) return progress
        if (!setUp || progress.plans.isNotEmpty()) return progress.copy(plansMigrated = true)
        val ids = goals.ifEmpty { listOf(GOAL_PROFI) }.flatMap { preset(worlds, it) }
        val plan = newPlan(worlds, progress, "p$today", goals.joinToString(" + ") { goalName(it) }, ids, today)
            .copy(minutes = minutes, followPath = followPath)
        return progress.copy(plans = listOf(plan), plansMigrated = true)
    }

    /** Phase eines Plans. */
    enum class Phase(val title: String, val text: String) {
        BUILD("Aufbau", "Neue Levels lernen, Gelerntes regelmäßig wiederholen"),
        CONSOLIDATE("Festigen", "Sicherer und schneller werden, viel gemischt üben"),
        FINAL("Endspurt", "Keine neuen Levels mehr – Simulationen und leichtes Wiederholen"),
        DONE("Abgeschlossen", "Der Zeitraum ist vorbei"),
    }

    fun phase(plan: TrainingPlan, progress: ClimbProgress, today: Long): Phase {
        val end = plan.endDay
        val allMastered = plan.skillIds.isNotEmpty() && plan.skillIds.all { progress.of(it).mastered }
        if (end == null) return if (allMastered) Phase.CONSOLIDATE else Phase.BUILD
        if (today > end) return Phase.DONE
        val total = (end - plan.startDay).coerceAtLeast(1)
        val left = end - today
        return when {
            left <= maxOf(3L, (total * 0.1).toLong()) -> Phase.FINAL
            allMastered || today >= plan.startDay + (total * 0.75).toLong() -> Phase.CONSOLIDATE
            else -> Phase.BUILD
        }
    }

    /** Übungstage des Plans von [from] bis [to] (beide eingeschlossen). */
    fun trainingDays(plan: TrainingPlan, from: Long, to: Long): Int =
        if (to < from) 0 else (from..to).count { weekday(it) in plan.weekdays }

    /** Stand eines Plans für die Übersicht. */
    data class Status(
        val phase: Phase,
        val mastered: Int,
        val total: Int,
        /** Übungstage, bis alle Levels gelernt sein sollten (null ohne Enddatum). */
        val learnDaysLeft: Int?,
        /** Nötige neue Levels pro Übungstag (null ohne Enddatum oder wenn alles gelernt ist). */
        val perDay: Double?,
        /** Anteil richtiger Antworten zuletzt in diesem Plan (null bei zu wenig Daten). */
        val accuracy: Double?,
        val daysLeft: Long?,
    ) {
        val tight: Boolean get() = (perDay ?: 0.0) > 2.0
    }

    fun status(plan: TrainingPlan, progress: ClimbProgress, today: Long): Status {
        val total = plan.skillIds.size
        val mastered = plan.skillIds.count { progress.of(it).mastered }
        val end = plan.endDay
        val learnEnd = end?.let { plan.startDay + ((it - plan.startDay) * 0.75).toLong() }
        val learnDays = learnEnd?.let { trainingDays(plan, today, maxOf(today, it)) }
        val open = total - mastered
        val perDay = if (learnDays != null && open > 0) open.toDouble() / learnDays.coerceAtLeast(1) else null
        return Status(phase(plan, progress, today), mastered, total, learnDays, perDay, accuracy(progress, plan.skillIds), end?.let { it - today })
    }

    /** Anteil richtiger Antworten der letzten Ergebnisse in den Levels (je Level die letzten 10). */
    fun accuracy(progress: ClimbProgress, skillIds: List<String>): Double? {
        val recent = skillIds.flatMap { progress.of(it).recent.takeLast(10) }
        if (recent.size < 10) return null
        return recent.count { it > 0 }.toDouble() / recent.size
    }

    // --- Plan „Heute“ ---------------------------------------------------------------------------

    fun settingsKey(plans: List<TrainingPlan>, today: Long): String =
        active(plans, today).joinToString(";") { p ->
            listOf(p.id, p.skillIds.hashCode(), p.minutes, p.weekdays.joinToString(""), p.startDay, p.endDay, p.followPath).joinToString("|")
        }

    /** Der heutige Plan: der gespeicherte, wenn er von heute ist und zu den Plänen passt, sonst ein neuer (Erledigtes bleibt abgehakt). */
    fun current(worlds: List<World>, progress: ClimbProgress, now: Long, today: Long): DayPlan {
        val key = settingsKey(progress.plans, today)
        val saved = progress.plan
        if (saved != null && saved.day == today && saved.settingsKey == key) return saved
        val fresh = build(worlds, progress, progress.plans, now, today)
        if (saved == null || saved.day != today) return fresh
        val ids = fresh.blocks.map { it.id }.toSet()
        return fresh.copy(done = saved.done.filter { it in ids })
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

    /** Nach einem Grundlagen-Check: für jede unsichere Grundlage „Auffrischen“ direkt hinter dem Check einfügen. */
    fun afterCheck(plan: DayPlan?, today: Long, outcome: CheckOutcome?): DayPlan? {
        if (plan == null || plan.day != today || outcome == null || outcome.weak.isEmpty()) return plan
        val check = plan.blocks.firstOrNull { it.kind == CHECK && outcome.skillId in it.skillIds }
        val extra = outcome.weak.filter { s -> plan.blocks.none { it.id == "$today-$WEAK-${s.id}" } }.map { s ->
            PlanBlock("$today-$WEAK-${s.id}", WEAK, "Grundlage auffrischen: ${s.title}", "im Grundlagen-Check nicht sicher", listOf(s.id), Progression.REFRESH_SIZE, planId = check?.planId)
        }
        if (extra.isEmpty()) return plan
        val at = check?.let { plan.blocks.indexOf(it) + 1 } ?: 0
        return plan.copy(blocks = plan.blocks.take(at) + extra + plan.blocks.drop(at))
    }

    /** Geschätzte Minuten. */
    fun minutesOf(blocks: List<PlanBlock>): Int =
        (blocks.sumOf { it.count } * SECONDS_PER_TASK / 60.0).roundToInt().coerceAtLeast(1)

    /** Punktwert „braucht Übung“ eines Levels, dazu der wichtigste Grund. */
    data class Weakness(val skill: Skill, val score: Double, val reason: String, val pattern: String? = null)

    fun weaknesses(progress: ClimbProgress, now: Long, among: List<Skill>): List<Weakness> {
        val errors = Memory.recentErrors(progress, now)
        val patterns = Memory.patterns(progress, now).groupBy { it.skillId }
        return among.mapNotNull { skill ->
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
                pattern != null -> "Fehlermuster (${pattern.count}×): ${pattern.note.removePrefix(SessionLogic.MISTAKE_PREFIX)}"
                sp.failStreak > 0 -> "Prüfung zuletzt nicht bestanden"
                err > 0 -> "$err Fehler in den letzten 14 Tagen"
                form != null -> "Sicherheit erst ${form.score} %"
                else -> "braucht Übung"
            }
            Weakness(skill, score, reason, pattern?.note?.takeIf { !sp.needsAttention })
        }.filter { it.score >= WEAK_THRESHOLD }.sortedByDescending { it.score }
    }

    /** Neue Levels für heute aus der Auswahl: der Reihe nach oder abwechselnd aus den gewählten Welten. */
    fun newSkills(worlds: List<World>, progress: ClimbProgress, plan: TrainingPlan, count: Int, today: Long, exclude: Set<String> = emptySet()): List<Skill> {
        if (count <= 0) return emptyList()
        val open = plan.skillIds.mapNotNull { Progression.skill(worlds, it) }
            .filter { it.id !in exclude && !progress.of(it.id).mastered && Progression.skillUnlocked(worlds, progress, it.id) }
        if (plan.followPath) return open.take(count)
        val byWorld = open.groupBy { s -> worlds.first { w -> w.skills.any { it.id == s.id } }.id }.values.toList()
        if (byWorld.isEmpty()) return emptyList()
        val result = mutableListOf<Skill>()
        var round = 0
        while (result.size < count && round < count * byWorld.size) {
            val group = byWorld[((today + round) % byWorld.size).toInt()]
            group.firstOrNull { it !in result }?.let { result += it }
            round++
        }
        return result
    }

    fun build(worlds: List<World>, progress: ClimbProgress, plans: List<TrainingPlan>, now: Long, today: Long): DayPlan {
        val todays = active(plans, today).sortedBy { it.endDay ?: Long.MAX_VALUE }
        val key = settingsKey(plans, today)
        if (todays.isEmpty()) return DayPlan(day = today, minutes = 0, blocks = emptyList(), settingsKey = key)
        val blocks = mutableListOf<PlanBlock>()
        val notes = mutableListOf<String>()
        val skillOf = { id: String -> Progression.skill(worlds, id) }

        // 1. Aufwärmen (gemeinsam): fällige Levels aller heutigen Pläne, bei Plänen mit Enddatum auch
        //    alle, die bis zum Enddatum unter 90 % fallen würden und lange genug nicht geübt wurden.
        val weakFirst = todays.associate { p ->
            p.id to weaknesses(progress, now, p.skillIds.mapNotNull(skillOf)).firstOrNull()
        }
        val reserved = weakFirst.values.mapNotNull { it?.skill?.id }.toSet()
        val warmScores = mutableMapOf<String, Double>()
        for (p in todays) {
            val end = p.endDay
            for (id in p.skillIds) {
                if (id in reserved) continue
                val sp = progress.of(id)
                if (!sp.mastered || sp.needsAttention) continue
                val r = Memory.recall(sp, now) ?: continue
                val due = r < Memory.TARGET || sp.nextReviewAt in 1..now
                val examDue = end != null && run {
                    val daysToEnd = (end - today).coerceAtLeast(1)
                    val atEnd = Memory.recall(sp, end * Progression.DAY_MS) ?: 1.0
                    val since = (now - maxOf(sp.memoryAt, sp.lastPracticedAt)) / Progression.DAY_MS.toDouble()
                    atEnd < 0.9 && since >= maxOf(1.0, daysToEnd * 0.1)
                }
                if (due || examDue) warmScores[id] = minOf(warmScores[id] ?: 2.0, r)
            }
        }
        val warm = warmScores.entries.sortedBy { it.value }.take(WARMUP_MAX_SKILLS)
        if (warm.isNotEmpty()) {
            val pct = (warm.first().value * 100).roundToInt()
            val reason = "${warmScores.size} ${if (warmScores.size == 1) "Level ist" else "Levels sind"} dran – geschätzte Sicherheit zuletzt $pct %"
            blocks += PlanBlock("$today-$WARMUP", WARMUP, "Aufwärmen", reason, warm.map { it.key }, warm.size * Progression.REVIEW_PER_SKILL)
        }
        val inWarm = warm.map { it.key }.toSet()

        // 2. Je Plan: Grundlagen-Check, Neues, Schwachstelle, Tempo, Anwenden.
        val warmShare = (blocks.sumOf { it.count }) / todays.size
        val usedIds = inWarm.toMutableSet()
        for (p in todays) {
            val budget = (p.minutes * 60 / SECONDS_PER_TASK).coerceAtLeast(8)
            var used = warmShare
            val mine = mutableListOf<PlanBlock>()
            fun add(b: PlanBlock) { mine += b.copy(id = "$today-${p.id}-${b.id}", planId = p.id); used += b.count; usedIds += b.skillIds }
            fun fits(n: Int) = used + n <= budget + 4
            val selection = p.skillIds.mapNotNull(skillOf)
            val status = status(p, progress, today)
            val phase = status.phase
            val acc = status.accuracy

            // Wie viele neue Levels? Nach Phase, Tempo bis zum Enddatum und 85-%-Regel.
            var newCount = when (phase) {
                Phase.FINAL, Phase.DONE -> 0
                else -> if ((status.perDay ?: 0.0) > 1.2 || p.minutes >= 30) 2 else 1
            }
            if (acc != null && acc < TARGET_ACCURACY - 0.1 && newCount > 0 && selection.any { progress.of(it.id).mastered }) {
                newCount = if (acc < TARGET_ACCURACY - 0.2) 0 else newCount - 1
                notes += "${p.name}: Trefferquote zuletzt ${pct(acc)} (Ziel ≈ 85 %) – heute ${if (newCount == 0) "kein neues Level" else "weniger Neues"}, erst festigen."
            }
            if (acc != null && acc > TARGET_ACCURACY + 0.08 && phase == Phase.BUILD) newCount += 1
            if (phase == Phase.FINAL) notes += "${p.name}: Endspurt – keine neuen Levels mehr, dafür Simulation und Wiederholung."
            newCount = newCount.coerceAtMost(3)
            val weak = weakFirst[p.id]?.takeIf { it.skill.id !in usedIds }
            val fresh = newSkills(worlds, progress, p, newCount, today, usedIds + listOfNotNull(weak?.skill?.id))

            val checkFor = (fresh.take(1) + listOfNotNull(weak?.skill)).firstOrNull { Drills.checkUseful(worlds, progress, it.id) }
            if (checkFor != null) {
                add(PlanBlock("$CHECK-${checkFor.id}", CHECK, "Grundlagen-Check", "Prüfung „${checkFor.title}“ zuletzt nicht bestanden – sitzen die Grundlagen?", listOf(checkFor.id), Drills.CHECK_SIZE, CHECK))
            }
            fresh.forEachIndexed { i, s ->
                if (i == 0 || fits(s.practiceCount)) {
                    val reason = when {
                        i > 0 -> "läuft gut (${pct(acc)} richtig) – heute ein Level mehr"
                        status.perDay != null && status.perDay > 1.2 -> "für dein Enddatum nötig: etwa ${fmt1(status.perDay)} Levels pro Übungstag"
                        p.followPath -> "nächstes Level deiner Auswahl"
                        else -> "nächster Schritt – abwechselnd aus deinen Welten"
                    }
                    add(PlanBlock("$NEW-$i", NEW, "Neu: ${s.title}", reason, listOf(s.id), s.practiceCount))
                }
            }
            if (weak != null && weak.skill.id !in usedIds && fits(weak.skill.practiceCount)) {
                val sp = progress.of(weak.skill.id)
                if (weak.pattern != null) {
                    add(PlanBlock(WEAK, WEAK, "Fehlermuster: ${weak.skill.title}", weak.reason, listOf(weak.skill.id), Drills.PATTERN_SIZE, TARGET_PATTERN, weak.pattern))
                } else {
                    val count = if (sp.mastered && sp.needsAttention) Progression.REFRESH_SIZE else weak.skill.practiceCount
                    add(PlanBlock(WEAK, WEAK, "Schwachstelle: ${weak.skill.title}", weak.reason, listOf(weak.skill.id), count))
                }
            }
            // Tempo erst, wenn es sicher sitzt; in Aufbau-Phase nur bei guter Quote.
            if (phase != Phase.BUILD || (acc ?: 0.0) >= 0.9) {
                val speed = selection.filter { it.id !in usedIds && !progress.of(it.id).needsAttention && Drills.speedUseful(it, progress) }
                    .mapNotNull { s -> Stats.form(s, progress.of(s.id))?.let { s to it } }
                    .maxByOrNull { (s, f) -> f.medianSec!! / s.goals.star2 }
                if (speed != null && fits(speed.first.practiceCount)) {
                    val (s, f) = speed
                    val reason = "sicher, aber ${fmt1(f.medianSec!!)} s pro Aufgabe – Ziel unter ${fmt1(s.goals.star2)} s, Zeitlimit ${fmt1(s.goals.star2 * Drills.SPEED_FACTOR)} s"
                    add(PlanBlock(SPEED, SPEED, "Tempo: ${s.title}", reason, listOf(s.id), s.practiceCount, TARGET_SPEED))
                }
            }
            applyBlock(worlds, progress, p, phase, today, usedIds)?.let { if (mine.isEmpty() || fits(it.count)) add(it) }
            // Noch viel Zeit übrig (z. B. ganz am Anfang)? Weiteres Neues, solange die Quote stimmt.
            if (used < budget * 0.6 && phase != Phase.FINAL && (acc == null || acc >= TARGET_ACCURACY)) {
                newSkills(worlds, progress, p, 3, today, usedIds + listOfNotNull(weak?.skill?.id)).take(if (used < budget * 0.3) 2 else 1).forEachIndexed { i, s ->
                    if (fits(s.practiceCount)) add(PlanBlock("$NEW-x$i", NEW, "Neu: ${s.title}", "noch Zeit übrig – ein weiteres Level", listOf(s.id), s.practiceCount))
                }
            }
            // Noch Zeit übrig? Eine weitere Schwachstelle oder gemischtes Wiederholen.
            if (used < budget * 0.7) {
                weaknesses(progress, now, selection).firstOrNull { it.skill.id !in usedIds }?.let { w ->
                    add(PlanBlock("${WEAK}2", WEAK, "Schwachstelle: ${w.skill.title}", w.reason, listOf(w.skill.id), w.skill.practiceCount))
                }
            }
            blocks += mine
        }
        return DayPlan(day = today, minutes = todays.sumOf { it.minutes }, blocks = blocks, settingsKey = key, notes = notes)
    }

    /** Anwenden: gemischt üben passend zur Auswahl und Phase. */
    private fun applyBlock(worlds: List<World>, progress: ClimbProgress, plan: TrainingPlan, phase: Phase, today: Long, used: Set<String>): PlanBlock? {
        val ids = plan.skillIds.toSet()
        val hasAp = ids.any { it.startsWith("w13.") }
        val hasUnits = ids.any { it.startsWith("w14.") }
        val apMastered = ids.count { it.startsWith("w13.") && progress.of(it).mastered }
        if (hasAp && phase == Phase.FINAL) {
            return PlanBlock(APPLY, APPLY, "Endspurt: TMSnat-Kurztest", "Testsimulation mit 8 Aufgaben unter Zeitdruck – wie im echten Test", count = 8, target = TARGET_SIM)
        }
        if ((hasAp || hasUnits) && (today % 2 == 0L || !hasUnits) && hasAp && (apMastered >= 2 || phase != Phase.BUILD)) {
            return PlanBlock(APPLY, APPLY, "Anwenden: TMSnat-Aufgaben", "gemischte Textaufgaben wie im Test", count = 8, target = TARGET_AP)
        }
        if (hasUnits && ids.count { it.startsWith("w14.") && progress.of(it).mastered } >= 2) {
            return PlanBlock(APPLY, APPLY, "Anwenden: Einheiten umrechnen", "gemischte Umrechnungen wie im Test", count = 10, target = TARGET_UNITS)
        }
        // Gemischt: zuletzt gelernte und am ehesten vergessene Levels der Auswahl (Interleaving).
        val mastered = plan.skillIds.mapNotNull { Progression.skill(worlds, it) }
            .filter { it.id !in used && progress.of(it.id).mastered && !progress.of(it.id).needsAttention }
        if (mastered.size < 2) return null
        val recent = mastered.sortedByDescending { s -> progress.of(s.id).tests.filter { it.stars > 0 }.maxOfOrNull { it.at } ?: 0L }.take(2)
        val faded = (mastered - recent.toSet()).sortedBy { Memory.recall(progress.of(it.id), today * Progression.DAY_MS) ?: 1.0 }.take(2)
        val mix = (recent + faded).distinctBy { it.id }
        return PlanBlock(APPLY, APPLY, "Gemischt üben", "verschiedene Methoden durcheinander – die passende selbst erkennen", mix.map { it.id }, mix.size * Progression.REVIEW_PER_SKILL, TARGET_MIX)
    }

    private fun pct(v: Double?): String = if (v == null) "–" else "${(v * 100).roundToInt()} %"

    private fun fmt1(v: Double): String = String.format(java.util.Locale.GERMANY, "%.1f", v)

    /** Aufgerundet. */
    fun ceilInt(v: Double): Int = ceil(v).toInt()
}
