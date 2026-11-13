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

import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.bd
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.ds
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.mistake
import com.helddertierwelt.mentalmath.domain.climb.model.BookRef
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.World
import java.math.BigDecimal
import kotlin.random.Random

/**
 * Welt 15 (Ergänzung): weitere Kopfrechen-Tricks – mal 25/125, durch 25/125, mal 9/99/999,
 * mal 5/50, mal 15, Quadrate um 50 und 100, Differenz von Quadraten, nahe an 100, Prozent tauschen.
 */
object World15 {

    private val book = BookRef(null, "Ergänzung: weitere Rechentricks")

    private fun mul25(id: String, r: Random): Problem {
        val big = r.nextInt(3) == 0
        return if (!big) {
            val a = r.between(12, 400).toLong().let { if (r.nextBoolean()) it - it % 4 + 4 else it }
            val hundred = a * 100
            ExtraKit.int(
                id, "$a*25", Prompt("${n(a)} $X 25"), a * 25,
                listOf(step("${n(a)} $X 100", hundred, "25 ist ein Viertel von 100: erst mal 100 …"), step("${n(hundred)} $DIV 4", a * 25, "… dann durch 4 (zweimal halbieren).")),
                "Mal 100, durch 4",
                listOf(mistake(a * 4, "Mal 4 statt durch 4 – 25 ist 100 geteilt durch 4.")),
            ).let { p -> if (hundred % 4 == 0L) p else p }
        } else {
            val a = r.between(2, 99).toLong() * 8 + if (r.nextInt(3) == 0) r.between(1, 7) else 0
            val thousand = a * 1000
            ExtraKit.int(
                id, "$a*125", Prompt("${n(a)} $X 125"), a * 125,
                listOf(step("${n(a)} $X 1.000", thousand, "125 ist ein Achtel von 1.000: erst mal 1.000 …"), step("${n(thousand)} $DIV 8", a * 125, "… dann durch 8 (dreimal halbieren).")),
                "Mal 1.000, durch 8",
            )
        }
    }

    private fun div25(id: String, r: Random): Problem {
        return if (r.nextInt(3) > 0) {
            val a = r.between(3, 500).toLong() * if (r.nextBoolean()) 25 else 5
            val times4 = a * 4
            val res = bd(times4).divide(bd(100)).stripTrailingZeros()
            ExtraKit.dec(
                id, "$a/25", Prompt("${n(a)} $DIV 25"), res,
                listOf(step("${n(a)} $X 4", times4, "Durch 25 ist dasselbe wie mal 4 und durch 100."), ds("${n(times4)} $DIV 100", res, "Komma zwei Stellen nach links.")),
                "Mal 4, durch 100",
                listOf(mistake(bd(a).divide(bd(4), java.math.MathContext.DECIMAL64), "Durch 4 statt mal 4 gerechnet.")),
            )
        } else {
            val a = r.between(2, 200).toLong() * if (r.nextBoolean()) 125 else 25
            val times8 = a * 8
            val res = bd(times8).divide(bd(1000)).stripTrailingZeros()
            ExtraKit.dec(
                id, "$a/125", Prompt("${n(a)} $DIV 125"), res,
                listOf(step("${n(a)} $X 8", times8, "Durch 125 ist dasselbe wie mal 8 und durch 1.000."), ds("${n(times8)} $DIV 1.000", res, "Komma drei Stellen nach links.")),
                "Mal 8, durch 1.000",
            )
        }
    }

    private fun mul9(id: String, r: Random): Problem {
        val f = r.pick(listOf(9L, 99L, 999L, 9L, 99L))
        val round = f + 1
        val a = when (f) {
            9L -> r.between(12, 999).toLong()
            99L -> r.between(12, 999).toLong()
            else -> r.between(12, 99).toLong()
        }
        val big = a * round
        return ExtraKit.int(
            id, "$a*$f", Prompt("${n(a)} $X ${n(f)}"), a * f,
            listOf(step("${n(a)} $X ${n(round)}", big, "${n(f)} ist ${n(round)} minus 1: erst mal ${n(round)} …"), step("${n(big)} $MINUS ${n(a)}", a * f, "… dann die Zahl einmal abziehen.")),
            "Mal ${n(round)}, dann minus die Zahl",
            listOf(mistake(big - 1, "Nur 1 abgezogen – abziehen muss man die ganze Zahl ${n(a)}.")),
        )
    }

