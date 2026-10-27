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
import java.math.BigDecimal

/**
 * Welt 5 – Kommazahlen.
 * Eine Ergänzung zum Buch: Man rechnet ohne Komma mit den Methoden aus Kapitel 2
 * und setzt das Komma am Ende.
 */
internal object World5 {

    private val extra = BookRef(null, "Ergänzung: Kommazahlen mit den Methoden aus Kapitel 2")

    private fun placesWord(k: Int): String = if (k == 1) "eine Stelle" else "$k Stellen"

    // --- Komma verschieben ----------------------------------------------------------------------

    /** value × 10^k (k > 0) oder value ÷ 10^-k (k < 0). */
    fun shift(value: BigDecimal, k: Int): Problem {
        val power = BigDecimal.TEN.pow(kotlin.math.abs(k))
        val result = if (k > 0) value.multiply(power) else value.divide(power)
        val op = if (k > 0) X else DIV
        val hint = if (k > 0) "Mal ${n(power.toLong())}: Komma um ${placesWord(k)} nach rechts." else "Geteilt durch ${n(power.toLong())}: Komma um ${placesWord(-k)} nach links."
        return decProblem(
            skillId = "w5.shift",
            key = "${value.toPlainString()}s$k",
            prompt = Prompt("${d(value)} $op ${n(power.toLong())}"),
            value = result,
            places = null,
            steps = listOf(decStep("${d(value)} $op ${n(power.toLong())}", result, null, hint)),
            method = "Komma verschieben",
        )
    }

    private val shift = Skill(
        id = "w5.shift",
        title = "Komma verschieben",
        method = "Mal und geteilt durch 10, 100, 1.000",
        book = extra,
        summary = "Kommazahlen mit 10, 100 und 1.000 malnehmen und teilen.",
        explanation = listOf(
            Block.Text("Mit Kommazahlen rechnest du im Kopf fast genauso wie mit ganzen Zahlen. Der Trick besteht darin, das Komma am Anfang wegzudenken und am Ende an die richtige Stelle zu setzen. Dafür musst du sicher wissen, wie sich das Komma beim Malnehmen und Teilen mit 10, 100 und 1.000 bewegt."),
            Block.Steps(
                listOf(
                    "Mal 10, 100, 1.000: Das Komma rückt um so viele Stellen nach rechts, wie die Zahl Nullen hat.",
                    "Geteilt durch 10, 100, 1.000: Das Komma rückt nach links.",
                    "Fehlen Stellen, füllst du mit Nullen auf: 4,5 × 100 = 450 und 7 ÷ 100 = 0,07.",
                ),
            ),
            Block.Example(shift(BigDecimal("3.85"), 2)),
            Block.Example(shift(BigDecimal("62"), -3)),
        ),
        goals = Goals(testSize = 10, star1 = 4.5, star2 = 3.0, star3 = 1.8),
        guidedCount = 3,
        generator = { r ->
            val k = r.pick(listOf(1, 2, 3, -1, -2, -3))
            val scale = r.between(0, 2)
            val unscaled = r.betweenL(2, 999).let { if (it % 10 == 0L) it + 1 else it }
            shift(dec(unscaled, scale), k)
        },
    )

    // --- Kommazahl mal ganze Zahl -------------------------------------------------------------

