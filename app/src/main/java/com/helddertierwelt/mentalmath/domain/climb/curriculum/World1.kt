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

/**
 * Welt 1 – Zahlengefühl.
 * Die Grundlagen, die das Buch voraussetzt: Ergänzen, Zehnerübergang, Einmaleins,
 * Verdoppeln/Halbieren und Rechnen mit Nullen.
 */
internal object World1 {

    private val basics = BookRef(null, "Grundlagen, die das Buch voraussetzt")

    // --- Ergänzen zur 10 und zur 100 ---------------------------------------------------------

    fun complementTen(value: Long, target: Long): Problem {
        val missing = target - value
        val hint = if (target == 10L) "Welche Zahl fehlt bis 10?" else "Nur die Zehner ansehen: ${n(value / 10)} + ${n(missing / 10)} = 10."
        return intProblem(
            skillId = "w1.complement",
            key = "$value+?=$target",
            prompt = Prompt(text = "${n(value)} + ?", caption = "Ergänze zu ${n(target)}"),
            value = missing,
            steps = listOf(step("${n(target)} $MINUS ${n(value)}", missing, hint)),
        )
    }

    private val complement = Skill(
        id = "w1.complement",
        title = "Ergänzen zur 10 und zur 100",
        method = "Zahlenpaare",
        book = basics,
        summary = "Blitzschnell sehen, was bis zur nächsten runden Zahl fehlt.",
        explanation = listOf(
            Block.Text("Fast jede Methode in diesem Kletterweg rundet irgendwann auf eine runde Zahl und gleicht den Rest aus. Dafür musst du sofort wissen, was bis 10 oder bis 100 fehlt – ohne nachzudenken."),
            Block.Steps(
                listOf(
                    "Bis 10: Merke dir die Paare 1 + 9, 2 + 8, 3 + 7, 4 + 6 und 5 + 5.",
                    "Bis 100 bei glatten Zehnern: Es sind dieselben Paare, nur mit einer 0 dahinter. 30 + 70 = 100, weil 3 + 7 = 10.",
                ),
            ),
            Block.Example(complementTen(60, 100)),
            Block.Tip("Ziel ist, die Antwort zu wissen, nicht zu rechnen. Übe so lange, bis die Zahl einfach „da“ ist."),
        ),
        goals = Goals(testSize = 12, star1 = 3.0, star2 = 2.0, star3 = 1.2),
        guidedCount = 3,
        generator = { r ->
            if (r.nextBoolean()) complementTen(r.betweenL(1, 9), 10) else complementTen(r.betweenL(1, 9) * 10, 100)
        },
    )

    // --- Plus und Minus bis 20 (Zehnerübergang) ------------------------------------------------

    fun addOverTen(a: Long, b: Long): Problem {
        val fill = 10 - a
        val rest = b - fill
        return intProblem(
            skillId = "w1.addsub20",
            key = "$a+$b",
            prompt = Prompt("${n(a)} + ${n(b)}"),
            value = a + b,
            steps = listOf(
                step("${n(a)} + ${n(fill)}", 10, "Erst bis 10 auffüllen: Von ${n(b)} nimmst du ${n(fill)}."),
                step("10 + ${n(rest)}", a + b, "Der Rest von ${n(b)} kommt dazu."),
            ),
        )
    }

    fun subOverTen(a: Long, b: Long): Problem {
        val down = a - 10
        val rest = b - down
        return intProblem(
            skillId = "w1.addsub20",
            key = "$a-$b",
            prompt = Prompt("${n(a)} $MINUS ${n(b)}"),
            value = a - b,
            steps = listOf(
                step("${n(a)} $MINUS ${n(down)}", 10, "Erst bis auf 10 hinunter: Von ${n(b)} nimmst du ${n(down)}."),
                step("10 $MINUS ${n(rest)}", a - b, "Den Rest von ${n(b)} abziehen."),
            ),
        )
    }

