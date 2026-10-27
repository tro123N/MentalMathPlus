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
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.random.Random

/** Welt 3 – Addition und Subtraktion von links nach rechts (1. Kapitel), dazu Geld und Wechselgeld. */
internal object World3 {

    private const val ADD = "Addition von links nach rechts"
    private const val SUB = "Subtraktion von links nach rechts"

    // --- Addition -------------------------------------------------------------------------------

    fun addition(skillId: String, a: Long, b: Long): Problem {
        val nearHundred = b >= 100 && b % 100 >= 95
        val steps = if (nearHundred) Methods.addRoundingUp(a, b, 100) else Methods.addLeftToRight(a, b)
        return intProblem(
            skillId = skillId,
            key = "$a+$b",
            prompt = Prompt("${n(a)} + ${n(b)}"),
            value = a + b,
            steps = steps,
            method = if (nearHundred) "Aufrunden und ausgleichen" else "Von links nach rechts",
        )
    }

    private val add2 = Skill(
        id = "w3.add2",
        title = "Zweistellig plus zweistellig",
        method = "Von links nach rechts",
        book = BookRef(1, "Addition von zweistelligen Zahlen"),
        summary = "Zweistellige Zahlen addieren, indem du erst die Zehner und dann die Einer dazurechnest.",
        explanation = listOf(
            Block.Text("In der Schule rechnet man auf dem Papier von rechts nach links. Im Kopf ist es umgekehrt viel leichter: Du fängst mit der größten Stelle an. So kennst du die Größenordnung des Ergebnisses sofort, und jeder Schritt macht die Aufgabe kleiner."),
            Block.Steps(
                listOf(
                    "Die erste Zahl bleibt stehen.",
                    "Die Zehner der zweiten Zahl dazuzählen – jetzt hast du eine neue, leichtere Aufgabe.",
                    "Zum Schluss die Einer dazuzählen.",
                ),
            ),
            Block.Example(addition("w3.add2", 58, 36)),
            Block.Tip("Sprich die Zwischenergebnisse leise mit: „58 plus 30 ist 88, plus 6 ist 94“. So vergisst du nichts."),
        ),
        goals = Goals(testSize = 10, star1 = 7.0, star2 = 4.5, star3 = 3.0),
        generator = { r ->
            val a = r.betweenL(12, 89)
            val b = r.betweenL(12, 89).let { if (it % 10 == 0L) it + 4 else it }
            addition("w3.add2", a, b)
        },
    )

    private val add3 = Skill(
        id = "w3.add3",
        title = "Dreistellig plus dreistellig",
        method = "Hunderter, Zehner, Einer",
        book = BookRef(1, "Addition dreistelliger Zahlen"),
        summary = "Dreistellige Zahlen in drei kleinen Schritten addieren.",
        explanation = listOf(
            Block.Text("Bei dreistelligen Zahlen bleibt die Methode gleich, es kommt nur ein Schritt dazu. Du zerlegst die zweite Zahl in Hunderter, Zehner und Einer und zählst sie nacheinander zur ersten Zahl. Nach jedem Schritt musst du dir weniger Ziffern merken."),
            Block.Steps(
                listOf(
                    "Die Hunderter der zweiten Zahl addieren.",
                    "Dann die Zehner.",
                    "Dann die Einer.",
                ),
            ),
            Block.Example(addition("w3.add3", 467, 285)),
            Block.Text("Liegt die zweite Zahl knapp unter einem vollen Hunderter, geht es noch schneller: Addiere den vollen Hunderter und ziehe den kleinen Rest wieder ab."),
            Block.Example(addition("w3.add3", 634, 398)),
            Block.Tip("Höre die Zahlen innerlich: „vierhundertsiebenundsechzig plus zweihundert …“ Das betonte „hundert“ zeigt dir, wo du anfängst."),
        ),
        goals = Goals(testSize = 10, star1 = 12.0, star2 = 8.0, star3 = 5.0),
        generator = { r ->
            val a = r.betweenL(123, 879)
            val b = if (r.nextInt(5) == 0) {
                r.betweenL(2, 8) * 100 - r.betweenL(1, 5)
            } else {
                r.betweenL(112, 889).let { if (it % 10 == 0L) it + 3 else it }
            }
            addition("w3.add3", a, b)
        },
    )

