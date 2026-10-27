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
import com.helddertierwelt.mentalmath.domain.climb.model.Goals
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import com.helddertierwelt.mentalmath.domain.climb.model.World

/** Welt 2 – Blitztricks aus dem 0. Kapitel. */
internal object World2 {

    private const val CH0 = "Schnelle Tricks – einfache (und beeindruckende) Rechnungen"

    // --- Mal 11 ---------------------------------------------------------------------------------

    fun timesEleven(x: Long, skillId: String): Problem {
        val a = x / 10
        val b = x % 10
        val sum = a + b
        val result = x * 11
        val steps = mutableListOf<Step>()
        steps += step("${n(a)} + ${n(b)}", sum, "Die beiden Ziffern addieren.")
        if (sum >= 10) {
            steps += step("${n(a)} + 1", a + 1, "Die Summe ist zweistellig: Die 1 wandert zur vorderen Ziffer.")
            steps += step("${n(x)} $X 11", result, "Vorne ${n(a + 1)}, in der Mitte ${n(sum % 10)}, hinten ${n(b)}.")
        } else {
            steps += step("${n(x)} $X 11", result, "Die Summe ${n(sum)} kommt zwischen ${n(a)} und ${n(b)}.")
        }
        return intProblem(
            skillId = skillId,
            key = "${x}x11",
            prompt = Prompt("${n(x)} $X 11"),
            value = result,
            steps = steps,
            method = "Elfer-Trick",
        )
    }

    private val timesElevenSimple = Skill(
        id = "w2.times11a",
        title = "Mal 11 – der Elfer-Trick",
        method = "Ziffernsumme in die Mitte",
        book = BookRef(0, "Sofort-Multiplikation"),
        summary = "Jede zweistellige Zahl in einer Sekunde mal 11 nehmen.",
        explanation = listOf(
            Block.Text("Bei einer zweistelligen Zahl mal 11 bleiben die beiden Ziffern außen stehen. Dazwischen kommt ihre Summe. Das Ergebnis entsteht also fast von allein."),
            Block.Steps(
                listOf(
                    "Die beiden Ziffern der Zahl addieren.",
                    "Die Summe zwischen die beiden Ziffern schreiben.",
                ),
            ),
            Block.Example(timesEleven(43, "w2.times11a")),
            Block.Text("Warum das klappt: 43 × 11 ist 430 + 43. Die 4 und die 3 landen in der Mitte übereinander und werden addiert."),
            Block.Tip("In diesem Level ist die Summe immer höchstens 9. Den Fall mit Übertrag übst du im nächsten Level."),
        ),
        goals = Goals(testSize = 10, star1 = 4.0, star2 = 2.5, star3 = 1.6),
        guidedCount = 3,
        generator = { r ->
            val a = r.betweenL(1, 8)
            val b = r.betweenL(1, 9 - a)
            timesEleven(10 * a + b, "w2.times11a")
        },
    )

    private val timesElevenCarry = Skill(
        id = "w2.times11b",
        title = "Mal 11 mit Übertrag",
        method = "Übertrag nach vorne",
        book = BookRef(0, "Sofort-Multiplikation"),
        summary = "Den Elfer-Trick auch nutzen, wenn die Ziffernsumme zweistellig ist.",
        explanation = listOf(
            Block.Text("Ist die Summe der beiden Ziffern 10 oder mehr, passt sie nicht in die Mitte. Dann kommt nur ihre Einerziffer in die Mitte, und die vordere Ziffer wird um 1 größer."),
            Block.Steps(
                listOf(
                    "Die beiden Ziffern addieren, zum Beispiel 7 + 8 = 15.",
                    "Die Einerziffer der Summe (5) kommt in die Mitte.",
                    "Die vordere Ziffer um 1 erhöhen (7 wird 8). Die hintere bleibt.",
                ),
            ),
            Block.Example(timesEleven(78, "w2.times11b")),
            Block.Tip("Rechne immer erst die Summe aus – dann weißt du sofort, ob ein Übertrag kommt."),
        ),
        goals = Goals(testSize = 10, star1 = 5.0, star2 = 3.0, star3 = 2.0),
        guidedCount = 3,
        generator = { r ->
            val a = r.betweenL(2, 9)
            val b = r.betweenL(10 - a, 9)
            timesEleven(10 * a + b, "w2.times11b")
        },
    )

    // --- Quadrate mit Endziffer 5 --------------------------------------------------------------

