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

import com.helddertierwelt.mentalmath.domain.climb.math.Num
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.BookRef
import com.helddertierwelt.mentalmath.domain.climb.model.Goals
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.math.abs
import kotlin.random.Random

/** Ein Lösungsweg mit dem Namen der Methode. */
internal data class Solution(val method: String, val steps: List<Step>)

/** Methoden für zweistellig × zweistellig aus dem 3. Kapitel (und die Nahe-beieinander-Methode aus dem 8.). */
internal object TwoByTwo {

    const val ADDITION = "Additionsmethode"
    const val SUBTRACTION = "Subtraktionsmethode"
    const val FACTORING = "Faktorisierungsmethode"
    const val CLOSE = "Nahe-beieinander-Methode"
    const val PARTNERS = "Zehner-Partner"

    /** Welche Zahl wird bei der Additionsmethode aufgespalten? Regeln aus dem Buch. */
    fun splitChoice(x: Long, y: Long): Pair<Long, Long> {
        fun fifties(v: Long) = v in 50L..59L
        return when {
            fifties(x) && y % 2 == 0L && !fifties(y) -> x to y
            fifties(y) && x % 2 == 0L && !fifties(x) -> y to x
            x % 10 == y % 10 -> if (x >= y) x to y else y to x
            maxOf(x, y) >= 2.5 * minOf(x, y) -> if (x >= y) x to y else y to x
            x % 10 < y % 10 -> x to y
            else -> y to x
        }
    }

    fun addition(x: Long, y: Long): Solution {
        val (split, other) = splitChoice(x, y)
        val tens = split / 10 * 10
        val ones = split % 10
        val steps = mutableListOf(
            step("${n(tens)} $X ${n(other)}", tens * other, "${n(split)} aufspalten in ${n(tens)} + ${n(ones)}. ${Methods.zerosHint(tens, other)}"),
        )
        if (ones > 0) {
            steps += step("${n(ones)} $X ${n(other)}", ones * other, "Jetzt der zweite Teil – zweistellig mal einstellig.")
            steps += step("${n(tens * other)} + ${n(ones * other)}", x * y, "Beide Teilergebnisse addieren.")
        }
        return Solution(ADDITION, steps)
    }

    /** Kandidat für die Subtraktionsmethode: Zahl, die auf 8 oder 9 endet oder in den hohen 90ern liegt. */
    fun subtractionCandidate(x: Long, y: Long): Long? {
        fun score(v: Long): Int = when {
            v >= 96 -> 3 - (100 - v).toInt().coerceAtMost(3) + 2
            v % 10 == 9L -> 4
            v % 10 == 8L -> 3
            else -> 0
        }
        val sx = score(x)
        val sy = score(y)
        return when {
            sx == 0 && sy == 0 -> null
            sx >= sy -> x
            else -> y
        }
    }

    fun subtraction(x: Long, y: Long): Solution {
        val round = subtractionCandidate(x, y) ?: (if (x % 10 >= y % 10) x else y)
        val other = if (round == x) y else x
        val up = if (round >= 96) 100L else Num.roundUp(round, 10)
        val diff = up - round
        val big = up * other
        val small = diff * other
        return Solution(
            SUBTRACTION,
            listOf(
                step("${n(up)} $X ${n(other)}", big, "${n(round)} auf ${n(up)} aufrunden. ${Methods.zerosHint(up, other)}"),
                step("${n(diff)} $X ${n(other)}", small, "So viel ist zu viel."),
                step("${n(big)} $MINUS ${n(small)}", x * y, "Den Überschuss abziehen – Komplemente helfen, wenn du borgen müsstest."),
            ),
        )
    }

    /** Ist das Zwischenergebnis ein „freundliches Produkt“ (dreistellig mit 0 in der Mitte)? */
    private fun friendly(v: Long): Boolean = v in 100L..999L && (v / 10) % 10 == 0L

    fun canFactor(x: Long, y: Long): Boolean =
        Num.singleDigitFactorPairs(x).isNotEmpty() || Num.singleDigitFactorPairs(y).isNotEmpty()