    private val addZeros = Skill(
        id = "w3.addzeros",
        title = "Große Zahlen mit Nullen",
        method = "Nur die Stellen addieren, die sich treffen",
        book = BookRef(1, "Addition dreistelliger Zahlen"),
        summary = "Vierstellige Zahlen mit Nullen am Ende in einem oder zwei Schritten addieren.",
        explanation = listOf(
            Block.Text("Viele Additionen beim Kopfrechnen haben eine Zahl, die auf 0 oder 00 endet – das passiert vor allem beim Multiplizieren. Dann musst du nur dort rechnen, wo beide Zahlen eine Ziffer haben. Den Rest kannst du einfach einsetzen."),
            Block.Steps(
                listOf(
                    "Schau, an welchen Stellen beide Zahlen eine Ziffer ungleich 0 haben.",
                    "Nur diese Stellen addieren.",
                    "Wo die große Zahl Nullen hat, die Ziffern der kleinen Zahl einfach einsetzen.",
                ),
            ),
            Block.Example(addition("w3.addzeros", 3400, 728)),
        ),
        goals = Goals(testSize = 10, star1 = 7.0, star2 = 4.5, star3 = 3.0),
        generator = { r ->
            val a = if (r.nextBoolean()) r.betweenL(12, 89) * 100 else r.betweenL(101, 899) * 10
            val b = if (r.nextBoolean()) r.betweenL(11, 99) else r.betweenL(101, 989)
            addition("w3.addzeros", a, b)
        },
    )

    // --- Subtraktion ----------------------------------------------------------------------------

    fun needsBorrow(a: Long, b: Long): Boolean {
        val da = Num.digits(a).reversed()
        val db = Num.digits(b).reversed()
        return db.indices.any { i -> db[i] > da.getOrElse(i) { 0 } }
    }

    fun subtraction2(a: Long, b: Long): Problem {
        val borrow = needsBorrow(a, b)
        return intProblem(
            skillId = "w3.sub2",
            key = "$a-$b",
            prompt = Prompt("${n(a)} $MINUS ${n(b)}"),
            value = a - b,
            steps = if (borrow) Methods.subtractRoundingUp(a, b, 10) else Methods.subtractLeftToRight(a, b),
            method = if (borrow) "Aufrunden und ausgleichen" else "Von links nach rechts",
        )
    }

    private val sub2 = Skill(
        id = "w3.sub2",
        title = "Zweistellig minus zweistellig",
        method = "Aufrunden statt borgen",
        book = BookRef(1, "Subtraktion zweistelliger Zahlen"),
        summary = "Zweistellige Zahlen abziehen – und das lästige Borgen vermeiden.",
        explanation = listOf(
            Block.Text("Auch beim Abziehen gehst du von links nach rechts: erst die Zehner, dann die Einer. Schwierig wird es nur, wenn hinten eine größere Ziffer von einer kleineren abgezogen werden müsste. Dann hilft ein Kniff: Runde die abzuziehende Zahl auf den nächsten Zehner auf."),
            Block.Steps(
                listOf(
                    "Prüfen: Ist die Einerziffer der zweiten Zahl größer als die der ersten? Wenn nein, einfach erst die Zehner und dann die Einer abziehen.",
                    "Wenn ja: Die zweite Zahl auf den nächsten Zehner aufrunden und diesen abziehen.",
                    "Weil du zu viel abgezogen hast, den Unterschied wieder dazuzählen.",
                ),
            ),
            Block.Example(subtraction2(73, 41)),
            Block.Example(subtraction2(62, 27)),
            Block.Tip("Aus einer schweren Subtraktion wird so eine leichte Subtraktion plus eine kleine Addition."),
        ),
        goals = Goals(testSize = 10, star1 = 7.0, star2 = 4.5, star3 = 3.0),
        generator = { r ->
            val a = r.betweenL(31, 98)
            val b = r.betweenL(12, a - 9).let { if (it % 10 == 0L) it + 1 else it }
            subtraction2(a, b)
        },
    )

