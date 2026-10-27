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

import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.math.Num
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.BookRef
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.Goals
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import com.helddertierwelt.mentalmath.domain.climb.model.World
import java.math.BigDecimal
import kotlin.random.Random

/** Welt 9 – Rechnen mit Papier und Stift, Proben (6. Kapitel). */
internal object World9 {

    private const val CH = 6
    private val yesNo = listOf("Ja", "Nein")

    private val placeNames = listOf("Einer", "Zehner", "Hunderter", "Tausender", "Zehntausender", "Hunderttausender")
    private val centPlaceNames = listOf("Hundertstel", "Zehntel", "Einer", "Zehner", "Hunderter", "Tausender")

    private fun digitAt(x: Long, k: Int): Long = (x / Num.pow10(k)) % 10

    /** Zahlen für eine Kolonne gleich breit machen (rechtsbündig in Monospace). */
    private fun column(texts: List<String>): List<String> {
        val width = texts.maxOf { it.length }
        return texts.map { it.padStart(width) }
    }

    // --- Zahlenkolonnen ----------------------------------------------------------------------

    /** Kolonne addieren; bei [cents] sind die Zahlen Cent-Beträge (Ergebnis in Euro). */
    fun columnSum(numbers: List<Long>, cents: Boolean): Problem {
        val total = numbers.sum()
        val names = if (cents) centPlaceNames else placeNames
        val maxLen = numbers.maxOf { it.toString().length }
        val steps = mutableListOf<Step>()
        var carry = 0L
        for (col in 0 until maxLen) {
            val digits = numbers.filter { it.toString().length > col }.map { digitAt(it, col) }.filter { it != 0L }.ifEmpty { listOf(0L) }
            val terms = (if (carry > 0) listOf(carry) else emptyList()) + digits
            val sum = terms.sum()
            val partials = terms.runningReduce { a, b -> a + b }.joinToString(" … ") { n(it) }
            val last = col == maxLen - 1
            val hint = if (last) "Teilsummen: $partials – ${n(sum)} hinschreiben, fertig." else "Teilsummen: $partials – ${sum % 10} hinschreiben, ${sum / 10} merken."
            steps += step("${names[col]}: ${terms.joinToString(" + ")}", sum, hint)
            carry = sum / 10
        }
        val texts = if (cents) numbers.map { d(BigDecimal.valueOf(it, 2), 2) } else numbers.map { n(it) }
        val prompt = Prompt(
            text = "Summe?",
            caption = "Spalte für Spalte von rechts nach links",
            lines = column(texts),
            unit = if (cents) "€" else null,
        )
        return if (cents) {
            val value = BigDecimal.valueOf(total, 2)
            steps += decStep("Ergebnis", value, 2, "Die hingeschriebenen Ziffern ergeben ${d(value, 2)}.")
            decProblem("w9.columns", "c" + numbers.joinToString(","), prompt, value, 2, steps, "Spaltenweise mit Teilsummen")
        } else {
            steps += step("Ergebnis", total, "Die hingeschriebenen Ziffern ergeben ${n(total)}.")
            intProblem("w9.columns", numbers.joinToString(","), prompt, total, steps, "Spaltenweise mit Teilsummen")
        }
    }

    private val columns = Skill(
        id = "w9.columns",
        title = "Zahlenkolonnen addieren",
        method = "Spaltenweise mit Teilsummen",
        book = BookRef(CH, "Zahlenkolonnen"),
        summary = "Lange Listen von Zahlen und Beträgen sicher zusammenzählen.",
        explanation = listOf(
            Block.Text("Für lange Zahlenkolonnen ist die Schulmethode genau richtig: Spalte für Spalte von rechts nach links. Der Kniff ist, nur die Teilsummen zu „hören“ – also nicht „8 plus 4 ist 12, plus 7 ist 19“, sondern nur „8 … 12 … 19“. Das geht viel schneller."),
            Block.Steps(
                listOf(
                    "Die Einerspalte von oben nach unten addieren, nur die Teilsummen denken.",
                    "Die Einerziffer der Summe hinschreiben, den Rest als Übertrag in die nächste Spalte mitnehmen.",
                    "So weiter bis zur letzten Spalte.",
                    "Zur Kontrolle von unten nach oben addieren: Dann macht man denselben Fehler kaum zweimal.",
                ),
            ),
            Block.Example(columnSum(listOf(3_917, 586, 2_045, 7_368, 459), cents = false)),
            Block.Tip("Bei Euro-Beträgen funktioniert es genauso – die Cent sind einfach die beiden letzten Spalten."),
        ),
        goals = Goals(testSize = 5, minAccuracy = 0.8, star1 = 45.0, star2 = 30.0, star3 = 20.0),
        guidedCount = 3,
        practiceCount = 6,
        generator = { r ->
            if (r.nextInt(3) == 0) {
                columnSum(List(r.between(4, 6)) { r.betweenL(105, 9_999) }, cents = true)
            } else {
                columnSum(List(r.between(4, 6)) { if (r.nextInt(4) == 0) r.betweenL(102, 999) else r.betweenL(1_002, 9_999) }, cents = false)
            }
        },
    )