    /**
     * (digits / 10^scale) × number: ohne Komma rechnen (von links nach rechts), dann Komma setzen.
     * [digits] ist einstellig oder zweistellig.
     */
    fun decimalTimes(skillId: String, digits: Long, scale: Int, number: Long, percentText: String? = null): Problem {
        val factor = dec(digits, scale)
        val intProduct = digits * number
        val result = dec(intProduct, scale)
        val steps = mutableListOf<Step>()
        val mulSteps = when {
            digits == 1L -> emptyList()
            digits < 10 -> Methods.multiplyLeftToRight(number, digits)
            else -> Methods.multiplyLeftToRight(digits, number)
        }
        if (mulSteps.isNotEmpty()) {
            val first = mulSteps.first()
            val lead = if (percentText != null) {
                "$percentText sind ${d(factor)}. Rechne erst ohne Komma: ${n(digits)} $X ${n(number)}."
            } else {
                "Erst das Komma weglassen: ${n(digits)} $X ${n(number)}."
            }
            steps += first.copy(hint = lead + (first.hint?.let { " $it" } ?: ""))
            steps += mulSteps.drop(1)
        }
        if (digits != 1L && steps.lastOrNull()?.expected != com.helddertierwelt.mentalmath.domain.climb.model.exact(intProduct)) {
            steps += step("${n(digits)} $X ${n(number)}", intProduct)
        }
        val finalHint = if (steps.isEmpty() && percentText != null) {
            "$percentText sind ${d(factor)}, also ein Zehntel: Komma um ${placesWord(scale)} nach links."
        } else {
            "${d(factor)} hat ${placesWord(scale)} nach dem Komma – also das Komma um ${placesWord(scale)} nach links."
        }
        steps += decStep(
            "${n(intProduct)} $DIV ${n(com.helddertierwelt.mentalmath.domain.climb.math.Num.pow10(scale))}",
            result,
            null,
            finalHint,
        )
        val promptText = if (percentText != null) "$percentText von ${n(number)}" else "${d(factor)} $X ${n(number)}"
        return decProblem(
            skillId = skillId,
            key = "${digits}e$scale x$number${if (percentText != null) "%" else ""}",
            prompt = Prompt(promptText),
            value = result,
            places = null,
            steps = steps,
            method = "Ohne Komma rechnen, Komma setzen",
        )
    }

    private val dec21 = Skill(
        id = "w5.dec21",
        title = "Kommazahl mal zweistellig",
        method = "Ohne Komma rechnen, Komma setzen",
        book = extra,
        summary = "Aufgaben wie 0,7 × 86 sicher und schnell lösen.",
        explanation = listOf(
            Block.Text("Bei 0,7 × 86 rechnest du zuerst so, als gäbe es kein Komma: 7 × 86. Das ist eine Aufgabe aus Welt 4 und geht von links nach rechts. Erst ganz am Ende kommt das Komma zurück – genau eine Stelle, weil 0,7 eine Nachkommastelle hat."),
            Block.Steps(
                listOf(
                    "Komma weglassen: 0,7 × 86 wird 7 × 86.",
                    "Von links nach rechts rechnen: 7 × 80 = 560, 7 × 6 = 42, zusammen 602.",
                    "Nachkommastellen zählen und das Komma setzen: 60,2.",
                ),
            ),
            Block.Example(decimalTimes("w5.dec21", 7, 1, 86)),
            Block.Tip("Mach am Ende den Überschlag: 0,7 ist etwas weniger als 1, also muss das Ergebnis etwas kleiner als 86 sein. 60,2 passt."),
        ),
        goals = Goals(testSize = 10, star1 = 10.0, star2 = 7.0, star3 = 4.5),
        generator = { r ->
            val number = r.betweenL(12, 98).let { if (it % 10 == 0L) it + 3 else it }
            decimalTimes("w5.dec21", r.betweenL(2, 9), 1, number)
        },
    )

    private val dec31 = Skill(
        id = "w5.dec31",
        title = "Kommazahl mal dreistellig",
        method = "Ohne Komma rechnen, Komma setzen",
        book = extra,
        summary = "Aufgaben wie 0,7 × 345 im Kopf lösen.",
        explanation = listOf(
            Block.Text("Hier wird die ganze Zahl dreistellig. Die Methode bleibt dieselbe: Komma weg, mit der Methode aus Welt 4 rechnen, Komma zurück."),
            Block.Example(decimalTimes("w5.dec31", 7, 1, 345)),
            Block.Tip("Die Zwischensumme nach den Hundertern und Zehnern ist der schwierigste Teil – sag sie dir leise vor."),
        ),
        goals = Goals(testSize = 10, star1 = 16.0, star2 = 11.0, star3 = 7.0),
        generator = { r ->
            val number = r.betweenL(112, 989).let { if (it % 10 == 0L) it + 4 else it }
            decimalTimes("w5.dec31", r.betweenL(2, 9), 1, number)
        },
    )