    fun complementProblem(x: Long): Problem = intProblem(
        skillId = "w3.complement",
        key = "c$x",
        prompt = Prompt("100 $MINUS ${n(x)}", caption = "Komplement: Wie weit ist ${n(x)} von 100 entfernt?"),
        value = 100 - x,
        steps = Methods.complement100(x),
        method = "Komplement",
    )

    private val complement = Skill(
        id = "w3.complement",
        title = "Komplemente zu 100",
        method = "Ziffern ergänzen zu 9 und 10",
        book = BookRef(1, "Der Einsatz von Komplementen"),
        summary = "Sofort sehen, wie weit eine zweistellige Zahl von 100 entfernt ist.",
        explanation = listOf(
            Block.Text("Das Komplement einer Zahl ist der Abstand bis zur nächsten runden Zahl, hier bis 100. Mit Komplementen werden schwierige Subtraktionen zu einfachen Additionen. Und es gibt ein schönes Muster: Bei zwei Zahlen, die zusammen 100 ergeben, ergänzen sich die Zehnerziffern zu 9 und die Einerziffern zu 10."),
            Block.Steps(
                listOf(
                    "Zehnerziffer: Was fehlt bis 9?",
                    "Einerziffer: Was fehlt bis 10?",
                    "Beide Ziffern nebeneinander – fertig. Das Komplement von 36 ist 64.",
                ),
            ),
            Block.Example(complementProblem(36)),
            Block.Tip("Auch das geht von links nach rechts. Mit etwas Übung siehst du das Komplement, ohne zu rechnen."),
        ),
        goals = Goals(testSize = 12, star1 = 4.0, star2 = 2.5, star3 = 1.6),
        guidedCount = 3,
        generator = { r -> complementProblem(r.betweenL(11, 99).let { if (it % 10 == 0L) it - 1 else it }) },
    )

    fun subtraction3(skillId: String, a: Long, b: Long): Problem {
        val borrow = needsBorrow(a, b)
        val up = Num.roundUp(b, 100)
        val (steps, method) = when {
            !borrow -> Methods.subtractLeftToRight(a, b) to "Von links nach rechts"
            a >= up -> Methods.subtractWithComplement(a, b) to "Aufrunden und Komplement"
            else -> {
                // Gleiche Hunderter: erst die Hunderter abziehen, dann zweistellig weiter.
                val hundreds = b / 100 * 100
                val restA = a - hundreds
                val restB = b - hundreds
                val tail = if (needsBorrow(restA, restB)) Methods.subtractRoundingUp(restA, restB, 10) else Methods.subtractLeftToRight(restA, restB)
                (listOf(step("${n(a)} $MINUS ${n(hundreds)}", restA, "Erst die Hunderter abziehen.")) + tail) to "Hunderter, dann zweistellig"
            }
        }
        return intProblem(
            skillId = skillId,
            key = "$a-$b",
            prompt = Prompt("${n(a)} $MINUS ${n(b)}"),
            value = a - b,
            steps = steps,
            method = method,
        )
    }