    // --- Mod-Summen --------------------------------------------------------------------------

    /** Schritte zur Mod-Summe: Quersummen bilden, bis eine Ziffer übrig bleibt. */
    fun modSumSteps(x: Long, first: Boolean = true): List<Step> {
        val steps = mutableListOf<Step>()
        var v = x
        var i = 0
        while (v >= 10) {
            val digits = Num.digits(v)
            val sum = digits.sum().toLong()
            val hint = if (i == 0 && first) "Alle Ziffern addieren." else if (sum < 10) null else "Noch einmal die Ziffern addieren."
            steps += step(digits.joinToString(" + "), sum, hint)
            v = sum
            i++
        }
        return steps
    }

    fun modSum(x: Long): Problem {
        val steps = modSumSteps(x).toMutableList()
        steps += step("Mod-Summe von ${n(x)}", Num.modSum(x).toLong(), "Die letzte einstellige Zahl ist die Mod-Summe.")
        return intProblem(
            skillId = "w9.modsum",
            key = "m$x",
            prompt = Prompt("Mod-Summe von ${n(x)}", caption = "Quersumme, bis eine Ziffer übrig bleibt"),
            value = Num.modSum(x).toLong(),
            steps = steps,
            method = "Quersumme der Quersumme",
        )
    }

    private val modSumSkill = Skill(
        id = "w9.modsum",
        title = "Mod-Summen bilden",
        method = "Quersumme der Quersumme",
        book = BookRef(CH, "Mod-Summen"),
        summary = "Die Mod-Summe einer Zahl blitzschnell bestimmen – die Grundlage der 9er-Probe.",
        explanation = listOf(
            Block.Text("Die Mod-Summe einer Zahl bekommst du, indem du ihre Ziffern addierst – und mit dem Ergebnis so lange weitermachst, bis nur noch eine Ziffer übrig ist. Sie ist nichts anderes als der Rest beim Teilen durch 9, nur dass statt 0 eine 9 herauskommt."),
            Block.Steps(
                listOf(
                    "Ziffern von 7.586 addieren: 7 + 5 + 8 + 6 = 26.",
                    "Noch einmal: 2 + 6 = 8.",
                    "Die Mod-Summe von 7.586 ist 8.",
                ),
            ),
            Block.Example(modSum(7_586)),
            Block.Tip("Abkürzung: Neunen und Ziffern, die zusammen 9 ergeben, darfst du gleich weglassen. Bei 7.586 fällt nichts weg, aber bei 4.959 bleibt nur 4 + 5 = 9."),
        ),
        goals = Goals(testSize = 10, star1 = 9.0, star2 = 6.0, star3 = 4.0),
        generator = { r ->
            val x = when (r.nextInt(3)) {
                0 -> r.betweenL(100, 999)
                1 -> r.betweenL(1_000, 99_999)
                else -> r.betweenL(100_000, 9_999_999)
            }
            modSum(x)
        },
    )

    // --- 9er-Probe ---------------------------------------------------------------------------

    enum class Op { ADD, SUB, MUL }

    /** Eine Ziffer der Zahl so verändern, dass die Probe den Fehler sicher findet. */
    private fun corrupt(x: Long, r: Random): Long {
        val digits = Num.digits(x).toMutableList()
        while (true) {
            val i = r.nextInt(digits.size)
            val delta = r.pick(listOf(-3, -2, -1, 1, 2, 3))
            val nd = digits[i] + delta
            if (nd !in 0..9 || (i == 0 && nd == 0)) continue
            digits[i] = nd
            return digits.joinToString("").toLong()
        }
    }