    fun factoring(x: Long, y: Long): Solution? {
        data class Option(val factored: Long, val other: Long, val first: Long, val second: Long)
        val options = mutableListOf<Option>()
        for ((factored, other) in listOf(x to y, y to x)) {
            for ((p, q) in Num.singleDigitFactorPairs(factored)) {
                options += Option(factored, other, p, q)
                if (p != q) options += Option(factored, other, q, p)
            }
        }
        if (options.isEmpty()) return null
        val best = options.maxByOrNull { o ->
            val mid = o.other * o.first
            var score = 0
            if (friendly(mid)) score += 100
            if (o.first > o.second) score += 10
            if (o.other == maxOf(x, y)) score += 1
            score
        } ?: return null
        val mid = best.other * best.first
        val hint1 = "${n(best.factored)} = ${n(best.first)} $X ${n(best.second)}. Erst ${n(best.other)} $X ${n(best.first)}."
        val hint2 = if (friendly(mid)) "${n(mid)} hat eine 0 in der Mitte – ein freundliches Produkt." else "Jetzt dreistellig mal einstellig."
        return Solution(
            FACTORING,
            listOf(
                step("${n(best.other)} $X ${n(best.first)}", mid, hint1),
                step("${n(mid)} $X ${n(best.second)}", x * y, hint2),
            ),
        )
    }

    /**
     * Basis für die Nahe-beieinander-Methode: ein Vielfaches von 10 (oder 100) nah an beiden Zahlen.
     * Gewählt wird die Basis mit den kleinsten Abständen; bei Gleichstand die untere (dann wird addiert).
     */
    fun closeBase(x: Long, y: Long): Long? {
        val unit = if (x >= 100 || y >= 100) 100L else 10L
        val candidates = listOf(
            Num.roundDown(minOf(x, y), unit),
            Num.roundUp(maxOf(x, y), unit),
            Num.roundDown((x + y) / 2 + unit / 2, unit),
        ).distinct().filter { z ->
            z > 0 && abs(x - z) <= 9 && abs(y - z) <= 9 && x != z && y != z
        }
        return candidates.minWithOrNull(compareBy<Long>({ abs(x - it) + abs(y - it) }, { it }))
    }

    fun closeApplies(x: Long, y: Long): Boolean = closeBase(x, y) != null

    /** Nahe-beieinander-Methode: (z + a)(z + b) = z(z + a + b) + ab. */
    fun close(x: Long, y: Long, base: Long? = null): Solution {
        val z = base ?: closeBase(x, y) ?: Num.roundDown((x + y) / 2 + 5, 10)
        val a = x - z
        val b = y - z
        val sum = x + b
        val big = z * sum
        val prod = abs(a * b)
        val sign = if (a * b >= 0) "+" else MINUS
        val diffText = { v: Long -> if (v >= 0) "+${n(v)}" else "$MINUS${n(-v)}" }
        val steps = mutableListOf(
            step("${n(x)} ${if (b >= 0) "+" else MINUS} ${n(abs(b))}", sum, "Abstände zur Basis ${n(z)}: ${n(x)} (${diffText(a)}), ${n(y)} (${diffText(b)}). Eine Zahl um den Abstand der anderen verschieben."),
            step("${n(z)} $X ${n(sum)}", big, Methods.zerosHint(z, sum)),
            step("${n(abs(a))} $X ${n(abs(b))}", prod, "Die beiden Abstände miteinander multiplizieren."),
        )
        steps += step(
            "${n(big)} $sign ${n(prod)}",
            x * y,
            if (a * b >= 0) "Das Produkt der Abstände addieren." else "Ein Abstand ist negativ – deshalb abziehen.",
        )
        return Solution(CLOSE, steps)
    }

