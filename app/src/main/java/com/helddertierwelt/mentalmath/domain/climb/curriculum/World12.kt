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
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.random.Random

/** Welt 12 – Mathe-Magie: Zaubertricks, Wurzeln und Wochentage (9. und 0. Kapitel). */
internal object World12 {

    private const val CH = 9

    private fun reverse3(x: Long): Long = x.toString().padStart(3, '0').reversed().toLong()

    // --- Die magische 1089 -------------------------------------------------------------------

    fun magic1089(x: Long): Problem {
        val r = reverse3(x)
        val d = x - r
        val rd = reverse3(d)
        return intProblem(
            skillId = "w12.magic1089",
            key = "m$x",
            prompt = Prompt("${n(x)}", caption = "Umdrehen, abziehen, Ergebnis umdrehen, addieren"),
            value = d + rd,
            steps = listOf(
                step("${n(x)} umgedreht", r, "Die Ziffern rückwärts lesen."),
                step("${n(x)} $MINUS ${n(r)}", d, "Die kleinere von der größeren Zahl abziehen."),
                step("${n(d)} umgedreht", rd, "Das Ergebnis rückwärts lesen."),
                step("${n(d)} + ${n(rd)}", d + rd, "Und addieren – es kommt immer dasselbe heraus."),
            ),
            method = "Umdrehen und abziehen",
        )
    }

    private val magic1089Skill = Skill(
        id = "w12.magic1089",
        title = "Die magische 1089",
        method = "Umdrehen und abziehen",
        book = BookRef(CH, "Die magische 1.089!"),
        summary = "Ein Zahlentrick, der immer aufgeht – und warum.",
        explanation = listOf(
            Block.Text("Nimm eine dreistellige Zahl, deren Ziffern von links nach rechts kleiner werden. Dreh sie um und zieh die kleinere von der größeren Zahl ab. Dreh das Ergebnis wieder um und addiere beide. Egal, womit du anfängst: Am Ende steht immer dieselbe Zahl."),
            Block.Steps(listOf("Beispiel 742: umgedreht 247, und 742 − 247 = 495.", "495 umgedreht ist 594, und 495 + 594 = 1.089.")),
            Block.Example(magic1089(742)),
            Block.Tip("Warum? Nach dem Abziehen hat man immer ein Vielfaches von 99 (198, 297, … 891), und jedes davon ergibt mit seiner Umkehrung 1.089. Ein ähnlicher Trick: Denk dir eine Zahl, verdopple sie, zähl 12 dazu, halbiere und zieh die gedachte Zahl ab – es bleibt immer 6."),
        ),
        goals = Goals(testSize = 6, minAccuracy = 0.83, star1 = 25.0, star2 = 16.0, star3 = 10.0),
        generator = { r ->
            var x: Long
            do {
                val a = r.between(3, 9)
                val b = r.between(1, a - 1)
                val c = r.between(0, b - 1)
                x = (a * 100 + b * 10 + c).toLong()
            } while (x / 100 - x % 10 < 2)
            magic1089(x)
        },
    )

    // --- Fehlende Ziffer ---------------------------------------------------------------------

    fun missingDigit(product: Long, missingIndex: Int, order: List<Int>): Problem {
        val digits = product.toString().map { it - '0' }
        val missing = digits[missingIndex]
        val shown = digits.filterIndexed { i, _ -> i != missingIndex }.let { list -> order.map { list[it] } }
        val sum = shown.sum().toLong()
        val next = (sum / 9 + 1) * 9
        return intProblem(
            skillId = "w12.missing",
            key = "f$product|$missingIndex",
            prompt = Prompt(
                "Welche fehlt?",
                caption = "Eine Ziffer eines Vielfachen von 9 wurde verschwiegen",
                lines = listOf(shown.joinToString("  ")),
            ),
            value = missing.toLong(),
            steps = listOf(
                step(shown.joinToString(" + "), sum, "Alle genannten Ziffern addieren."),
                step("Nächstes Vielfaches von 9", next, "Die Quersumme eines Vielfachen von 9 ist selbst ein Vielfaches von 9."),
                step("${n(next)} $MINUS ${n(sum)}", next - sum, "So viel fehlt bis dahin – das ist die verschwiegene Ziffer."),
            ),
            method = "Quersumme bis zum Vielfachen von 9",
        )
    }