    private fun calcLines(operands: List<Long>, op: Op): List<String> = when (op) {
        Op.ADD -> column(operands.mapIndexed { i, x -> (if (i == 0) "" else "+ ") + n(x) })
        Op.SUB -> column(listOf(n(operands[0]), "$MINUS ${n(operands[1])}"))
        Op.MUL -> listOf("${n(operands[0])} $X ${n(operands[1])}")
    }

    private fun exact(operands: List<Long>, op: Op): Long = when (op) {
        Op.ADD -> operands.sum()
        Op.SUB -> operands[0] - operands[1]
        Op.MUL -> operands[0] * operands[1]
    }

    fun nineCheck(operands: List<Long>, op: Op, claimed: Long): Problem {
        val ms = operands.map { Num.modSum(it).toLong() }
        val steps = mutableListOf<Step>()
        operands.forEachIndexed { i, x ->
            steps += step("Mod-Summe von ${n(x)}", ms[i], if (i == 0) "Von jeder Zahl die Mod-Summe bilden." else null)
        }
        val combined: Long
        when (op) {
            Op.ADD -> {
                val s = ms.sum()
                steps += step(ms.joinToString(" + "), s, "Die Mod-Summen genauso addieren wie die Zahlen.")
                combined = Num.modSum(s).toLong()
                if (s >= 10) steps += step("Mod-Summe von ${n(s)}", combined)
            }

            Op.SUB -> {
                if (ms[0] > ms[1]) {
                    combined = ms[0] - ms[1]
                    steps += step("${ms[0]} $MINUS ${ms[1]}", combined, "Die Mod-Summen genauso abziehen wie die Zahlen.")
                } else {
                    combined = ms[0] + 9 - ms[1]
                    steps += step("${ms[0]} + 9 $MINUS ${ms[1]}", combined, "Die Mod-Summen abziehen – ist das Ergebnis 0 oder kleiner, 9 dazuzählen.")
                }
            }

            Op.MUL -> {
                val p = ms[0] * ms[1]
                steps += step("${ms[0]} $X ${ms[1]}", p, "Die Mod-Summen genauso malnehmen wie die Zahlen.")
                combined = Num.modSum(p).toLong()
                if (p >= 10) steps += step("Mod-Summe von ${n(p)}", combined)
            }
        }
        val msClaimed = Num.modSum(claimed).toLong()
        steps += step("Mod-Summe von ${n(claimed)}", msClaimed, "Jetzt die Mod-Summe des angeblichen Ergebnisses.")
        val passes = msClaimed == combined
        steps += Step(
            "Besteht die Rechnung die 9er-Probe?",
            Expected.Choice(if (passes) 0 else 1),
            InputKind.CHOICE,
            if (passes) "Beide Mod-Summen sind $combined – die Probe passt." else "$combined und $msClaimed sind verschieden – da steckt ein Fehler drin.",
        )
        return Problem(
            skillId = "w9.check9",
            key = "9|${operands.joinToString(",")}|$op|$claimed",
            prompt = Prompt("= ${n(claimed)}", caption = "Besteht die Rechnung die 9er-Probe?", lines = calcLines(operands, op)),
            expected = Expected.Choice(if (passes) 0 else 1),
            input = InputKind.CHOICE,
            steps = steps,
            method = "Mod-Summen vergleichen",
            choices = yesNo,
        )
    }

    private fun calculation(r: Random): Pair<List<Long>, Op> = when (r.nextInt(3)) {
        0 -> List(r.between(2, 3)) { r.betweenL(1_000, 9_999) } to Op.ADD
        1 -> {
            val a = r.betweenL(3_000, 99_999)
            listOf(a, r.betweenL(1_000, a - 100)) to Op.SUB
        }
        else -> listOf(r.betweenL(102, 999), r.betweenL(12, 999)) to Op.MUL
    }