    private val sub3 = Skill(
        id = "w3.sub3",
        title = "Dreistellig minus dreistellig",
        method = "Aufrunden und Komplement",
        book = BookRef(1, "Subtraktion dreistelliger Zahlen"),
        summary = "Dreistellige Zahlen abziehen, indem du auf Hunderter rundest und das Komplement addierst.",
        explanation = listOf(
            Block.Text("Muss nirgends geborgt werden, ziehst du einfach Hunderter, Zehner und Einer nacheinander ab. Sonst rundest du die abzuziehende Zahl auf den nächsten vollen Hunderter auf. Wie viel du dabei zu viel abgezogen hast, verrät dir das Komplement."),
            Block.Steps(
                listOf(
                    "Die zweite Zahl auf den nächsten Hunderter aufrunden und abziehen.",
                    "Den Abstand zwischen der zweiten Zahl und dem Hunderter bestimmen – das ist das Komplement ihrer letzten beiden Ziffern.",
                    "Dieses Komplement zum Zwischenergebnis addieren.",
                ),
            ),
            Block.Example(subtraction3("w3.sub3", 846, 397)),
            Block.Example(subtraction3("w3.sub3", 632, 274)),
            Block.Tip("Bei 632 − 274 fragst du: Wie weit ist 274 von 300 weg? Genauso weit wie 74 von 100, also 26."),
        ),
        goals = Goals(testSize = 10, star1 = 13.0, star2 = 8.5, star3 = 5.5),
        generator = { r ->
            val a = r.betweenL(312, 989)
            val b = r.betweenL(123, a - 40).let { if (it % 10 == 0L) it + 7 else it }.coerceAtMost(a - 11)
            subtraction3("w3.sub3", a, b)
        },
    )

    private val sub4 = Skill(
        id = "w3.sub4",
        title = "Vierstellig minus dreistellig",
        method = "Aufrunden und Komplement",
        book = BookRef(1, "Subtraktion dreistelliger Zahlen"),
        summary = "Dreistellige Zahlen von vierstelligen abziehen.",
        explanation = listOf(
            Block.Text("Eine dreistellige Zahl von einer vierstelligen abzuziehen ist kaum schwerer. Du rundest wieder auf den nächsten Hunderter auf, ziehst ab und addierst das Komplement."),
            Block.Example(subtraction3("w3.sub4", 1352, 687)),
            Block.Tip("Merke dir das Zwischenergebnis, bevor du das Komplement bildest – sag es dir zur Not zweimal vor."),
        ),
        goals = Goals(testSize = 10, star1 = 14.0, star2 = 9.0, star3 = 6.0),
        generator = { r ->
            val a = r.betweenL(1012, 1989)
            val b = r.betweenL(213, 989).let { if (it % 100 == 0L) it - 13 else it }
            subtraction3("w3.sub4", a, b)
        },
    )

    // --- Geld -----------------------------------------------------------------------------------

    fun moneyAdd(aCents: Long, bCents: Long): Problem {
        val bEuros = bCents / 100
        val bRest = bCents % 100
        val afterEuros = aCents + bEuros * 100
        val total = aCents + bCents
        val aRest = aCents % 100
        val steps = mutableListOf(
            decStep("${d(dec(aCents, 2), 2)} + ${n(bEuros)}", dec(afterEuros, 2), 2, "Erst die ganzen Euro dazu."),
        )
        if (bRest > 0) {
            val centHint = if (aRest + bRest >= 100) {
                "${n(aRest)} + ${n(bRest)} = ${n(aRest + bRest)} Cent, also ein Euro mehr."
            } else {
                "${n(aRest)} + ${n(bRest)} = ${n(aRest + bRest)} Cent."
            }
            steps += decStep("${d(dec(afterEuros, 2), 2)} + ${d(dec(bRest, 2), 2)}", dec(total, 2), 2, centHint)
        }
        return decProblem(
            skillId = "w3.moneyadd",
            key = "$aCents+$bCents",
            prompt = Prompt("${d(dec(aCents, 2), 2)} € + ${d(dec(bCents, 2), 2)} €", unit = "€"),
            value = dec(total, 2),
            places = 2,
            steps = steps,
            method = "Euro, dann Cent",
        )
    }