    fun partners(x: Long, y: Long): Solution? {
        if (x / 10 != y / 10 || x % 10 + y % 10 != 10L) return null
        val a = x / 10
        val front = a * (a + 1)
        val back = (x % 10) * (y % 10)
        return Solution(
            PARTNERS,
            listOf(
                step("${n(a)} $X ${n(a + 1)}", front, "Gleiche Zehner, Einer ergeben 10 – der Blitztrick aus Welt 2."),
                step("${n(x % 10)} $X ${n(y % 10)}", back, "Zweistellig schreiben: ${Methods.twoDigits(back)}."),
                step("${n(x)} $X ${n(y)}", x * y, "Vorne ${n(front)}, hinten ${Methods.twoDigits(back)}."),
            ),
        )
    }

    /** Der bequemste Weg für eine beliebige Aufgabe (Freistil). */
    fun best(x: Long, y: Long): Solution {
        partners(x, y)?.let { return it }
        factoring(x, y)?.let { return it }
        if (subtractionCandidate(x, y) != null) return subtraction(x, y)
        if (closeApplies(x, y) && abs(x - y) <= 8) return close(x, y)
        return addition(x, y)
    }
}

/** Welt 6 – zweistellig mal zweistellig, Quadrate dreistelliger Zahlen und Kubikzahlen (3. Kapitel). */
internal object World6 {

    private fun productProblem(skillId: String, x: Long, y: Long, solution: Solution): Problem = intProblem(
        skillId = skillId,
        key = "${minOf(x, y)}x${maxOf(x, y)}",
        prompt = Prompt("${n(x)} $X ${n(y)}"),
        value = x * y,
        steps = solution.steps,
        method = solution.method,
    )

    private fun twoDigit(r: Random, avoidTens: Boolean = true): Long {
        var v: Long
        do {
            v = r.betweenL(12, 98)
        } while ((avoidTens && v % 10 == 0L) || v == 11L)
        return v
    }

    fun additionProblem(x: Long, y: Long): Problem = productProblem("w6.addmethod", x, y, TwoByTwo.addition(x, y))
    fun subtractionProblem(x: Long, y: Long): Problem = productProblem("w6.submethod", x, y, TwoByTwo.subtraction(x, y))
    fun factoringProblem(x: Long, y: Long): Problem = productProblem("w6.factor", x, y, TwoByTwo.factoring(x, y) ?: TwoByTwo.addition(x, y))
    fun closeProblem(x: Long, y: Long): Problem = productProblem("w6.close", x, y, TwoByTwo.close(x, y))
    fun freeProblem(x: Long, y: Long): Problem = productProblem("w6.free", x, y, TwoByTwo.best(x, y))

