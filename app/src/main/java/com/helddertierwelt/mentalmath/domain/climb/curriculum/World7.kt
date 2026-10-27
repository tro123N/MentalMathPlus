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
import kotlin.random.Random

/** Welt 7 – Teilen, Teilbarkeit und Brüche (4. Kapitel). */
internal object World7 {

    private const val CH = 4

    // --- Division von links nach rechts ------------------------------------------------------

    /** Schritte für n ÷ d nach dem Buch: größte passende Vielfache von 100, 10, 1 abziehen. */
    fun divisionSteps(n: Long, d: Long): List<Step> {
        val q = n / d
        val r = n % d
        val steps = mutableListOf<Step>()
        var rest = n
        val parts = Num.placeParts(q)
        parts.forEachIndexed { i, part ->
            val chunk = d * part
            val unit = Num.pow10(part.toString().length - 1)
            val tooMuch = d * (part + unit)
            val placeName = when (unit) {
                1L -> "Einer"
                10L -> "Zehner"
                100L -> "Hunderter"
                else -> "Tausender"
            }
            val hint = if (i == 0) {
                "Welche ${placeName} passen? ${n(d)} $X ${n(part)} = ${n(chunk)}, ${n(d)} $X ${n(part + unit)} = ${n(tooMuch)} wäre zu viel."
            } else {
                "Wie oft passt ${n(d)} in ${n(rest)}?"
            }
            steps += step("${n(d)} $X ${n(part)}", chunk, hint)
            val overshoot = tooMuch - rest
            val subHint = if (i == parts.lastIndex && d >= 10 && overshoot in 1..(d / 3)) {
                "Abkürzung: ${n(d)} $X ${n(part + unit)} = ${n(tooMuch)} ist nur ${n(overshoot)} zu viel – der Rest ist also ${n(d)} $MINUS ${n(overshoot)} = ${n(d - overshoot)}."
            } else null
            steps += step("${n(rest)} $MINUS ${n(chunk)}", rest - chunk, subHint)
            rest -= chunk
        }
        steps += Step(
            label = "${n(n)} $DIV ${n(d)}",
            expected = Expected.Remainder(q, r),
            input = InputKind.REMAINDER,
            hint = if (r == 0L) "Geht glatt auf: ${n(q)}." else "Ergebnis ${n(q)}, Rest ${n(r)} – eintippen als ${q}R$r.",
        )
        return steps
    }

    fun division(skillId: String, n: Long, d: Long): Problem = Problem(
        skillId = skillId,
        key = "$n:$d",
        prompt = Prompt("${n(n)} $DIV ${n(d)}", caption = "Mit Rest – Taste „Rest“ für R"),
        expected = Expected.Remainder(n / d, n % d),
        input = InputKind.REMAINDER,
        steps = divisionSteps(n, d),
        method = "Von links nach rechts teilen",
    )

    private val div1 = Skill(
        id = "w7.div1",
        title = "Durch einstellige Zahlen teilen",
        method = "Von links nach rechts",
        book = BookRef(CH, "Division durch eine einstellige Zahl"),
        summary = "Zwei- und dreistellige Zahlen im Kopf teilen, mit Rest.",
        explanation = listOf(
            Block.Text("Beim Teilen rechnet man ganz natürlich von links nach rechts – genau so, wie man es in der Schule gelernt hat. Zuerst überlegst du, wie groß das Ergebnis ungefähr wird. Dann nimmst du Stück für Stück so viel weg, wie hineinpasst."),
            Block.Steps(
                listOf(
                    "Größenordnung: Liegt die Zahl zwischen 10-mal und 100-mal dem Teiler, wird das Ergebnis zweistellig.",
                    "Das größte passende Vielfache von 10 suchen: bei 283 ÷ 6 ist 6 × 40 = 240 (6 × 50 = 300 wäre zu viel).",
                    "Abziehen: 283 − 240 = 43. Aus der großen Aufgabe ist 43 ÷ 6 geworden.",
                    "Wie oft passt 6 in 43? 7-mal (42), Rest 1. Ergebnis: 47 Rest 1.",
                ),
            ),
            Block.Example(division("w7.div1", 283, 6)),
            Block.Tip("Bei dreistelligen Ergebnissen hilft es, die schon bekannten Stellen laut zu sagen – oder sie mit den Fingern festzuhalten, wie es das Buch mit den „Daumenregeln“ vorschlägt."),
        ),
        goals = Goals(testSize = 10, star1 = 14.0, star2 = 9.0, star3 = 6.0),
        generator = { r ->
            val d = r.betweenL(3, 9)
            val q = if (r.nextInt(3) == 0) r.betweenL(102, 299) else r.betweenL(12, 99)
            division("w7.div1", d * q + r.betweenL(0, d - 1), d)
        },
    )