    private fun mul5(id: String, r: Random): Problem {
        val f = r.pick(listOf(5L, 50L, 500L))
        val a = r.between(11, 999).toLong()
        val base = a * (f * 2)
        val res = a * f
        return ExtraKit.int(
            id, "$a*$f", Prompt("${n(a)} $X ${n(f)}"), res,
            listOf(step("${n(a)} $X ${n(f * 2)}", base, "${n(f)} ist die Hälfte von ${n(f * 2)}: erst mal ${n(f * 2)} …"), step("${n(base)} $DIV 2", res, "… dann halbieren.")),
            "Mal ${n(f * 2)}, halbieren",
        )
    }

    private fun mul15(id: String, r: Random): Problem {
        val a = r.between(6, 400).toLong() * 2
        val ten = a * 10
        val half = ten / 2
        return ExtraKit.int(
            id, "$a*15", Prompt("${n(a)} $X 15"), a * 15,
            listOf(step("${n(a)} $X 10", ten, "Mal 15 = mal 10 plus die Hälfte davon."), step("${n(ten)} $DIV 2", half, "Die Hälfte von ${n(ten)}."), step("${n(ten)} + ${n(half)}", a * 15)),
            "Mal 10 plus die Hälfte",
        )
    }

    private fun square50(id: String, r: Random): Problem {
        val d = r.between(1, 9).toLong()
        val above = r.nextBoolean()
        val a = if (above) 50 + d else 50 - d
        val hundreds = if (above) 25 + d else 25 - d
        val sq = d * d
        return ExtraKit.int(
            id, "$a^2", Prompt("${n(a)}²"), a * a,
            listOf(
                step(if (above) "25 + $d" else "25 $MINUS $d", hundreds, "Bei Zahlen um 50: 25 ${if (above) "plus" else "minus"} den Abstand zu 50 ergibt die Hunderter."),
                step("${n(hundreds)} $X 100", hundreds * 100),
                step("$d $X $d", sq, "Dazu kommt das Quadrat des Abstands (zweistellig: ${"%02d".format(sq)})."),
                step("${n(hundreds * 100)} + $sq", a * a),
            ),
            "Um 50: Hunderter aus 25 ± Abstand",
            listOf(mistake(hundreds * 100 + sq * 10, "Das Quadrat des Abstands gehört an die letzten zwei Stellen: ${"%02d".format(sq)}.")),
        )
    }

    private fun square100(id: String, r: Random): Problem {
        val d = r.between(1, 12).toLong()
        val above = r.nextBoolean()
        val a = if (above) 100 + d else 100 - d
        val base = if (above) a + d else a - d
        val sq = d * d
        return ExtraKit.int(
            id, "$a^2", Prompt("${n(a)}²"), a * a,
            listOf(
                step(if (above) "${n(a)} + $d" else "${n(a)} $MINUS $d", base, "Um den Abstand zu 100 ${if (above) "weiter nach oben" else "weiter nach unten"} gehen."),
                step("${n(base)} $X 100", base * 100),
                step("$d $X $d", sq, "Dazu das Quadrat des Abstands."),
                step("${n(base * 100)} + $sq", a * a),
            ),
            "Um 100: Abstand doppelt",
        )
    }

    private fun diffSquares(id: String, r: Random): Problem {
        val m = r.pick(listOf(20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L, 200L, 25L, 35L, 45L))
        val d = r.between(1, 9).toLong().coerceAtMost(m - 1)
        val a = m - d
        val b = m + d
        return ExtraKit.int(
            id, "$a*$b", Prompt("${n(a)} $X ${n(b)}"), a * b,
            listOf(
                step("${n(m)} $X ${n(m)}", m * m, "Beide liegen ${n(d)} von ${n(m)} entfernt: (${n(m)} − $d)(${n(m)} + $d) = ${n(m)}² − $d²."),
                step("$d $X $d", d * d),
                step("${n(m * m)} $MINUS ${n(d * d)}", a * b),
            ),
            "Mitte zum Quadrat minus Abstand zum Quadrat",
            listOf(mistake(m * m + d * d, "Das Quadrat des Abstands wird abgezogen, nicht addiert.")),
        )
    }