    private val check9 = Skill(
        id = "w9.check9",
        title = "Die 9er-Probe",
        method = "Mod-Summen vergleichen",
        book = BookRef(CH, "Mod-Summen"),
        summary = "Rechnungen in Sekunden auf Fehler prüfen.",
        explanation = listOf(
            Block.Text("Mit Mod-Summen kannst du jede Addition, Subtraktion und Multiplikation überprüfen: Du rechnest dieselbe Aufgabe noch einmal mit den Mod-Summen. Stimmt das Ergebnis nicht mit der Mod-Summe der Lösung überein, ist die Lösung sicher falsch. Stimmt es überein, ist sie sehr wahrscheinlich richtig – die Probe findet 8 von 9 Fehlern."),
            Block.Steps(
                listOf(
                    "Von jeder Zahl der Aufgabe die Mod-Summe bilden.",
                    "Mit den Mod-Summen genauso rechnen wie mit den Zahlen (plus, minus oder mal) und wieder die Mod-Summe bilden.",
                    "Beim Abziehen gilt: Kommt 0 oder weniger heraus, 9 dazuzählen.",
                    "Mit der Mod-Summe der Lösung vergleichen.",
                ),
            ),
            Block.Example(nineCheck(listOf(4_726, 3_158), Op.ADD, 7_884)),
            Block.Example(nineCheck(listOf(624, 87), Op.MUL, 54_288)),
            Block.Tip("Eine Fehlerart rutscht immer durch: vertauschte Ziffern. 7.884 und 7.848 haben dieselbe Mod-Summe."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 30.0, star2 = 20.0, star3 = 12.0),
        guidedCount = 4,
        generator = { r ->
            val (operands, op) = calculation(r)
            val correct = exact(operands, op)
            nineCheck(operands, op, if (r.nextBoolean()) correct else corrupt(correct, r))
        },
    )

    // --- 11er-Probe --------------------------------------------------------------------------

    /** Schritte zum 11er-Rest: Ziffern von rechts abwechselnd addieren und abziehen. */
    fun rest11Steps(x: Long): List<Step> {
        val digits = Num.digits(x).reversed()
        val plus = digits.filterIndexed { i, _ -> i % 2 == 0 }.map { it.toLong() }
        val minusDigits = digits.filterIndexed { i, _ -> i % 2 == 1 }.map { it.toLong() }
        val steps = mutableListOf<Step>()
        val a = plus.sum()
        val b = minusDigits.sum()
        if (plus.size > 1) {
            steps += step(plus.joinToString(" + "), a, "Von rechts gezählt: 1., 3., 5. … Ziffer addieren.")
        }
        if (minusDigits.size > 1) {
            steps += step(minusDigits.joinToString(" + "), b, "Dann die 2., 4., 6. … Ziffer addieren – diese werden abgezogen.")
        }
        val rest = x % 11
        when {
            minusDigits.isEmpty() -> Unit
            a >= b -> {
                val diff = a - b
                steps += step("${n(a)} $MINUS ${n(b)}", diff, "Abziehen.")
                var v = diff
                while (v >= 11) {
                    steps += step("${n(v)} $MINUS 11", v - 11, "Größer als 10: 11 abziehen.")
                    v -= 11
                }
            }

            else -> {
                val k = (b - a + 10) / 11
                steps += step("${n(a)} + ${n(11 * k)} $MINUS ${n(b)}", rest, "${n(a)} $MINUS ${n(b)} wäre negativ – so oft 11 dazuzählen, bis es passt.")
            }
        }
        return steps
    }

    fun rest11(x: Long): Problem {
        val steps = rest11Steps(x).toMutableList()
        steps += step("11er-Rest von ${n(x)}", x % 11, "Der Rest beim Teilen durch 11.")
        return intProblem(
            skillId = "w9.rest11",
            key = "e$x",
            prompt = Prompt("11er-Rest von ${n(x)}", caption = "Ziffern von rechts: plus, minus, plus, …"),
            value = x % 11,
            steps = steps,
            method = "Abwechselnde Quersumme",
        )
    }

    private val rest11Skill = Skill(
        id = "w9.rest11",
        title = "Den 11er-Rest bestimmen",
        method = "Abwechselnde Quersumme",
        book = BookRef(CH, "11er-Probe"),
        summary = "Den Rest beim Teilen durch 11 ohne Division finden.",
        explanation = listOf(
            Block.Text("Für die 11er-Probe brauchst du den Rest einer Zahl beim Teilen durch 11. Dazu gehst du die Ziffern von rechts nach links durch und rechnest abwechselnd plus und minus. Ist das Ergebnis negativ, zählst du 11 dazu; ist es 11 oder mehr, ziehst du 11 ab."),
            Block.Steps(
                listOf(
                    "Bei 52.817 von rechts: 7 − 1 + 8 − 2 + 5.",
                    "Einfacher in zwei Gruppen: 7 + 8 + 5 = 20 und 1 + 2 = 3.",
                    "20 − 3 = 17, das ist mehr als 10, also 17 − 11 = 6.",
                    "Der 11er-Rest von 52.817 ist 6.",
                ),
            ),
            Block.Example(rest11(52_817)),
            Block.Tip("Wichtig ist die Richtung: Die Einerziffer wird immer addiert. Von links angefangen klappt es nur, wenn die Zahl eine ungerade Anzahl von Ziffern hat."),
        ),
        goals = Goals(testSize = 10, star1 = 12.0, star2 = 8.0, star3 = 5.0),
        generator = { r -> rest11(if (r.nextBoolean()) r.betweenL(100, 9_999) else r.betweenL(10_000, 999_999)) },
    )

    fun elevenCheck(operands: List<Long>, op: Op, claimed: Long): Problem {
        val rs = operands.map { it % 11 }
        val steps = mutableListOf<Step>()
        operands.forEachIndexed { i, x ->
            steps += step("11er-Rest von ${n(x)}", rs[i], if (i == 0) "Von jeder Zahl den 11er-Rest bilden (von rechts: plus, minus, plus …)." else null)
        }
        val combined: Long
        when (op) {
            Op.ADD -> {
                val s = rs.sum()
                steps += step(rs.joinToString(" + "), s, "Die Reste genauso addieren wie die Zahlen.")
                combined = s % 11
                if (s >= 11) steps += step("11er-Rest von ${n(s)}", combined)
            }

            Op.SUB -> {
                if (rs[0] >= rs[1]) {
                    combined = rs[0] - rs[1]
                    steps += step("${rs[0]} $MINUS ${rs[1]}", combined, "Die Reste genauso abziehen wie die Zahlen.")
                } else {
                    combined = rs[0] + 11 - rs[1]
                    steps += step("${rs[0]} + 11 $MINUS ${rs[1]}", combined, "Negativ – also 11 dazuzählen.")
                }
            }

            Op.MUL -> {
                val p = rs[0] * rs[1]
                steps += step("${rs[0]} $X ${rs[1]}", p, "Die Reste genauso malnehmen wie die Zahlen.")
                combined = p % 11
                if (p >= 11) steps += step("11er-Rest von ${n(p)}", combined)
            }
        }
        val rc = claimed % 11
        steps += step("11er-Rest von ${n(claimed)}", rc, "Jetzt der 11er-Rest des angeblichen Ergebnisses.")
        val passes = rc == combined
        steps += Step(
            "Besteht die Rechnung die 11er-Probe?",
            Expected.Choice(if (passes) 0 else 1),
            InputKind.CHOICE,
            if (passes) "Beide Reste sind $combined – die Probe passt." else "$combined und $rc sind verschieden – da steckt ein Fehler drin.",
        )
        return Problem(
            skillId = "w9.check11",
            key = "11|${operands.joinToString(",")}|$op|$claimed",
            prompt = Prompt("= ${n(claimed)}", caption = "Besteht die Rechnung die 11er-Probe?", lines = calcLines(operands, op)),
            expected = Expected.Choice(if (passes) 0 else 1),
            input = InputKind.CHOICE,
            steps = steps,
            method = "11er-Reste vergleichen",
            choices = yesNo,
        )
    }

    private val check11 = Skill(
        id = "w9.check11",
        title = "Die 11er-Probe",
        method = "11er-Reste vergleichen",
        book = BookRef(CH, "11er-Probe"),
        summary = "Eine zweite Probe, die auch vertauschte Ziffern entdeckt.",
        explanation = listOf(
            Block.Text("Die 11er-Probe funktioniert genau wie die 9er-Probe, nur mit den 11er-Resten statt der Mod-Summen. Sie findet 10 von 11 Fehlern – auch vertauschte Ziffern, die der 9er-Probe durchrutschen. Wer beide Proben macht, übersieht nur noch etwa einen von 99 Fehlern."),
            Block.Steps(
                listOf(
                    "Von jeder Zahl der Aufgabe den 11er-Rest bestimmen.",
                    "Mit den Resten genauso rechnen wie mit den Zahlen; negative Ergebnisse +11, große Ergebnisse wieder auf den 11er-Rest bringen.",
                    "Mit dem 11er-Rest der Lösung vergleichen.",
                ),
            ),
            Block.Example(elevenCheck(listOf(5_384, 2_917), Op.SUB, 2_467)),
            Block.Tip("Vertauschte Ziffern wie 2.467 statt 2.476 ändern den 11er-Rest – deshalb ist diese Probe eine gute Ergänzung."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 35.0, star2 = 22.0, star3 = 14.0),
        guidedCount = 4,
        generator = { r ->
            val (operands, op) = calculation(r)
            val correct = exact(operands, op)
            elevenCheck(operands, op, if (r.nextBoolean()) correct else corrupt(correct, r))
        },
    )

    // --- Kreuzmultiplikation -----------------------------------------------------------------

    fun crossMultiply(skillId: String, a: Long, b: Long): Problem {
        val da = Num.digits(a).reversed()
        val db = Num.digits(b).reversed()
        val columns = da.size + db.size - 1
        val steps = mutableListOf<Step>()
        var carry = 0L
        for (k in 0 until columns) {
            val products = mutableListOf<String>()
            var sum = carry
            for (i in da.indices.reversed()) {
                val j = k - i
                if (j !in db.indices) continue
                if (da[i] == 0 || db[j] == 0) continue
                products += "${da[i]} $X ${db[j]}"
                sum += da[i].toLong() * db[j]
            }
            val terms = (if (carry > 0) listOf(n(carry)) else emptyList()) + products
            val label = "${placeNames[k]}: ${terms.ifEmpty { listOf("0") }.joinToString(" + ")}"
            val last = k == columns - 1
            val hint = when {
                last -> "${n(sum)} vorne hinschreiben – fertig."
                k == 0 -> "Einer mal Einer: ${sum % 10} hinschreiben, ${sum / 10} merken."
                else -> "Alle Paare, die zusammen diese Stelle ergeben: ${sum % 10} hinschreiben, ${sum / 10} merken."
            }
            steps += step(label, sum, hint)
            carry = sum / 10
        }
        steps += step("Ergebnis", a * b, "Die Ziffern stehen von links nach rechts in einer Zeile.")
        return intProblem(
            skillId = skillId,
            key = "$a*$b",
            prompt = Prompt("${n(a)} $X ${n(b)}", caption = "Kreuzmultiplikation – Ergebnis in einer Zeile"),
            value = a * b,
            steps = steps,
            method = "Kreuzmultiplikation",
        )
    }

    private fun noZero(x: Long) = !x.toString().contains('0')

    private val cross2 = Skill(
        id = "w9.cross2",
        title = "Kreuzmultiplikation: zweistellig",
        method = "Über Kreuz malnehmen",
        book = BookRef(CH, "Multiplikation auf Papier"),
        summary = "Das Ergebnis direkt in einer Zeile hinschreiben – ohne Zwischenrechnungen.",
        explanation = listOf(
            Block.Text("Mit der Kreuzmultiplikation schreibst du das Ergebnis einer Malaufgabe Ziffer für Ziffer von rechts nach links hin, ohne Zwischenergebnisse zu notieren. Für jede Stelle multiplizierst du die Ziffernpaare, die zusammen genau diese Stelle ergeben."),
            Block.Steps(
                listOf(
                    "58 × 36 – Einer: 8 × 6 = 48. Schreibe 8, merke 4.",
                    "Zehner: über Kreuz 5 × 6 und 8 × 3, plus Übertrag: 4 + 30 + 24 = 58. Schreibe 8, merke 5.",
                    "Hunderter: 5 + 5 × 3 = 20. Schreibe 20.",
                    "Das Ergebnis steht da: 2.088.",
                ),
            ),
            Block.Example(crossMultiply("w9.cross2", 58, 36)),
            Block.Tip("Bei einer dreistelligen mal einer zweistelligen Zahl denkst du dir vor die zweistellige eine 0 – alle Produkte mit 0 fallen einfach weg."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 20.0, star2 = 13.0, star3 = 8.0),
        generator = { r ->
            var a: Long
            var b: Long
            do {
                a = if (r.nextInt(3) == 0) r.betweenL(111, 999) else r.betweenL(11, 99)
                b = r.betweenL(11, 99)
            } while (!noZero(a) || !noZero(b))
            crossMultiply("w9.cross2", a, b)
        },
    )

    private val cross3 = Skill(
        id = "w9.cross3",
        title = "Kreuzmultiplikation: dreistellig",
        method = "Über Kreuz: 1, 2, 3, 2, 1 Produkte",
        book = BookRef(CH, "Multiplikation auf Papier"),
        summary = "Zwei dreistellige Zahlen in einer Zeile multiplizieren.",
        explanation = listOf(
            Block.Text("Bei dreistelligen Zahlen gibt es fünf Stellen zu berechnen. Die Zahl der Produkte pro Stelle wächst erst und schrumpft dann wieder: 1, 2, 3, 2, 1. Hinter der Methode steckt nur das Ausmultiplizieren der Stellenwerte."),
            Block.Steps(
                listOf(
                    "427 × 356 – Einer: 7 × 6 = 42. Schreibe 2, merke 4.",
                    "Zehner: 4 + 2 × 6 + 7 × 5 = 51. Schreibe 1, merke 5.",
                    "Hunderter: 5 + 4 × 6 + 2 × 5 + 7 × 3 = 60. Schreibe 0, merke 6.",
                    "Tausender: 6 + 4 × 5 + 2 × 3 = 32. Schreibe 2, merke 3.",
                    "Zehntausender: 3 + 4 × 3 = 15. Ergebnis: 152.012.",
                ),
            ),
            Block.Example(crossMultiply("w9.cross3", 427, 356)),
            Block.Tip("Mit der 9er- oder 11er-Probe kannst du das Ergebnis danach in Sekunden kontrollieren."),
        ),
        goals = Goals(testSize = 5, minAccuracy = 0.8, star1 = 60.0, star2 = 40.0, star3 = 25.0),
        guidedCount = 3,
        practiceCount = 6,
        generator = { r ->
            var a: Long
            var b: Long
            do {
                a = r.betweenL(111, 999)
                b = r.betweenL(111, 999)
            } while (!noZero(a) || !noZero(b))
            crossMultiply("w9.cross3", a, b)
        },
    )

    // --- Wurzeln mit Zettel und Stift --------------------------------------------------------

    /** Wurzel Ziffer für Ziffer; [decimals] Nachkommastellen (bei Quadratzahlen 0). */
    fun paperRoot(x: Long, decimals: Int): Problem {
        val text = x.toString()
        val groups = mutableListOf<Long>()
        var rest = text
        if (rest.length % 2 == 1) {
            groups += rest.substring(0, 1).toLong()
            rest = rest.substring(1)
        }
        rest.chunked(2).forEach { groups += it.toLong() }
        repeat(decimals) { groups += 0L }
        val integerGroups = groups.size - decimals

        val steps = mutableListOf<Step>()
        val first = groups[0]
        var digit = 0L
        while ((digit + 1) * (digit + 1) <= first) digit++
        steps += step(
            "Erste Ziffer: größte Zahl mit Quadrat bis ${n(first)}",
            digit,
            "${digit} × ${digit} = ${n(digit * digit)} passt in ${n(first)}, ${digit + 1} × ${digit + 1} = ${n((digit + 1) * (digit + 1))} nicht mehr.",
        )
        var remainder = first - digit * digit
        steps += step("${n(first)} $MINUS ${n(digit * digit)}", remainder, "Das Quadrat abziehen.")
        var result = digit
        for (g in 1 until groups.size) {
            val group = groups[g]
            val current = remainder * 100 + group
            val groupText = group.toString().padStart(2, '0')
            steps += step(
                "Rest ${n(remainder)}, „$groupText“ herunterholen",
                current,
                if (g >= integerGroups) "Hinter dem Komma kommen Nullen: zwei Nullen anhängen." else "Die nächsten zwei Ziffern anhängen.",
            )
            val doubled = 2 * result
            var x2 = 0L
            while ((doubled * 10 + x2 + 1) * (x2 + 1) <= current) x2++
            val product = (doubled * 10 + x2) * x2
            steps += step(
                "Nächste Ziffer: ${n(doubled)}_ $X _ bis ${n(current)}",
                x2,
                "Das bisherige Ergebnis ${n(result)} verdoppeln: ${n(doubled)}. Dann ${n(doubled * 10 + x2)} $X $x2 = ${n(product)}" +
                    if (x2 < 9) ", ${n(doubled * 10 + x2 + 1)} $X ${x2 + 1} = ${n((doubled * 10 + x2 + 1) * (x2 + 1))} wäre zu viel." else ".",
            )
            steps += step("${n(current)} $MINUS ${n(product)}", current - product, if (current == product) "Rest 0." else null)
            remainder = current - product
            result = result * 10 + x2
        }
        val value = BigDecimal.valueOf(result, decimals)
        val expected: Expected = if (decimals == 0) {
            Expected.Exact(value)
        } else {
            Expected.Approx(value, BigDecimal.valueOf(1, decimals), decimals)
        }
        steps += Step(
            if (decimals == 0) "√${n(x)}" else "√${n(x)} auf $decimals ${if (decimals == 1) "Stelle" else "Stellen"}",
            expected,
            if (decimals == 0) InputKind.INTEGER else InputKind.DECIMAL,
            if (decimals == 0) "Rest 0 – die Wurzel geht genau auf." else "Die gefundenen Ziffern ergeben ${Fmt.dec(value, decimals)}.",
        )
        return Problem(
            skillId = "w9.sqrtpaper",
            key = "p$x|$decimals",
            prompt = Prompt(
                "√${n(x)}",
                caption = if (decimals == 0) "Ziffer für Ziffer – geht genau auf" else "Ziffer für Ziffer – auf $decimals ${if (decimals == 1) "Stelle" else "Stellen"} nach dem Komma",
            ),
            expected = expected,
            input = if (decimals == 0) InputKind.INTEGER else InputKind.DECIMAL,
            steps = steps,
            method = "Ziffer für Ziffer",
        )
    }

    private val sqrtPaper = Skill(
        id = "w9.sqrtpaper",
        title = "Wurzeln Ziffer für Ziffer",
        method = "Verdoppeln und passende Ziffer suchen",
        book = BookRef(CH, "Wurzeln ziehen mit Zettel und Stift"),
        summary = "Quadratwurzeln exakt berechnen, Stelle für Stelle.",
        explanation = listOf(
            Block.Text("Fast vergessen, aber erstaunlich elegant: Mit Zettel und Stift lässt sich jede Wurzel Ziffer für Ziffer genau ausrechnen. Die Zahl wird dazu vom Komma aus in Zweiergruppen geteilt; jede Gruppe liefert eine Ziffer des Ergebnisses."),
            Block.Steps(
                listOf(
                    "Erste Ziffer: die größte Zahl, deren Quadrat in die erste Gruppe passt. Bei √53 ist das 7 (49).",
                    "Quadrat abziehen (53 − 49 = 4) und die nächsten zwei Ziffern herunterholen – hinter dem Komma Nullen: 400.",
                    "Das bisherige Ergebnis verdoppeln (14) und eine Lücke dahinter lassen: 14_ × _.",
                    "Die größte Ziffer suchen, die in beide Lücken passt, ohne 400 zu überschreiten: 142 × 2 = 284. Nächste Ziffer: 2, also 7,2.",
                    "Abziehen (400 − 284 = 116), wieder zwei Nullen anhängen und weitermachen.",
                ),
            ),
            Block.Example(paperRoot(53, 2)),
            Block.Example(paperRoot(3_249, 0)),
            Block.Tip("Bleibt irgendwann der Rest 0 und es gibt nichts mehr herunterzuholen, ist die Zahl ein perfektes Quadrat."),
        ),
        goals = Goals(testSize = 5, minAccuracy = 0.8, star1 = 75.0, star2 = 50.0, star3 = 32.0),
        guidedCount = 3,
        practiceCount = 6,
        generator = { r ->
            when (r.nextInt(3)) {
                0 -> {
                    val root = r.betweenL(32, 99)
                    paperRoot(root * root, 0)
                }

                1 -> {
                    var x: Long
                    do {
                        x = r.betweenL(101, 999)
                        val s = kotlin.math.sqrt(x.toDouble()).toLong()
                    } while (s * s == x)
                    paperRoot(x, 1)
                }

                else -> {
                    var x: Long
                    do {
                        x = r.betweenL(11, 99)
                        val s = kotlin.math.sqrt(x.toDouble()).toLong()
                    } while (s * s == x)
                    paperRoot(x, 2)
                }
            }
        },
    )

    val world = World(
        id = "w9",
        number = 9,
        title = "Papier, Stift und Proben",
        subtitle = "Kapitel 6 – Mathe für die Tafel",
        skills = listOf(columns, modSumSkill, check9, rest11Skill, check11, cross2, cross3, sqrtPaper),
    )
}