    private val addSub20 = Skill(
        id = "w1.addsub20",
        title = "Plus und Minus über die 10",
        method = "Zehnerübergang",
        book = basics,
        summary = "Einstellige Zahlen über die 10 hinweg addieren und subtrahieren.",
        explanation = listOf(
            Block.Text("Das Buch geht davon aus, dass du einstellige Zahlen sicher addieren und subtrahieren kannst. Schwierig wird es nur, wenn das Ergebnis über die 10 springt. Der Trick: an der 10 einen Zwischenhalt machen."),
            Block.Steps(
                listOf(
                    "Plus: Fülle zuerst bis 10 auf und addiere dann den Rest.",
                    "Minus: Gehe zuerst auf 10 hinunter und ziehe dann den Rest ab.",
                ),
            ),
            Block.Example(addOverTen(7, 6)),
            Block.Example(subOverTen(15, 8)),
            Block.Tip("Mit der Zeit brauchst du den Zwischenhalt nicht mehr – dann weißt du 7 + 6 = 13 einfach."),
        ),
        goals = Goals(testSize = 12, star1 = 3.5, star2 = 2.2, star3 = 1.3),
        guidedCount = 4,
        generator = { r ->
            val a = r.betweenL(2, 9)
            val b = r.betweenL(11 - a, 9)
            if (r.nextBoolean()) addOverTen(a, b) else subOverTen(a + b, b)
        },
    )

    // --- Kleines Einmaleins -----------------------------------------------------------------------

    fun timesFact(a: Long, b: Long): Problem {
        val steps: List<Step> = if (b > 5 && a > 2) {
            listOf(
                step("${n(a)} $X 5", a * 5, "Über die Fünferreihe: erst ${n(a)} mal 5."),
                step("${n(a)} $X ${n(b - 5)}", a * (b - 5), "Dann die restlichen ${n(b - 5)}."),
                step("${n(a * 5)} + ${n(a * (b - 5))}", a * b),
            )
        } else {
            listOf(step("${n(a)} $X ${n(b)}", a * b))
        }
        return intProblem(
            skillId = "w1.times",
            key = "${minOf(a, b)}x${maxOf(a, b)}",
            prompt = Prompt("${n(a)} $X ${n(b)}"),
            value = a * b,
            steps = steps,
        )
    }

    private val times = Skill(
        id = "w1.times",
        title = "Das kleine Einmaleins",
        method = "Auswendig wissen",
        book = BookRef(2, "Multiplikationstabelle der Zahlen von 1 bis 10"),
        summary = "Alle Produkte von 2 × 2 bis 10 × 10 sofort abrufen.",
        explanation = listOf(
            Block.Text("Alle Multiplikationsmethoden im Buch zerlegen große Aufgaben in kleine Einmaleins-Aufgaben. Wer hier zögert, verliert später bei jeder Rechnung Zeit. Deshalb lohnt es sich, das Einmaleins wirklich auswendig zu können – vorwärts und rückwärts."),
            Block.Steps(
                listOf(
                    "Die meisten Produkte kennst du schon. Konzentriere dich auf die, bei denen du kurz überlegen musst – oft sind es 6, 7, 8 und 9.",
                    "Wenn dir ein Ergebnis fehlt, leite es über die Fünferreihe her: 8 × 7 = 8 × 5 + 8 × 2 = 40 + 16.",
                    "Übe, bis du das Ergebnis weißt, statt es herzuleiten.",
                ),
            ),
            Block.Example(timesFact(8, 7)),
            Block.Tip("Tauschaufgaben sind gleich: 7 × 8 ist dasselbe wie 8 × 7. Du musst dir also nur die Hälfte merken."),
        ),
        goals = Goals(testSize = 15, star1 = 3.0, star2 = 2.0, star3 = 1.3),
        guidedCount = 3,
        generator = { r ->
            val a = if (r.nextInt(4) == 0) r.betweenL(2, 10) else r.betweenL(3, 9)
            val b = if (r.nextInt(4) == 0) r.betweenL(2, 10) else r.betweenL(6, 9)
            if (r.nextBoolean()) timesFact(a, b) else timesFact(b, a)
        },
    )