    fun squareFive(x: Long): Problem {
        val a = x / 10
        val front = a * (a + 1)
        return intProblem(
            skillId = "w2.square5",
            key = "${x}sq",
            prompt = Prompt("${n(x)}²"),
            value = x * x,
            steps = listOf(
                step("${n(a)} $X ${n(a + 1)}", front, "Die Zehnerziffer mal die nächsthöhere Zahl."),
                step("${n(x)}²", x * x, "Hinten immer 25 anhängen."),
            ),
            method = "Fünfer-Quadrat",
        )
    }

    private val squareFive = Skill(
        id = "w2.square5",
        title = "Quadrate mit Endziffer 5",
        method = "Mal die nächste Zahl, dann 25",
        book = BookRef(0, "Quadrieren und mehr"),
        summary = "Zahlen wie 65 oder 85 sofort quadrieren.",
        explanation = listOf(
            Block.Text("Endet eine zweistellige Zahl auf 5, ist ihr Quadrat besonders leicht. Das Ergebnis besteht aus zwei Teilen: vorne ein kleines Produkt, hinten immer 25."),
            Block.Steps(
                listOf(
                    "Die Zehnerziffer mit der nächsthöheren Zahl multiplizieren. Bei 65 also 6 × 7 = 42.",
                    "25 anhängen: 4.225.",
                ),
            ),
            Block.Example(squareFive(65)),
            Block.Tip("Du kannst den vorderen Teil schon laut sagen, bevor du „fünfundzwanzig“ anhängst."),
        ),
        goals = Goals(testSize = 8, star1 = 4.0, star2 = 2.5, star3 = 1.6),
        guidedCount = 3,
        generator = { r -> squareFive(10 * r.betweenL(1, 9) + 5) },
    )

    // --- Gleiche Zehner, Einer ergeben 10 --------------------------------------------------

    fun sameTens(x: Long, y: Long): Problem {
        val a = x / 10
        val b = x % 10
        val c = y % 10
        val front = a * (a + 1)
        val back = b * c
        return intProblem(
            skillId = "w2.sametens",
            key = "${minOf(x, y)}x${maxOf(x, y)}",
            prompt = Prompt("${n(x)} $X ${n(y)}"),
            value = x * y,
            steps = listOf(
                step("${n(a)} $X ${n(a + 1)}", front, "Gleiche Zehnerziffer: sie mal die nächsthöhere Zahl."),
                step("${n(b)} $X ${n(c)}", back, if (back < 10) "Das Produkt der Einer zweistellig schreiben: ${Methods.twoDigits(back)}." else "Das Produkt der Einerziffern."),
                step("${n(x)} $X ${n(y)}", x * y, "Vorne ${n(front)}, hinten ${Methods.twoDigits(back)}."),
            ),
            method = "Zehner-Partner",
        )
    }

    private val sameTens = Skill(
        id = "w2.sametens",
        title = "Gleiche Zehner, Einer ergeben 10",
        method = "Zehner-Partner",
        book = BookRef(0, "Quadrieren und mehr"),
        summary = "Aufgaben wie 47 × 43 in Sekunden lösen.",
        explanation = listOf(
            Block.Text("Der Trick mit den Fünfer-Quadraten funktioniert auch für zwei verschiedene Zahlen – wenn sie die gleiche Zehnerziffer haben und ihre Einerziffern zusammen 10 ergeben."),
            Block.Steps(
                listOf(
                    "Prüfen: gleiche Zehnerziffer? Einer zusammen 10? Bei 47 × 43: 4 und 4, 7 + 3 = 10. Passt.",
                    "Vorderer Teil: Zehnerziffer mal die nächsthöhere Zahl, 4 × 5 = 20.",
                    "Hinterer Teil: Einer mal Einer, 7 × 3 = 21. Immer zweistellig schreiben, also 1 × 9 = 09.",
                    "Zusammensetzen: 2.021.",
                ),
            ),
            Block.Example(sameTens(47, 43)),
            Block.Tip("Ergeben die Einer nicht 10, funktioniert der Trick nicht. Dafür lernst du später die Nahe-beieinander-Methode."),
        ),
        goals = Goals(testSize = 8, star1 = 5.0, star2 = 3.0, star3 = 2.0),
        guidedCount = 3,
        generator = { r ->
            val a = r.betweenL(1, 9)
            val b = r.betweenL(1, 4)
            val x = 10 * a + b
            val y = 10 * a + (10 - b)
            if (r.nextBoolean()) sameTens(x, y) else sameTens(y, x)
        },
    )

    val world = World(
        id = "w2",
        number = 2,
        title = "Blitztricks",
        subtitle = "Kapitel 0 – erste Kunststücke",
        skills = listOf(timesElevenSimple, timesElevenCarry, squareFive, sameTens),
    )
}
