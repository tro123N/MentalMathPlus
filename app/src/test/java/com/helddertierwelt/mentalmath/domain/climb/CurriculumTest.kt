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
import com.helddertierwelt.mentalmath.domain.climb.engine.Planned
import com.helddertierwelt.mentalmath.domain.climb.engine.Session
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionLogic
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.domain.climb.math.AnswerCheck
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.random.Random

/**
 * Prüft alle Aufgaben-Generatoren: Jede erzeugte Aufgabe muss rechnerisch stimmen,
 * jeder Zwischenschritt muss zu seiner Beschriftung passen und die richtige Antwort
 * muss von der Antwortprüfung akzeptiert werden.
 */
class CurriculumTest {

    private var evaluatedSteps = 0
    private var totalSteps = 0
    private var evaluatedPrompts = 0
    private var totalPrompts = 0

    private val minus = Fmt.MINUS
    private val times = Fmt.TIMES
    private val divide = Fmt.DIVIDE

    /** Zahl im deutschen Format lesen: Tausenderpunkte weg, Komma → Punkt. */
    private fun num(text: String): BigDecimal? {
        val t = text.trim().replace(".", "").replace(',', '.')
        if (t.isEmpty() || !t.all { it.isDigit() || it == '.' }) return null
        return t.toBigDecimalOrNull()
    }

    private val numberPattern = "[0-9][0-9.]*(?:,[0-9]+)?"
    private val binary = Regex("^($numberPattern) ([+$minus$times$divide]) ($numberPattern)$")
    private val square = Regex("^($numberPattern)²$")
    private val cube = Regex("^($numberPattern)³$")

    /** Rechnet eine einfache Beschriftung wie „47 × 6“ aus; null, wenn sie nicht so aufgebaut ist. */
    private fun evaluate(label: String): BigDecimal? {
        binary.matchEntire(label)?.let { m ->
            val a = num(m.groupValues[1]) ?: return null
            val b = num(m.groupValues[3]) ?: return null
            return when (m.groupValues[2]) {
                "+" -> a + b
                minus -> a - b
                times -> a * b
                divide -> a.divide(b, MathContext.DECIMAL128)
                else -> null
            }
        }
        square.matchEntire(label)?.let { m -> return num(m.groupValues[1])?.let { it * it } }
        cube.matchEntire(label)?.let { m -> return num(m.groupValues[1])?.let { it * it * it } }
        return null
    }

    /** Wert, den die Aufgabenstellung selbst ergibt (für bekannte Formen). */
    private fun evaluatePrompt(p: Problem): BigDecimal? {
        val text = p.prompt.text
        evaluate(text)?.let { return it }
        Regex("^Das Doppelte von ($numberPattern)$").matchEntire(text)?.let { return num(it.groupValues[1])!! * BigDecimal(2) }
        Regex("^Die Hälfte von ($numberPattern)$").matchEntire(text)?.let { return num(it.groupValues[1])!!.divide(BigDecimal(2)) }
        Regex("^([0-9]+) % von ($numberPattern)$").matchEntire(text)?.let {
            return num(it.groupValues[1])!!.multiply(num(it.groupValues[2])!!).divide(BigDecimal(100))
        }
        Regex("^($numberPattern) € ([+$minus]) ($numberPattern) €$").matchEntire(text)?.let {
            val a = num(it.groupValues[1])!!
            val b = num(it.groupValues[3])!!
            return if (it.groupValues[2] == "+") a + b else a - b
        }
        Regex("^($numberPattern) \\+ \\?$").matchEntire(text)?.let { m ->
            val target = p.prompt.caption?.let { c -> Regex("($numberPattern)$").find(c)?.groupValues?.get(1) }?.let { num(it) }
            if (target != null) return target - num(m.groupValues[1])!!
        }
        return null
    }

    private fun rawInput(e: Expected): String = when (e) {
        is Expected.Exact -> e.value.stripTrailingZeros().toPlainString().replace('.', ',')
        is Expected.Approx -> e.value.stripTrailingZeros().toPlainString().replace('.', ',')
        is Expected.Remainder -> "${e.quotient}R${e.remainder}"
        is Expected.Frac -> "${e.numerator}/${e.denominator}"
        is Expected.Choice -> e.index.toString()
        is Expected.Digits -> e.digits
    }