    // --- Einmaleins rückwärts ---------------------------------------------------------------------

    fun divisionFact(divisor: Long, quotient: Long): Problem {
        val dividend = divisor * quotient
        return intProblem(
            skillId = "w1.divfacts",
            key = "$dividend:$divisor",
            prompt = Prompt("${n(dividend)} $DIV ${n(divisor)}"),
            value = quotient,
            steps = listOf(step("${n(divisor)} $X ? = ${n(dividend)}", quotient, "Frag dich: Welche Zahl mal ${n(divisor)} ergibt ${n(dividend)}?")),
        )
    }

    private val divFacts = Skill(
        id = "w1.divfacts",
        title = "Einmaleins rückwärts",
        method = "Umkehraufgabe",
        book = BookRef(2, "Multiplikationstabelle der Zahlen von 1 bis 10"),
        summary = "Geteilt-Aufgaben des Einmaleins sofort lösen.",
        explanation = listOf(
            Block.Text("Beim Teilen im Kopf fragst du immer: Wie oft passt die Zahl hinein? Das geht nur schnell, wenn du das Einmaleins auch rückwärts kennst."),
            Block.Steps(
                listOf(
                    "Lies 56 ÷ 7 als Frage: 7 mal wie viel ist 56?",
                    "Die Antwort kommt aus dem Einmaleins: 7 × 8 = 56, also 8.",
                ),
            ),
            Block.Example(divisionFact(6, 7)),
        ),
        goals = Goals(testSize = 12, star1 = 3.5, star2 = 2.3, star3 = 1.5),
        guidedCount = 3,
        generator = { r -> divisionFact(r.betweenL(3, 9), r.betweenL(3, 9)) },
    )

    // --- Verdoppeln und Halbieren -------------------------------------------------------------

    fun double(x: Long): Problem {
        val tens = x / 10 * 10
        val ones = x % 10
        val steps = if (ones == 0L) {
            listOf(step("${n(x)} $X 2", 2 * x))
        } else {
            listOf(
                step("${n(tens)} $X 2", 2 * tens, "Erst die Zehner verdoppeln."),
                step("${n(ones)} $X 2", 2 * ones, "Dann die Einer."),
                step("${n(2 * tens)} + ${n(2 * ones)}", 2 * x),
            )
        }
        return intProblem(
            skillId = "w1.doublehalf",
            key = "2x$x",
            prompt = Prompt("Das Doppelte von ${n(x)}"),
            value = 2 * x,
            steps = steps,
        )
    }

    /** Halbieren: in einen leicht halbierbaren großen Teil und einen Rest zerlegen. */
    fun half(x: Long): Problem {
        val unit = if (x >= 100) 100L else 10L
        var big = x / unit * unit
        if ((big / unit) % 2 == 1L) big -= unit
        val rest = x - big
        val steps = mutableListOf<Step>()
        if (big > 0 && rest > 0) {
            steps += step("${n(big)} $DIV 2", big / 2, "Zerlege ${n(x)} in ${n(big)} + ${n(rest)} – beides lässt sich leicht halbieren.")
            steps += step("${n(rest)} $DIV 2", rest / 2)
            steps += step("${n(big / 2)} + ${n(rest / 2)}", x / 2)
        } else {
            steps += step("${n(x)} $DIV 2", x / 2)
        }
        return intProblem(
            skillId = "w1.doublehalf",
            key = "$x/2",
            prompt = Prompt("Die Hälfte von ${n(x)}"),
            value = x / 2,
            steps = steps,
        )
    }

