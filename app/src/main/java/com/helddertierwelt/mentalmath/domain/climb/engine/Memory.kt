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
import com.helddertierwelt.mentalmath.domain.climb.model.ErrorEntry
import com.helddertierwelt.mentalmath.domain.climb.model.SkillProgress
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.math.ln
import kotlin.math.pow

/**
 * Gedächtnismodell und Fehlerprotokoll.
 *
 * Für jedes Level wird eine „Stabilität“ S geschätzt: nach S Tagen ist die Sicherheit auf 90 %
 * gesunken (Vergessenskurve R = 0,9^(t/S)). Jede Übung – egal ob Kletterweg, Wiederholung, freies
 * Training mit Level-Bezug oder TMSnat-Training – passt S an: Richtig und schnell nach längerer Pause
 * verlängert S stark, kurz hintereinander nur wenig, viele Fehler verkürzen S. Ein Level ist fällig,
 * sobald die Sicherheit unter [TARGET] fällt.
 */
object Memory {

    /** Ab dieser geschätzten Sicherheit wird wiederholt. */
    const val TARGET = 0.85

    /** Längste Stabilität in Tagen. */
    const val MAX_STABILITY = 365.0

    /** So viele Einträge behält das Fehlerprotokoll. */
    const val ERRORS_KEPT = 300

    private const val DAY_MS = Progression.DAY_MS

    /** Stabilität in Tagen – bei alten Ständen aus dem bisherigen Wiederholungsabstand geschätzt. */
    fun stability(sp: SkillProgress): Double = when {
        sp.stability > 0 -> sp.stability
        sp.mastered && sp.intervalDays > 0 -> sp.intervalDays / dueFactor
        else -> 0.0
    }

    /** Zeitpunkt, auf den sich die Stabilität bezieht. */
    private fun anchor(sp: SkillProgress): Long = when {
        sp.memoryAt > 0 -> sp.memoryAt
        sp.nextReviewAt > 0 && sp.intervalDays > 0 -> sp.nextReviewAt - sp.intervalDays * DAY_MS
        else -> sp.lastPracticedAt
    }

    /** Geschätzte Sicherheit (0–1) zum Zeitpunkt [now]; null, wenn das Level noch nie geübt wurde. */
    fun recall(sp: SkillProgress, now: Long): Double? {
        val s = stability(sp)
        if (s <= 0) return null
        val days = ((now - anchor(sp)).coerceAtLeast(0)) / DAY_MS.toDouble()
        return 0.9.pow(days / s)
    }

    /** Nach wie vielen Stabilitäten die Sicherheit auf [TARGET] fällt (≈ 1,54). */
    private val dueFactor = ln(TARGET) / ln(0.9)

    /** Tage, bis ein Level mit Stabilität [s] wieder fällig ist. */
    fun dueDays(s: Double): Double = s * dueFactor

    /**
     * Ergebnisse einer Einheit einarbeiten: Stabilität jedes betroffenen Levels anpassen, bei
     * geschafften Levels den nächsten Wiederholungstermin danach setzen und Fehler protokollieren.
     * Mit [memory] = false (geführtes Üben, Einstufungstest) werden nur Fehler protokolliert.
     */
    fun update(worlds: List<World>, progress: ClimbProgress, results: List<ProblemResult>, now: Long, memory: Boolean = true): ClimbProgress {
        // Nicht bearbeitete Aufgaben einer Testsimulation zählen hier nicht.
        val answered = results.filter { !it.retry && !(it.timedOut && it.answer.isEmpty() && it.timeMs == 0L) }
        if (answered.isEmpty()) return progress
        var p = progress
        val errors = answered.filter { !it.correct }.map { ErrorEntry(it.problem.skillId, now, it.note) }
        if (errors.isNotEmpty()) p = p.copy(errors = (p.errors + errors).takeLast(ERRORS_KEPT))
        if (!memory) return p
        answered.groupBy { it.problem.skillId }.forEach { (skillId, list) ->
            val skill = Progression.skill(worlds, skillId) ?: return@forEach
            val accuracy = list.count { it.correct }.toDouble() / list.size
            val correctTimes = list.filter { it.correct }.map { it.timeMs / 1000.0 }.sorted()
            val median = correctTimes.getOrNull(correctTimes.size / 2)
            val fast = median != null && median <= skill.goals.star1
            p = p.with(skillId) { sp ->
                val s = stability(sp)
                val r = recall(sp, now) ?: 1.0
                val next = when {
                    s <= 0 -> if (accuracy >= 0.8) 1.0 else 0.5
                    accuracy >= 0.8 -> s * (1 + (if (fast) 1.5 else 0.8) * (1 - r) * 4)
                    accuracy >= 0.5 -> s * 0.5
                    else -> s * 0.3
                }.coerceIn(0.3, MAX_STABILITY)
                val updated = sp.copy(stability = next, memoryAt = now)
                if (updated.mastered && !updated.needsAttention) {
                    val days = dueDays(next)
                    updated.copy(nextReviewAt = now + (days * DAY_MS).toLong(), intervalDays = days.toInt().coerceAtLeast(1))
                } else {
                    updated
                }
            }
        }
        return p
    }

    /** Fehler der letzten [days] Tage je Level. */
    fun recentErrors(progress: ClimbProgress, now: Long, days: Int = 14): Map<String, Int> =
        progress.errors.filter { now - it.at <= days * DAY_MS }.groupingBy { it.skillId }.eachCount()

    /** Ein Fehlermuster: derselbe Denkfehler mehrfach; [note] ist das jüngste Beispiel. */
    data class Pattern(val skillId: String, val note: String, val count: Int)

    private val number = Regex("[-−]?\\d+(?:[.,]\\d+)*")

    /** Art des Denkfehlers ohne die konkreten Zahlen – so werden gleiche Fehler mit anderen Zahlen erkannt. */
    fun patternKey(note: String): String =
        note.removePrefix(SessionLogic.MISTAKE_PREFIX).replace(number, "#").trim()

    /** Denkfehler, die in den letzten [days] Tagen mindestens zweimal vorkamen (häufigste zuerst). */
    fun patterns(progress: ClimbProgress, now: Long, days: Int = 30): List<Pattern> =
        progress.errors.filter { it.note != null && now - it.at <= days * DAY_MS }
            .groupBy { it.skillId to patternKey(it.note!!) }
            .map { (k, v) -> Pattern(k.first, v.last().note!!, v.size) }
            .filter { it.count >= 2 }
            .sortedByDescending { it.count }
}
