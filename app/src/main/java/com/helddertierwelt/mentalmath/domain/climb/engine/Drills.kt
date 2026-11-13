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
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.random.Random

/** Auswertung eines Grundlagen-Checks. */
data class CheckOutcome(
    /** Das Level, das gerade hakt. */
    val skillId: String,
    /** Geprüfte Grundlagen. */
    val checked: List<Skill>,
    /** Grundlagen, die nicht sicher saßen (jetzt zum Auffrischen markiert). */
    val weak: List<Skill>,
)

/**
 * Gezielte Übungen des Trainingsplans (Stufe 2): Grundlagen-Check (5 Aufgaben aus den Levels, auf
 * denen ein hakendes Level aufbaut), Fehlermuster-Training (Aufgaben, bei denen genau der erkannte
 * Denkfehler passieren kann), Tempotraining (Zeitlimit knapp über dem Ziel für 2 Sterne) und
 * gemischtes Üben der zuletzt gelernten Levels (Methode selbst erkennen).
 */
object Drills {

    const val CHECK_SIZE = 5
    const val CHECK_TITLE = "Grundlagen-Check"
    const val PATTERN_SIZE = 8
    const val MIX_SKILLS = 4

    /** Tempotraining: Zeitlimit pro Aufgabe = Ziel für 2 Sterne mal diesem Faktor. */
    const val SPEED_FACTOR = 1.3

    /** Die (geschafften) Grundlagen eines Levels: das Level davor und dessen Vorgänger. */
    fun prerequisites(worlds: List<World>, progress: ClimbProgress, skillId: String): List<Skill> {
        val first = Progression.prerequisite(worlds, skillId) ?: return emptyList()
        val second = Progression.prerequisite(worlds, first.id)
        return listOfNotNull(first, second).filter { progress.of(it.id).mastered }
    }

    /** Lohnt sich ein Grundlagen-Check? Wenn eine Prüfung zuletzt nicht bestanden wurde und es Grundlagen gibt. */
    fun checkUseful(worlds: List<World>, progress: ClimbProgress, skillId: String): Boolean =
        progress.of(skillId).failStreak > 0 && prerequisites(worlds, progress, skillId).isNotEmpty()

    /** Grundlagen-Check: 5 Aufgaben, mehr vom direkten Vorgänger. */
    fun check(worlds: List<World>, progress: ClimbProgress, skillId: String, random: Random): Session? {
        val pre = prerequisites(worlds, progress, skillId)
        if (pre.isEmpty()) return null
        val counts = if (pre.size == 1) listOf(CHECK_SIZE) else listOf(3, CHECK_SIZE - 3)
        val problems = pre.zip(counts).flatMap { (s, n) -> SessionPlanner.problems(s, n, random) }.shuffled(random)
        return Session(type = SessionType.PRACTICE, skillId = null, title = CHECK_TITLE, problems = problems.map { Planned(it) }, check = skillId)
    }

    /** Auswertung: Grundlagen mit Fehlern werden zum Auffrischen markiert. */
    fun afterCheck(worlds: List<World>, progress: ClimbProgress, session: Session): Pair<ClimbProgress, CheckOutcome?> {
        val skillId = session.check ?: return progress to null
        val answered = session.results.filter { !it.retry }
        val checked = answered.map { it.problem.skillId }.distinct().mapNotNull { Progression.skill(worlds, it) }
        val weak = checked.filter { s ->
            val list = answered.filter { it.problem.skillId == s.id }
            list.isNotEmpty() && list.count { it.correct }.toDouble() / list.size < 0.8
        }
        var p = progress
        weak.forEach { s -> p = p.with(s.id) { it.copy(refreshPending = true) } }
        return p to CheckOutcome(skillId, checked, weak)
    }

    /** Passt [note] (aus dem Fehlerprotokoll) zu einem typischen Fehler dieser Aufgabe? */
    fun matches(problem: Problem, note: String): Boolean {
        val key = Memory.patternKey(note)
        return problem.mistakes.any { Memory.patternKey(it.why) == key }
    }

    /** Fehlermuster-Training: Aufgaben, bei denen der Denkfehler [note] passieren kann; null, wenn das Level keine solchen erzeugt. */
    fun patternProblems(skill: Skill, note: String, random: Random, count: Int = PATTERN_SIZE): List<Problem>? {
        val found = mutableListOf<Problem>()
        val keys = mutableSetOf<String>()
        repeat(400) {
            if (found.size >= count) return@repeat
            val p = skill.generate(random)
            if (matches(p, note) && keys.add(p.key)) found += p
        }
        return if (found.size >= 3) found else null
    }

    fun patternSession(skill: Skill, note: String, random: Random): Session {
        val problems = patternProblems(skill, note, random) ?: SessionPlanner.problems(skill, skill.practiceCount, random)
        return Session(type = SessionType.PRACTICE, skillId = skill.id, title = "Fehlermuster: ${skill.title}", problems = problems.map { Planned(it) })
    }

    /** Lohnt sich Tempotraining? Sicher (≥ 90 %), aber im Mittel langsamer als das Ziel für 2 Sterne. */
    fun speedUseful(skill: Skill, progress: ClimbProgress): Boolean {
        val sp = progress.of(skill.id)
        if (!sp.mastered) return false
        val form = Stats.form(skill, sp) ?: return false
        return form.accuracy >= 0.9 && form.medianSec != null && form.medianSec > skill.goals.star2
    }

    /** Tempotraining: freies Üben mit Zeitlimit knapp über dem Ziel für 2 Sterne. */
    fun speedSession(skill: Skill, random: Random): Session {
        val limit = (skill.goals.star2 * SPEED_FACTOR * 1000).toLong()
        val problems = SessionPlanner.problems(skill, skill.practiceCount, random).map { Planned(it) }
        return Session(type = SessionType.PRACTICE, skillId = skill.id, title = "Tempo: ${skill.title}", problems = problems, timeLimitMs = limit)
    }

    /** Die zuletzt geschafften Levels des Hauptwegs (nach der letzten bestandenen Prüfung). */
    fun recentlyLearned(worlds: List<World>, progress: ClimbProgress, count: Int = MIX_SKILLS): List<Skill> =
        worlds.filter { !it.independent }.flatMap { it.skills }
            .filter { progress.of(it.id).mastered && !progress.of(it.id).needsAttention }
            .sortedByDescending { s -> progress.of(s.id).tests.filter { it.stars > 0 }.maxOfOrNull { it.at } ?: 0L }
            .take(count)

    /** Gemischt üben: Aufgaben der zuletzt gelernten Levels durcheinander. */
    fun mixSession(skills: List<Skill>, random: Random, title: String): Session =
        SessionLogic.createReview(skills, random, title).copy(type = SessionType.PRACTICE)
}