    private fun missingFor(r: Random): Problem {
        while (true) {
            val product = 1_089L * r.betweenL(102, 987)
            val digits = product.toString()
            val idx = r.nextInt(digits.length)
            val d = digits[idx] - '0'
            if (d == 0 || d == 9) continue
            val order = (0 until digits.length - 1).shuffled(r)
            return missingDigit(product, idx, order)
        }
    }

    private val missing = Skill(
        id = "w12.missing",
        title = "Die fehlende Ziffer",
        method = "Quersumme bis zum Vielfachen von 9",
        book = BookRef(CH, "Tricks mit fehlenden Ziffern"),
        summary = "Eine verschwiegene Ziffer erraten – mit der Neunerregel.",
        explanation = listOf(
            Block.Text("Jemand rechnet 1.089 mal eine beliebige Zahl, nennt dir alle Ziffern des Ergebnisses bis auf eine – in beliebiger Reihenfolge. Weil das Ergebnis ein Vielfaches von 9 ist, muss auch seine Quersumme ein Vielfaches von 9 sein. Du addierst also die genannten Ziffern und schaust, wie viel bis zum nächsten Vielfachen von 9 fehlt."),
            Block.Steps(listOf("Genannt werden 3, 5, 1, 8, 4.", "3 + 5 + 1 + 8 + 4 = 21, das nächste Vielfache von 9 ist 27.", "Es fehlt die 6.")),
            Block.Tip("Ist die Summe schon ein Vielfaches von 9, fehlt eine 0 oder eine 9 – das lässt sich nicht unterscheiden. In dieser Übung kommt das nicht vor."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 14.0, star2 = 9.0, star3 = 6.0),
        generator = { r -> missingFor(r) },
    )

    // --- Additions-Sprünge -------------------------------------------------------------------

    fun jumps(x: Long, y: Long): Problem {
        val seq = mutableListOf(x, y)
        while (seq.size < 10) seq += seq[seq.size - 1] + seq[seq.size - 2]
        val seventh = seq[6]
        return intProblem(
            skillId = "w12.jumps",
            key = "j$x,$y",
            prompt = Prompt(
                "Summe?",
                caption = "Jede Zahl ist die Summe der beiden davor",
                lines = seq.chunked(5).map { row -> row.joinToString("  ") { n(it) } },
            ),
            value = seq.sum(),
            steps = listOf(
                step("Die 7. Zahl", seventh, "Abzählen: die siebte Zahl der Reihe."),
                step("${n(seventh)} $X 11", seventh * 11, "Die Summe aller zehn Zahlen ist immer das 11-Fache der siebten."),
            ),
            method = "Siebte Zahl mal 11",
        )
    }

    private val jumpsSkill = Skill(
        id = "w12.jumps",
        title = "Additions-Sprünge",
        method = "Siebte Zahl mal 11",
        book = BookRef(CH, "Additions-Sprünge"),
        summary = "Zehn Zahlen in einer Sekunde zusammenzählen.",
        explanation = listOf(
            Block.Text("Schreib zwei beliebige Zahlen untereinander und bilde jede weitere Zahl als Summe der beiden davor, bis es zehn Zahlen sind. Die Summe aller zehn ist dann immer genau das 11-Fache der siebten Zahl – und mal 11 rechnest du seit Welt 2 im Schlaf."),
            Block.Steps(listOf("Start mit 3 und 8: 3, 8, 11, 19, 30, 49, 79, 128, 207, 335.", "Die siebte Zahl ist 79, und 79 × 11 = 869.", "Das ist die Summe aller zehn Zahlen.")),
            Block.Example(jumps(3, 8)),
            Block.Tip("Bonus: Teilt man die zehnte durch die neunte Zahl, beginnt das Ergebnis fast immer mit 1,61 – dem goldenen Schnitt."),
        ),
        goals = Goals(testSize = 6, minAccuracy = 0.83, star1 = 15.0, star2 = 10.0, star3 = 6.0),
        generator = { r -> jumps(r.betweenL(1, 20), r.betweenL(1, 20)) },
    )

    // --- Magisches Quadrat -------------------------------------------------------------------

    /** Grundquadrat mit Summe 34; jede Zeile, Spalte, Diagonale und jedes Viertel enthält genau eine Zahl ab 13. */
    val base: List<List<Long>> = listOf(
        listOf(13, 8, 10, 3),
        listOf(2, 11, 5, 16),
        listOf(7, 14, 4, 9),
        listOf(12, 1, 15, 6),
    )

    private val baseLines: List<String> = base.map { row -> row.joinToString(" ") { it.toString().padStart(2) } }

    fun magicSquare(total: Long, row: Int, col: Int): Problem {
        val d = total - 34
        val q = d / 4
        val rest = d % 4
        val cell = base[row][col]
        val big = cell >= 13
        val add = if (big) q + rest else q
        val steps = mutableListOf(
            step("${n(total)} $MINUS 34", d, "Erst den Unterschied zur Summe des Grundquadrats."),
            step("${n(d)} $DIV 4 (ohne Rest)", q, "Das ist die erste magische Zahl."),
        )
        if (big) {
            steps += step("${n(q)} + ${n(rest)}", q + rest, "Die zweite magische Zahl: erste plus Rest (${n(rest)}).")
        }
        steps += step("Zahl im Grundquadrat", cell, if (big) "Dort steht ${cell} – ab 13 kommt die zweite magische Zahl dazu." else "Dort steht ${cell} – unter 13 kommt die erste magische Zahl dazu.")
        steps += step("${n(cell)} + ${n(add)}", cell + add, "Diese Zahl gehört ins Feld.")
        return intProblem(
            skillId = "w12.magic",
            key = "q$total|$row|$col",
            prompt = Prompt(
                "Zeile ${row + 1}, Spalte ${col + 1}",
                caption = "Magisches Quadrat mit Summe ${n(total)} – Vorlage mit Summe 34:",
                lines = baseLines,
            ),
            value = cell + add,
            steps = steps,
            method = "Grundquadrat plus magische Zahlen",
        )
    }

    private val magicSkill = Skill(
        id = "w12.magic",
        title = "Magische Quadrate",
        method = "Grundquadrat plus magische Zahlen",
        book = BookRef(CH, "Wie man ein magisches Quadrat konstruiert"),
        summary = "Ein magisches Quadrat für jede gewünschte Summe füllen.",
        explanation = listOf(
            Block.Text("In einem magischen Quadrat ergeben alle Zeilen, Spalten und Diagonalen dieselbe Summe. Mit einem Grundquadrat der Summe 34 kannst du in Sekunden eines für jede größere Summe bauen. Das Grundquadrat hier ist so gewählt, dass in jeder Zeile, Spalte, Diagonale und in jedem Viertel genau eine Zahl ab 13 steht."),
            Block.Formula(baseLines.joinToString("\n")),
            Block.Steps(
                listOf(
                    "Wunschsumme minus 34: bei 60 sind das 26.",
                    "Durch 4 teilen: 6 Rest 2. Erste magische Zahl 6, zweite 6 + 2 = 8.",
                    "Zu jeder Zahl des Grundquadrats unter 13 die erste magische Zahl addieren, zu 13, 14, 15 und 16 die zweite.",
                ),
            ),
            Block.Example(magicSquare(60, 1, 3)),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 18.0, star2 = 12.0, star3 = 8.0),
        generator = { r -> magicSquare(r.betweenL(35, 150), r.nextInt(4), r.nextInt(4)) },
    )

    // --- Kubikwurzeln und Quadratwurzeln -----------------------------------------------------

    private val lastDigitOfCubeRoot = mapOf(0 to 0, 1 to 1, 8 to 2, 7 to 3, 4 to 4, 5 to 5, 6 to 6, 3 to 7, 2 to 8, 9 to 9)

    fun cubeRoot(x: Long): Problem {
        val cube = x * x * x
        val thousands = cube / 1_000
        val first = x / 10
        val last = x % 10
        return intProblem(
            skillId = "w12.cuberoot",
            key = "c$x",
            prompt = Prompt("∛${n(cube)}", caption = "Kubikwurzel einer Kubikzahl"),
            value = x,
            steps = listOf(
                step("Erste Ziffer (Tausender ${n(thousands)})", first, "${first}³ = ${n(first * first * first)} ≤ ${n(thousands)} < ${first + 1}³ = ${n((first + 1) * (first + 1) * (first + 1))}."),
                step("Letzte Ziffer (Endziffer ${cube % 10})", last, "Nur ${last}³ = ${n(last * last * last)} endet auf ${cube % 10}."),
                step("∛${n(cube)}", x, "Beide Ziffern zusammensetzen."),
            ),
            method = "Größenordnung und Endziffer",
        )
    }

    private val cubeSkill = Skill(
        id = "w12.cuberoot",
        title = "Kubikwurzeln im Nu",
        method = "Größenordnung und Endziffer",
        book = BookRef(CH, "Schnelle Kubikwurzeln"),
        summary = "Aus einer sechsstelligen Kubikzahl sofort die Wurzel nennen.",
        explanation = listOf(
            Block.Text("Hat jemand eine zweistellige Zahl hoch drei genommen, findest du sie blitzschnell zurück. Du brauchst nur die Kubikzahlen von 1 bis 10 zu kennen."),
            Block.Formula("1³=1  2³=8  3³=27  4³=64  5³=125\n6³=216  7³=343  8³=512  9³=729  10³=1.000"),
            Block.Steps(
                listOf(
                    "∛148.877: Die Tausender sind 148 – das liegt zwischen 5³ = 125 und 6³ = 216. Erste Ziffer: 5.",
                    "Die Zahl endet auf 7. Nur 3³ = 27 endet auf 7. Letzte Ziffer: 3.",
                    "Ergebnis: 53.",
                ),
            ),
            Block.Example(cubeRoot(53)),
            Block.Tip("Jede Endziffer kommt genau einmal vor: 1, 4, 5, 6, 9 und 0 bleiben gleich, 2 und 8 sowie 3 und 7 tauschen."),
        ),
        goals = Goals(testSize = 10, star1 = 10.0, star2 = 6.0, star3 = 4.0),
        generator = { r -> cubeRoot(r.betweenL(11, 99)) },
    )

    fun squareRoot(x: Long): Problem {
        val sq = x * x
        val hundreds = sq / 100
        val first = x / 10
        val lastDigit = (sq % 10).toInt()
        val candidates = (0..9).filter { (it * it) % 10 == lastDigit }
        val steps = mutableListOf(
            step("Erste Ziffer (Hunderter ${n(hundreds)})", first, "${first}² = ${n(first * first)} ≤ ${n(hundreds)} < ${first + 1}² = ${n((first + 1) * (first + 1))}."),
        )
        if (candidates.size == 2) {
            val mid = first * 10 + 5
            steps += step("${n(mid)}²", mid * mid, "Endziffer $lastDigit: Infrage kommen ${candidates[0]} und ${candidates[1]}. Vergleich mit ${mid}².")
        }
        steps += step(
            "√${n(sq)}",
            x,
            if (candidates.size == 2) "${n(sq)} ist ${if (sq > (first * 10 + 5) * (first * 10 + 5)) "größer" else "kleiner"} als ${first}5² – also die ${if (sq > (first * 10 + 5) * (first * 10 + 5)) "größere" else "kleinere"} Möglichkeit." else "Die Endziffer legt die letzte Ziffer eindeutig fest.",
        )
        return intProblem(
            skillId = "w12.sqrtexact",
            key = "s$x",
            prompt = Prompt("√${n(sq)}", caption = "Wurzel einer Quadratzahl"),
            value = x,
            steps = steps,
            method = "Größenordnung, Endziffer, Vergleich mit …5²",
        )
    }

    private val sqrtSkill = Skill(
        id = "w12.sqrtexact",
        title = "Wurzeln aus Quadratzahlen",
        method = "Größenordnung, Endziffer, Vergleich mit …5²",
        book = BookRef(CH, "Vereinfachte Wurzeln"),
        summary = "Aus einer vierstelligen Quadratzahl die Wurzel ablesen.",
        explanation = listOf(
            Block.Text("Auch Quadratwurzeln lassen sich sofort finden, wenn die Zahl ein echtes Quadrat ist. Die vorderen Ziffern verraten die Zehnerstelle, die Endziffer lässt zwei Möglichkeiten – und ein Vergleich mit der Zahl auf 5 entscheidet."),
            Block.Steps(
                listOf(
                    "√5.184: Die Hunderter sind 51, das liegt zwischen 7² = 49 und 8² = 64. Erste Ziffer: 7.",
                    "Endziffer 4: Die Wurzel endet auf 2 oder 8.",
                    "75² = 5.625. 5.184 ist kleiner, also die kleinere Möglichkeit: 72.",
                ),
            ),
            Block.Example(squareRoot(72)),
        ),
        goals = Goals(testSize = 10, star1 = 12.0, star2 = 8.0, star3 = 5.0),
        generator = { r ->
            var x: Long
            do { x = r.betweenL(11, 99) } while (x % 10 == 0L)
            squareRoot(x)
        },
    )

    // --- Wochentage --------------------------------------------------------------------------

    val weekdays = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    private val monthNames = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
    private val monthCodes = listOf(6, 2, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    private val monthLengths = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private fun centuryOffset(year: Int): Int = when (year / 100) {
        18 -> 3
        19 -> 1
        20 -> 0
        21 -> 5
        else -> error("Jahr $year außerhalb von 1800–2199")
    }

    /** Wochentag nach der Methode des Buches: 1 = Montag … 6 = Samstag, 0 = Sonntag. */
    fun weekdayCode(day: Int, month: Int, year: Int): Int {
        val yy = year % 100
        val yearCode = (yy + yy / 4 + centuryOffset(year)) % 7
        val mc = if (isLeap(year) && month <= 2) monthCodes[month - 1] - 1 else monthCodes[month - 1]
        return (day + mc + yearCode) % 7
    }

    private fun choiceIndex(code: Int): Int = if (code == 0) 6 else code - 1

    private fun reduceStep(value: Long, hint: String?): Step {
        val k = value / 7 * 7
        return step("${n(value)} $MINUS ${n(k)}", value - k, hint ?: "Das größte Vielfache von 7 abziehen.")
    }

    fun newYear(year: Int): Problem {
        val yy = year % 100
        val q = yy / 4
        val steps = mutableListOf(
            step("$yy $DIV 4 (ohne Rest)", q.toLong(), "Ein Viertel der letzten beiden Ziffern, abgerundet – wie 25 % Trinkgeld ohne Cent."),
            step("$yy + $q", (yy + q).toLong(), "Rechnung plus Trinkgeld."),
        )
        var total = (yy + q).toLong()
        if (isLeap(year)) {
            if (total >= 1) {
                steps += step("${n(total)} $MINUS 1", total - 1, "$year ist ein Schaltjahr: 1 abziehen.")
                total -= 1
            } else {
                steps += step("0 + 7 $MINUS 1", 6, "$year ist ein Schaltjahr: 1 abziehen – vorher 7 dazuzählen, damit es nicht negativ wird.")
                total = 6
            }
        }
        if (total >= 7) steps += reduceStep(total, null)
        val code = (total % 7).toInt()
        val index = choiceIndex(code)
        steps += Step("Neujahr $year", Expected.Choice(index), InputKind.CHOICE, "Tag $code: 1 = Montag … 6 = Samstag, 7 oder 0 = Sonntag.")
        return Problem(
            skillId = "w12.newyear",
            key = "n$year",
            prompt = Prompt("1. Januar $year", caption = "Auf welchen Wochentag fällt Neujahr?"),
            expected = Expected.Choice(index),
            input = InputKind.CHOICE,
            steps = steps,
            method = "Jahr plus ein Viertel",
            choices = weekdays,
        )
    }

    private val newYearSkill = Skill(
        id = "w12.newyear",
        title = "Neujahr: welcher Wochentag?",
        method = "Jahr plus ein Viertel",
        book = BookRef(0, "Verbessern Sie Ihr Gedächtnis"),
        summary = "Für jedes Jahr von 2000 bis 2099 den Wochentag des 1. Januar finden.",
        explanation = listOf(
            Block.Text("Für Neujahr im 21. Jahrhundert gibt es eine kleine Formel: Nimm die letzten beiden Ziffern des Jahres und zähle ein Viertel davon dazu (abgerundet). In Schaltjahren ziehst du 1 ab. Dann so viele Siebenen abziehen wie möglich – die Zahl, die übrig bleibt, ist der Wochentag."),
            Block.Formula("1 Montag   2 Dienstag   3 Mittwoch   4 Donnerstag\n5 Freitag   6 Samstag   7 oder 0 Sonntag"),
            Block.Steps(listOf("2041: 41 + 10 = 51.", "51 − 49 = 2.", "Neujahr 2041 ist ein Dienstag.")),
            Block.Example(newYear(2041)),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 14.0, star2 = 9.0, star3 = 6.0),
        generator = { r -> newYear(r.between(2000, 2099)) },
    )

    fun weekday(day: Int, month: Int, year: Int): Problem {
        val yy = year % 100
        val q = yy / 4
        val offset = centuryOffset(year)
        val leapShift = isLeap(year) && month <= 2
        val mc = if (leapShift) monthCodes[month - 1] - 1 else monthCodes[month - 1]
        val steps = mutableListOf(
            step("Monatscode ${monthNames[month - 1]}", mc.toLong(), if (leapShift) "Schaltjahr: Januar hat dann Code 5, Februar Code 1." else "Monatscodes: Jan 6, Feb 2, Mär 2, Apr 5, Mai 0, Jun 3, Jul 5, Aug 1, Sep 4, Okt 6, Nov 2, Dez 4."),
            step("$yy $DIV 4 (ohne Rest)", q.toLong(), "Für den Jahrescode: ein Viertel der letzten beiden Ziffern, abgerundet."),
        )
        val yearSumLabel = if (offset > 0) "$yy + $q + $offset" else "$yy + $q"
        val yearSum = (yy + q + offset).toLong()
        steps += step(
            yearSumLabel,
            yearSum,
            when (offset) {
                1 -> "1900–1999: 1 dazuzählen."
                3 -> "1800–1899: 3 dazuzählen."
                5 -> "2100–2199: 5 dazuzählen."
                else -> "2000–2099: nichts dazuzählen."
            },
        )
        val yearCode = yearSum % 7
        if (yearSum >= 7) steps += reduceStep(yearSum, "Größtes Vielfaches von 7 abziehen – das ist der Jahrescode.")
        val total = day + mc + yearCode
        steps += step("$day + $mc + $yearCode", total, "Tag + Monatscode + Jahrescode.")
        if (total >= 7) steps += reduceStep(total, null)
        val code = (total % 7).toInt()
        val index = choiceIndex(code)
        steps += Step("Wochentag", Expected.Choice(index), InputKind.CHOICE, "Tag $code: 1 = Montag … 6 = Samstag, 7 oder 0 = Sonntag.")
        return Problem(
            skillId = "w12.weekday",
            key = "d$year-$month-$day",
            prompt = Prompt("$day. ${monthNames[month - 1]} $year", caption = "Welcher Wochentag?"),
            expected = Expected.Choice(index),
            input = InputKind.CHOICE,
            steps = steps,
            method = "Tag + Monatscode + Jahrescode",
            choices = weekdays,
        )
    }

    private fun randomDate(r: Random): Problem {
        val year = when (r.nextInt(10)) {
            0 -> r.between(1800, 1899)
            1 -> r.between(2100, 2199)
            2, 3, 4, 5 -> r.between(1900, 1999)
            else -> r.between(2000, 2099)
        }
        val month = r.between(1, 12)
        val length = if (month == 2 && isLeap(year)) 29 else monthLengths[month - 1]
        return weekday(r.between(1, length), month, year)
    }

    private val weekdaySkill = Skill(
        id = "w12.weekday",
        title = "Wochentag für jedes Datum",
        method = "Tag + Monatscode + Jahrescode",
        book = BookRef(CH, "Ein Wochentag für jedes Datum"),
        summary = "Zu jedem Geburtstag den Wochentag ausrechnen.",
        explanation = listOf(
            Block.Text("Zum Abschluss ein Klassiker: der Wochentag zu einem beliebigen Datum. Du addierst den Tag, einen Code für den Monat und einen Code für das Jahr und ziehst so viele Siebenen ab wie möglich."),
            Block.Formula("Monatscodes:\nJan 6  Feb 2  Mär 2  Apr 5\nMai 0  Jun 3  Jul 5  Aug 1\nSep 4  Okt 6  Nov 2  Dez 4\n(Schaltjahr: Jan 5, Feb 1)"),
            Block.Steps(
                listOf(
                    "Jahrescode: letzte zwei Ziffern plus ein Viertel davon (abgerundet), dazu 1 für die 1900er, 3 für die 1800er, 5 für die 2100er. Dann Siebenen abziehen.",
                    "Beispiel 8. Mai 1987: Jahrescode 87 + 21 + 1 = 109, 109 − 105 = 4.",
                    "Tag + Monat + Jahr: 8 + 0 + 4 = 12, 12 − 7 = 5.",
                    "Tag 5 ist ein Freitag.",
                ),
            ),
            Block.Example(weekday(8, 5, 1987)),
            Block.Tip("Die Codes für die Wochentage: 1 Montag bis 6 Samstag, 7 oder 0 Sonntag – genau wie bei Neujahr."),
        ),
        goals = Goals(testSize = 6, minAccuracy = 0.83, star1 = 35.0, star2 = 24.0, star3 = 15.0),
        generator = { r -> randomDate(r) },
    )

    val world = World(
        id = "w12",
        number = 12,
        title = "Mathe-Magie",
        subtitle = "Kapitel 9 – Ohne Kaninchen und Zylinder",
        skills = listOf(magic1089Skill, missing, jumpsSkill, magicSkill, cubeSkill, sqrtSkill, newYearSkill, weekdaySkill),
    )
}
