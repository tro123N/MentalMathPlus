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
import com.helddertierwelt.mentalmath.domain.climb.math.Phonetic
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

/** Welt 11 – Multiplikation für Fortgeschrittene (8. Kapitel). */
internal object World11 {

    private const val CH = 8

    /** Merkhilfe für ein Zwischenergebnis wie 136.000: Wort für 136. */
    private fun memo(value: Long, unit: Long = 1_000): String {
        val head = value / unit
        if (head <= 0) return ""
        if (head < 1_000) return " Merkwort für ${n(head)}: „${Phonetic.wordFor(head.toString())}“."
        val last = (head % 1_000).toString().padStart(3, '0')
        return " Merkhilfe: ${n(head / 1_000)} laut sagen, $last als „${Phonetic.wordFor(last)}“ merken."
    }

    private fun product(skillId: String, a: Long, b: Long, steps: List<Step>, method: String, caption: String? = null): Problem =
        intProblem(skillId, "$a*$b", Prompt("${n(a)} $X ${n(b)}", caption = caption), a * b, steps, method)

    // --- Vierstellig mal einstellig ----------------------------------------------------------

    fun mul41Steps(a: Long, d: Long): List<Step> {
        val high = a / 100 * 100
        val low = a % 100
        val p1 = high * d
        val p2 = low * d
        return listOf(
            step("${n(high)} $X $d", p1, "Die ersten zwei Ziffern wie eine zweistellige Zahl rechnen: ${n(high / 100)} $X $d = ${n(high / 100 * d)}, dann zwei Nullen."),
            step("${n(low)} $X $d", p2, "Jetzt die letzten zwei Ziffern."),
            step("${n(p1)} + ${n(p2)}", p1 + p2, "Zusammenzählen."),
        )
    }

    private val mul41 = Skill(
        id = "w11.mul41",
        title = "Vierstellig mal einstellig",
        method = "In zwei Zweistellige zerlegen",
        book = BookRef(CH, "Quadrieren vierstelliger Zahlen"),
        summary = "Die Grundlage für vierstellige Quadrate: 4.738 × 6 in Sekunden.",
        explanation = listOf(
            Block.Text("Eine vierstellige Zahl mal eine einstellige zerlegst du in zwei zweistellige Teile: die ersten beiden Ziffern (als Hunderter) und die letzten beiden. Beide Teile malnehmen, dann addieren."),
            Block.Steps(listOf("4.738 × 6: zuerst 4.700 × 6 = 28.200 (47 × 6 = 282, zwei Nullen dran).", "Dann 38 × 6 = 228.", "28.200 + 228 = 28.428.")),
            Block.Example(product("w11.mul41", 4_738, 6, mul41Steps(4_738, 6), "In zwei Zweistellige zerlegen")),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 16.0, star2 = 10.0, star3 = 7.0),
        generator = { r ->
            var a: Long
            do { a = r.betweenL(1_012, 9_989) } while (a % 100 < 11)
            val d = r.betweenL(3, 9)
            product("w11.mul41", a, d, mul41Steps(a, d), "In zwei Zweistellige zerlegen")
        },
    )

    // --- Vierstellige Quadrate ---------------------------------------------------------------

    fun square4(a: Long): Problem {
        val base = (a + 500) / 1_000 * 1_000
        val dist = abs(a - base)
        val other = if (base > a) a - dist else a + dist
        val big = base * other
        val small = dist * dist
        val steps = listOf(
            step("Abstand von ${n(a)} zu ${n(base)}", dist, "Auf den nächsten Tausender runden: ${n(base)}."),
            step(if (base > a) "${n(a)} $MINUS ${n(dist)}" else "${n(a)} + ${n(dist)}", other, "Die andere Zahl um genauso viel in die Gegenrichtung ändern."),
            step("${n(base)} $X ${n(other)}", big, "Vierstellig mal einstellig, dann drei Nullen anhängen.${memo(big % 1_000_000)}"),
            step("${n(dist)}²", small, "Das Quadrat des Abstands (dreistellig quadrieren wie in Welt 6)."),
            step("${n(big)} + ${n(small)}", big + small, "Zum Schluss beide Teile addieren."),
        )
        return intProblem("w11.square4", "q$a", Prompt("${n(a)}²"), a * a, steps, "Runden auf Tausender, Abstand quadrieren")
    }