    private fun value(e: Expected): BigDecimal? = when (e) {
        is Expected.Exact -> e.value
        is Expected.Approx -> e.value
        else -> null
    }

    /** So würde man die Antwort auf dem Ziffernblock eintippen (null bei Auswahl). */
    private fun typed(e: Expected): String? = when (e) {
        is Expected.Exact -> e.value.stripTrailingZeros().toPlainString().replace('.', ',')
        is Expected.Approx -> e.value.setScale(maxOf(0, e.tolerance.scale()), RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')
        is Expected.Remainder -> if (e.remainder == 0L) "${e.quotient}" else "${e.quotient}R${e.remainder}"
        is Expected.Frac -> "${e.numerator}/${e.denominator}"
        is Expected.Choice -> null
        is Expected.Digits -> e.digits
    }

    private fun kindFits(e: Expected, kind: InputKind): Boolean = when (e) {
        is Expected.Exact, is Expected.Approx -> kind == InputKind.INTEGER || kind == InputKind.DECIMAL
        is Expected.Remainder -> kind == InputKind.REMAINDER
        is Expected.Frac -> kind == InputKind.FRACTION
        is Expected.Choice -> kind == InputKind.CHOICE
        is Expected.Digits -> kind == InputKind.DIGITS
    }

    /** Tippt die richtige Antwort wie auf dem Ziffernblock ein und prüft, dass sie ankommt und akzeptiert wird. */
    private fun checkTypeable(p: Problem, context: String) {
        fun typeInto(start: Session, expected: Expected, label: String) {
            val keys = typed(expected) ?: return
            var s = start
            keys.forEach { c -> s = SessionLogic.type(s, c).first }
            assertEquals("$context: „$label“ lässt sich nicht eintippen ($keys)", keys, s.input)
            assertTrue("$context: „$label“ eingetippt als $keys wird abgelehnt", AnswerCheck.check(expected, s.input).correct)
        }
        assertTrue("$context: Eingabeart ${p.input} passt nicht zur Antwort", kindFits(p.expected, p.input))
        val practice = Session(type = SessionType.PRACTICE, skillId = p.skillId, title = "", problems = listOf(Planned(p)))
        typeInto(practice, p.expected, "Ergebnis")
        val guided = Session(type = SessionType.GUIDED, skillId = p.skillId, title = "", problems = listOf(Planned(p)))
        p.steps.forEachIndexed { i, step ->
            assertTrue("$context: Schritt „${step.label}“ hat Eingabeart ${step.input}, passt nicht zur Antwort", kindFits(step.expected, step.input))
            if (step.expected is Expected.Choice) {
                assertTrue("$context: Auswahlschritt ohne Auswahlmöglichkeiten", (step.expected as Expected.Choice).index in p.choices.indices)
            }
            typeInto(guided.copy(stepIndex = i), step.expected, step.label)
        }
    }

    private fun checkProblem(p: Problem, context: String) {
        checkTypeable(p, context)
        assertTrue("$context: keine Schritte", p.steps.isNotEmpty())
        val verdict = AnswerCheck.check(p.expected, rawInput(p.expected))
        assertTrue("$context: richtige Antwort ${rawInput(p.expected)} wird abgelehnt", verdict.correct)

        val last = p.steps.last()
        val pv = value(p.expected)
        val lv = value(last.expected)
        if (pv != null && lv != null) {
            assertEquals("$context: letzter Schritt ≠ Ergebnis", 0, pv.compareTo(lv))
        } else {
            assertEquals("$context: letzter Schritt ≠ Ergebnis", p.expected, last.expected)
        }

        totalPrompts++
        evaluatePrompt(p)?.let { promptValue ->
            evaluatedPrompts++
            if (pv != null) {
                assertEquals("$context: Aufgabe „${p.prompt.text}“ ergibt $promptValue, erwartet ${pv}", 0, promptValue.compareTo(pv))
            }
        }

        p.steps.forEachIndexed { i, s ->
            val sv = value(s.expected)
            assertTrue("$context: Schritt ${i + 1} „${s.label}“ wird mit richtiger Antwort abgelehnt", AnswerCheck.check(s.expected, rawInput(s.expected)).correct)
            if (sv != null) {
                assertTrue("$context: negativer Zwischenschritt „${s.label}“", sv.signum() >= 0)
                if (s.input == InputKind.INTEGER) {
                    assertTrue("$context: Ganzzahl-Schritt „${s.label}“ hat Nachkommastellen ($sv)", sv.stripTrailingZeros().scale() <= 0)
                }
                totalSteps++
                evaluate(s.label)?.let { lv2 ->
                    evaluatedSteps++
                    assertEquals("$context: Schritt „${s.label}“ = $sv, gerechnet $lv2", 0, lv2.compareTo(sv))
                }
            }
        }
        if (p.input == InputKind.INTEGER && pv != null) {
            assertTrue("$context: Ganzzahl-Aufgabe mit Nachkommastellen", pv.stripTrailingZeros().scale() <= 0)
        }
        if (p.input == InputKind.CHOICE) {
            val idx = (p.expected as Expected.Choice).index
            assertTrue("$context: Auswahlindex außerhalb", idx in p.choices.indices)
        }
    }

    @Test
    fun skillIdsAreUnique() {
        val ids = Curriculum.skills.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(Curriculum.worlds.all { it.skills.isNotEmpty() })
        val titles = Curriculum.skills.map { it.title } + Curriculum.worlds.map { it.title }
        val duplicates = titles.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("Doppelte Titel: $duplicates", duplicates.isEmpty())
        Curriculum.worlds.forEach { w -> w.skills.forEach { assertTrue("${it.id} gehört nicht zu ${w.id}", it.id.startsWith(w.id + ".")) } }
    }

    @Test
    fun everyGeneratedProblemIsCorrect() {
        for (skill in Curriculum.skills) {
            val random = Random(skill.id.hashCode().toLong())
            repeat(400) { i ->
                val p = try {
                    skill.generate(random)
                } catch (e: Exception) {
                    fail("${skill.id} #$i: Generator wirft ${e::class.simpleName}: ${e.message}")
                    return
                }
                assertEquals("${skill.id}: falsche skillId", skill.id, p.skillId)
                checkProblem(p, "${skill.id} #$i (${p.prompt.text})")
            }
        }
        println("Geprüfte Schritte: $evaluatedSteps von $totalSteps, Aufgabenstellungen: $evaluatedPrompts von $totalPrompts")
        assertTrue("Zu wenige Schritte nachgerechnet", evaluatedSteps * 2 > totalSteps)
        assertTrue("Zu wenige Aufgaben nachgerechnet", evaluatedPrompts * 10 > totalPrompts * 8)
    }

    @Test
    fun explanationExamplesAreCorrect() {
        for (skill in Curriculum.skills) {
            skill.explanation.filterIsInstance<Block.Example>().forEach { ex ->
                checkProblem(ex.problem, "${skill.id} Beispiel (${ex.problem.prompt.text})")
            }
        }
    }

    @Test
    fun generatorsProduceVariety() {
        for (skill in Curriculum.skills) {
            val random = Random(42)
            val keys = (1..60).map { skill.generate(random).key }.toSet()
            assertTrue("${skill.id}: zu wenig verschiedene Aufgaben (${keys.size})", keys.size >= 6)
        }
    }

    @Test
    fun goalsAreConsistent() {
        for (skill in Curriculum.skills) {
            val g = skill.goals
            assertTrue("${skill.id}: Sternzeiten nicht absteigend", g.star1 > g.star2 && g.star2 > g.star3)
            assertTrue("${skill.id}: Zeitlimit zu knapp", g.timeLimit >= g.star1)
            assertTrue("${skill.id}: Prüfung zu kurz", g.testSize >= 5)
        }
    }
}