    private val doubleHalf = Skill(
        id = "w1.doublehalf",
        title = "Verdoppeln und Halbieren",
        method = "Zerlegen",
        book = basics,
        summary = "Zahlen bis 1000 schnell verdoppeln und halbieren.",
        explanation = listOf(
            Block.Text("Verdoppeln und Halbieren tauchen ständig auf: beim Rechnen mit 5 und 25, beim Teilen, bei Prozenten und beim Trinkgeld. Beides geht im Kopf am besten in zwei Teilen."),
            Block.Steps(
                listOf(
                    "Verdoppeln: Zehner und Einer getrennt verdoppeln und zusammenzählen.",
                    "Halbieren: Die Zahl so zerlegen, dass sich beide Teile glatt halbieren lassen, zum Beispiel 74 = 60 + 14.",
                    "Beide Hälften addieren: 30 + 7 = 37.",
                ),
            ),
            Block.Example(double(68)),
            Block.Example(half(346)),
        ),
        goals = Goals(testSize = 10, star1 = 5.0, star2 = 3.2, star3 = 2.0),
        guidedCount = 4,
        generator = { r ->
            if (r.nextBoolean()) {
                double(r.betweenL(13, 99).let { if (it % 10 == 0L) it + 3 else it })
            } else {
                val x = if (r.nextBoolean()) r.betweenL(16, 98) else r.betweenL(102, 998)
                half(if (x % 2 == 1L) x + 1 else x)
            }
        },
    )

    // --- Rechnen mit Nullen -----------------------------------------------------------------------

    fun withZeros(digit: Long, zeros: Int, factor: Long): Problem {
        val big = digit * pow10(zeros)
        val zerosText = if (zeros == 1) "eine 0" else "$zeros Nullen"
        return intProblem(
            skillId = "w1.zeros",
            key = "${big}x$factor",
            prompt = Prompt("${n(big)} $X ${n(factor)}"),
            value = big * factor,
            steps = listOf(
                step("${n(digit)} $X ${n(factor)}", digit * factor, "Die Nullen zuerst weglassen."),
                step("${n(digit * factor)} mit $zerosText", big * factor, "Jetzt die Nullen wieder anhängen."),
            ),
        )
    }

    private fun pow10(k: Int): Long = com.helddertierwelt.mentalmath.domain.climb.math.Num.pow10(k)

    private val zeros = Skill(
        id = "w1.zeros",
        title = "Rechnen mit Nullen",
        method = "Nullen anhängen",
        book = BookRef(2, "Multiplikationen von zweistelligen Zahlen mit einstelligen"),
        summary = "Aufgaben wie 40 × 7 oder 600 × 8 ohne Zögern lösen.",
        explanation = listOf(
            Block.Text("Die Methoden im Buch arbeiten von links nach rechts, also mit den großen Stellen zuerst. Dabei rechnest du ständig Dinge wie 40 × 7: Das ist einfach 4 × 7 mit einer Null dahinter."),
            Block.Steps(
                listOf(
                    "Die Nullen am Ende weglassen und das Einmaleins rechnen.",
                    "Die weggelassenen Nullen wieder anhängen.",
                ),
            ),
            Block.Example(withZeros(6, 2, 8)),
            Block.Tip("Achte auf Ergebnisse, die selbst schon auf 0 enden: 50 × 4 = 200, nicht 20."),
        ),
        goals = Goals(testSize = 12, star1 = 3.5, star2 = 2.2, star3 = 1.4),
        guidedCount = 3,
        generator = { r -> withZeros(r.betweenL(2, 9), if (r.nextInt(3) == 0) 2 else 1, r.betweenL(3, 9)) },
    )

    val world = World(
        id = "w1",
        number = 1,
        title = "Zahlengefühl",
        subtitle = "Die Bausteine für alles Weitere",
        skills = listOf(complement, addSub20, times, divFacts, doubleHalf, zeros),
    )
}