    private val square4Skill = Skill(
        id = "w11.square4",
        title = "Vierstellige Zahlen quadrieren",
        method = "Runden auf Tausender, Abstand quadrieren",
        book = BookRef(CH, "Quadrieren vierstelliger Zahlen"),
        summary = "Quadrate wie 3.712² komplett im Kopf.",
        explanation = listOf(
            Block.Text("Vierstellige Quadrate gehen nach demselben Muster wie zwei- und dreistellige: Eine Zahl auf den nächsten Tausender runden, die andere um genauso viel in die Gegenrichtung ändern, die beiden malnehmen und das Quadrat des Abstands addieren."),
            Block.Steps(
                listOf(
                    "3.712²: Der nächste Tausender ist 4.000, der Abstand beträgt 288.",
                    "Die andere Zahl: 3.712 − 288 = 3.424.",
                    "4.000 × 3.424 = 13.696.000. Die Millionen (13) kannst du schon sagen, die 696 merkst du dir als Wort.",
                    "288² = 82.944 dazu: 13.778.944.",
                ),
            ),
            Block.Example(square4(3_712)),
            Block.Tip("Die Summe der beiden Faktoren ist immer das Doppelte der Ausgangszahl. Den Abstand zum Tausender bekommst du schnell über das Komplement."),
        ),
        goals = Goals(testSize = 5, minAccuracy = 0.8, star1 = 90.0, star2 = 60.0, star3 = 40.0),
        guidedCount = 3,
        practiceCount = 6,
        generator = { r ->
            var a: Long
            do { a = r.betweenL(1_101, 9_899) } while (a % 1_000 < 100 || a % 1_000 > 900 || a % 10 == 0L)
            square4(a)
        },
    )

    // --- Dreistellig mal zweistellig ---------------------------------------------------------

    fun mul32(a: Long, b: Long): Problem {
        val pairs = Num.singleDigitFactorPairs(b)
        val steps = mutableListOf<Step>()
        val method: String
        when {
            pairs.isNotEmpty() -> {
                val (f1, f2) = pairs.first()
                val p = a * f1
                steps += step("${n(b)} = $f1 $X $f2", f1 * f2, "Die zweistellige Zahl zerlegen.")
                steps += step("${n(a)} $X $f1", p, "Erst mit dem einen Faktor …")
                steps += step("${n(p)} $X $f2", p * f2, "… dann mit dem anderen. Nichts zu addieren!")
                method = "Faktorisierungsmethode"
            }

            a % 100 >= 88 -> {
                val up = Num.roundUp(a, 100)
                val diff = up - a
                steps += step("${n(up)} $X ${n(b)}", up * b, "${n(a)} liegt knapp unter ${n(up)}.")
                steps += step("${n(diff)} $X ${n(b)}", diff * b, "So viel war zu viel.")
                steps += step("${n(up * b)} $MINUS ${n(diff * b)}", a * b, "Abziehen.")
                method = "Subtraktionsmethode"
            }

            else -> {
                val hundreds = a / 100 * 100
                val rest = a % 100
                steps += step("${n(hundreds)} $X ${n(b)}", hundreds * b, "Die Hunderter mal die zweistellige Zahl.${memo(hundreds * b, 100)}")
                steps += step("${n(rest)} $X ${n(b)}", rest * b, "Der Rest: zweistellig mal zweistellig.")
                steps += step("${n(hundreds * b)} + ${n(rest * b)}", a * b, "Addieren.")
                method = "Additionsmethode"
            }
        }
        return product("w11.mul32", a, b, steps, method)
    }

