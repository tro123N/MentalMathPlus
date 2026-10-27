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
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

/** Welt 8 – Überschlagen und Alltagsrechnen (5. Kapitel). */
internal object World8 {

    private const val CH = 5

    /** Auf das nächste Vielfache von [unit] runden (ab der Hälfte auf). */
    fun nearest(x: Long, unit: Long): Long = (x + unit / 2) / unit * unit

    private fun unitName(unit: Long): String = when (unit) {
        10L -> "Zehner"
        100L -> "Hunderter"
        1_000L -> "Tausender"
        10_000L -> "Zehntausender"
        100_000L -> "Hunderttausender"
        else -> n(unit) + "er"
    }

    private fun bd(x: Long): BigDecimal = BigDecimal.valueOf(x)

    private fun percentOf(x: Long, pct: Double): Long = kotlin.math.ceil(x * pct / 100.0).toLong()

    private fun approx(value: BigDecimal, tolerance: BigDecimal, places: Int? = 0): Expected.Approx =
        Expected.Approx(value, tolerance, places)

    // --- Summen ------------------------------------------------------------------------------

    fun estimateSum(numbers: List<Long>, unit: Long): Problem {
        val rounded = numbers.map { nearest(it, unit) }
        val exactSum = numbers.sum()
        val steps = mutableListOf<Step>()
        numbers.forEachIndexed { i, x ->
            val hint = if (i == 0) "Auf ${unitName(unit)} runden: ab der Hälfte aufrunden, sonst abrunden." else null
            steps += step("${n(x)} auf ${unitName(unit)}", rounded[i], hint)
        }
        var total = rounded.first()
        for (i in 1 until rounded.size) {
            val hint = if (i == rounded.lastIndex) "Nur noch die runden Zahlen addieren – das ist der Überschlag." else null
            steps += step("${n(total)} + ${n(rounded[i])}", total + rounded[i], hint)
            total += rounded[i]
        }
        val tolerance = maxOf(unit / 2 * numbers.size, percentOf(exactSum, 1.0))
        val expected = approx(bd(exactSum), bd(tolerance))
        return Problem(
            skillId = "w8.estsum",
            key = numbers.joinToString("+"),
            prompt = Prompt(numbers.joinToString(" + ") { n(it) }, caption = "Überschlagen – auf 1 % genau reicht"),
            expected = expected,
            input = InputKind.INTEGER,
            steps = steps,
            method = "Auf ${unitName(unit)} runden",
        )
    }