    private val moneyAdd = Skill(
        id = "w3.moneyadd",
        title = "Geldbeträge addieren",
        method = "Erst Euro, dann Cent",
        book = BookRef(null, "Ergänzung für den Alltag, Methode aus Kapitel 1"),
        summary = "Preise mit Kommastellen zusammenrechnen, wie an der Kasse.",
        explanation = listOf(
            Block.Text("Geldbeträge addierst du genauso von links nach rechts wie ganze Zahlen. Die Euro sind die großen Stellen, die Cent die kleinen. Gerechnet wird also erst mit den Euro und dann mit den Cent."),
            Block.Steps(
                listOf(
                    "Die ganzen Euro des zweiten Betrags zum ersten Betrag addieren.",
                    "Dann die Cent addieren. Werden es 100 Cent oder mehr, gibt es einen Euro dazu.",
                ),
            ),
            Block.Example(moneyAdd(675, 248)),
            Block.Tip("Tippe das Ergebnis mit Komma ein, zum Beispiel 9,23."),
        ),
        goals = Goals(testSize = 10, star1 = 9.0, star2 = 6.0, star3 = 4.0),
        generator = { r -> moneyAdd(r.betweenL(105, 1995).let { if (it % 100 == 0L) it + 35 else it }, r.betweenL(105, 1995).let { if (it % 100 == 0L) it + 49 else it }) },
    )

    fun change(paid: Long, priceCents: Long): Problem {
        val centsToEuro = (100 - priceCents % 100) % 100
        val nextEuro = (priceCents + centsToEuro) / 100
        val eurosToPaid = paid - nextEuro
        val total = paid * 100 - priceCents
        val steps = mutableListOf<com.helddertierwelt.mentalmath.domain.climb.model.Step>()
        if (centsToEuro > 0) {
            steps += decStep(
                "Von ${d(dec(priceCents, 2), 2)} bis ${n(nextEuro)}",
                dec(centsToEuro, 2),
                2,
                "Die Cent zum nächsten vollen Euro ergänzen: ${n(priceCents % 100)} + ${n(centsToEuro)} = 100.",
            )
        }
        steps += step("Von ${n(nextEuro)} bis ${n(paid)}", eurosToPaid, "Dann die ganzen Euro bis zum bezahlten Betrag.")
        if (centsToEuro > 0) {
            steps += decStep("${d(dec(centsToEuro, 2), 2)} + ${n(eurosToPaid)}", dec(total, 2), 2, "Beide Teile zusammen sind das Wechselgeld.")
        }
        return decProblem(
            skillId = "w3.change",
            key = "$paid-$priceCents",
            prompt = Prompt("${n(paid)} € $MINUS ${d(dec(priceCents, 2), 2)} €", caption = "Wechselgeld", unit = "€"),
            value = dec(total, 2),
            places = 2,
            steps = steps,
            method = "Ergänzen",
        )
    }

    private val change = Skill(
        id = "w3.change",
        title = "Wechselgeld",
        method = "Ergänzen wie mit Komplementen",
        book = BookRef(null, "Ergänzung für den Alltag, Komplemente aus Kapitel 1"),
        summary = "Ausrechnen, wie viel Geld du zurückbekommst.",
        explanation = listOf(
            Block.Text("Wechselgeld rechnest du am schnellsten nicht durch Abziehen, sondern durch Ergänzen – genau wie bei den Komplementen. Du zählst vom Preis aus hoch bis zum bezahlten Betrag."),
            Block.Steps(
                listOf(
                    "Die Cent bis zum nächsten vollen Euro ergänzen, zum Beispiel von 12,70 € bis 13 € sind es 0,30 €.",
                    "Von diesem Euro bis zum bezahlten Schein zählen, von 13 € bis 20 € sind es 7 €.",
                    "Beides zusammen: 7,30 €.",
                ),
            ),
            Block.Example(change(20, 1270)),
            Block.Tip("So rechnen auch Kassierer, wenn sie das Wechselgeld abzählen."),
        ),
        goals = Goals(testSize = 10, star1 = 8.0, star2 = 5.0, star3 = 3.5),
        generator = { r -> changeFor(r) },
    )

    private fun changeFor(r: Random): Problem {
        val paid = r.pick(listOf(5L, 10L, 10L, 20L, 20L, 50L, 50L, 100L))
        val price = r.betweenL(paid * 15, paid * 95).let { if (it % 100 == 0L) it + 45 else it }
        return change(paid, price)
    }

    val world = World(
        id = "w3",
        number = 3,
        title = "Plus und Minus",
        subtitle = "Kapitel 1 – von links nach rechts",
        skills = listOf(add2, add3, addZeros, sub2, complement, sub3, sub4, moneyAdd, change),
    )
}