    private val mul32Skill = Skill(
        id = "w11.mul32",
        title = "Dreistellig mal zweistellig",
        method = "Faktorisieren, addieren oder subtrahieren",
        book = BookRef(CH, "Multiplikation von dreistelligen mit zweistelligen Zahlen"),
        summary = "Erst die beste Methode wählen, dann rechnen.",
        explanation = listOf(
            Block.Text("Bei dreistellig mal zweistellig lohnt sich ein kurzer Blick, bevor du loslegst. Am bequemsten ist es, wenn sich die zweistellige Zahl in einstellige Faktoren zerlegen lässt – dann gibt es nichts zu addieren. Sonst zerlegst du die dreistellige Zahl (Additionsmethode) oder rundest sie auf, wenn sie knapp unter einem Hunderter liegt (Subtraktionsmethode)."),
            Block.Steps(
                listOf(
                    "Faktorisierung: 538 × 63 = 538 × 9 × 7 = 4.842 × 7 = 33.894.",
                    "Addition: 427 × 53 = 400 × 53 + 27 × 53 = 21.200 + 1.431 = 22.631.",
                    "Subtraktion: 694 × 37 = 700 × 37 − 6 × 37 = 25.900 − 222 = 25.678.",
                ),
            ),
            Block.Example(mul32(538, 63)),
            Block.Example(mul32(427, 53)),
            Block.Example(mul32(694, 37)),
        ),
        goals = Goals(testSize = 6, minAccuracy = 0.83, star1 = 45.0, star2 = 30.0, star3 = 20.0),
        generator = { r ->
            var a: Long
            var b: Long
            do {
                a = r.betweenL(112, 989)
                b = r.betweenL(12, 98)
            } while (a % 10 == 0L || b % 10 == 0L)
            mul32(a, b)
        },
    )

    // --- Nahe beieinander (dreistellig) ------------------------------------------------------

    fun close3(a: Long, b: Long): Problem {
        val base = ((a + b) / 2 + 50) / 100 * 100
        val da = a - base
        val db = b - base
        val second = a + db
        val first = base * second
        val corr = abs(da * db)
        val steps = mutableListOf(
            step("Basis", base, "Ein runder Hunderter in der Nähe beider Zahlen."),
            step(if (db >= 0) "${n(a)} + ${n(db)}" else "${n(a)} $MINUS ${n(-db)}", second, "Die eine Zahl plus den Abstand der anderen zur Basis (oder umgekehrt – es kommt dasselbe heraus)."),
            step("${n(base)} $X ${n(second)}", first, "Basis mal diese Zahl."),
            step("${n(abs(da))} $X ${n(abs(db))}", corr, "Die beiden Abstände zur Basis malnehmen."),
        )
        val sameSide = (da >= 0) == (db >= 0)
        steps += if (sameSide) {
            step("${n(first)} + ${n(corr)}", a * b, "Liegen beide Zahlen auf derselben Seite der Basis, wird addiert.")
        } else {
            step("${n(first)} $MINUS ${n(corr)}", a * b, "Liegt die Basis zwischen den Zahlen, wird abgezogen.")
        }
        return product("w11.close3", a, b, steps, "Nahe-beieinander-Methode")
    }

    private val close3Skill = Skill(
        id = "w11.close3",
        title = "Dreistellige, nah beieinander",
        method = "Nahe-beieinander-Methode",
        book = BookRef(CH, "Multiplikation zweier dreistelliger Zahlen"),
        summary = "Produkte wie 307 × 312 oder 596 × 588 fast ohne Rechnen.",
        explanation = listOf(
            Block.Text("Liegen zwei Zahlen nah an derselben runden Zahl, der Basis, wird die Multiplikation erstaunlich einfach: Basis mal (eine Zahl plus der Abstand der anderen), dazu das Produkt der beiden Abstände."),
            Block.Steps(
                listOf(
                    "306 × 312, Basis 300: Abstände 6 und 12.",
                    "306 + 12 = 318, und 300 × 318 = 95.400.",
                    "6 × 12 = 72 dazu: 95.472.",
                    "Liegt eine Zahl unter und eine über der Basis, wird das Abstandsprodukt abgezogen.",
                ),
            ),
            Block.Example(close3(306, 312)),
            Block.Example(close3(597, 584)),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 25.0, star2 = 16.0, star3 = 10.0),
        generator = { r ->
            val base = r.betweenL(1, 9) * 100
            var a: Long
            var b: Long
            do {
                a = base + r.betweenL(-15, 15)
                b = base + r.betweenL(-15, 15)
            } while (a == base || b == base || a < 101 || b < 101 || a > 999 || b > 999)
            close3(a, b)
        },
    )

    // --- Dreistellig mal dreistellig ---------------------------------------------------------