    private val addMethod = Skill(
        id = "w6.addmethod",
        title = "Additionsmethode",
        method = TwoByTwo.ADDITION,
        book = BookRef(3, "Die Additionsmethode"),
        summary = "Zweistellig mal zweistellig, indem du eine Zahl in Zehner und Einer aufspaltest.",
        explanation = listOf(
            Block.Text("Die Additionsmethode funktioniert bei jeder Aufgabe. Du spaltest eine der beiden Zahlen in Zehner und Einer auf. So entstehen zwei Aufgaben „zweistellig mal einstellig“, die du aus Welt 4 kennst. Am Ende addierst du die beiden Teilergebnisse."),
            Block.Steps(
                listOf(
                    "Entscheiden, welche Zahl du aufspaltest (siehe Tipp).",
                    "Den Zehneranteil mal die andere Zahl rechnen: bei 63 × 47 → 60 × 47 = 2.820.",
                    "Den Eineranteil mal die andere Zahl: 3 × 47 = 141.",
                    "Addieren: 2.820 + 141 = 2.961.",
                ),
            ),
            Block.Example(additionProblem(63, 47)),
            Block.Tip("Spalte meist die Zahl mit der kleineren Einerziffer auf – dann wird das zweite Teilergebnis klein und die Addition leicht. Enden beide gleich, spaltest du die größere auf. Eine Zahl aus den 50ern mal eine gerade Zahl spaltest du immer auf, weil 50 × gerade Zahl einen glatten Hunderter ergibt."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 30.0, star2 = 18.0, star3 = 11.0),
        generator = { r -> additionProblem(twoDigit(r), twoDigit(r)) },
    )

    private val subMethod = Skill(
        id = "w6.submethod",
        title = "Subtraktionsmethode",
        method = TwoByTwo.SUBTRACTION,
        book = BookRef(3, "Die Subtraktionsmethode"),
        summary = "Mit einer Zahl, die auf 8 oder 9 endet, über den nächsten Zehner rechnen.",
        explanation = listOf(
            Block.Text("Endet eine der beiden Zahlen auf 8 oder 9, rundest du sie auf den nächsten Zehner auf. Dann rechnest du mit der runden Zahl und ziehst am Ende ab, was zu viel war. Kleine Zahlen abzuziehen ist meist leichter als große zu addieren."),
            Block.Steps(
                listOf(
                    "Die Zahl mit 8 oder 9 am Ende aufrunden: 69 wird 70.",
                    "Runde Zahl mal andere Zahl: 70 × 34 = 2.380.",
                    "Den Überschuss bestimmen: 1 × 34 = 34.",
                    "Abziehen: 2.380 − 34 = 2.346.",
                ),
            ),
            Block.Example(subtractionProblem(69, 34)),
            Block.Tip("Das klappt auch mit Zahlen hoch in den 90ern: 97 × 46 rechnest du als 100 × 46 minus 3 × 46."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 28.0, star2 = 17.0, star3 = 10.0),
        generator = { r ->
            val round = if (r.nextInt(5) == 0) r.betweenL(96, 99) else r.betweenL(1, 9) * 10 + r.pick(listOf(8L, 9L))
            val other = twoDigit(r)
            if (r.nextBoolean()) subtractionProblem(round, other) else subtractionProblem(other, round)
        },
    )

    private val factorable = listOf(12L, 14L, 15L, 16L, 18L, 21L, 24L, 27L, 28L, 32L, 35L, 36L, 42L, 45L, 48L, 49L, 54L, 56L, 63L, 64L, 72L, 81L)

    private val factor = Skill(
        id = "w6.factor",
        title = "Faktorisierungsmethode",
        method = TwoByTwo.FACTORING,
        book = BookRef(3, "Die Faktorisierungsmethode"),
        summary = "Eine Zahl in zwei Ziffern zerlegen und nacheinander malnehmen – ganz ohne Addition.",
        explanation = listOf(
            Block.Text("Lässt sich eine der beiden Zahlen als Produkt zweier Ziffern schreiben, zum Beispiel 42 = 7 × 6, wird es besonders bequem. Du nimmst die andere Zahl erst mit dem einen Faktor mal und das Ergebnis dann mit dem anderen. Es gibt nichts zu addieren und kaum etwas zu merken."),
            Block.Steps(
                listOf(
                    "Eine Zahl in zwei einstellige Faktoren zerlegen: 42 = 7 × 6.",
                    "Die andere Zahl mit dem größeren Faktor malnehmen: 57 × 7 = 399.",
                    "Das Ergebnis mit dem kleineren Faktor malnehmen: 399 × 6 = 2.394.",
                ),
            ),
            Block.Example(factoringProblem(57, 42)),
            Block.Tip("Achte auf freundliche Produkte: Entsteht unterwegs eine Zahl mit 0 in der Mitte, etwa 67 × 6 = 402, wird der zweite Schritt sehr leicht. Dann lohnt es sich, die Faktoren in dieser Reihenfolge zu nehmen."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 26.0, star2 = 16.0, star3 = 10.0),
        generator = { r ->
            val f = r.pick(factorable)
            val other = twoDigit(r)
            if (r.nextBoolean()) factoringProblem(f, other) else factoringProblem(other, f)
        },
    )

    private val close = Skill(
        id = "w6.close",
        title = "Nahe-beieinander-Methode",
        method = TwoByTwo.CLOSE,
        book = BookRef(8, "Die Nahe-beieinander-Methode"),
        summary = "Zwei Zahlen, die nah an einer runden Zahl liegen, über diese Basis multiplizieren.",
        explanation = listOf(
            Block.Text("Liegen beide Zahlen nah an derselben runden Zahl – der Basis –, gibt es einen eleganten Weg. Du schreibst dir die Abstände zur Basis auf. Dann verschiebst du die eine Zahl um den Abstand der anderen, multiplizierst mit der Basis und addierst das Produkt der Abstände. Das Buch stellt die Methode in Kapitel 8 vor; sie funktioniert aber schon bei zweistelligen Zahlen."),
            Block.Formula("(z + a) × (z + b) = z × (z + a + b) + a × b"),
            Block.Steps(
                listOf(
                    "Basis wählen: bei 73 × 76 ist es 70. Die Abstände sind 3 und 6.",
                    "Eine Zahl um den Abstand der anderen verschieben: 73 + 6 = 79 (oder 76 + 3).",
                    "Mit der Basis malnehmen: 70 × 79 = 5.530.",
                    "Die Abstände multiplizieren und addieren: 3 × 6 = 18, also 5.548.",
                ),
            ),
            Block.Example(closeProblem(73, 76)),
            Block.Tip("Liegt eine Zahl unter und eine über der Basis, ist ein Abstand negativ. Dann ziehst du das Produkt der Abstände ab, statt es zu addieren."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 24.0, star2 = 15.0, star3 = 9.0),
        generator = { r ->
            var x: Long
            var y: Long
            do {
                val z = r.betweenL(2, 9) * 10
                val a = r.betweenL(-3, 9)
                val b = r.betweenL(1, 9)
                x = z + a
                y = z + b
            } while (x == y || x % 10 == 0L || y % 10 == 0L || x < 12 || y > 99 || !TwoByTwo.closeApplies(x, y))
            if (r.nextBoolean()) closeProblem(x, y) else closeProblem(y, x)
        },
    )

    private val free = Skill(
        id = "w6.free",
        title = "Freistil",
        method = "Die passende Methode wählen",
        book = BookRef(3, "Kreatives Herangehen an Multiplikationen"),
        summary = "Für jede Aufgabe selbst die bequemste Methode finden.",
        explanation = listOf(
            Block.Text("Jetzt kennst du mehrere Wege für dieselbe Aufgabe. Im Freistil entscheidest du selbst. Mit etwas Übung siehst du einer Aufgabe sofort an, welcher Weg am kürzesten ist."),
            Block.Steps(
                listOf(
                    "Lässt sich eine Zahl in zwei Ziffern zerlegen? → Faktorisierungsmethode.",
                    "Endet eine Zahl auf 8 oder 9? → Subtraktionsmethode.",
                    "Liegen beide nah an derselben runden Zahl? → Nahe-beieinander-Methode.",
                    "Gleiche Zehner und Einer zusammen 10? → Blitztrick aus Welt 2.",
                    "Sonst geht immer die Additionsmethode.",
                ),
            ),
            Block.Example(freeProblem(38, 64)),
            Block.Tip("Der Lösungsweg zeigt dir den Weg, den das Buch für den bequemsten halten würde. Dein eigener Weg ist genauso richtig, wenn das Ergebnis stimmt."),
        ),
        goals = Goals(testSize = 10, star1 = 28.0, star2 = 17.0, star3 = 10.0),
        generator = { r -> freeProblem(twoDigit(r), twoDigit(r)) },
    )

    // --- Quadrate dreistelliger Zahlen --------------------------------------------------------

    private fun squareHint(dist: Long): String {
        if (dist < 10) return "Das Quadrat des Abstands."
        val r = dist % 10
        if (r == 0L) return "Das Quadrat des Abstands: ${n(dist / 10)}² mit zwei Nullen."
        val up = r * 2 >= 10
        val e = if (up) 10 - r else r
        val round = if (up) dist + e else dist - e
        val other = if (up) dist - e else dist + e
        return "Quadrat einer zweistelligen Zahl wie in Welt 4: ${n(round)} $X ${n(other)} + ${n(e * e)}."
    }

    fun square3Problem(a: Long): Problem {
        val dist = Methods.squareDistance(a, 100)
        return intProblem(
            skillId = "w6.square3",
            key = "${a}sq",
            prompt = Prompt("${n(a)}²"),
            value = a * a,
            steps = Methods.square(a, 100, listOf(step("${n(dist)}²", dist * dist, squareHint(dist)))),
            method = "Auf- und Abrunden zum Hunderter",
        )
    }

    private val square3 = Skill(
        id = "w6.square3",
        title = "Quadrate dreistelliger Zahlen",
        method = "Auf- und Abrunden zum Hunderter",
        book = BookRef(3, "Quadrieren dreistelliger Zahlen"),
        summary = "Dreistellige Zahlen quadrieren – ein echtes Kunststück.",
        explanation = listOf(
            Block.Text("Das Quadrieren dreistelliger Zahlen funktioniert genau wie bei zweistelligen – nur rundest du jetzt auf den nächsten Hunderter. Dadurch wird die Hauptrechnung zu „dreistellig mal einstellig“ mit zwei Nullen dahinter."),
            Block.Formula("A² = (A + d) × (A − d) + d²"),
            Block.Steps(
                listOf(
                    "Abstand zum nächsten Hunderter: Bei 297 sind es 3 nach oben, also 300 und 294.",
                    "Multiplizieren: 3 × 294 = 882, zwei Nullen dran: 88.200.",
                    "Das Quadrat des Abstands addieren: 3² = 9, also 88.209.",
                ),
            ),
            Block.Example(square3Problem(297)),
            Block.Example(square3Problem(438)),
            Block.Tip("Die zweite Zahl findest du beim Aufrunden schnell, indem du die letzten beiden Ziffern verdoppelst: Bei 863 ist 63 × 2 = 126, also ist die zweite Zahl 826. Sag dir das große Zwischenergebnis vor, bevor du das Quadrat des Abstands rechnest."),
        ),
        goals = Goals(testSize = 6, minAccuracy = 0.83, star1 = 60.0, star2 = 35.0, star3 = 20.0),
        guidedCount = 3,
        practiceCount = 6,
        generator = { r ->
            val hundreds = r.betweenL(1, 9)
            val dist = if (r.nextInt(3) == 0) r.betweenL(1, 9) else r.betweenL(11, 49)
            val a = if (r.nextBoolean()) hundreds * 100 + dist else (hundreds + 1) * 100 - dist
            square3Problem(a.coerceIn(101, 999))
        },
    )

    // --- Kommazahl mal Kommazahl ----------------------------------------------------------------

    fun decimalProduct(x: Long, y: Long): Problem {
        val solution = TwoByTwo.best(x, y)
        val steps = solution.steps.toMutableList()
        val first = steps.first()
        steps[0] = first.copy(hint = "Ohne Komma rechnen: ${n(x)} $X ${n(y)} – mit der ${solution.method}. " + (first.hint ?: ""))
        val result = dec(x * y, 2)
        steps += decStep("${n(x * y)} $DIV 100", result, null, "Zusammen zwei Nachkommastellen – also Komma um zwei Stellen nach links.")
        return decProblem(
            skillId = "w6.decdec",
            key = "${minOf(x, y)}d${maxOf(x, y)}",
            prompt = Prompt("${d(dec(x, 1))} $X ${d(dec(y, 1))}"),
            value = result,
            places = null,
            steps = steps,
            method = solution.method,
        )
    }

    private val decDec = Skill(
        id = "w6.decdec",
        title = "Kommazahl mal Kommazahl",
        method = "Ohne Komma rechnen, Stellen zählen",
        book = BookRef(null, "Ergänzung: Kommazahlen mit den Methoden aus Kapitel 3"),
        summary = "Aufgaben wie 4,7 × 2,3 mit den Methoden für zweistellige Zahlen lösen.",
        explanation = listOf(
            Block.Text("Zwei Kommazahlen multiplizierst du, indem du beide Kommas weglässt. Aus 4,7 × 2,3 wird 47 × 23 – eine Aufgabe für die Methoden dieser Welt. Dann zählst du die Nachkommastellen beider Zahlen zusammen. Hier sind es zwei, also rückt das Komma zwei Stellen nach links."),
            Block.Example(decimalProduct(47, 23)),
            Block.Tip("Überschlag zur Kontrolle: 4,7 × 2,3 ist ungefähr 5 × 2 = 10. Das Ergebnis 10,81 passt."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 35.0, star2 = 22.0, star3 = 14.0),
        generator = { r -> decimalProduct(twoDigit(r), twoDigit(r)) },
    )

    // --- Kubikzahlen ---------------------------------------------------------------------------------

    fun cubeProblem(a: Long): Problem {
        val dist = Methods.squareDistance(a, 10)
        val lo = a - dist
        val hi = a + dist
        val steps = mutableListOf<Step>()
        val product: Long
        if (lo % 10 == 0L && hi % 10 == 0L) {
            steps += step("${n(lo)} $X ${n(hi)}", lo * hi, "Endet die Zahl auf 5, sind beide Nachbarn runde Zahlen.")
            steps += step("${n(lo * hi)} $X ${n(a)}", lo * hi * a, "Jetzt noch mit ${n(a)} malnehmen.")
            product = lo * hi * a
        } else {
            val round = if (lo % 10 == 0L) lo else hi
            val other = if (round == lo) hi else lo
            val first = TwoByTwo.best(a, other)
            steps += step("${n(a)} $X ${n(other)}", a * other, "Zweistellig mal zweistellig, zum Beispiel mit der ${first.method}.")
            steps += step("${n(a * other)} $X ${n(round)}", a * other * round, Methods.zerosHint(round, a * other))
            product = a * other * round
        }
        steps += step("${n(dist * dist)} $X ${n(a)}", dist * dist * a, "Jetzt d² × A: ${n(dist)}² = ${n(dist * dist)}.")
        steps += step("${n(product)} + ${n(dist * dist * a)}", a * a * a, "Beide Teile addieren.")
        return intProblem(
            skillId = "w6.cube",
            key = "${a}cube",
            prompt = Prompt("${n(a)}³"),
            value = a * a * a,
            steps = steps,
            method = "(A − d) × A × (A + d) + d² × A",
        )
    }

    private val cube = Skill(
        id = "w6.cube",
        title = "Kubikzahlen zweistelliger Zahlen",
        method = "Über die Nachbarn rechnen",
        book = BookRef(3, "Kubieren"),
        summary = "Zweistellige Zahlen hoch drei nehmen.",
        explanation = listOf(
            Block.Text("Eine Kubikzahl ist eine Zahl dreimal mit sich selbst multipliziert. Wie beim Quadrieren gehst du um den Abstand d zum nächsten Zehner nach oben und nach unten. Dann multiplizierst du die drei Zahlen und gleichst mit d² × A aus."),
            Block.Formula("A³ = (A − d) × A × (A + d) + d² × A"),
            Block.Steps(
                listOf(
                    "Bei 23³ ist d = 3. Die Nachbarn sind 20 und 26.",
                    "Multiplizieren: 23 × 26 = 598, dann × 20 = 11.960.",
                    "Ausgleich: d² × A = 9 × 23 = 207.",
                    "Addieren: 11.960 + 207 = 12.167.",
                ),
            ),
            Block.Example(cubeProblem(23)),
            Block.Tip("Wenn die Zahl auf 5 endet, sind beide Nachbarn rund – das macht die Rechnung besonders angenehm."),
        ),
        goals = Goals(testSize = 5, minAccuracy = 0.8, star1 = 120.0, star2 = 75.0, star3 = 45.0, timeLimit = 300.0),
        guidedCount = 3,
        practiceCount = 5,
        generator = { r -> cubeProblem(r.betweenL(11, 99).let { if (it % 10 == 0L) it + 2 else it }) },
    )

    val world = World(
        id = "w6",
        number = 6,
        title = "Zweistellig mal zweistellig",
        subtitle = "Kapitel 3 – drei Methoden und mehr",
        skills = listOf(addMethod, subMethod, factor, close, free, square3, decDec, cube),
    )
}