    private fun near100(id: String, r: Random): Problem {
        val above = r.nextBoolean()
        val da = r.between(1, 12).toLong()
        val db = r.between(1, 12).toLong()
        val a = if (above) 100 + da else 100 - da
        val b = if (above) 100 + db else 100 - db
        val cross = if (above) a + db else a - db
        val prod = da * db
        return ExtraKit.int(
            id, "$a*$b", Prompt("${n(a)} $X ${n(b)}"), a * b,
            listOf(
                step(if (above) "${n(a)} + $db" else "${n(a)} $MINUS $db", cross, "Über Kreuz: die eine Zahl ${if (above) "plus" else "minus"} den Abstand der anderen zu 100."),
                step("${n(cross)} $X 100", cross * 100),
                step("$da $X $db", prod, "Die beiden Abstände malnehmen."),
                step("${n(cross * 100)} + ${n(prod)}", a * b),
            ),
            "Abstände zu 100",
        )
    }

    private fun percentSwap(id: String, r: Random): Problem {
        val easy = r.pick(listOf(25L, 50L, 20L, 10L, 75L, 40L))
        val other = r.between(2, 99).toLong().let { if (easy == 75L || easy == 25L) it - it % 4 + 4 else if (easy == 40L) it - it % 5 + 5 else it }
        val res = bd(other * easy).divide(bd(100)).stripTrailingZeros()
        val share = when (easy) {
            50L -> "die Hälfte"; 25L -> "ein Viertel"; 20L -> "ein Fünftel"; 10L -> "ein Zehntel"; 75L -> "drei Viertel"; else -> "zwei Fünftel"
        }
        return ExtraKit.dec(
            id, "$other%of$easy", Prompt("${n(other)} % von ${n(easy)}", caption = "Prozent tauschen"), res,
            listOf(ds("${n(other)} $X ${n(easy)} $DIV 100", res, "${n(other)} % von ${n(easy)} ist dasselbe wie ${n(easy)} % von ${n(other)} – also $share von ${n(other)}.")),
            "a % von b = b % von a",
        )
    }