    fun mul33(a: Long, b: Long): Problem {
        val b1 = b / 100
        val b23 = b % 100
        val a1 = a / 100
        val a23 = a % 100
        val p1 = a * b1 * 100
        val p2 = a1 * 100 * b23
        val p3 = a23 * b23
        val steps = listOf(
            step("${n(a)} $X ${n(b1 * 100)}", p1, "Dreistellig mal einstellig, zwei Nullen dran.${memo(p1)}"),
            step("${n(a1 * 100)} $X ${n(b23)}", p2, "Einstellig mal zweistellig, zwei Nullen dran."),
            step("${n(p1)} + ${n(p2)}", p1 + p2, "Zwischensumme.${memo(p1 + p2)}"),
            step("${n(a23)} $X ${n(b23)}", p3, "Zweistellig mal zweistellig."),
            step("${n(p1 + p2)} + ${n(p3)}", a * b, "Alles zusammen."),
        )
        return product("w11.mul33", a, b, steps, "Die Methode, wenn alles andere versagt")
    }

    private val mul33Skill = Skill(
        id = "w11.mul33",
        title = "Dreistellig mal dreistellig",
        method = "In drei Teilprodukte zerlegen",
        book = BookRef(CH, "Die Methode, wenn alles andere versagt"),
        summary = "Jede Multiplikation zweier dreistelliger Zahlen sicher lösen.",
        explanation = listOf(
            Block.Text("Wenn keine Abkürzung passt, hilft eine Methode, die immer funktioniert: Die zweite Zahl wird in Hunderter und Rest zerlegt, die erste ebenso. So entstehen drei leichtere Teilprodukte, die du nacheinander addierst. Zwischenergebnisse kannst du dir mit dem Code aus Welt 10 als Wort merken."),
            Block.Steps(
                listOf(
                    "634 × 287: zuerst 634 × 200 = 126.800.",
                    "Dann 600 × 87 = 52.200; zusammen 179.000.",
                    "Zum Schluss 34 × 87 = 2.958; Ergebnis 181.958.",
                ),
            ),
            Block.Example(mul33(634, 287)),
            Block.Tip("Prüfe vorher, ob es leichter geht: Lässt sich eine Zahl in kleine Faktoren zerlegen, liegen beide Zahlen nah beieinander oder hat eine Zahl eine 0 in der Mitte?"),
        ),
        goals = Goals(testSize = 5, minAccuracy = 0.8, star1 = 90.0, star2 = 60.0, star3 = 40.0),
        guidedCount = 3,
        practiceCount = 6,
        generator = { r ->
            var a: Long
            var b: Long
            do {
                a = r.betweenL(112, 989)
                b = r.betweenL(112, 989)
            } while (a % 100 < 11 || b % 100 < 11)
            mul33(a, b)
        },
    )

    // --- Fünfstellige Quadrate ---------------------------------------------------------------

    fun square5(x: Long): Problem {
        val a = x / 1_000
        val b = x % 1_000
        val ab = a * b
        val twoAb = 2 * ab
        val aa = a * a
        val bb = b * b
        val high = aa * 1_000_000 + twoAb * 1_000
        val steps = listOf(
            step("${n(b)} $X ${n(a)}", ab, "Mit dem schwersten Teil anfangen: dreistellig mal zweistellig."),
            step("${n(ab)} $X 2", twoAb, "Verdoppeln – das sind Tausender.${memo(twoAb, 1)}"),
            step("${n(a)}²", aa, "Das Quadrat der ersten beiden Ziffern – das sind Millionen."),
            step("${n(aa * 1_000_000)} + ${n(twoAb * 1_000)}", high, "Millionen und Tausender zusammenfügen."),
            step("${n(b)}²", bb, "Das Quadrat der letzten drei Ziffern."),
            step("${n(high)} + ${n(bb)}", x * x, "Fertig."),
        )
        return intProblem("w11.square5", "q5$x", Prompt("${n(x)}²"), x * x, steps, "Zerlegen in 2 + 3 Ziffern")
    }

