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

import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import kotlin.math.abs
import kotlin.random.Random

/**
 * Punkt-Prognose für den TMSnat-Untertest „Arithmetisches Problemlösen“.
 *
 * Aus den letzten Ergebnissen je Kategorie (richtig/falsch und Zeit) werden viele Testdurchläufe
 * simuliert: 16 Aufgaben aus allen Kategorien, 25 Minuten. Reicht die Zeit nicht, werden die
 * übrigen Aufgaben geraten (eine von fünf Antworten). Kategorien mit wenigen Ergebnissen werden
 * mit dem Durchschnitt aller Kategorien aufgefüllt.
 */
object ApForecast {

    /** So viele Ergebnisse braucht es mindestens insgesamt … */
    const val MIN_RESULTS = 30

    /** … und in so vielen Kategorien mindestens [MIN_PER_CATEGORY] Ergebnisse. */
    const val MIN_CATEGORIES = 6
    const val MIN_PER_CATEGORY = 3

    /** Anzahl der simulierten Tests. */
    const val RUNS = 10_000

    /** Gewicht des Gesamtdurchschnitts bei Kategorien mit wenigen Ergebnissen. */
    private const val PRIOR = 4.0

    data class Forecast(
        /** Mittlere erwartete Punktzahl (Median). */
        val median: Int,
        /** In 80 % der Durchläufe liegt die Punktzahl zwischen [low] und [high]. */
        val low: Int,
        val high: Int,
        val mean: Double,
        /** Anteil der Durchläufe, in denen die Zeit nicht für alle Aufgaben reichte. */
        val timeShort: Double,
        /** Mittlere Zeit pro Aufgabe (ms) aus den Ergebnissen. */
        val avgTaskMs: Long,
        /** Häufigkeit jeder Punktzahl 0 bis [total] in Prozent. */
        val distribution: List<Double>,
        val total: Int,
        /** Grundlage: Zahl der Ergebnisse und Kategorien mit Ergebnissen. */
        val results: Int,
        val categories: Int,
        /** Kategorie, in der eine Verbesserung am meisten Punkte bringt (null: keine Schwäche). */
        val weakest: ApCategory?,
    )

    sealed interface Outcome {
        data class Ready(val forecast: Forecast) : Outcome

        /** Noch zu wenig Ergebnisse: so viele fehlen insgesamt bzw. in Kategorien. */
        data class NeedMore(val results: Int, val categories: Int) : Outcome
    }

    /** Ergebnis aus [com.helddertierwelt.mentalmath.domain.climb.model.SkillProgress.recent]: positiv = richtig in ms, negativ = falsch. */
    private fun samples(p: ClimbProgress, category: ApCategory): List<Long> = p.of(category.skillId).recent

    fun forecast(
        p: ClimbProgress,
        runs: Int = RUNS,
        count: Int = ApTasks.TEST_COUNT,
        seconds: Int = ApTasks.TEST_SECONDS,
        options: Int = 5,
    ): Outcome {
        val byCategory = ApCategory.entries.associateWith { samples(p, it) }
        val pool = byCategory.values.flatten() + p.of("w13.mixed").recent
        val withData = byCategory.count { it.value.size >= MIN_PER_CATEGORY }
        if (pool.size < MIN_RESULTS || withData < MIN_CATEGORIES) {
            return Outcome.NeedMore((MIN_RESULTS - pool.size).coerceAtLeast(0), (MIN_CATEGORIES - withData).coerceAtLeast(0))
        }
        // Gleiche Daten → gleiche Prognose (sie springt nicht bei jedem Öffnen).
        val seed = pool.fold(17L) { acc, v -> acc * 31 + v }
        val base = simulate(byCategory, pool, runs, count, seconds, options, Random(seed))
        // Wo bringt eine Verbesserung am meisten? Kategorie testweise so gut wie den Durchschnitt der besten Hälfte machen.
        val goodShare = pool.count { it > 0 }.toDouble() / pool.size
        val weakest = ApCategory.entries
            .filter { c -> byCategory.getValue(c).let { it.isNotEmpty() && it.count { v -> v > 0 }.toDouble() / it.size < goodShare } }
            .maxByOrNull { c ->
                val improved = byCategory + (c to byCategory.getValue(c).map { abs(it) })
                simulate(improved, pool, runs / 5, count, seconds, options, Random(seed + c.ordinal)).mean - base.mean
            }
        return Outcome.Ready(
            base.copy(
                results = pool.size,
                categories = byCategory.count { it.value.isNotEmpty() },
                weakest = weakest,
                avgTaskMs = pool.map { abs(it) }.average().toLong(),
            ),
        )
    }

    private fun simulate(
        byCategory: Map<ApCategory, List<Long>>,
        pool: List<Long>,
        runs: Int,
        count: Int,
        seconds: Int,
        options: Int,
        r: Random,
    ): Forecast {
        val counts = IntArray(count + 1)
        var short = 0
        var sum = 0L
        val cats = ApCategory.entries
        repeat(runs) {
            // Wie in der Simulation: jede Kategorie einmal, der Rest zufällig dazu.
            val order = cats.shuffled(r).toMutableList()
            while (order.size < count) order += cats.shuffled(r).take(count - order.size)
            val tasks = order.take(count).shuffled(r)
            var left = seconds * 1000L
            var score = 0
            for ((i, c) in tasks.withIndex()) {
                val own = byCategory.getValue(c)
                val useOwn = own.isNotEmpty() && r.nextDouble() < own.size / (own.size + PRIOR)
                val s = if (useOwn) own[r.nextInt(own.size)] else pool[r.nextInt(pool.size)]
                val t = abs(s)
                if (t > left) {
                    // Zeit um: diese und alle übrigen Aufgaben raten.
                    short++
                    repeat(count - i) { if (r.nextInt(options) == 0) score++ }
                    break
                }
                left -= t
                if (s > 0) score++
            }
            counts[score]++
            sum += score
        }
        fun percentile(q: Double): Int {
            val target = runs * q
            var acc = 0
            counts.forEachIndexed { score, c ->
                acc += c
                if (acc >= target) return score
            }
            return count
        }
        return Forecast(
            median = percentile(0.5),
            low = percentile(0.1),
            high = percentile(0.9),
            mean = sum.toDouble() / runs,
            timeShort = short.toDouble() / runs,
            avgTaskMs = 0,
            distribution = counts.map { 100.0 * it / runs },
            total = count,
            results = 0,
            categories = 0,
            weakest = null,
        )
    }
}