    private val skillList by lazy {
        val b = book
        listOf(
            ExtraKit.skill(
                "w15.mul25", "Mal 25 und mal 125", "Mal 100, durch 4",
                "25 ist ein Viertel von 100, 125 ein Achtel von 1.000.",
                "Statt mit 25 malzunehmen, nimmst du mit 100 mal und teilst durch 4. Bei 125 nimmst du mit 1.000 mal und teilst durch 8. Teilen durch 4 heißt zweimal halbieren, durch 8 dreimal halbieren – das geht im Kopf viel leichter.",
                listOf("Zahl mal 100 (bei 125: mal 1.000).", "Durch 4 teilen (bei 125: durch 8) – also mehrmals halbieren.", "Kurzer Check: Ist die Zahl durch 4 teilbar, wird das Ergebnis glatt."),
                listOf("a × 25 = a × 100 ÷ 4", "a × 125 = a × 1.000 ÷ 8"),
                "Bei geraden Zahlen geht es auch andersherum: 36 × 25 = 9 × 100, weil 36 ÷ 4 = 9.",
                ExtraKit.quick, b, 1501, ::mul25,
            ),
            ExtraKit.skill(
                "w15.div25", "Durch 25 und durch 125", "Mal 4, durch 100",
                "Durch 25 ist mal 4 durch 100, durch 125 ist mal 8 durch 1.000.",
                "Teilen durch 25 ist dasselbe wie mal 4 und dann durch 100 – und durch 100 ist nur ein Kommaschub. Genauso ist durch 125 dasselbe wie mal 8 und durch 1.000.",
                listOf("Zahl mal 4 (bei 125: mal 8).", "Komma zwei Stellen nach links (bei 125: drei Stellen)."),
                listOf("a ÷ 25 = a × 4 ÷ 100", "a ÷ 125 = a × 8 ÷ 1.000"),
                "Auch Prozent passt dazu: x ÷ 25 ist x mal 4 %.",
                ExtraKit.quick, b, 1502, ::div25,
            ),
            ExtraKit.skill(
                "w15.mul9", "Mal 9, 99 und 999", "Mal 10, minus die Zahl",
                "Mit der nächsten runden Zahl malnehmen und die Zahl einmal abziehen.",
                "9 ist 10 minus 1, 99 ist 100 minus 1, 999 ist 1.000 minus 1. Also nimmst du mit der runden Zahl mal und ziehst die ursprüngliche Zahl einmal ab: 47 × 99 = 4.700 − 47.",
                listOf("Zahl mal 10, 100 oder 1.000 (Nullen anhängen).", "Die ursprüngliche Zahl einmal abziehen."),
                listOf("a × 9 = a × 10 − a", "a × 99 = a × 100 − a", "a × 999 = a × 1.000 − a"),
                "Genauso geht mal 11, 101 oder 1.001 – dann wird die Zahl addiert statt abgezogen.",
                ExtraKit.quick, b, 1503, ::mul9,
            ),
            ExtraKit.skill(
                "w15.mul5", "Mal 5, 50 und 500", "Mal 10, halbieren",
                "5 ist die Hälfte von 10 – also mal 10 und halbieren.",
                "Mit 5 malzunehmen ist dasselbe wie mit 10 malzunehmen und zu halbieren. Bei 50 nimmst du mal 100 und halbierst, bei 500 mal 1.000.",
                listOf("Null(en) anhängen: mal 10, 100 oder 1.000.", "Halbieren."),
                listOf("a × 5 = a × 10 ÷ 2", "a × 50 = a × 100 ÷ 2"),
                "Bei geraden Zahlen geht es noch schneller: erst halbieren, dann die Null anhängen (68 × 5 = 34 × 10).",
                ExtraKit.quick, b, 1504, ::mul5,
            ),
            ExtraKit.skill(
                "w15.mul15", "Mal 15", "Mal 10 plus die Hälfte",
                "15 = 10 + 5: mal 10 und die Hälfte davon dazu.",
                "Mal 15 ist mal 10 plus mal 5 – und mal 5 ist die Hälfte von mal 10. Also: Null anhängen, die Hälfte davon nehmen und beides zusammenzählen.",
                listOf("Zahl mal 10.", "Die Hälfte davon nehmen.", "Beides addieren."),
                listOf("a × 15 = a × 10 + (a × 10) ÷ 2"),
                "Praktisch beim Trinkgeld: 15 % sind 10 % plus die Hälfte davon.",
                ExtraKit.quick, b, 1505, ::mul15,
            ),
            ExtraKit.skill(
                "w15.sq50", "Quadrate um 50", "Hunderter aus 25 ± Abstand",
                "Zahlen von 41 bis 59 im Nu quadrieren.",
                "Für Zahlen nahe 50 gilt: Die Hunderter sind 25 plus (oder minus) der Abstand zu 50, die letzten zwei Ziffern sind das Quadrat des Abstands. Beispiel 53²: 25 + 3 = 28 Hunderter, 3² = 09, also 2.809.",
                listOf("Abstand zu 50 bestimmen.", "25 plus Abstand (über 50) oder minus Abstand (unter 50) ergibt die Hunderter.", "Das Quadrat des Abstands als letzte zwei Ziffern anhängen."),
                listOf("(50 ± d)² = (25 ± d) × 100 + d²"),
                "Das Quadrat des Abstands immer zweistellig schreiben: 3² = 09, nicht 9 an der Einerstelle allein vergessen.",
                ExtraKit.quick, b, 1506, ::square50,
            ),
            ExtraKit.skill(
                "w15.sq100", "Quadrate um 100", "Um den Abstand weitergehen",
                "Zahlen von 88 bis 112 quadrieren.",
                "Bei Zahlen nahe 100 gehst du um den Abstand zu 100 noch einmal weiter: 97 – 3 = 94, das sind die Hunderter. Dahinter kommt das Quadrat des Abstands: 3² = 09. Also 97² = 9.409.",
                listOf("Abstand zu 100 bestimmen.", "Die Zahl um den Abstand weiter weg von 100 schieben – das sind die Hunderter.", "Das Quadrat des Abstands dazuzählen."),
                listOf("(100 ± d)² = (100 ± 2d) × 100 + d²"),
                "Ist der Abstand 10 oder mehr, wird das Quadrat dreistellig – dann trägt es in die Hunderter über (112² = 124 × 100 + 144 = 12.544).",
                ExtraKit.quick, b, 1507, ::square100,
            ),
            ExtraKit.skill(
                "w15.diffsq", "Differenz von Quadraten", "Mitte² minus Abstand²",
                "Zwei Zahlen gleich weit von einer runden Mitte: 47 × 53 = 50² − 3².",
                "Liegen zwei Zahlen gleich weit von einer runden Zahl entfernt, ist ihr Produkt die Mitte zum Quadrat minus den Abstand zum Quadrat. Das folgt aus (a − d)(a + d) = a² − d².",
                listOf("Die Mitte zwischen beiden Zahlen finden (am besten rund).", "Mitte zum Quadrat.", "Abstand zum Quadrat abziehen."),
                listOf("(m − d) × (m + d) = m² − d²"),
                "Prüfe vorher, ob die Summe gerade ist – nur dann gibt es eine ganze Mitte.",
                ExtraKit.quick, b, 1508, ::diffSquares,
            ),
            ExtraKit.skill(
                "w15.near100", "Nahe an 100 malnehmen", "Abstände zu 100",
                "Zwei Zahlen knapp unter oder knapp über 100 malnehmen.",
                "Für zwei Zahlen knapp unter 100 (oder beide knapp darüber) nimmst du über Kreuz die eine Zahl minus (plus) den Abstand der anderen – das sind die Hunderter. Dazu kommt das Produkt der beiden Abstände. 96 × 97: 96 − 3 = 93, 4 × 3 = 12, also 9.312.",
                listOf("Abstände beider Zahlen zu 100 bestimmen.", "Eine Zahl um den Abstand der anderen verschieben – die Hunderter.", "Die Abstände malnehmen und dazuzählen."),
                listOf("(100 − a)(100 − b) = (100 − a − b) × 100 + a × b"),
                "Das Produkt der Abstände kann über 99 liegen (88 × 89) – dann trägt es in die Hunderter über.",
                ExtraKit.quick, b, 1509, ::near100,
            ),
            ExtraKit.skill(
                "w15.pctswap", "Prozent tauschen", "a % von b = b % von a",
                "8 % von 25 ist dasselbe wie 25 % von 8.",
                "Prozent von etwas zu nehmen heißt malnehmen und durch 100 teilen. Weil man beim Malnehmen tauschen darf, ist a % von b immer gleich b % von a. Oft ist eine Richtung viel leichter: 18 % von 50 = 50 % von 18 = 9.",
                listOf("Schauen, ob die zweite Zahl ein „schöner“ Prozentsatz ist (10, 20, 25, 50, 75 …).", "Tauschen und den leichten Anteil nehmen."),
                listOf("a % von b = b % von a"),
                "Funktioniert auch mit Kommazahlen: 4 % von 75 = 75 % von 4 = 3.",
                ExtraKit.quick, b, 1510, ::percentSwap,
            ),
        )
    }

    private val mixed by lazy {
        ExtraKit.skill(
            "w15.mixed", "Rechentricks gemischt", "Den passenden Trick erkennen",
            "Alle Tricks dieser Welt durcheinander.",
            "Hier kommen alle Tricks durcheinander. Die eigentliche Kunst ist, schnell zu erkennen, welcher Trick zur Aufgabe passt.",
            listOf("Aufgabe ansehen: Steckt eine 25, 125, 9, 99, 5, 15 darin? Liegt eine Zahl nahe 50 oder 100?", "Den passenden Trick anwenden.", "Kurz überschlagen, ob das Ergebnis stimmt."),
            emptyList(),
            "Wer die Tricks gemischt beherrscht, sieht bei jeder Aufgabe sofort eine Abkürzung.",
            ExtraKit.quick, book, 1511, ExtraKit.mixedOf { skillList },
        )
    }

    val world by lazy {
        World(id = "w15", number = 15, title = "Mehr Rechentricks", subtitle = "Ergänzung · Abkürzungen für den Alltag", skills = skillList + mixed, independent = true)
    }
}
