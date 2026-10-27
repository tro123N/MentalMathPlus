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

import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World

/** Ein Trick-Tipp: welche Methode hier am schnellsten zum Ziel führt. */
data class TrickTip(
    /** Level, aus dem die Methode stammt; null bei einem allgemeinen Tipp. */
    val skillId: String?,
    val title: String,
    val text: String,
    /** Die Schritte der Methode (kurz). */
    val steps: List<String> = emptyList(),
)

/**
 * Schlägt zu einer Aufgabe den passenden Trick vor.
 * Aufgaben aus dem Kletterweg gehören zu ihrem Level; bei Aufgaben aus dem freien Training
 * wird die Rechnung erkannt (z. B. „47 × 11“) und das passende Level gesucht.
 */
object TrickAdvisor {

    private val binary = Regex("""^\s*([0-9.]+)\s*([+−×÷])\s*([0-9.]+)\s*$""")
    private val percent = Regex("""^\s*([0-9]+)\s*%\s*von\s*([0-9.]+)\s*$""")
    private val square = Regex("""^\s*([0-9.]+)²\s*$""")
    private val cube = Regex("""^\s*([0-9.]+)³\s*$""")

    fun tip(problem: Problem, worlds: List<World>): TrickTip {
        val skills = worlds.flatMap { it.skills }
        val own = skills.firstOrNull { it.id == problem.skillId }
        // Gemischte Levels: Die Methode der Aufgabe verrät die passende Kategorie.
        val byMethod = problem.method?.let { m -> skills.firstOrNull { it.method == m } }
        if (own != null && byMethod != null && own.method != problem.method) return fromSkill(byMethod)
        own?.let { return fromSkill(it) }
        val id = guessSkill(problem)
            ?: problem.method?.let { m -> skills.firstOrNull { it.method.equals(m, ignoreCase = true) }?.id }
        val skill = id?.let { s -> skills.firstOrNull { it.id == s } }
        if (skill != null) return fromSkill(skill)
        val first = problem.steps.firstOrNull()
        val text = if (first != null && problem.steps.size > 1) {
            "Kein besonderer Trick nötig – zerlege die Aufgabe. Fang an mit: ${first.label}."
        } else {
            first?.hint ?: "Kein besonderer Trick nötig – rechne in Ruhe Schritt für Schritt."
        }
        return TrickTip(null, problem.method ?: "Schritt für Schritt", text)
    }

    private fun fromSkill(skill: Skill): TrickTip {
        val steps = skill.explanation.filterIsInstance<Block.Steps>().firstOrNull()?.items.orEmpty()
        return TrickTip(skill.id, skill.method, skill.summary, steps)
    }

    private fun num(s: String): Long? = s.replace(".", "").toLongOrNull()

    private fun digits(x: Long): Int = x.toString().length

    /** Level-ID passend zur Rechnung, oder null. */
    fun guessSkill(problem: Problem): String? {
        val text = problem.prompt.text
        percent.find(text)?.let { return "w5.percent" }
        square.find(text)?.let { m ->
            val a = num(m.groupValues[1]) ?: return null
            return squareSkill(a)
        }
        cube.find(text)?.let { return "w6.cube" }
        if (text.trim().startsWith("√")) return "w12.sqrtexact"
        val m = binary.find(text) ?: return null
        val a = num(m.groupValues[1]) ?: return null
        val b = num(m.groupValues[3]) ?: return null
        return when (m.groupValues[2]) {
            "+" -> when {
                a < 20 && b < 20 -> "w1.addsub20"
                maxOf(digits(a), digits(b)) <= 2 -> "w3.add2"
                else -> "w3.add3"
            }

            Fmt.MINUS -> when {
                (a == 100L || a == 1000L) -> "w3.complement"
                a < 20 -> "w1.addsub20"
                digits(a) <= 2 -> "w3.sub2"
                else -> "w3.sub3"
            }

            Fmt.TIMES -> multiplySkill(a, b)
            Fmt.DIVIDE -> when {
                b <= 10 && a <= 100 -> "w1.divfacts"
                b == 5L -> "w7.div5"
                b < 10 -> "w7.div1"
                else -> "w7.div2"
            }

            else -> null
        }
    }

    private fun squareSkill(a: Long): String = when {
        a < 10 -> "w1.times"
        a < 100 && a % 10 == 5L -> "w2.square5"
        a < 100 -> "w4.square2"
        a < 1000 -> "w6.square3"
        a < 10000 -> "w11.square4"
        else -> "w11.square5"
    }

    private fun multiplySkill(x: Long, y: Long): String {
        val a = minOf(x, y)
        val b = maxOf(x, y)
        if (a == b) return squareSkill(a)
        if (a < 10 && b < 10) return "w1.times"
        if (a % 10 == 0L || b % 10 == 0L) return "w1.zeros"
        if ((a == 11L && b in 10..99) || (b == 11L && a in 10..99)) {
            val other = if (a == 11L) b else a
            return if (other / 10 + other % 10 >= 10) "w2.times11b" else "w2.times11a"
        }
        if (a in 10..99 && b in 10..99) {
            if (a / 10 == b / 10 && a % 10 + b % 10 == 10L) return "w2.sametens"
            if (a >= 90 && b >= 90) return "w6.close"
            if (b - a <= 10 && (a + b) % 2 == 0L) return "w6.close"
            if (b % 10 >= 8 || a % 10 >= 8) return "w6.submethod"
            if (listOf(a, b).any { v -> (2L..9L).any { f -> v % f == 0L && v / f < 10 } }) return "w6.factor"
            return "w6.addmethod"
        }
        if (a < 10 && b in 10..99) return if (b % 10 >= 8) "w4.roundup" else "w4.mul21"
        if (a < 10 && b in 100..999) return "w4.mul31"
        if (a in 10..99 && b in 100..999) return "w11.mul32"
        if (a in 100..999 && b in 100..999) return "w11.mul33"
        return "w6.addmethod"
    }
}