    private val square5Skill = Skill(
        id = "w11.square5",
        title = "Fünfstellige Zahlen quadrieren",
        method = "Zerlegen in 2 + 3 Ziffern",
        book = BookRef(CH, "Quadrieren fünfstelliger Zahlen"),
        summary = "Die Königsdisziplin: Quadrate mit zehnstelligem Ergebnis.",
        explanation = listOf(
            Block.Text("Eine fünfstellige Zahl zerlegst du in ihre ersten zwei und letzten drei Ziffern. Das Quadrat besteht dann aus drei Teilen: dem Quadrat der vorderen Zahl (Millionen), dem doppelten Produkt beider Teile (Tausender) und dem Quadrat der hinteren Zahl."),
            Block.Formula("(A·1000 + B)² = A²·1.000.000 + 2·A·B·1.000 + B²"),
            Block.Steps(
                listOf(
                    "37.416²: A = 37, B = 416.",
                    "416 × 37 = 15.392, verdoppelt 30.784 (Tausender).",
                    "37² = 1.369 (Millionen): 1.369.000.000 + 30.784.000 = 1.399.784.000.",
                    "416² = 173.056 dazu: 1.399.957.056.",
                ),
            ),
            Block.Example(square5(37_416)),
            Block.Tip("Wie beim Buch: erst das schwierigste Teilprodukt rechnen und als Merkwort ablegen, dann den Rest."),
        ),
        goals = Goals(testSize = 3, minAccuracy = 0.66, star1 = 240.0, star2 = 160.0, star3 = 100.0),
        guidedCount = 2,
        practiceCount = 4,
        generator = { r ->
            var x: Long
            do { x = r.betweenL(10_111, 99_989) } while (x % 1_000 < 101)
            square5(x)
        },
    )

    // --- Fünfstellig mal fünfstellig ---------------------------------------------------------

    fun mul55(x: Long, y: Long): Problem {
        val a1 = x / 1_000
        val a2 = x % 1_000
        val b1 = y / 1_000
        val b2 = y % 1_000
        val m1 = a1 * b2
        val m2 = a2 * b1
        val mid = m1 + m2
        val top = a1 * b1
        val high = top * 1_000_000 + mid * 1_000
        val low = a2 * b2
        val steps = listOf(
            step("${n(a1)} $X ${n(b2)}", m1, "Zuerst die beiden Produkte „zweistellig mal dreistellig“ über Kreuz."),
            step("${n(a2)} $X ${n(b1)}", m2, null),
            step("${n(m1)} + ${n(m2)}", mid, "Das sind Tausender.${memo(mid, 1)}"),
            step("${n(a1)} $X ${n(b1)}", top, "Die vorderen Teile – das sind Millionen."),
            step("${n(top * 1_000_000)} + ${n(mid * 1_000)}", high, "Millionen und Tausender zusammenfügen."),
            step("${n(a2)} $X ${n(b2)}", low, "Die hinteren Teile: dreistellig mal dreistellig."),
            step("${n(high)} + ${n(low)}", x * y, "Fertig."),
        )
        return product("w11.mul55", x, y, steps, "Zerlegen in 2 + 3 Ziffern")
    }

    private val mul55Skill = Skill(
        id = "w11.mul55",
        title = "Fünfstellig mal fünfstellig",
        method = "Zerlegen in 2 + 3 Ziffern",
        book = BookRef(CH, "Multiplikation zweier fünfstelliger Zahlen"),
        summary = "Das große Finale: zwei fünfstellige Zahlen im Kopf multiplizieren.",
        explanation = listOf(
            Block.Text("Hier kommt alles zusammen: Jede Zahl wird in die ersten zwei und die letzten drei Ziffern zerlegt. So entstehen vier Teilprodukte – einmal zweistellig mal zweistellig, zweimal zweistellig mal dreistellig und einmal dreistellig mal dreistellig. Mit Merkwörtern für die Zwischenergebnisse ist das im Kopf machbar."),
            Block.Formula("(A·1000 + B)(C·1000 + D) = A·C·1.000.000 + (A·D + B·C)·1.000 + B·D"),
            Block.Example(mul55(24_613, 51_287)),
            Block.Tip("Schreib dir die Aufgabe ruhig auf – auch Profis rechnen solche Aufgaben im Kopf, lassen die Zahlen aber vor sich stehen."),
        ),
        goals = Goals(testSize = 3, minAccuracy = 0.66, star1 = 300.0, star2 = 200.0, star3 = 130.0),
        guidedCount = 2,
        practiceCount = 4,
        generator = { r ->
            mul55(r.betweenL(10_101, 99_899), r.betweenL(10_101, 99_899))
        },
    )

    val world = World(
        id = "w11",
        number = 11,
        title = "Profi-Multiplikation",
        subtitle = "Kapitel 8 – Das Schwere leicht gemacht",
        skills = listOf(mul41, square4Skill, mul32Skill, close3Skill, mul33Skill, square5Skill, mul55Skill),
    )
}
