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
import com.helddertierwelt.mentalmath.domain.climb.model.World

/** Welt 4 – Multiplikation mit einstelligen Zahlen und Quadrate zweistelliger Zahlen (2. Kapitel). */
internal object World4 {

    fun multiply(skillId: String, number: Long, factor: Long): Problem {
        val roundUp = skillId == "w4.roundup"
        return intProblem(
            skillId = skillId,
            key = "${number}x$factor",
            prompt = Prompt("${n(number)} $X ${n(factor)}"),
            value = number * factor,
            steps = if (roundUp) Methods.multiplyRoundingUp(number, factor) else Methods.multiplyLeftToRight(number, factor),
            method = if (roundUp) "Aufrunden" else "Von links nach rechts",
        )
    }

    private val mul21 = Skill(
        id = "w4.mul21",
        title = "Zweistellig mal einstellig",
        method = "Von links nach rechts",
        book = BookRef(2, "Multiplikationen von zweistelligen Zahlen mit einstelligen"),
        summary = "Zweistellige Zahlen im Kopf mit einer Ziffer malnehmen.",
        explanation = listOf(
            Block.Text("Auch beim Multiplizieren beginnst du mit der großen Stelle. Die zweistellige Zahl wird in Zehner und Einer zerlegt, beide Teile werden einzeln multipliziert und dann addiert. Das große Teilergebnis kommt zuerst – so kannst du den Anfang der Antwort schon nennen, bevor du fertig bist."),
            Block.Steps(
                listOf(
                    "Zehner mal Zahl: bei 57 × 8 also 50 × 8 = 400.",
                    "Einer mal Zahl: 7 × 8 = 56.",
                    "Beides addieren: 400 + 56 = 456.",
                ),
            ),
            Block.Example(multiply("w4.mul21", 57, 8)),
            Block.Tip("Fängt die Zahl mit 5 an und ist der Faktor gerade, wird das erste Teilergebnis ein glatter Hunderter. Dann ist die Addition geschenkt."),
        ),
        goals = Goals(testSize = 10, star1 = 9.0, star2 = 6.0, star3 = 4.0),
        generator = { r ->
            val number = r.betweenL(12, 97).let { if (it % 10 == 0L) it + 3 else it }
            multiply("w4.mul21", number, r.betweenL(3, 9))
        },
    )

    private val roundUp = Skill(
        id = "w4.roundup",
        title = "Aufrunden bei 8 und 9",
        method = "Aufrunden und abziehen",
        book = BookRef(2, "Aufrunden"),
        summary = "Zahlen, die auf 8 oder 9 enden, mit einem Trick schneller multiplizieren.",
        explanation = listOf(
            Block.Text("Endet eine Zahl auf 8 oder 9, ist sie fast ein runder Zehner. Dann ist es oft leichter, mit dem Zehner zu rechnen und den kleinen Überschuss am Ende abzuziehen."),
            Block.Steps(
                listOf(
                    "Auf den nächsten Zehner aufrunden: 79 wird 80.",
                    "Mit der runden Zahl multiplizieren: 80 × 7 = 560.",
                    "Den Überschuss ausrechnen: 1 × 7 = 7.",
                    "Abziehen: 560 − 7 = 553.",
                ),
            ),
            Block.Example(multiply("w4.roundup", 79, 7)),
            Block.Tip("Das lohnt sich vor allem, wenn die Zahl nur 1 oder 2 vom Zehner entfernt ist. Bei größerem Abstand wird die Subtraktion zu schwer – dann lieber normal von links nach rechts."),
        ),
        goals = Goals(testSize = 10, star1 = 9.0, star2 = 6.0, star3 = 4.0),
        generator = { r ->
            val tens = r.betweenL(1, 9)
            val number = tens * 10 + r.pick(listOf(8L, 9L, 9L))
            multiply("w4.roundup", number, r.betweenL(3, 9))
        },
    )