    private val estSum = Skill(
        id = "w8.estsum",
        title = "Summen überschlagen",
        method = "Runden, dann addieren",
        book = BookRef(CH, "Überschlagen von Summen"),
        summary = "Große Summen in Sekunden auf 1 % genau abschätzen.",
        explanation = listOf(
            Block.Text("Oft braucht man keine genaue Summe, sondern nur eine gute Hausnummer. Dann rundest du jede Zahl auf eine runde Zahl und addierst nur noch die runden Zahlen. Der Trick ist, nicht zu grob zu runden: Wer bei vierstelligen und fünfstelligen Zahlen auf Hunderter rundet, liegt fast immer weniger als 1 % daneben."),
            Block.Steps(
                listOf(
                    "Jede Zahl auf Hunderter runden: 4.738 → 4.700, 2.861 → 2.900.",
                    "Die runden Zahlen von links nach rechts addieren: 4.700 + 2.900 = 7.600.",
                    "Genau wären es 7.599 – der Überschlag liegt nur 1 daneben.",
                ),
            ),
            Block.Example(estimateSum(listOf(4_738, 2_861), 100)),
            Block.Example(estimateSum(listOf(31_472, 8_219, 15_655), 100)),
            Block.Tip("Bei riesigen Zahlen funktioniert es genauso: Millionenbeträge rundest du auf Hunderttausender und rechnest dann mit „Millionen Komma …“."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 14.0, star2 = 9.0, star3 = 6.0),
        generator = { r ->
            when (r.nextInt(5)) {
                0 -> estimateSum(List(2) { r.betweenL(1_000_000, 39_999_999) }, 100_000)
                1, 2 -> estimateSum(List(3) { r.betweenL(1_000, 39_999) }, 100)
                else -> estimateSum(List(2) { r.betweenL(1_000, 99_999) }, 100)
            }
        },
    )

    // --- Einkauf -----------------------------------------------------------------------------

    private fun cents(c: Long): BigDecimal = BigDecimal.valueOf(c, 2)

    fun shopping(prices: List<Long>): Problem {
        val rounded = prices.map { nearest(it, 50) }
        val exactTotal = prices.sum()
        val steps = mutableListOf<Step>()
        steps += decStep("${Fmt.money(cents(prices[0]))} gerundet", cents(rounded[0]), 2, "Jeden Preis auf die nächsten 50 Cent runden.")
        var total = rounded[0]
        for (i in 1 until prices.size) {
            steps += decStep(
                "${d(cents(total), 2)} + ${d(cents(rounded[i]), 2)}",
                cents(total + rounded[i]),
                2,
                "${Fmt.money(cents(prices[i]))} ≈ ${Fmt.money(cents(rounded[i]))}",
            )
            total += rounded[i]
        }
        return Problem(
            skillId = "w8.shopping",
            key = prices.joinToString(","),
            prompt = Prompt(
                text = "Summe?",
                caption = "Einkauf überschlagen – höchstens 1 € daneben",
                lines = prices.map { Fmt.money(cents(it)).padStart(7) }.chunked(3).map { it.joinToString("  ") },
                unit = "€",
            ),
            expected = approx(cents(exactTotal), BigDecimal.ONE, 2),
            input = InputKind.DECIMAL,
            steps = steps,
            method = "Auf 50 Cent runden und mitzählen",
        )
    }

    private val endings = listOf(9L, 19L, 25L, 29L, 39L, 45L, 49L, 59L, 69L, 79L, 89L, 95L, 99L)

    private fun shoppingFor(r: Random): Problem {
        while (true) {
            val count = r.between(5, 7)
            val prices = List(count) { r.betweenL(0, 9) * 100 + r.pick(endings) }
            val rounded = prices.sumOf { nearest(it, 50) }
            if (abs(rounded - prices.sum()) <= 90) return shopping(prices)
        }
    }

    private val shoppingSkill = Skill(
        id = "w8.shopping",
        title = "Einkauf überschlagen",
        method = "Auf 50 Cent runden",
        book = BookRef(CH, "Überschlagen im Supermarkt"),
        summary = "An der Kasse mitrechnen und wissen, ob das Geld reicht.",
        explanation = listOf(
            Block.Text("Beim Einkaufen musst du nicht auf den Cent genau rechnen. Runde jeden Preis auf die nächsten 50 Cent und zähle die gerundeten Preise mit, während sie über das Band laufen. Weil manche Preise aufgerundet und andere abgerundet werden, gleichen sich die Fehler meistens aus – du liegst fast immer weniger als einen Euro daneben."),
            Block.Steps(
                listOf(
                    "Preis runden: 2,19 € → 2,00 €, 3,79 € → 4,00 €, 0,45 € → 0,50 €.",
                    "Laufende Summe im Kopf behalten: 2,00 → 6,00 → 6,50 …",
                    "Am Ende die Summe mit dem Kassenzettel vergleichen.",
                ),
            ),
            Block.Example(shopping(listOf(219, 379, 45, 129, 599))),
            Block.Tip("Mit Preisen auf 50 Cent genau zu rechnen, ist im Kopf fast so leicht wie mit ganzen Euro: Es gibt nur „glatt“ oder „Komma fünf“."),
        ),
        goals = Goals(testSize = 6, minAccuracy = 0.83, star1 = 30.0, star2 = 20.0, star3 = 14.0),
        generator = { r -> shoppingFor(r) },
    )

    // --- Differenzen -------------------------------------------------------------------------

    fun estimateDifference(a: Long, b: Long, unit: Long): Problem {
        val ra = nearest(a, unit)
        val rb = nearest(b, unit)
        val exactDiff = a - b
        val tolerance = maxOf(unit, percentOf(exactDiff, 3.0))
        return Problem(
            skillId = "w8.estdiff",
            key = "$a-$b",
            prompt = Prompt("${n(a)} $MINUS ${n(b)}", caption = "Überschlagen – auf 3 % genau reicht"),
            expected = approx(bd(exactDiff), bd(tolerance)),
            input = InputKind.INTEGER,
            steps = listOf(
                step("${n(a)} auf ${unitName(unit)}", ra, "Beide Zahlen auf ${unitName(unit)} runden."),
                step("${n(b)} auf ${unitName(unit)}", rb),
                step("${n(ra)} $MINUS ${n(rb)}", ra - rb, "Jetzt eine leichte Subtraktion."),
            ),
            method = "Runden, dann abziehen",
        )
    }

    private val estDiff = Skill(
        id = "w8.estdiff",
        title = "Differenzen überschlagen",
        method = "Runden, dann abziehen",
        book = BookRef(CH, "Überschlagen von Differenzen"),
        summary = "Unterschiede zwischen großen Zahlen schnell abschätzen.",
        explanation = listOf(
            Block.Text("Bei Differenzen gehst du genau wie bei Summen vor: beide Zahlen runden und dann die runden Zahlen abziehen. Wichtig ist, auf die zweite Stelle zu runden (bei fünfstelligen Zahlen also auf Hunderter) – wer auf Tausender rundet, liegt bei Differenzen oft weit daneben."),
            Block.Example(estimateDifference(8_436, 3_752, 100)),
            Block.Tip("Je weiter hinten du rundest, desto genauer wird es. Rundest du auf die dritte Stelle, wird der Überschlag noch einmal deutlich besser."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 12.0, star2 = 8.0, star3 = 5.0),
        generator = { r ->
            if (r.nextInt(4) == 0) {
                val a = r.betweenL(200_000, 999_999)
                estimateDifference(a, r.betweenL(100_000, a - 20_000), 1_000)
            } else {
                val a = r.betweenL(3_000, 99_999)
                estimateDifference(a, r.betweenL(1_000, a - 800), 100)
            }
        },
    )

    // --- Quotienten --------------------------------------------------------------------------

    /** Die ersten zwei Stellen einer Zahl, gerundet, und der Stellenwert dahinter. */
    private fun lead2(x: Long): Pair<Long, Int> {
        val len = x.toString().length
        if (len <= 2) return x to 0
        val shift = len - 2
        var lead = (x + pow10(shift) / 2) / pow10(shift)
        var s = shift
        if (lead >= 100) {
            lead /= 10
            s += 1
        }
        return lead to s
    }

    private fun pow10(k: Int): Long {
        var p = 1L
        repeat(k) { p *= 10 }
        return p
    }

    /** Überschlag für n ÷ d nach dem Buch; null, wenn die Kurzmethode hier zu ungenau wäre. */
    fun estimateQuotient(nn: Long, dd: Long): Problem? {
        val q = nn / dd
        val digits = q.toString().length
        val (nLead, nShift) = lead2(nn)
        val (dLead, dShift) = if (dd < 10) dd to 0 else lead2(dd)
        val ratio = BigDecimal.valueOf(nLead).divide(BigDecimal.valueOf(dLead), 2, RoundingMode.HALF_UP)
        val scale = nShift - dShift
        val estimate = if (scale >= 0) ratio.movePointRight(scale) else ratio.movePointLeft(-scale)
        val exactQ = BigDecimal.valueOf(nn).divide(BigDecimal.valueOf(dd), 6, RoundingMode.HALF_UP)
        val tolerance = exactQ.multiply(BigDecimal("0.03")).setScale(0, RoundingMode.CEILING).max(BigDecimal.ONE)
        if ((estimate - exactQ).abs() > exactQ.multiply(BigDecimal("0.025"))) return null
        val low = pow10(digits - 1)
        val roundedN = nLead * pow10(nShift)
        val roundedD = dLead * pow10(dShift)
        val expected = approx(exactQ, tolerance)
        val steps = listOf(
            step(
                "Stellen vor dem Komma",
                digits.toLong(),
                "${n(dd)} $X ${n(low)} = ${n(dd * low)} und ${n(dd)} $X ${n(low * 10)} = ${n(dd * low * 10)} – ${n(nn)} liegt dazwischen.",
            ),
            Step(
                "${n(nLead)} $DIV ${n(dLead)} (auf 2 Stellen)",
                Expected.Approx(BigDecimal.valueOf(nLead).divide(BigDecimal.valueOf(dLead), 6, RoundingMode.HALF_UP), BigDecimal("0.01"), 2),
                InputKind.DECIMAL,
                if (dd < 10) "${n(nn)} ≈ ${n(roundedN)}: nur mit den ersten zwei Stellen rechnen." else "${n(nn)} ≈ ${n(roundedN)} und ${n(dd)} ≈ ${n(roundedD)}: nur die ersten zwei Stellen teilen.",
            ),
            Step(
                "Überschlag",
                Expected.Approx(estimate.setScale(0, RoundingMode.HALF_UP), tolerance, 0),
                InputKind.INTEGER,
                "${d(ratio)} an die richtige Stelle setzen: ${digits} Stellen vor dem Komma, also etwa ${n(estimate.setScale(0, RoundingMode.HALF_UP).toLong())}.",
            ),
        )
        return Problem(
            skillId = "w8.estquot",
            key = "$nn:$dd",
            prompt = Prompt("${n(nn)} $DIV ${n(dd)}", caption = "Überschlagen – auf 3 % genau reicht"),
            expected = expected,
            input = InputKind.INTEGER,
            steps = steps,
            method = "Größenordnung, dann die ersten Stellen teilen",
        )
    }

    private fun quotientFor(r: Random): Problem {
        while (true) {
            val p = when (r.nextInt(3)) {
                0 -> estimateQuotient(r.betweenL(10_000, 99_999), r.betweenL(3, 9))
                1 -> estimateQuotient(r.betweenL(100_000, 9_999_999), r.betweenL(12, 98))
                else -> estimateQuotient(r.betweenL(1_000_000, 99_999_999), r.betweenL(102, 989))
            }
            if (p != null) return p
        }
    }

    private val estQuot = Skill(
        id = "w8.estquot",
        title = "Quotienten überschlagen",
        method = "Erst die Größenordnung",
        book = BookRef(CH, "Überschlagen von Quotienten"),
        summary = "Abschätzen, was bei einer großen Division ungefähr herauskommt.",
        explanation = listOf(
            Block.Text("Beim Überschlagen einer Division ist der wichtigste Schritt, die Größenordnung zu finden: Hat das Ergebnis drei, vier oder fünf Stellen? Das findest du heraus, indem du den Teiler mit 10, 100, 1.000 … malnimmst, bis du die Zahl überholst. Danach reicht es, mit den ersten zwei Stellen zu teilen."),
            Block.Steps(
                listOf(
                    "Größenordnung: 7 × 1.000 = 7.000 und 7 × 10.000 = 70.000. 43.618 liegt dazwischen – das Ergebnis hat 4 Stellen.",
                    "Erste zwei Stellen: 43.618 ≈ 44.000, also 44 ÷ 7 ≈ 6,29.",
                    "An die richtige Stelle setzen: etwa 6.290 (genau: 6.231).",
                ),
            ),
            Block.Example(estimateQuotient(43_618, 7)!!),
            Block.Example(estimateQuotient(2_400_000, 52)!!),
            Block.Tip("Ein Jahresbetrag geteilt durch 365 ergibt den Betrag pro Tag, durch 52 pro Woche, durch 12 pro Monat – solche Überschläge braucht man ständig."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 20.0, star2 = 13.0, star3 = 8.0),
        generator = { r -> quotientFor(r) },
    )

    // --- Produkte ----------------------------------------------------------------------------

    /**
     * Überschlag für a × b: Den Faktor, der näher an einer runden Zahl liegt, auf diese runden
     * und den anderen um gleich viel in die Gegenrichtung ändern. null, wenn das zu ungenau wäre.
     */
    fun estimateProduct(a: Long, b: Long, percent: Int): Problem? {
        fun target(x: Long): Long {
            val unit = if (x >= 100 && abs(x - nearest(x, 100)) <= 12) 100L else 10L
            return nearest(x, unit)
        }
        val ta = target(a)
        val tb = target(b)
        val roundA = abs(ta - a) <= abs(tb - b)
        val (x, y) = if (roundA) a to b else b to a
        val tx = if (roundA) ta else tb
        val delta = tx - x
        if (delta == 0L) return null
        val ty = y - delta
        val estimate = tx * ty
        val exactP = a * b
        if (abs(estimate - exactP) * 1000 > exactP * (percent * 10 - 5)) return null
        val tolerance = percentOf(exactP, percent.toDouble())
        val sign = if (delta > 0) "+" else MINUS
        val opposite = if (delta > 0) MINUS else "+"
        val expected = approx(bd(exactP), bd(tolerance))
        return Problem(
            skillId = "w8.estprod",
            key = "$a*$b",
            prompt = Prompt("${n(a)} $X ${n(b)}", caption = "Überschlagen – auf $percent % genau reicht"),
            expected = expected,
            input = InputKind.INTEGER,
            steps = listOf(
                step("${n(x)} $sign ${n(abs(delta))}", tx, "${n(x)} liegt nah an ${n(tx)} – dorthin runden."),
                step("${n(y)} $opposite ${n(abs(delta))}", ty, "Den anderen Faktor um genauso viel in die Gegenrichtung ändern."),
                step("${n(tx)} $X ${n(ty)}", estimate, "Das ist eine leichte Aufgabe – und ein sehr guter Überschlag."),
            ),
            method = "Gegensinnig runden",
        )
    }

    private fun productFor(r: Random): Problem {
        fun notRound(x: Long) = x % 10 != 0L
        while (true) {
            val p = when (r.nextInt(3)) {
                0 -> {
                    val a = r.betweenL(31, 99)
                    val b = r.betweenL(maxOf(21, a - 40), 99)
                    if (notRound(a) && notRound(b) && a != b) estimateProduct(a, b, 3) else null
                }

                1 -> {
                    val a = r.betweenL(101, 999)
                    val b = r.betweenL(21, 99)
                    if (notRound(a) && notRound(b)) estimateProduct(a, b, 5) else null
                }

                else -> {
                    val a = r.betweenL(2, 9) * 100 + r.pick(listOf(-1L, 1L)) * r.betweenL(2, 12)
                    val b = r.betweenL(201, 989)
                    if (notRound(a) && notRound(b)) estimateProduct(a, b, 2) else null
                }
            }
            if (p != null) return p
        }
    }

    private val estProd = Skill(
        id = "w8.estprod",
        title = "Produkte überschlagen",
        method = "Gegensinnig runden",
        book = BookRef(CH, "Überschlagen von Produkten"),
        summary = "Malaufgaben schätzen – genauer als mit gewöhnlichem Runden.",
        explanation = listOf(
            Block.Text("Rundest du bei einer Malaufgabe beide Zahlen einfach auf Zehner, liegst du oft mehrere Prozent daneben. Viel genauer wird es, wenn du nur eine Zahl auf eine runde Zahl bringst und die andere um genau so viel in die Gegenrichtung veränderst."),
            Block.Steps(
                listOf(
                    "Den Faktor wählen, der näher an einer runden Zahl liegt: Bei 72 × 47 ist das 72 (nur 2 bis zur 70).",
                    "Diesen runden: 72 − 2 = 70.",
                    "Den anderen um genauso viel in die Gegenrichtung ändern: 47 + 2 = 49.",
                    "Leicht rechnen: 70 × 49 = 3.430. Genau wären es 3.384 – nur gut 1 % daneben.",
                ),
            ),
            Block.Example(estimateProduct(72, 47, 3)!!),
            Block.Tip("Faustregel: Rückst du die Zahlen dabei näher zusammen, wird der Überschlag etwas zu groß; schiebst du sie auseinander, etwas zu klein. Am genauesten ist es, wenn die Zahlen nah beieinander liegen."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 16.0, star2 = 10.0, star3 = 7.0),
        generator = { r -> productFor(r) },
    )

    // --- Wurzeln -----------------------------------------------------------------------------

    fun estimateRoot(x: Long): Problem {
        val root = sqrt(x.toDouble())
        val g = root.roundToLong()
        val qExact = BigDecimal.valueOf(x).divide(BigDecimal.valueOf(g), 6, RoundingMode.HALF_UP)
        val q2 = qExact.setScale(2, RoundingMode.HALF_UP)
        val mean = BigDecimal.valueOf(g).add(qExact).divide(BigDecimal(2), 6, RoundingMode.HALF_UP)
        val exactRoot = BigDecimal(root).setScale(6, RoundingMode.HALF_UP)
        val expected = approx(exactRoot, BigDecimal("0.05"), 2)
        val error = x - g * g
        return Problem(
            skillId = "w8.sqrt",
            key = "r$x",
            prompt = Prompt("√${n(x)}", caption = "Auf 2 Stellen nach dem Komma schätzen"),
            expected = expected,
            input = InputKind.DECIMAL,
            steps = listOf(
                step("Ganze Zahl nah an √${n(x)}", g, "${n(g)} $X ${n(g)} = ${n(g * g)} liegt am nächsten an ${n(x)}."),
                Step(
                    "${n(x)} $DIV ${n(g)} (auf 2 Stellen)",
                    Expected.Approx(qExact, BigDecimal("0.01"), 2),
                    InputKind.DECIMAL,
                    "Teilen: Die Wurzel liegt zwischen ${n(g)} und diesem Ergebnis.",
                ),
                Step(
                    "Mitte von ${n(g)} und ${d(q2)}",
                    Expected.Approx(mean, BigDecimal("0.01"), 2),
                    InputKind.DECIMAL,
                    "Mitteln: (${n(g)} + ${d(q2)}) ÷ 2. Kontrolle: ${n(g)} ${if (error >= 0) "+" else MINUS} ${n(abs(error))}/${n(2 * g)} ergibt dasselbe.",
                ),
            ),
            method = "Teilen und Mitteln",
        )
    }

    private val sqrtSkill = Skill(
        id = "w8.sqrt",
        title = "Wurzeln schätzen",
        method = "Teilen und Mitteln",
        book = BookRef(CH, "Überschlagen von Wurzeln: Teilen und Mitteln"),
        summary = "Quadratwurzeln auf zwei Nachkommastellen genau schätzen.",
        explanation = listOf(
            Block.Text("Die Wurzel einer Zahl ist die Zahl, die mit sich selbst malgenommen die Zahl ergibt. Für eine schnelle, sehr genaue Schätzung brauchst du nur eine Division und einen Mittelwert."),
            Block.Steps(
                listOf(
                    "Eine ganze Zahl suchen, deren Quadrat nah an der Zahl liegt: Für √53 ist das 7, denn 7 × 7 = 49.",
                    "Die Zahl durch diese Schätzung teilen: 53 ÷ 7 ≈ 7,57.",
                    "Die Wurzel liegt zwischen 7 und 7,57 – nimm die Mitte: 7,29 (genau: 7,28).",
                ),
            ),
            Block.Example(estimateRoot(53)),
            Block.Formula("Abkürzung:  √Zahl ≈ Schätzung + Fehler ÷ (2 × Schätzung)\n√53 ≈ 7 + 4 ÷ 14 ≈ 7,29"),
            Block.Tip("Das Ergebnis liegt immer ein kleines bisschen über der echten Wurzel. Bei dreistelligen Zahlen hat die Wurzel zwei Stellen vor dem Komma, bei fünf- und sechsstelligen drei."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 25.0, star2 = 16.0, star3 = 10.0),
        generator = { r ->
            var x: Long
            do {
                x = if (r.nextInt(3) == 0) r.betweenL(101, 999) else r.betweenL(11, 99)
                val s = sqrt(x.toDouble()).roundToLong()
            } while (s * s == x)
            estimateRoot(x)
        },
    )

    // --- Trinkgeld ---------------------------------------------------------------------------

    fun tip(amount: Long, percent: Int): Problem {
        val tenth = cents(amount * 10)
        val steps = mutableListOf<Step>()
        val result: BigDecimal
        when (percent) {
            10 -> {
                result = tenth
                steps += decStep("10 % von ${n(amount)} €", tenth, 2, "10 %: Komma eine Stelle nach links.")
            }

            20 -> {
                result = tenth.multiply(BigDecimal(2))
                steps += decStep("10 % von ${n(amount)} €", tenth, 2, "Erst 10 %: Komma eine Stelle nach links.")
                steps += decStep("${d(tenth, 2)} $X 2", result, 2, "20 % sind das Doppelte von 10 %.")
            }

            5 -> {
                result = tenth.divide(BigDecimal(2))
                steps += decStep("10 % von ${n(amount)} €", tenth, 2, "Erst 10 %: Komma eine Stelle nach links.")
                steps += decStep("${d(tenth, 2)} $DIV 2", result, 2, "5 % sind die Hälfte von 10 %.")
            }

            15 -> {
                val half = tenth.divide(BigDecimal(2))
                result = tenth + half
                steps += decStep("10 % von ${n(amount)} €", tenth, 2, "Erst 10 %: Komma eine Stelle nach links.")
                steps += decStep("${d(tenth, 2)} $DIV 2", half, 2, "Die Hälfte davon sind 5 %.")
                steps += decStep("${d(tenth, 2)} + ${d(half, 2)}", result, 2, "10 % + 5 % = 15 %.")
            }

            25 -> {
                val half = BigDecimal.valueOf(amount).divide(BigDecimal(2))
                result = half.divide(BigDecimal(2))
                steps += decStep("${n(amount)} $DIV 2", half, null, "25 % ist ein Viertel: zweimal halbieren.")
                steps += decStep("${d(half)} $DIV 2", result, 2, "Noch einmal halbieren.")
            }

            else -> error("Prozentsatz $percent nicht vorgesehen")
        }
        return decProblem(
            skillId = "w8.tip",
            key = "$percent%$amount",
            prompt = Prompt("$percent % von ${n(amount)}", caption = "Trinkgeld in Euro", unit = "€"),
            value = result,
            places = 2,
            steps = steps,
            method = when (percent) {
                15 -> "10 % plus die Hälfte davon"
                25 -> "Zweimal halbieren"
                20 -> "10 % verdoppeln"
                5 -> "10 % halbieren"
                else -> "Komma verschieben"
            },
        )
    }

    private val tipSkill = Skill(
        id = "w8.tip",
        title = "Trinkgeld: 5, 10, 15, 20 und 25 %",
        method = "Alles aus 10 % ableiten",
        book = BookRef(CH, "Weitere Trinkgeld-Tipps"),
        summary = "Trinkgeld und Rabatte ohne Taschenrechner ausrechnen.",
        explanation = listOf(
            Block.Text("Die meisten Trinkgeld- und Rabattsätze lassen sich aus 10 % ableiten – und 10 % sind nur ein Komma, das eine Stelle nach links rückt."),
            Block.Steps(
                listOf(
                    "10 % von 38 € = 3,80 €.",
                    "20 %: das Doppelte, 7,60 €. 5 %: die Hälfte, 1,90 €.",
                    "15 %: 10 % plus die Hälfte davon, 3,80 € + 1,90 € = 5,70 €.",
                    "25 %: ein Viertel, also zweimal halbieren: 38 → 19 → 9,50 €.",
                ),
            ),
            Block.Example(tip(38, 15)),
            Block.Example(tip(38, 25)),
            Block.Tip("15 % kannst du auch als Mittelwert von 10 % und 20 % sehen: (3,80 + 7,60) ÷ 2 = 5,70."),
        ),
        goals = Goals(testSize = 10, star1 = 12.0, star2 = 8.0, star3 = 5.0),
        generator = { r ->
            val percent = r.pick(listOf(15, 15, 25, 25, 20, 5, 10))
            tip(if (r.nextInt(4) == 0) r.betweenL(101, 260) else r.betweenL(12, 99), percent)
        },
    )

    // --- Krumme Prozente ---------------------------------------------------------------------

    /** [hundredths] = Nachkommateil des Prozentsatzes in Hundertstel (50 → ,5 %; 25 → ,25 %; 75 → ,75 %). */
    fun oddPercent(amount: Long, whole: Int, hundredths: Int): Problem {
        val product = amount * whole
        val base = cents(product)
        val steps = mutableListOf(
            step("${n(amount)} $X $whole", product, "Erst den ganzen Teil: $whole % von ${n(amount)} €."),
            decStep("${n(product)} $DIV 100", base, 2, "Durch 100: das sind $whole %."),
        )
        val extra: BigDecimal
        when (hundredths) {
            50 -> {
                extra = cents(amount / 2)
                steps += decStep("0,5 % von ${n(amount)} €", extra, 2, "Ein halbes Prozent: den Betrag halbieren und als Cent lesen – ${n(amount / 2)} Cent.")
            }

            25 -> {
                extra = cents(amount / 4)
                steps += decStep("0,25 % von ${n(amount)} €", extra, 2, "Ein Viertelprozent: den Betrag durch 4 teilen und als Cent lesen – ${n(amount / 4)} Cent.")
            }

            75 -> {
                val quarter = cents(amount / 4)
                extra = quarter.multiply(BigDecimal(3))
                steps += decStep("0,25 % von ${n(amount)} €", quarter, 2, "Ein Viertelprozent: Betrag durch 4, als Cent lesen.")
                steps += decStep("${d(quarter, 2)} $X 3", extra, 2, "0,75 % sind drei Viertelprozent.")
            }

            else -> error("Nachkommateil $hundredths nicht vorgesehen")
        }
        val total = base + extra
        steps += decStep("${d(base, 2)} + ${d(extra, 2)}", total, 2, "Beide Teile zusammen.")
        val label = "$whole,${if (hundredths == 50) "5" else hundredths.toString()} %"
        return decProblem(
            skillId = "w8.oddpercent",
            key = "$label|$amount",
            prompt = Prompt("$label von ${n(amount)} €", caption = "In Euro"),
            value = total,
            places = 2,
            steps = steps,
            method = "Ganzer Teil plus Bruchteil",
        )
    }

    /** 19 % als 20 % minus 1 % (z. B. Mehrwertsteuer). */
    fun nineteenPercent(amount: Long): Problem {
        val ten = cents(amount * 10)
        val twenty = ten.multiply(BigDecimal(2))
        val one = cents(amount)
        val result = twenty - one
        return decProblem(
            skillId = "w8.oddpercent",
            key = "19%|$amount",
            prompt = Prompt("19 % von ${n(amount)} €", caption = "In Euro – z. B. Mehrwertsteuer"),
            value = result,
            places = 2,
            steps = listOf(
                decStep("10 % von ${n(amount)} €", ten, 2, "19 % = 20 % − 1 %. Erst 10 %: Komma eine Stelle nach links."),
                decStep("${d(ten, 2)} $X 2", twenty, 2, "20 % sind das Doppelte."),
                decStep("1 % von ${n(amount)} €", one, 2, "1 %: Komma zwei Stellen nach links."),
                decStep("${d(twenty, 2)} $MINUS ${d(one, 2)}", result, 2, "20 % − 1 % = 19 %."),
            ),
            method = "20 % minus 1 %",
        )
    }

    private val oddPercentSkill = Skill(
        id = "w8.oddpercent",
        title = "Krumme Prozentsätze",
        method = "Zerlegen: ganzer Teil plus Rest",
        book = BookRef(CH, "Krumme Hunde"),
        summary = "Sätze wie 6,5 %, 7,25 % oder 19 % schnell ausrechnen.",
        explanation = listOf(
            Block.Text("Ein krummer Prozentsatz ist leicht, wenn du ihn zerlegst: zuerst der ganze Teil wie gewohnt, dann der kleine Rest. Ein halbes Prozent eines Euro-Betrags ist besonders bequem: Du halbierst den Betrag und liest das Ergebnis in Cent."),
            Block.Steps(
                listOf(
                    "6,5 % von 46 €: zuerst 6 % = 46 × 6 ÷ 100 = 2,76 €.",
                    "0,5 %: die Hälfte von 46 ist 23 – also 23 Cent.",
                    "Zusammen: 2,76 € + 0,23 € = 2,99 €.",
                    "Ein Viertelprozent geht genauso: Betrag durch 4, in Cent lesen.",
                ),
            ),
            Block.Example(oddPercent(46, 6, 50)),
            Block.Example(nineteenPercent(64)),
            Block.Tip("In Deutschland kommt das ständig vor: 19 % Mehrwertsteuer sind 20 % minus 1 %, und 7 % sind 7 × Betrag ÷ 100."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 22.0, star2 = 14.0, star3 = 9.0),
        generator = { r ->
            when (r.nextInt(4)) {
                0 -> nineteenPercent(r.betweenL(12, 199))
                1 -> oddPercent(r.betweenL(6, 60) * 2, r.between(3, 9), 50)
                2 -> oddPercent(r.betweenL(4, 60) * 4, r.between(3, 9), 25)
                else -> oddPercent(r.betweenL(4, 60) * 4, r.between(3, 9), 75)
            }
        },
    )

    // --- 70er- und 110er-Regel ---------------------------------------------------------------

    fun ruleOf(rule: Int, given: Int, askYears: Boolean): Problem {
        val exactValue = BigDecimal(rule).divide(BigDecimal(given), 6, RoundingMode.HALF_UP)
        val terminating = (rule * 100) % given == 0
        val action = if (rule == 70) "verdoppelt" else "verdreifacht"
        val label = if (terminating) "$rule $DIV $given" else "$rule $DIV $given (ungefähr)"
        val places = if (exactValue.stripTrailingZeros().scale() <= 1) null else 1
        val expected = Expected.Approx(exactValue, if (askYears) BigDecimal.ONE else BigDecimal("0.5"), places)
        val prompt = if (askYears) {
            Prompt("$given % Zinsen", caption = "Nach wie vielen Jahren hat sich das Geld $action?", unit = "Jahre")
        } else {
            Prompt("$given Jahre", caption = "Welcher Zinssatz, damit sich das Geld in $given Jahren $action?", unit = "%")
        }
        return Problem(
            skillId = "w8.rule70",
            key = "$rule|$given|$askYears",
            prompt = prompt,
            expected = expected,
            input = InputKind.DECIMAL,
            steps = listOf(
                Step(
                    label,
                    expected,
                    InputKind.DECIMAL,
                    if (rule == 70) "70er-Regel: 70 geteilt durch ${if (askYears) "den Zinssatz" else "die Jahre"}." else "110er-Regel: 110 geteilt durch ${if (askYears) "den Zinssatz" else "die Jahre"}.",
                ),
            ),
            method = if (rule == 70) "70er-Regel" else "110er-Regel",
        )
    }

    private val rule70 = Skill(
        id = "w8.rule70",
        title = "Verdoppeln und Verdreifachen",
        method = "70er- und 110er-Regel",
        book = BookRef(CH, "Berechnungen für Anleger und Häuslebauer"),
        summary = "Abschätzen, wie lange Geld mit Zinsen zum Wachsen braucht.",
        explanation = listOf(
            Block.Text("Mit Zinseszins wächst Geld immer schneller. Wie lange es dauert, bis es sich verdoppelt, verrät dir eine einfache Faustregel: 70 geteilt durch den Zinssatz ergibt ungefähr die Zahl der Jahre. Für das Verdreifachen nimmst du 110 statt 70."),
            Block.Steps(
                listOf(
                    "Verdoppeln bei 4 % Zinsen: 70 ÷ 4 ≈ 17,5 Jahre.",
                    "Verdreifachen bei 4 %: 110 ÷ 4 ≈ 27,5 Jahre.",
                    "Umgekehrt: Soll sich Geld in 10 Jahren verdoppeln, braucht es etwa 70 ÷ 10 = 7 % Zinsen.",
                ),
            ),
            Block.Example(ruleOf(70, 4, askYears = true)),
            Block.Tip("Die Regeln gelten auch für Preise: Bei 3 % Inflation verdoppeln sich die Preise in gut 23 Jahren. Hier zählt ein Ergebnis bis 1 Jahr bzw. 0,5 % daneben als richtig."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 10.0, star2 = 6.0, star3 = 4.0),
        generator = { r ->
            val rule = if (r.nextInt(3) == 0) 110 else 70
            if (r.nextInt(3) == 0) {
                ruleOf(rule, r.pick(if (rule == 70) listOf(5, 7, 10, 14, 20, 28, 35) else listOf(5, 10, 11, 20, 22)), askYears = false)
            } else {
                ruleOf(rule, r.pick(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12)), askYears = true)
            }
        },
    )

    // --- Kreditrate --------------------------------------------------------------------------

    fun loan(years: Int, unitsOf1200: Int, rate: Int): Problem {
        val amount = 1_200L * years * unitsOf1200
        val months = 12L * years
        val principal = amount / months
        val yearlyInterest = amount * rate / 100
        val monthlyInterest = yearlyInterest / 12
        val bound = principal + monthlyInterest
        return intProblem(
            skillId = "w8.loan",
            key = "$years|$amount|$rate",
            prompt = Prompt(
                text = "Rate höchstens?",
                caption = "Obergrenze der Monatsrate in Euro",
                lines = listOf("Kredit: ${n(amount)} €", "Laufzeit: $years Jahre", "Zinsen: $rate % pro Jahr"),
                unit = "€",
            ),
            value = bound,
            steps = listOf(
                step("$years $X 12", months, "Zuerst die Zahl der Monatsraten."),
                step("${n(amount)} $DIV ${n(months)}", principal, "So viel wäre es pro Monat ganz ohne Zinsen."),
                step("$rate % von ${n(amount)}", yearlyInterest, "Zinsen im ersten Jahr – später werden sie weniger."),
                step("${n(yearlyInterest)} $DIV 12", monthlyInterest, "Zinsen pro Monat."),
                step("${n(principal)} + ${n(monthlyInterest)}", bound, "Mehr als das kann die Rate nicht sein."),
            ),
            method = "Tilgung plus Zinsen",
        )
    }

    private val loanSkill = Skill(
        id = "w8.loan",
        title = "Kreditrate abschätzen",
        method = "Tilgung plus Zinsen",
        book = BookRef(CH, "Berechnungen für Anleger und Häuslebauer"),
        summary = "Schnell sagen, wie hoch eine Monatsrate höchstens sein kann.",
        explanation = listOf(
            Block.Text("Die genaue Rate eines Kredits braucht eine Formel und einen Taschenrechner. Eine gute Obergrenze bekommst du aber im Kopf: Teile den Kreditbetrag durch die Zahl der Monate – so viel müsstest du ganz ohne Zinsen zahlen. Dazu kommen die Zinsen eines Monats am Anfang. Die echte Rate ist etwas niedriger, weil die Zinsen mit der Zeit sinken."),
            Block.Steps(
                listOf(
                    "Kredit 24.000 € über 4 Jahre: 48 Raten, also 24.000 ÷ 48 = 500 € ohne Zinsen.",
                    "Bei 5 % sind es im ersten Jahr 1.200 € Zinsen, also 100 € im Monat.",
                    "Obergrenze: 500 € + 100 € = 600 € im Monat.",
                ),
            ),
            Block.Example(loan(4, 5, 5)),
        ),
        goals = Goals(testSize = 6, minAccuracy = 0.83, star1 = 35.0, star2 = 24.0, star3 = 16.0),
        generator = { r ->
            loan(r.pick(listOf(2, 3, 4, 5, 6, 8, 10, 15, 20, 25, 30)), r.between(1, 12), r.between(2, 7))
        },
    )

    val world = World(
        id = "w8",
        number = 8,
        title = "Überschlagen und Alltag",
        subtitle = "Kapitel 5 – Passt schon",
        skills = listOf(estSum, shoppingSkill, estDiff, estQuot, estProd, sqrtSkill, tipSkill, oddPercentSkill, rule70, loanSkill),
    )
}