    fun divideByFive(n: Long): Problem {
        val doubled = 2 * n
        val result = dec(doubled, 1)
        return decProblem(
            skillId = "w7.div5",
            key = "$n:5",
            prompt = Prompt("${n(n)} $DIV 5"),
            value = result,
            places = null,
            steps = listOf(
                step("${n(n)} $X 2", doubled, "Teilen durch 5 ist dasselbe wie verdoppeln und durch 10 teilen."),
                decStep("${n(doubled)} $DIV 10", result, null, "Durch 10: Komma eine Stelle nach links."),
            ),
            method = "Verdoppeln, dann durch 10",
        )
    }

    private val div5 = Skill(
        id = "w7.div5",
        title = "Durch 5 teilen",
        method = "Verdoppeln, dann durch 10",
        book = BookRef(CH, "Division durch eine einstellige Zahl"),
        summary = "Jede Zahl blitzschnell durch 5 teilen.",
        explanation = listOf(
            Block.Text("Durch 5 zu teilen ist eigentlich gar nicht nötig: Wenn du beide Zahlen verdoppelst, ändert sich das Ergebnis nicht. Aus „geteilt durch 5“ wird „geteilt durch 10“ – und das ist nur ein Komma verschieben."),
            Block.Steps(
                listOf(
                    "Die Zahl verdoppeln: 1.347 × 2 = 2.694.",
                    "Durch 10 teilen: 269,4.",
                ),
            ),
            Block.Example(divideByFive(1347)),
        ),
        goals = Goals(testSize = 10, star1 = 8.0, star2 = 5.0, star3 = 3.5),
        generator = { r ->
            val n = if (r.nextBoolean()) r.betweenL(21, 499) else r.betweenL(1003, 4999)
            divideByFive(if (n % 5 == 0L) n + 2 else n)
        },
    )