    private val mul31 = Skill(
        id = "w4.mul31",
        title = "Dreistellig mal einstellig",
        method = "Hunderter, Zehner, Einer",
        book = BookRef(2, "Multiplikationen mit dreistelligen Zahlen"),
        summary = "Dreistellige Zahlen mit einer Ziffer malnehmen.",
        explanation = listOf(
            Block.Text("Bei dreistelligen Zahlen kommt nur ein Teil dazu: Hunderter, Zehner und Einer werden nacheinander multipliziert. Die eigentliche Kunst ist, die Zwischensumme im Kopf zu behalten, während du den nächsten Teil ausrechnest."),
            Block.Steps(
                listOf(
                    "Hunderter mal Zahl: 486 × 7 → 400 × 7 = 2.800.",
                    "Zehner mal Zahl und dazuzählen: 80 × 7 = 560, zusammen 3.360.",
                    "Einer mal Zahl und dazuzählen: 6 × 7 = 42, zusammen 3.402.",
                ),
            ),
            Block.Example(multiply("w4.mul31", 486, 7)),
            Block.Tip("Sag die Zwischensumme ein paarmal leise vor dich hin, während du weiterrechnest. Wenn du weißt, dass beim letzten Schritt ein Übertrag kommt, kannst du den Anfang des Ergebnisses schon aussprechen."),
        ),
        goals = Goals(testSize = 10, star1 = 15.0, star2 = 10.0, star3 = 6.5),
        generator = { r ->
            val number = r.betweenL(112, 989).let { if (it % 10 == 0L) it + 6 else it }
            multiply("w4.mul31", number, r.betweenL(3, 9))
        },
    )

    fun squareProblem(a: Long): Problem = intProblem(
        skillId = "w4.square2",
        key = "${a}sq",
        prompt = Prompt("${n(a)}²"),
        value = a * a,
        steps = Methods.square(a, 10),
        method = "Auf- und Abrunden",
    )

    private val square2 = Skill(
        id = "w4.square2",
        title = "Quadrate zweistelliger Zahlen",
        method = "Auf- und Abrunden",
        book = BookRef(2, "Quadratisch, praktisch: Das Quadrieren zweistelliger Zahlen"),
        summary = "Jede zweistellige Zahl schneller als ein Taschenrechner quadrieren.",
        explanation = listOf(
            Block.Text("Statt eine Zahl mit sich selbst zu multiplizieren, gehst du von ihr aus um denselben Abstand nach oben und nach unten. Eine der beiden neuen Zahlen ist rund, deshalb ist das Produkt leicht. Weil die beiden Zahlen gleich weit von der Ausgangszahl entfernt sind, fehlt danach nur noch das Quadrat des Abstands."),
            Block.Formula("A² = (A + d) × (A − d) + d²"),
            Block.Steps(
                listOf(
                    "Den Abstand d zum nächsten Zehner bestimmen. Endet die Zahl auf 6 bis 9, rundest du auf, bei 1 bis 4 ab.",
                    "Eine Zahl um d nach oben, die andere um d nach unten verschieben.",
                    "Die beiden Zahlen multiplizieren – eine davon ist rund.",
                    "d² addieren. Das ist immer 1, 4, 9, 16 oder 25.",
                ),
            ),
            Block.Example(squareProblem(47)),
            Block.Example(squareProblem(92)),
            Block.Tip("Bei Zahlen in den 90ern rundest du auf 100 auf. Dann ist die Multiplikation fast geschenkt."),
        ),
        goals = Goals(testSize = 8, star1 = 15.0, star2 = 10.0, star3 = 6.0),
        generator = { r -> squareProblem(r.betweenL(11, 99).let { if (it % 10 == 0L) it + 1 else it }) },
    )

    val world = World(
        id = "w4",
        number = 4,
        title = "Mal einstellig",
        subtitle = "Kapitel 2 – Multiplikation und Quadrate",
        skills = listOf(mul21, roundUp, mul31, square2),
    )
}