    private val decMix = Skill(
        id = "w5.decmix",
        title = "Mehr Stellen nach dem Komma",
        method = "Nachkommastellen zählen",
        book = extra,
        summary = "Aufgaben wie 4,3 × 6 oder 0,07 × 86 lösen.",
        explanation = listOf(
            Block.Text("Egal wo das Komma steht: Du rechnest immer ohne Komma und zählst am Ende die Nachkommastellen. Bei 0,07 sind es zwei, also rückt das Komma zwei Stellen nach links."),
            Block.Steps(
                listOf(
                    "4,3 × 6: rechne 43 × 6 = 258. Eine Nachkommastelle → 25,8.",
                    "0,07 × 86: rechne 7 × 86 = 602. Zwei Nachkommastellen → 6,02.",
                ),
            ),
            Block.Example(decimalTimes("w5.decmix", 43, 1, 6)),
            Block.Example(decimalTimes("w5.decmix", 7, 2, 86)),
            Block.Tip("Überschlag als Kontrolle: 4,3 × 6 ist etwas mehr als 4 × 6 = 24. Also passt 25,8."),
        ),
        goals = Goals(testSize = 10, star1 = 11.0, star2 = 7.5, star3 = 5.0),
        generator = { r ->
            if (r.nextBoolean()) {
                val digits = r.betweenL(12, 98).let { if (it % 10 == 0L) it + 1 else it }
                decimalTimes("w5.decmix", digits, 1, r.betweenL(3, 9))
            } else {
                val number = r.betweenL(12, 98).let { if (it % 10 == 0L) it + 7 else it }
                decimalTimes("w5.decmix", r.betweenL(2, 9), 2, number)
            }
        },
    )

    private val percent = Skill(
        id = "w5.percent",
        title = "Prozent als Kommazahl",
        method = "Prozent → Kommazahl → rechnen",
        book = BookRef(5, "Weitere Trinkgeld-Tipps"),
        summary = "Aufgaben wie „70 % von 86“ mit der Kommazahl-Methode lösen.",
        explanation = listOf(
            Block.Text("Prozent heißt „von hundert“. 70 % sind also 70 Hundertstel oder 0,7. Damit wird jede Prozentaufgabe zu einer Kommazahl-Aufgabe, die du schon kannst. Im Buch wird so zum Beispiel ein Trinkgeld von 10 % oder 20 % berechnet."),
            Block.Steps(
                listOf(
                    "Prozent in eine Kommazahl umwandeln: 70 % = 0,7 und 30 % = 0,3.",
                    "Ohne Komma rechnen: 7 × 86 = 602.",
                    "Komma setzen: 60,2.",
                ),
            ),
            Block.Example(decimalTimes("w5.percent", 7, 1, 86, "70 %")),
            Block.Tip("10 % sind einfach Komma eine Stelle nach links. 20 % sind das Doppelte davon, 30 % das Dreifache."),
        ),
        goals = Goals(testSize = 10, star1 = 11.0, star2 = 7.5, star3 = 5.0),
        generator = { r ->
            val digit = r.betweenL(1, 9)
            val number = if (r.nextBoolean()) r.betweenL(12, 98) else r.betweenL(112, 489)
            decimalTimes("w5.percent", digit, 1, number.let { if (it % 10 == 0L) it + 2 else it }, "${digit * 10} %")
        },
    )

    val world = World(
        id = "w5",
        number = 5,
        title = "Kommazahlen",
        subtitle = "Ergänzung – rechnen wie ohne Komma",
        skills = listOf(shift, dec21, dec31, decMix, percent),
    )
}
