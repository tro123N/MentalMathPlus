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

package com.helddertierwelt.mentalmath.domain.climb.curriculum

import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.BookRef
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.Goals
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Mistake
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import java.math.BigDecimal
import kotlin.random.Random

/** Bausteine für die Ergänzungswelten 15–17 (Rechentricks, Alltag, Oberstufe). */
internal object ExtraKit {

    private val superDigits = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    private val subDigits = "₀₁₂₃₄₅₆₇₈₉"

    /** Hochgestellte Zahl, z. B. 10 → „¹⁰“, −2 → „⁻²“. */
    fun sup(n: Int): String = (if (n < 0) "⁻" else "") + kotlin.math.abs(n).toString().map { superDigits[it - '0'] }.joinToString("")

    /** Tiefgestellte Zahl, z. B. 2 → „₂“. */
    fun sub(n: Int): String = n.toString().map { subDigits[it - '0'] }.joinToString("")

    /** Ganze Zahl mit echtem Minuszeichen. */
    fun signed(x: Long): String = if (x < 0) "$MINUS${n(-x)}" else n(x)

    fun bd(x: Long): BigDecimal = BigDecimal.valueOf(x)
    fun bd(s: String): BigDecimal = BigDecimal(s)

    fun ds(label: String, value: BigDecimal, hint: String? = null): Step = Step(label, Expected.Exact(value.stripTrailingZeros()), InputKind.DECIMAL, hint)

    fun mistake(value: Long, why: String) = Mistake(Expected.Exact(bd(value)), why)
    fun mistake(value: BigDecimal, why: String) = Mistake(Expected.Exact(value.stripTrailingZeros()), why)

    /** Ganzzahlige Aufgabe mit typischen Fehlern. */
    fun int(skillId: String, key: String, prompt: Prompt, value: Long, steps: List<Step>, method: String, mistakes: List<Mistake> = emptyList()): Problem =
        intProblem(skillId, key, prompt, value, steps, method).copy(mistakes = clean(mistakes, bd(value)))

    /** Kommazahl-Aufgabe mit typischen Fehlern. */
    fun dec(skillId: String, key: String, prompt: Prompt, value: BigDecimal, steps: List<Step>, method: String, mistakes: List<Mistake> = emptyList()): Problem =
        decProblem(skillId, key, prompt, value.stripTrailingZeros(), null, steps, method).copy(mistakes = clean(mistakes, value))

    private fun clean(mistakes: List<Mistake>, value: BigDecimal): List<Mistake> =
        mistakes.filter { m -> (m.answer as? Expected.Exact)?.value?.let { it.compareTo(value) != 0 && it.signum() >= 0 } ?: true }
            .distinctBy { (it.answer as? Expected.Exact)?.value?.stripTrailingZeros() ?: it.answer }

    /** Auswahlaufgabe (A–E): [wrong] wird auf vier verschiedene Antworten gekürzt, [why] erklärt einzelne Fehler. */
    fun choice(
        skillId: String,
        key: String,
        prompt: Prompt,
        correct: String,
        wrong: List<String>,
        label: String,
        hint: String,
        method: String,
        r: Random,
        why: Map<String, String> = emptyMap(),
    ): Problem {
        val options = (listOf(correct) + wrong.filter { it != correct }.distinct().take(4)).shuffled(r)
        val index = options.indexOf(correct)
        return Problem(
            skillId = skillId,
            key = key,
            prompt = prompt,
            expected = Expected.Choice(index),
            input = InputKind.CHOICE,
            steps = listOf(Step(label, Expected.Choice(index), InputKind.CHOICE, hint)),
            method = method,
            choices = options,
            mistakes = options.mapIndexedNotNull { i, o -> why[o]?.takeIf { i != index }?.let { Mistake(Expected.Choice(i), it) } },
        )
    }

    /** Ein Level einer Ergänzungswelt mit Erklärung, Schritten, Formeln, Beispiel und Tipp. */
    fun skill(
        id: String,
        title: String,
        method: String,
        summary: String,
        intro: String,
        steps: List<String>,
        formulas: List<String>,
        tip: String,
        goals: Goals,
        book: BookRef,
        exampleSeed: Long,
        generator: (String, Random) -> Problem,
    ): Skill = Skill(
        id = id,
        title = title,
        method = method,
        book = book,
        summary = summary,
        explanation = listOf(Block.Text(intro), Block.Steps(steps)) + formulas.map { Block.Formula(it) } +
            listOf(Block.Example(generator(id, Random(exampleSeed))), Block.Tip(tip)),
        goals = goals,
        guidedCount = 3,
        practiceCount = 10,
        generator = { r -> generator(id, r) },
    )

    /** Gemischtes Level: eine Aufgabe aus einem zufälligen anderen Level, unter der eigenen ID. */
    fun mixedOf(skills: () -> List<Skill>): (String, Random) -> Problem = { id, r ->
        val pool = skills().filter { it.id != id }
        r.pick(pool).generate(r).copy(skillId = id)
    }

    val quick = Goals(testSize = 10, minAccuracy = 0.9, star1 = 30.0, star2 = 20.0, star3 = 12.0)
    val medium = Goals(testSize = 10, minAccuracy = 0.9, star1 = 45.0, star2 = 30.0, star3 = 20.0)
    val slow = Goals(testSize = 8, minAccuracy = 0.85, star1 = 60.0, star2 = 40.0, star3 = 25.0)
}