    private val div2 = Skill(
        id = "w7.div2",
        title = "Durch zweistellige Zahlen teilen",
        method = "Vielfache abziehen, mit Abkürzung",
        book = BookRef(CH, "Division durch zweistellige Zahlen"),
        summary = "Drei- und vierstellige Zahlen durch zweistellige teilen.",
        explanation = listOf(
            Block.Text("Bei einem zweistelligen Teiler gehst du genauso vor wie bei einem einstelligen. Nur das Abschätzen, wie oft der Teiler hineinpasst, braucht etwas mehr Übung."),
            Block.Steps(
                listOf(
                    "Größenordnung bestimmen: 743 ÷ 18 liegt zwischen 10 und 100.",
                    "Passendes Vielfaches von 10: 18 × 40 = 720 (18 × 50 = 900 ist zu viel).",
                    "Abziehen: 743 − 720 = 23. Bleibt 23 ÷ 18 = 1 Rest 5.",
                    "Ergebnis: 41 Rest 5.",
                ),
            ),
            Block.Example(division("w7.div2", 743, 18)),
            Block.Tip("Rätst du beim letzten Schritt eins zu hoch, war es trotzdem nicht umsonst: Ist 62 × 8 = 496 um 5 zu groß für 491, dann ist das Ergebnis 7 und der Rest 62 − 5 = 57."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 25.0, star2 = 16.0, star3 = 10.0),
        generator = { r ->
            val d = r.betweenL(12, 79).let { if (it % 10 == 0L) it + 3 else it }
            val q = if (r.nextInt(4) == 0) r.betweenL(2, 9) else r.betweenL(12, 89)
            division("w7.div2", d * q + r.betweenL(0, d - 1), d)
        },
    )

    // --- Vereinfachen ------------------------------------------------------------------------

    fun simplified(n: Long, d: Long): Problem {
        val steps = mutableListOf<Step>()
        var a = n
        var b = d
        var method = "Vereinfachen"
        when {
            a % 10 == 0L && b % 10 == 0L -> {
                steps += step("${n(a)} $DIV 10", a / 10, "Beide Zahlen enden auf 0 – beide durch 10 teilen.")
                steps += step("${n(b)} $DIV 10", b / 10)
                a /= 10
                b /= 10
                method = "Beide durch 10"
            }

            b % 5 == 0L && b % 2 == 1L && (a % 5 == 0L) -> {
                steps += step("${n(a)} $X 2", a * 2, "Der Teiler endet auf 5 – beide Zahlen verdoppeln …")
                steps += step("${n(b)} $X 2", b * 2)
                a *= 2
                b *= 2
                steps += step("${n(a)} $DIV 10", a / 10, "… und dann beide durch 10 teilen.")
                steps += step("${n(b)} $DIV 10", b / 10)
                a /= 10
                b /= 10
                method = "Verdoppeln, dann durch 10"
            }

            else -> {
                var round = 0
                while (a % 2 == 0L && b % 2 == 0L && b > 9) {
                    round++
                    steps += step("${n(a)} $DIV 2", a / 2, if (round == 1) "Beide Zahlen sind gerade – beide halbieren." else "Noch einmal halbieren.")
                    steps += step("${n(b)} $DIV 2", b / 2)
                    a /= 2
                    b /= 2
                }
                method = "Beide halbieren"
            }
        }
        steps += step("${n(a)} $DIV ${n(b)}", a / b, "Diese Aufgabe ist viel leichter – und hat dasselbe Ergebnis.")
        return intProblem(
            skillId = "w7.simplify",
            key = "$n:$d",
            prompt = Prompt("${n(n)} $DIV ${n(d)}"),
            value = n / d,
            steps = steps,
            method = method,
        )
    }

    private val simplify = Skill(
        id = "w7.simplify",
        title = "Teilen vereinfachen",
        method = "Beide Zahlen gleich verändern",
        book = BookRef(CH, "Vereinfachen von Divisionsaufgaben"),
        summary = "Schwere Divisionen in leichte verwandeln, bevor du rechnest.",
        explanation = listOf(
            Block.Text("Das Ergebnis einer Division ändert sich nicht, wenn du beide Zahlen durch dieselbe Zahl teilst oder mit derselben Zahl malnimmst. Damit lassen sich viele Aufgaben stark vereinfachen, bevor du überhaupt anfängst."),
            Block.Steps(
                listOf(
                    "Beide gerade? Beide halbieren – notfalls zweimal.",
                    "Beide enden auf 0? Beide durch 10 teilen.",
                    "Endet der Teiler auf 5? Beide verdoppeln und dann durch 10 teilen.",
                ),
            ),
            Block.Example(simplified(1632, 48)),
            Block.Example(simplified(1645, 35)),
            Block.Tip("Geht die Aufgabe nicht glatt auf, ändert sich beim Vereinfachen der Rest. Als Bruch geschrieben bleibt er aber gleich – deshalb gibt man das Ergebnis dann als gemischten Bruch an. Hier gehen alle Aufgaben glatt auf."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 20.0, star2 = 13.0, star3 = 8.0),
        generator = { r -> simplifyFor(r) },
    )

    private fun simplifyFor(r: Random): Problem = when (r.nextInt(4)) {
        0 -> {
            val d = r.pick(listOf(12L, 14L, 16L, 18L, 24L, 28L, 32L, 36L))
            simplified(d * r.betweenL(12, 79), d)
        }

        1 -> {
            val d = r.betweenL(2, 9) * 10
            simplified(d * r.betweenL(12, 99), d)
        }

        2 -> {
            val d = r.pick(listOf(15L, 25L, 35L, 45L))
            simplified(d * (r.betweenL(6, 49) * 2 + 1), d)
        }

        else -> {
            val d = r.pick(listOf(15L, 35L, 45L))
            simplified(d * r.betweenL(6, 49) * 2, d)
        }
    }

    // --- Brüche als Dezimalzahl -------------------------------------------------------------

    private fun fractionHint(a: Long, b: Long): String = when (b) {
        2L, 4L, 8L -> "Halbe, Viertel und Achtel: 1/8 = 0,125 – davon das ${n(a)}-Fache."
        3L -> "Drittel wiederholen sich: 1/3 = 0,333…, 2/3 = 0,666…"
        5L -> "Fünftel: 1/5 = 0,2 – davon das ${n(a)}-Fache."
        6L -> "Sechstel: 1/6 = 0,1666… und 5/6 = 0,8333…; die anderen lassen sich kürzen."
        7L -> "Siebtel: immer die Ziffernfolge 142857, Start bei ${n(a)} × 0,14 ≈ ${d(BigDecimal.valueOf(a * 14, 2))}."
        9L -> "Neuntel: Der Zähler wiederholt sich, ${n(a)}/9 = 0,${a}${a}${a}…"
        10L -> "Zehntel: einfach 0,${a}."
        11L -> "Elftel: ${n(a)} × 0,0909… = 0,${Methods.twoDigits(a * 9)}${Methods.twoDigits(a * 9)}…"
        else -> "Teilen von links nach rechts."
    }

    /** a/b als Dezimalzahl auf drei Nachkommastellen (gerundet oder abgeschnitten wird akzeptiert). */
    fun fractionToDecimal(skillId: String, a: Long, b: Long, via: Pair<Long, Long>? = null): Problem {
        val exactValue = BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 6, RoundingMode.DOWN)
        val terminating = BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(1000)).remainder(BigDecimal.valueOf(b)).signum() == 0
        val expected: Expected = if (terminating) {
            Expected.Exact(BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 3, RoundingMode.UNNECESSARY))
        } else {
            Expected.Approx(exactValue, BigDecimal("0.001"), places = 6)
        }
        val steps = mutableListOf<Step>()
        if (via != null) {
            val (na, nb) = via
            val factor = nb / b
            steps += step("${n(b)} $X ${n(factor)}", nb, "Nenner auf eine bequeme Zahl bringen: ${n(a)}/${n(b)} = ${n(na)}/${n(nb)}.")
            steps += step("${n(a)} $X ${n(factor)}", na, "Den Zähler genauso malnehmen.")
        }
        steps += Step(
            label = "${n(a)}/${n(b)}",
            expected = expected,
            input = InputKind.DECIMAL,
            hint = if (via != null) "Jetzt nur noch durch ${n(via.second)} teilen." else fractionHint(a, b),
        )
        return Problem(
            skillId = skillId,
            key = "$a/$b",
            prompt = Prompt("${n(a)}/${n(b)}", caption = "Als Dezimalzahl – mindestens 3 Stellen nach dem Komma"),
            expected = expected,
            input = InputKind.DECIMAL,
            steps = steps,
            method = "Bekannte Brüche",
        )
    }

    private val fracDec = Skill(
        id = "w7.fracdec",
        title = "Brüche als Dezimalzahl",
        method = "Halbe bis Elftel kennen",
        book = BookRef(CH, "Gegen den Taschenrechner antreten: Lernen, wie man Brüche in Dezimalbrüche umwandelt"),
        summary = "Brüche mit Nennern bis 11 sofort als Dezimalzahl sagen.",
        explanation = listOf(
            Block.Text("Die Brüche mit Nennern von 2 bis 11 kommen so oft vor, dass es sich lohnt, ihre Dezimalzahlen zu kennen. Das klingt nach viel, aber fast jede Familie hat ein einfaches Muster."),
            Block.Steps(
                listOf(
                    "Halbe, Viertel, Achtel: 1/2 = 0,5 · 1/4 = 0,25 · 1/8 = 0,125 – alles andere sind Vielfache davon.",
                    "Fünftel und Zehntel: 1/5 = 0,2 und 1/10 = 0,1.",
                    "Drittel, Sechstel, Neuntel wiederholen sich: 1/3 = 0,333…, 1/6 = 0,1666…, 4/9 = 0,444…",
                    "Elftel: Vielfache von 0,0909…, also 3/11 = 0,2727…",
                    "Siebtel: immer dieselbe Ziffernfolge 142857, nur mit anderem Start.",
                ),
            ),
            Block.Example(fractionToDecimal("w7.fracdec", 5, 8)),
            Block.Example(fractionToDecimal("w7.fracdec", 3, 7)),
            Block.Tip("Bei Brüchen, die sich nicht abbrechen lassen, reicht es hier, drei Stellen nach dem Komma richtig zu haben – gerundet oder abgeschnitten."),
        ),
        goals = Goals(testSize = 10, star1 = 8.0, star2 = 5.0, star3 = 3.0),
        generator = { r ->
            var a: Long
            var b: Long
            do {
                b = r.betweenL(2, 11)
                a = r.betweenL(1, b - 1)
            } while (Num.gcd(a, b) != 1L)
            fractionToDecimal("w7.fracdec", a, b)
        },
    )

    private val fracTricks = Skill(
        id = "w7.fractricks",
        title = "Brüche umwandeln mit Kniffen",
        method = "Nenner bequem machen",
        book = BookRef(CH, "Gegen den Taschenrechner antreten: Lernen, wie man Brüche in Dezimalbrüche umwandelt"),
        summary = "Brüche mit Nennern wie 25, 15 oder 14 schnell umrechnen.",
        explanation = listOf(
            Block.Text("Viele Brüche lassen sich in einen bekannten oder bequemen Bruch verwandeln, bevor du rechnest. Dazu nimmst du Zähler und Nenner mit derselben Zahl mal."),
            Block.Steps(
                listOf(
                    "Nenner endet auf 5: beide verdoppeln, dann ist der Nenner ein Zehner. 7/15 = 14/30.",
                    "Nenner 25 oder 75: beide mal 4, dann steht 100 oder 300 unten. 9/25 = 36/100 = 0,36.",
                    "Nenner gerade, Zähler ungerade: Kürzen geht nicht – aber du kannst mit dem halben Nenner weiterrechnen, zum Beispiel über die Siebtel.",
                ),
            ),
            Block.Example(fractionToDecimal("w7.fractricks", 9, 25, 36L to 100L)),
            Block.Example(fractionToDecimal("w7.fractricks", 7, 15, 14L to 30L)),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 14.0, star2 = 9.0, star3 = 6.0),
        generator = { r ->
            if (r.nextBoolean()) {
                val b = r.pick(listOf(25L, 75L))
                var a: Long
                do {
                    a = r.betweenL(1, b - 1)
                } while (Num.gcd(a, b) != 1L)
                fractionToDecimal("w7.fractricks", a, b, a * 4 to b * 4)
            } else {
                val b = r.pick(listOf(15L, 35L, 45L, 5L * 11))
                var a: Long
                do {
                    a = r.betweenL(1, b - 1)
                } while (Num.gcd(a, b) != 1L)
                fractionToDecimal("w7.fractricks", a, b, a * 2 to b * 2)
            }
        },
    )

    // --- Teilbarkeit -------------------------------------------------------------------------

    private val yesNo = listOf("Ja", "Nein")

    fun divisibility(number: Long, k: Long): Problem {
        val divisible = number % k == 0L
        val digits = Num.digits(number)
        val steps = mutableListOf<Step>()
        when (k) {
            3L, 9L -> steps += step("Quersumme von ${n(number)}", digits.sum().toLong(), "Eine Zahl ist genau dann durch $k teilbar, wenn ihre Quersumme es ist.")
            4L -> steps += step("Letzte zwei Ziffern", number % 100, "Durch 4 teilbar, wenn die letzten zwei Ziffern es sind.")
            8L -> steps += step("Letzte drei Ziffern", number % 1000, "Durch 8 teilbar, wenn die letzten drei Ziffern es sind.")
            6L -> steps += step("Quersumme von ${n(number)}", digits.sum().toLong(), "Durch 6 teilbar = gerade und durch 3 teilbar.")
            11L -> {
                var alt = 0L
                digits.forEachIndexed { i, dg -> alt += if (i % 2 == 0) dg else -dg }
                val shown = kotlin.math.abs(alt)
                steps += step("Ziffern abwechselnd − und +", shown, "Abwechselnd subtrahieren und addieren (${digits.joinToString(" ") { it.toString() }}). Ergebnis 0 oder Vielfaches von 11 → teilbar.${if (alt < 0) " (Vorzeichen spielt keine Rolle.)" else ""}")
            }
            7L -> {
                // Vielfache von 7 so addieren/abziehen, dass eine 0 am Ende entsteht, dann die 0 streichen.
                var m = number
                var guard = 0
                while (m >= 70 && guard < 6) {
                    val last = m % 10
                    val adjust = (0L..9L).first { t -> (last + 7 * t) % 10 == 0L || (last - 7 * t) % 10 == 0L && last - 7 * t >= 0 }
                    val plus = (last + 7 * adjust) % 10 == 0L
                    val next = if (plus) m + 7 * adjust else m - 7 * adjust
                    steps += step(
                        if (plus) "${n(m)} + ${n(7 * adjust)}" else "${n(m)} $MINUS ${n(7 * adjust)}",
                        next,
                        "Ein Vielfaches von 7 so dazu- oder abziehen, dass hinten eine 0 steht.",
                    )
                    steps += step("${n(next)} $DIV 10", next / 10, "Die 0 streichen – das ändert nichts an der Teilbarkeit durch 7.")
                    m = next / 10
                    guard++
                }
            }
        }
        steps += Step(
            label = "${n(number)} durch $k teilbar?",
            expected = Expected.Choice(if (divisible) 0 else 1),
            input = InputKind.CHOICE,
            hint = if (divisible) "Ja – die Regel passt." else "Nein – die Regel passt nicht.",
        )
        return Problem(
            skillId = "w7.divisible",
            key = "$number|$k",
            prompt = Prompt("Ist ${n(number)} durch $k teilbar?"),
            expected = Expected.Choice(if (divisible) 0 else 1),
            input = InputKind.CHOICE,
            steps = steps,
            method = "Teilbarkeitsregel für $k",
            choices = yesNo,
        )
    }

    private val divisible = Skill(
        id = "w7.divisible",
        title = "Teilbarkeit erkennen",
        method = "Regeln für 3, 4, 6, 7, 8, 9, 11",
        book = BookRef(CH, "Wie man auf Teilbarkeit testet"),
        summary = "Ohne zu teilen erkennen, ob eine Zahl glatt teilbar ist.",
        explanation = listOf(
            Block.Text("Wer schnell sieht, ob eine Zahl durch eine andere teilbar ist, kann Divisionen vereinfachen und Zahlen beim Multiplizieren in Faktoren zerlegen. Die Regeln sind kurz und überraschend elegant."),
            Block.Steps(
                listOf(
                    "2, 4, 8: auf die letzte, die letzten zwei bzw. die letzten drei Ziffern schauen.",
                    "3 und 9: Quersumme bilden. Ist sie durch 3 bzw. 9 teilbar, ist es die Zahl auch.",
                    "6: gerade und durch 3 teilbar.",
                    "11: Ziffern abwechselnd abziehen und addieren. Kommt 0 oder ein Vielfaches von 11 heraus, ist die Zahl teilbar.",
                    "7: ein Vielfaches von 7 so addieren oder abziehen, dass die Zahl auf 0 endet, die 0 streichen – und wiederholen, bis die Zahl klein ist.",
                ),
            ),
            Block.Example(divisibility(4_368, 7)),
            Block.Tip("Die Regel für 7 funktioniert genauso für jede ungerade Zahl, die nicht auf 5 endet, zum Beispiel 13 oder 17."),
        ),
        goals = Goals(testSize = 10, star1 = 10.0, star2 = 6.0, star3 = 4.0),
        guidedCount = 5,
        generator = { r ->
            val k = r.pick(listOf(3L, 4L, 6L, 7L, 8L, 9L, 11L))
            val base = r.betweenL(1_000, 99_999)
            val number = if (r.nextBoolean()) base - base % k + (if (base - base % k < 1000) k else 0) else base.let { if (it % k == 0L) it + 1 else it }
            divisibility(number, k)
        },
    )

    // --- Bruchrechnen ------------------------------------------------------------------------

    private fun reduced(a: Long, b: Long): Pair<Long, Long> {
        val g = Num.gcd(a, b)
        return a / g to b / g
    }

    fun fractionProduct(a: Long, b: Long, c: Long, d: Long, divide: Boolean): Problem {
        val (cc, dd) = if (divide) d to c else c to d
        val num = a * cc
        val den = b * dd
        val (rn, rd) = reduced(num, den)
        val steps = mutableListOf<Step>()
        if (divide) {
            steps += Step("Kehrwert von ${n(c)}/${n(d)}", Expected.Frac(d, c), InputKind.FRACTION, "Beim Teilen den zweiten Bruch auf den Kopf stellen und malnehmen.")
        }
        steps += step("${n(a)} $X ${n(cc)}", num, "Zähler mal Zähler.")
        steps += step("${n(b)} $X ${n(dd)}", den, "Nenner mal Nenner.")
        steps += Step("${n(num)}/${n(den)}", Expected.Frac(rn, rd), InputKind.FRACTION, if (rn != num) "Noch kürzen: durch ${n(num / rn)} teilen." else "Lässt sich nicht kürzen.")
        val op = if (divide) DIV else X
        return Problem(
            skillId = "w7.fracmul",
            key = "$a/$b$op$c/$d",
            prompt = Prompt("${n(a)}/${n(b)} $op ${n(c)}/${n(d)}", caption = "Ergebnis als Bruch, z. B. 3/4"),
            expected = Expected.Frac(rn, rd),
            input = InputKind.FRACTION,
            steps = steps,
            method = if (divide) "Mit dem Kehrwert malnehmen" else "Zähler mal Zähler, Nenner mal Nenner",
        )
    }

    private val fracMul = Skill(
        id = "w7.fracmul",
        title = "Brüche malnehmen und teilen",
        method = "Zähler mal Zähler, Nenner mal Nenner",
        book = BookRef(CH, "Multiplizieren von Brüchen, Division von Brüchen"),
        summary = "Brüche miteinander multiplizieren und durcheinander teilen.",
        explanation = listOf(
            Block.Text("Brüche multipliziert man, indem man oben mit oben und unten mit unten malnimmt. Beim Teilen kommt nur ein Schritt dazu: Man stellt den zweiten Bruch auf den Kopf – das ist sein Kehrwert – und multipliziert dann."),
            Block.Example(fractionProduct(2, 5, 3, 4, divide = false)),
            Block.Example(fractionProduct(3, 4, 5, 6, divide = true)),
            Block.Tip("Tippe Brüche mit der Bruchstrich-Taste ein. Ein ungekürztes, aber gleichwertiges Ergebnis zählt hier auch."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 18.0, star2 = 12.0, star3 = 7.0),
        generator = { r ->
            fun frac(): Pair<Long, Long> {
                var a: Long
                var b: Long
                do {
                    b = r.betweenL(2, 9)
                    a = r.betweenL(1, b - 1)
                } while (Num.gcd(a, b) != 1L)
                return a to b
            }
            val (a, b) = frac()
            val (c, d) = frac()
            fractionProduct(a, b, c, d, divide = r.nextBoolean())
        },
    )

    fun reduceFraction(a: Long, b: Long): Problem {
        val g = Num.gcd(a, b)
        return Problem(
            skillId = "w7.fracreduce",
            key = "k$a/$b",
            prompt = Prompt("${n(a)}/${n(b)}", caption = "Vollständig kürzen"),
            expected = Expected.Frac(a / g, b / g, requireReduced = true),
            input = InputKind.FRACTION,
            steps = listOf(
                step("Größter gemeinsamer Teiler", g, "Die größte Zahl, durch die sich Zähler und Nenner teilen lassen."),
                step("${n(a)} $DIV ${n(g)}", a / g),
                step("${n(b)} $DIV ${n(g)}", b / g),
                Step("${n(a)}/${n(b)}", Expected.Frac(a / g, b / g, requireReduced = true), InputKind.FRACTION, "Gekürzter Bruch."),
            ),
            method = "Kürzen",
        )
    }

    fun expandFraction(a: Long, b: Long, target: Long): Problem {
        val factor = target / b
        return intProblem(
            skillId = "w7.fracreduce",
            key = "e$a/$b>$target",
            prompt = Prompt("${n(a)}/${n(b)} = ?/${n(target)}", caption = "Erweitern: Welcher Zähler fehlt?"),
            value = a * factor,
            steps = listOf(
                step("${n(target)} $DIV ${n(b)}", factor, "Womit wurde der Nenner malgenommen?"),
                step("${n(a)} $X ${n(factor)}", a * factor, "Den Zähler mit derselben Zahl malnehmen."),
            ),
            method = "Erweitern",
        )
    }

    private val fracReduce = Skill(
        id = "w7.fracreduce",
        title = "Kürzen und Erweitern",
        method = "Oben und unten gleich behandeln",
        book = BookRef(CH, "Kürzen von Brüchen"),
        summary = "Brüche vereinfachen und auf einen anderen Nenner bringen.",
        explanation = listOf(
            Block.Text("Ein Bruch behält seinen Wert, wenn du Zähler und Nenner mit derselben Zahl malnimmst (Erweitern) oder durch dieselbe Zahl teilst (Kürzen). Vollständig gekürzt ist ein Bruch, wenn sich Zähler und Nenner durch keine gemeinsame Zahl mehr teilen lassen."),
            Block.Example(reduceFraction(18, 24)),
            Block.Example(expandFraction(3, 4, 20)),
            Block.Tip("Beim Kürzen muss das Ergebnis hier wirklich vollständig gekürzt sein."),
        ),
        goals = Goals(testSize = 10, star1 = 12.0, star2 = 8.0, star3 = 5.0),
        generator = { r ->
            if (r.nextBoolean()) {
                var a: Long
                var b: Long
                do {
                    b = r.betweenL(3, 12)
                    a = r.betweenL(1, b - 1)
                } while (Num.gcd(a, b) != 1L)
                val g = r.betweenL(2, 9)
                reduceFraction(a * g, b * g)
            } else {
                var a: Long
                var b: Long
                do {
                    b = r.betweenL(2, 9)
                    a = r.betweenL(1, b - 1)
                } while (Num.gcd(a, b) != 1L)
                expandFraction(a, b, b * r.betweenL(2, 9))
            }
        },
    )

    fun fractionSum(a: Long, b: Long, c: Long, d: Long, subtract: Boolean): Problem {
        val l = b / Num.gcd(b, d) * d
        val fa = l / b
        val fc = l / d
        val na = a * fa
        val nc = c * fc
        val num = if (subtract) na - nc else na + nc
        val (rn, rd) = reduced(num, l)
        val op = if (subtract) MINUS else "+"
        val steps = mutableListOf<Step>()
        if (b != d) {
            steps += step("Gemeinsamer Nenner von ${n(b)} und ${n(d)}", l, "Die kleinste Zahl, die beide Nenner teilen.")
            steps += step("${n(a)}/${n(b)} = ?/${n(l)}", na, "Erweitern mit ${n(fa)}.")
            steps += step("${n(c)}/${n(d)} = ?/${n(l)}", nc, "Erweitern mit ${n(fc)}.")
        }
        steps += step("${n(na)} $op ${n(nc)}", num, if (b == d) "Gleiche Nenner: nur die Zähler ${if (subtract) "abziehen" else "addieren"}." else "Jetzt die Zähler ${if (subtract) "abziehen" else "addieren"}.")
        steps += Step("${n(num)}/${n(l)}", Expected.Frac(rn, rd), InputKind.FRACTION, if (rn != num) "Kürzen: durch ${n(num / rn)} teilen." else "Fertig – kürzen geht nicht.")
        return Problem(
            skillId = "w7.fracadd",
            key = "$a/$b$op$c/$d",
            prompt = Prompt("${n(a)}/${n(b)} $op ${n(c)}/${n(d)}", caption = "Ergebnis als Bruch, z. B. 5/6"),
            expected = Expected.Frac(rn, rd),
            input = InputKind.FRACTION,
            steps = steps,
            method = if (b == d) "Gleiche Nenner" else "Auf gleichen Nenner bringen",
        )
    }

    private val fracAdd = Skill(
        id = "w7.fracadd",
        title = "Brüche addieren und subtrahieren",
        method = "Auf gleichen Nenner bringen",
        book = BookRef(CH, "Addition von Brüchen, Subtraktion von Brüchen"),
        summary = "Brüche mit gleichen und verschiedenen Nennern zusammenrechnen.",
        explanation = listOf(
            Block.Text("Haben zwei Brüche denselben Nenner, addierst oder subtrahierst du einfach die Zähler. Sind die Nenner verschieden, erweiterst du beide Brüche zuerst auf einen gemeinsamen Nenner."),
            Block.Example(fractionSum(2, 9, 4, 9, subtract = false)),
            Block.Example(fractionSum(3, 4, 1, 6, subtract = true)),
            Block.Tip("Ein ungekürztes, aber gleichwertiges Ergebnis zählt hier auch – der Lösungsweg zeigt dir die gekürzte Form."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 22.0, star2 = 14.0, star3 = 9.0),
        generator = { r ->
            fun frac(): Pair<Long, Long> {
                var a: Long
                var b: Long
                do {
                    b = r.betweenL(2, 9)
                    a = r.betweenL(1, b - 1)
                } while (Num.gcd(a, b) != 1L)
                return a to b
            }
            var (a, b) = frac()
            var (c, d) = frac()
            if (r.nextInt(4) == 0) d = b.also { c = r.betweenL(1, b - 1) }
            val subtract = r.nextBoolean()
            if (subtract && a * d < c * b) {
                val ta = a
                val tb = b
                a = c
                b = d
                c = ta
                d = tb
            }
            if (subtract && a * d == c * b) fractionSum(a, b, c, d, subtract = false) else fractionSum(a, b, c, d, subtract)
        },
    )

    val world = World(
        id = "w7",
        number = 7,
        title = "Teilen und Brüche",
        subtitle = "Kapitel 4 – Teile und herrsche",
        skills = listOf(div1, div5, div2, simplify, fracDec, fracTricks, divisible, fracMul, fracReduce, fracAdd),
    )
}
