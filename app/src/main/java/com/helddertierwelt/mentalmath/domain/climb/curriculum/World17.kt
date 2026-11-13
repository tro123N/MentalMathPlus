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
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.signed
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.sub
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.sup
import com.helddertierwelt.mentalmath.domain.climb.model.BookRef
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import com.helddertierwelt.mentalmath.domain.climb.model.World
import java.math.BigDecimal
import kotlin.random.Random

/**
 * Welt 17 (Ergänzung): Oberstufe und Mathe-LK im Kopf – Logarithmen, Potenzen und Wachstum,
 * Ableitungen, Scheitelpunkt, quadratische Gleichungen, binomische Formeln, Trigonometrie,
 * Pythagoras, Statistik, Wahrscheinlichkeit, Vektoren, Integrale.
 */
object World17 {

    private val book = BookRef(null, "Ergänzung: Oberstufe und Mathe-LK")

    private fun pow(b: Long, e: Int): Long {
        var v = 1L
        repeat(e) { v *= b }
        return v
    }

    /** „3x⁴“, „x²“, „5x“, „7“ – ohne Vorzeichen. */
    private fun term(c: Long, e: Int): String {
        val coef = if (c == 1L && e > 0) "" else n(c)
        return when (e) {
            0 -> n(c)
            1 -> "${coef}x"
            else -> "${coef}x${sup(e)}"
        }
    }

    /** Polynom aus (Koeffizient, Exponent), z. B. „3x⁴ + 2x − 5“. */
    private fun poly(terms: List<Pair<Long, Int>>): String = terms.mapIndexed { i, (c, e) ->
        val body = term(kotlin.math.abs(c), e)
        when {
            i == 0 -> if (c < 0) "$MINUS$body" else body
            c < 0 -> " $MINUS $body"
            else -> " + $body"
        }
    }.joinToString("")

    // --- Logarithmen -----------------------------------------------------------------------------

    private fun log(id: String, r: Random): Problem = when (r.nextInt(4)) {
        0, 1 -> {
            val base = r.pick(listOf(2L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))
            val maxE = generateSequence(1) { it + 1 }.takeWhile { pow(base, it) <= 1_000_000_000L }.last()
            val e = r.between(2, minOf(maxE, 12))
            val v = pow(base, e)
            ExtraKit.int(
                id, "log$base($v)", Prompt("log${sub(base.toInt())} ${n(v)}", caption = "Logarithmus"), e.toLong(),
                listOf(step("$base hoch wie viel ergibt ${n(v)}?", e.toLong(), "$base${sup(e)} = ${n(v)} – der Logarithmus ist der Exponent.")),
                "Exponent suchen",
                listOf(mistake(v / base, "Durch die Basis geteilt – gefragt ist, wie oft man $base mit sich selbst malnehmen muss.")),
            )
        }
        2 -> {
            val e = r.between(1, 9)
            val v = pow(10, e)
            ExtraKit.int(
                id, "lg$v", Prompt("lg ${n(v)}", caption = "Zehnerlogarithmus"), e.toLong(),
                listOf(step("Nullen zählen", e.toLong(), "lg ist der Logarithmus zur Basis 10: Bei 10er-Potenzen zählt man die Nullen.")),
                "Nullen zählen",
            )
        }
        else -> {
            val base = r.pick(listOf(2L, 3L, 5L))
            val a = r.between(1, if (base == 2L) 8 else 4)
            val b = r.between(1, if (base == 2L) 8 else 4)
            val sb = sub(base.toInt())
            ExtraKit.int(
                id, "log$base:${pow(base, a)}+${pow(base, b)}", Prompt("log$sb ${n(pow(base, a))} + log$sb ${n(pow(base, b))}", caption = "Logarithmengesetze"), (a + b).toLong(),
                listOf(step("log$sb ${n(pow(base, a))}", a.toLong()), step("log$sb ${n(pow(base, b))}", b.toLong()), step("$a + $b", (a + b).toLong(), "Logarithmen von Produkten addieren sich: log(u·v) = log u + log v.")),
                "Einzeln bestimmen, dann addieren",
            )
        }
    }

    // --- Potenzen und Wachstum -------------------------------------------------------------------

    private fun growth(id: String, r: Random): Problem = when (r.nextInt(4)) {
        0 -> {
            val base = r.pick(listOf(2L, 2L, 3L, 4L, 5L, 6L, 7L, 9L, 11L, 12L))
            val maxE = generateSequence(2) { it + 1 }.takeWhile { pow(base, it) <= 100_000L }.last()
            val e = r.between(2, minOf(maxE, 16))
            ExtraKit.int(
                id, "pow$base^$e", Prompt("$base${sup(e)}", caption = "Potenz"), pow(base, e),
                listOf(step("$base hoch ${e - 1}", pow(base, e - 1)), step("${n(pow(base, e - 1))} $X $base", pow(base, e), "Eine Stufe weiter: noch einmal mal $base.")),
                "Schrittweise malnehmen",
                listOf(mistake(base * e, "Basis mal Exponent gerechnet – eine Potenz ist wiederholtes Malnehmen.")),
            )
        }
        1 -> {
            val every = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 20L, 30L))
            val times = r.between(2, 8)
            val unit = if (every >= 20) "min" else "h"
            ExtraKit.int(
                id, "double$every*$times", Prompt("Verdopplung alle $every $unit: Faktor nach ${n(every * times)} $unit = ?", caption = "Exponentielles Wachstum"), pow(2, times),
                listOf(step("${n(every * times)} $DIV $every", times.toLong(), "So oft wird verdoppelt."), step("2 hoch $times", pow(2, times), "Jede Verdopplung ist mal 2.")),
                "Verdopplungen zählen",
                listOf(mistake(2L * times, "Verdopplungen addiert – sie werden malgenommen: 2 × 2 × 2 …")),
            )
        }
        2 -> {
            val half = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 8L, 10L))
            val times = r.between(1, 5)
            val end = bd(r.pick(listOf(5L, 10L, 15L, 20L, 25L, 50L, 100L, 125L)))
            val start = end.multiply(bd(pow(2, times)))
            ExtraKit.dec(
                id, "half$start/$half*$times", Prompt("${d(start)} mg, Halbwertszeit $half h: nach ${n(half * times)} h = ? mg", caption = "Halbwertszeit"), end,
                listOf(step("${n(half * times)} $DIV $half", times.toLong(), "So viele Halbwertszeiten."), ds("${d(start)} $DIV ${n(pow(2, times))}", end, "Jede Halbwertszeit halbiert die Menge.")),
                "Halbwertszeiten zählen, halbieren",
                listOf(mistake(start.divide(bd(2L * times), java.math.MathContext.DECIMAL64), "Durch die Anzahl mal 2 geteilt statt wiederholt zu halbieren.")),
            )
        }
        else -> {
            val base = r.pick(listOf(2L, 3L, 5L, 10L))
            val a = r.between(1, 9)
            val b = r.between(1, 9)
            val multiply = r.nextBoolean()
            if (!multiply && a <= b) return growth(id, Random(r.nextLong()))
            val e = if (multiply) a + b else a - b
            ExtraKit.int(
                id, "powlaw$base:$a${if (multiply) "*" else "/"}$b", Prompt("$base${sup(a)} ${if (multiply) "·" else DIV} $base${sup(b)} = $base^?", caption = "Potenzgesetze"), e.toLong(),
                listOf(step(if (multiply) "$a + $b" else "$a $MINUS $b", e.toLong(), if (multiply) "Gleiche Basis: Exponenten addieren." else "Gleiche Basis: Exponenten abziehen.")),
                if (multiply) "Exponenten addieren" else "Exponenten abziehen",
                listOf(mistake((a * b).toLong(), "Exponenten malgenommen – das gilt nur für (aᵐ)ⁿ.")),
            )
        }
    }

    // --- Ableitungen -----------------------------------------------------------------------------

    private fun derivative(id: String, r: Random): Problem {
        val count = r.between(1, 3)
        val exps = (1..5).shuffled(r).take(count).sortedDescending()
        val terms = exps.map { e -> r.between(1, 6).toLong() to e }
        val x0 = if (exps.first() <= 3) r.between(1, 3).toLong() else r.between(1, 2).toLong()
        val parts = terms.map { (c, e) -> c * e * pow(x0, e - 1) }
        val value = parts.sum()
        val derived = poly(terms.map { (c, e) -> c * e to e - 1 })
        val steps = terms.zip(parts).map { (t, part) ->
            val (c, e) = t
            step(if (e == 1) n(c) else "${n(c * e)} $X ${n(pow(x0, e - 1))}", part, "Ableitung von ${term(c, e)}: ${term(c * e, e - 1)} (Exponent nach vorne, Exponent minus 1), bei x = $x0.")
        }.toMutableList()
        if (parts.size > 1) steps += step(parts.joinToString(" + ") { n(it) }, value, "f'(x) = $derived – die Ableitungen der Summanden zusammenzählen.")
        val noPower = terms.sumOf { (c, e) -> c * pow(x0, e - 1) }
        return ExtraKit.int(
            id, "deriv:${poly(terms)}@$x0", Prompt("f(x) = ${poly(terms)}: f'($x0) = ?", caption = "Steigung an einer Stelle"), value, steps,
            "Potenzregel: n·xⁿ⁻¹",
            listOf(mistake(noPower, "Den Exponenten nicht nach vorne geholt – die Ableitung von c·xⁿ ist c·n·xⁿ⁻¹.")),
        )
    }

    // --- Scheitelpunkt ---------------------------------------------------------------------------

    private fun vertex(id: String, r: Random): Problem {
        val down = r.nextInt(3) == 0
        val a = r.pick(listOf(1L, 1L, 2L, 3L)) * if (down) -1 else 1
        val h = r.between(1, 9).toLong()
        val k = r.between(1, 40).toLong()
        val b = -2 * a * h
        val c = k + a * h * h
        if (c <= 0) return vertex(id, Random(r.nextLong()))
        val text = poly(listOf(a to 2, b to 1, c to 0))
        val askY = r.nextBoolean()
        val xStep = step("${n(kotlin.math.abs(b))} $DIV ${n(2 * kotlin.math.abs(a))}", h, "x-Wert des Scheitels: −b ÷ (2a).")
        return if (!askY) {
            ExtraKit.int(
                id, "vertex-x:$text", Prompt("f(x) = $text: Scheitel bei x = ?", caption = if (down) "Hochpunkt" else "Tiefpunkt"), h, listOf(xStep),
                "x = −b ÷ 2a",
                listOf(mistake(kotlin.math.abs(b), "Nicht durch 2a geteilt.")),
            )
        } else {
            val ahh = kotlin.math.abs(a) * h * h
            val yStep = if (a > 0) step("${n(c)} $MINUS ${n(ahh)}", k, "f($h) ausrechnen: Der Scheitel ist der ${if (down) "höchste" else "tiefste"} Punkt.")
            else step("${n(c)} + ${n(ahh)}", k, "f($h) ausrechnen: Der Scheitel ist der höchste Punkt.")
            ExtraKit.int(
                id, "vertex-y:$text", Prompt("f(x) = $text: Scheitel bei y = ?", caption = if (down) "Hochpunkt" else "Tiefpunkt"), k, listOf(xStep, yStep),
                "Erst x, dann f(x)",
            )
        }
    }

    // --- Quadratische Gleichungen ----------------------------------------------------------------

    private fun quadratic(id: String, r: Random): Problem {
        val big = r.between(2, 15).toLong()
        val other = r.between(-12, 14).toLong().let { if (it == 0L || it == big) big - 1 else it }
        val small = minOf(big, other)
        val large = maxOf(big, other)
        if (large <= 0) return quadratic(id, Random(r.nextLong()))
        val p = -(small + large)
        val q = small * large
        val text = poly(listOf(1L to 2, p to 1, q to 0).filter { it.first != 0L })
        val half = bd(-p).divide(bd(2))
        val disc = half.multiply(half) - bd(q)
        val root = bd(large) - half
        val steps = listOf(
            ds("${n(kotlin.math.abs(p))} $DIV 2", half.abs(), "p-q-Formel: x = −p/2 ± √((p/2)² − q). Hier p = ${signed(p)}, q = ${signed(q)}."),
            ds(if (q >= 0) "${d(half.abs())} $X ${d(half.abs())} $MINUS ${n(q)}" else "${d(half.abs())} $X ${d(half.abs())} + ${n(-q)}", disc, "Unter der Wurzel."),
            ds("√${d(disc)}", root.abs(), "Die Wurzel."),
            ds(if (half.signum() >= 0) "${d(half)} + ${d(root)}" else "${d(root)} $MINUS ${d(half.abs())}", bd(large), "Die größere Lösung: mit Plus vor der Wurzel."),
        )
        return ExtraKit.int(
            id, "quad:$text", Prompt("$text = 0: größere Lösung x = ?", caption = "Quadratische Gleichung"), large, steps,
            "p-q-Formel oder Vieta",
            listOf(mistake(kotlin.math.abs(small), "Das ist die kleinere Lösung (bzw. ihr Betrag).")),
        )
    }

    // --- Binomische Formeln ----------------------------------------------------------------------

    private fun binomial(id: String, r: Random): Problem {
        val a = r.between(2, 15).toLong()
        return when (r.nextInt(4)) {
            0 -> ExtraKit.int(
                id, "binom1:$a", Prompt("x² + ${n(2 * a)}x + ${n(a * a)} = (x + ?)²", caption = "Erste binomische Formel"), a,
                listOf(step("${n(2 * a)} $DIV 2", a, "(x + a)² = x² + 2ax + a² – das mittlere Glied ist 2a.")), "Mittleres Glied halbieren",
                listOf(mistake(2 * a, "Das ist 2a – die Zahl in der Klammer ist die Hälfte.")),
            )
            1 -> ExtraKit.int(
                id, "binom3:$a", Prompt("x² $MINUS ${n(a * a)} = (x $MINUS ?)(x + ?)", caption = "Dritte binomische Formel"), a,
                listOf(step("√${n(a * a)}", a, "x² − a² = (x − a)(x + a).")), "Wurzel der Quadratzahl",
            )
            2 -> ExtraKit.int(
                id, "binom1m:$a", Prompt("(x + $a)² = x² + ?x + ${n(a * a)}", caption = "Erste binomische Formel"), 2 * a,
                listOf(step("2 $X $a", 2 * a, "Das mittlere Glied ist 2 · a · x.")), "2ab",
                listOf(mistake(a, "Die 2 im mittleren Glied vergessen.")),
            )
            else -> ExtraKit.int(
                id, "binom2m:$a", Prompt("(x $MINUS $a)² = x² $MINUS ?x + ${n(a * a)}", caption = "Zweite binomische Formel"), 2 * a,
                listOf(step("2 $X $a", 2 * a, "(x − a)² = x² − 2ax + a².")), "2ab",
                listOf(mistake(a, "Die 2 im mittleren Glied vergessen.")),
            )
        }
    }

    // --- Trigonometrie ---------------------------------------------------------------------------

    private val trigTable = listOf(
        Triple("sin", 0, "0"), Triple("sin", 30, "1/2"), Triple("sin", 45, "√2/2"), Triple("sin", 60, "√3/2"), Triple("sin", 90, "1"),
        Triple("cos", 0, "1"), Triple("cos", 30, "√3/2"), Triple("cos", 45, "√2/2"), Triple("cos", 60, "1/2"), Triple("cos", 90, "0"),
        Triple("tan", 0, "0"), Triple("tan", 30, "√3/3"), Triple("tan", 45, "1"), Triple("tan", 60, "√3"),
    )

    private fun trig(id: String, r: Random): Problem = when (r.nextInt(3)) {
        0 -> {
            val (f, deg, value) = r.pick(trigTable)
            val wrong = listOf("0", "1/2", "√2/2", "√3/2", "1", "√3", "√3/3").filter { it != value }.shuffled(r)
            ExtraKit.choice(
                id, "trig:$f$deg", Prompt("$f $deg° = ?", caption = "Werte von sin, cos und tan"), value, wrong, "Wert",
                "Merkhilfe: sin von 0°, 30°, 45°, 60°, 90° ist √0/2, √1/2, √2/2, √3/2, √4/2 – bei cos genau andersherum. tan = sin ÷ cos.",
                "√0/2 bis √4/2", r,
            )
        }
        1 -> {
            val deg = r.pick(listOf(45L, 90L, 135L, 180L, 225L, 270L, 315L, 360L, 18L, 36L, 72L, 540L, 720L, 9L))
            val res = bd(deg).divide(bd(180)).stripTrailingZeros()
            ExtraKit.dec(
                id, "rad:$deg", Prompt("${n(deg)}° = ? · π", caption = "Gradmaß ins Bogenmaß"), res,
                listOf(ds("${n(deg)} $DIV 180", res, "180° entsprechen π.")), "Durch 180",
                listOf(mistake(bd(deg).divide(bd(360)).stripTrailingZeros(), "Durch 360 geteilt – π entspricht 180°, nicht 360°.")),
            )
        }
        else -> {
            val den = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 9L, 10L, 12L, 18L))
            val num = r.between(1, (2 * den - 1).toInt()).toLong()
            val deg = 180 * num / den
            if (180 * num % den != 0L) return trig(id, Random(r.nextLong()))
            val g = java.math.BigInteger.valueOf(num).gcd(java.math.BigInteger.valueOf(den)).toLong()
            if (g > 1) return trig(id, Random(r.nextLong()))
            val text = (if (num == 1L) "π" else "${num}π") + "/$den"
            ExtraKit.int(
                id, "deg:$num/$den", Prompt("$text = ? °", caption = "Bogenmaß ins Gradmaß"), deg,
                listOf(step("180 $DIV $den", 180 / den, "π = 180°, also π/$den = ${n(180 / den)}°."), step("${n(180 / den)} $X $num", deg)).let { if (num == 1L) it.take(1) else it },
                "π durch 180° ersetzen",
            )
        }
    }

    // --- Pythagoras ------------------------------------------------------------------------------

    private val triples = listOf(Triple(3L, 4L, 5L), Triple(5L, 12L, 13L), Triple(8L, 15L, 17L), Triple(7L, 24L, 25L), Triple(20L, 21L, 29L), Triple(9L, 40L, 41L))

    private fun pythagoras(id: String, r: Random): Problem {
        val (x, y, z) = r.pick(triples)
        val k = r.between(1, if (z > 20) 2 else 6).toLong()
        val a = x * k
        val b = y * k
        val c = z * k
        return if (r.nextBoolean()) {
            ExtraKit.int(
                id, "pyth:$a,$b", Prompt("Katheten ${n(a)} und ${n(b)}: Hypotenuse = ?", caption = "Satz des Pythagoras"), c,
                listOf(step("${n(a)} $X ${n(a)} + ${n(b)} $X ${n(b)}", c * c, "a² + b² = c²."), step("√${n(c * c)}", c, if (k > 1) "Tipp: Das ist das $k-Fache des Tripels $x, $y, $z." else "Ein bekanntes Pythagoras-Tripel.")),
                "a² + b² = c²",
                listOf(mistake(a + b, "Katheten einfach addiert – erst quadrieren, addieren, dann die Wurzel.")),
            )
        } else {
            ExtraKit.int(
                id, "pyth:$c-$a", Prompt("Hypotenuse ${n(c)}, Kathete ${n(a)}: andere Kathete = ?", caption = "Satz des Pythagoras"), b,
                listOf(step("${n(c)} $X ${n(c)} $MINUS ${n(a)} $X ${n(a)}", b * b, "b² = c² − a²."), step("√${n(b * b)}", b)),
                "b² = c² − a²",
            )
        }
    }

    // --- Statistik -------------------------------------------------------------------------------

    private fun statistics(id: String, r: Random): Problem = when (r.nextInt(3)) {
        0 -> {
            val count = r.between(4, 7)
            val mean = r.between(3, 60).toLong()
            val values = MutableList(count - 1) { (mean + r.between(-8, 8)).coerceAtLeast(1) }
            val last = mean * count - values.sum()
            if (last < 1 || last > mean + 15) return statistics(id, Random(r.nextLong()))
            values += last
            values.shuffle(r)
            val sum = values.sum()
            ExtraKit.int(
                id, "mean:${values.joinToString(",")}", Prompt("Mittelwert von ${values.joinToString(", ") { n(it) }}", caption = "Arithmetisches Mittel"), mean,
                listOf(step(values.joinToString(" + ") { n(it) }, sum, "Alle Werte zusammenzählen."), step("${n(sum)} $DIV $count", mean, "Durch die Anzahl ($count) teilen.")),
                "Summe ÷ Anzahl",
            )
        }
        1 -> {
            val count = r.pick(listOf(5, 7, 6))
            val values = List(count) { r.between(1, 60).toLong() }
            val sorted = values.sorted()
            val median = if (count % 2 == 1) bd(sorted[count / 2]) else bd(sorted[count / 2 - 1] + sorted[count / 2]).divide(bd(2))
            ExtraKit.dec(
                id, "median:${values.joinToString(",")}", Prompt("Median von ${values.joinToString(", ") { n(it) }}", caption = "Zentralwert"), median,
                listOf(ds(if (count % 2 == 1) n(sorted[count / 2]) else "${n(sorted[count / 2 - 1])} + ${n(sorted[count / 2])} $DIV 2".let { "(${n(sorted[count / 2 - 1])} + ${n(sorted[count / 2])}) ÷ 2" }, median, "Sortiert: ${sorted.joinToString(", ") { n(it) }}. " + if (count % 2 == 1) "Der mittlere Wert." else "Bei gerader Anzahl: Mittel der beiden mittleren Werte.")),
                "Sortieren, Mitte nehmen",
                listOf(mistake(bd(values[count / 2]), "Nicht sortiert – der Median ist die Mitte der sortierten Liste.")),
            )
        }
        else -> {
            val g1 = r.pick(listOf(2L, 5L, 10L, 20L, 50L, 100L))
            val p1 = r.pick(listOf(10L, 20L, 25L, 30L, 40L, 50L))
            val g2 = r.pick(listOf(1L, 2L, 4L, 5L, 10L))
            val p2 = r.pick(listOf(10L, 20L, 30L, 40L).filter { it + p1 <= 90 })
            val e1 = bd(g1 * p1).divide(bd(100))
            val e2 = bd(g2 * p2).divide(bd(100))
            val e = (e1 + e2).stripTrailingZeros()
            ExtraKit.dec(
                id, "ev:$g1@$p1+$g2@$p2", Prompt("${n(g1)} € mit $p1 %, ${n(g2)} € mit $p2 %, sonst 0 €: Erwartungswert = ? €", caption = "Erwartungswert"), e,
                listOf(ds("${n(g1)} $X 0,${"%02d".format(p1)}", e1.stripTrailingZeros(), "Gewinn mal Wahrscheinlichkeit."), ds("${n(g2)} $X 0,${"%02d".format(p2)}", e2.stripTrailingZeros()), ds("${d(e1)} + ${d(e2)}", e, "Alle Beiträge zusammenzählen (0 € trägt nichts bei).")),
                "Summe aus Wert × Wahrscheinlichkeit",
            )
        }
    }

    // --- Wahrscheinlichkeit ----------------------------------------------------------------------

    private fun choose(nn: Long, k: Long): Long {
        var v = 1L
        for (i in 1..k) v = v * (nn - k + i) / i
        return v
    }

    private fun probability(id: String, r: Random): Problem = when (r.nextInt(4)) {
        0 -> {
            val nn = r.between(4, 12).toLong()
            val k = r.between(1, minOf(4, (nn - 1).toInt())).toLong()
            val num = (0 until k).fold(1L) { acc, i -> acc * (nn - i) }
            val fact = (1..k).fold(1L) { acc, i -> acc * i }
            ExtraKit.int(
                id, "choose:$nn,$k", Prompt("$nn über $k", caption = "Binomialkoeffizient"), choose(nn, k),
                listOf(step((0 until k).joinToString(" $X ") { n(nn - it) }, num, "Oben $k Faktoren absteigend ab $nn …"), step("${n(num)} $DIV ${n(fact)}", choose(nn, k), "… durch $k! = ${n(fact)}.")),
                "n·(n−1)·… ÷ k!",
                listOf(mistake(num, "Nicht durch k! geteilt – die Reihenfolge spielt beim Auswählen keine Rolle.")),
            )
        }
        1 -> {
            val pa = bd(r.between(1, 9).toLong()).divide(bd(10))
            val pb = bd(r.between(1, 9).toLong()).divide(bd(10))
            val res = pa.multiply(pb).stripTrailingZeros()
            ExtraKit.dec(
                id, "indep:$pa*$pb", Prompt("P(A) = ${d(pa)}, P(B) = ${d(pb)}, unabhängig: P(A und B) = ?", caption = "Pfadregel"), res,
                listOf(ds("${d(pa)} $X ${d(pb)}", res, "Unabhängige Ereignisse: Wahrscheinlichkeiten entlang des Pfades malnehmen.")),
                "Pfadregel: malnehmen",
                listOf(mistake(pa + pb, "Addiert – „und“ heißt entlang eines Pfades malnehmen.")),
            )
        }
        2 -> {
            val total = r.pick(listOf(100L, 200L, 400L, 500L, 1000L))
            val sick = total * r.pick(listOf(10L, 20L, 25L, 40L)) / 100
            val pos = sick * r.pick(listOf(50L, 60L, 75L, 80L, 90L)) / 100
            val res = bd(pos).divide(bd(sick)).stripTrailingZeros()
            ExtraKit.dec(
                id, "cond:$total,$sick,$pos", Prompt("${n(total)} Personen, ${n(sick)} krank, davon ${n(pos)} positiv getestet: P(positiv | krank) = ?", caption = "Bedingte Wahrscheinlichkeit"), res,
                listOf(ds("${n(pos)} $DIV ${n(sick)}", res, "Bedingt auf „krank“: Nur die Kranken zählen als Gesamtheit.")),
                "Nur in der Bedingung zählen",
                listOf(mistake(bd(pos).divide(bd(total)).stripTrailingZeros(), "Durch alle Personen geteilt – die Bedingung „krank“ schränkt die Gesamtheit ein.")),
            )
        }
        else -> {
            val sum = r.between(2, 12)
            val ways = 6 - kotlin.math.abs(7 - sum)
            val g = java.math.BigInteger.valueOf(ways.toLong()).gcd(java.math.BigInteger.valueOf(36)).toLong()
            Problem(
                skillId = id,
                key = "dice:$sum",
                prompt = Prompt("Zwei Würfel: P(Augensumme $sum) = ?", caption = "Als Bruch eingeben"),
                expected = Expected.Frac(ways.toLong(), 36),
                input = InputKind.FRACTION,
                steps = listOf(
                    step("Günstige Paare für $sum", ways.toLong(), "Es gibt 36 gleich wahrscheinliche Paare; die Summe $sum entsteht auf $ways ${if (ways == 1) "Art" else "Arten"}."),
                    Step("$ways ÷ 36", Expected.Frac(ways.toLong(), 36), InputKind.FRACTION, "Günstige durch mögliche: $ways/36" + if (g > 1) " = ${ways / g}/${36 / g}." else "."),
                ),
                method = "Günstige ÷ mögliche Fälle",
            )
        }
    }

    // --- Vektoren --------------------------------------------------------------------------------

    private fun vec(v: List<Long>) = "(" + v.joinToString("|") { signed(it) } + ")"

    private val quadruples = listOf(listOf(1L, 2L, 2L, 3L), listOf(2L, 3L, 6L, 7L), listOf(1L, 4L, 8L, 9L), listOf(4L, 4L, 7L, 9L), listOf(2L, 6L, 9L, 11L), listOf(6L, 6L, 7L, 11L), listOf(3L, 4L, 12L, 13L), listOf(2L, 10L, 11L, 15L), listOf(1L, 12L, 12L, 17L))

    private fun vectors(id: String, r: Random): Problem = when (r.nextInt(3)) {
        0 -> {
            repeat(50) {
                val v = List(3) { r.between(-4, 6).toLong() }
                val w = List(3) { r.between(-4, 6).toLong() }
                val products = v.zip(w).map { (a, b) -> a * b }
                val dot = products.sum()
                if (dot > 0 && products.all { it >= 0 }) {
                    return ExtraKit.int(
                        id, "dot:${vec(v)}${vec(w)}", Prompt("${vec(v)} · ${vec(w)}", caption = "Skalarprodukt"), dot,
                        listOf(step(products.joinToString(" + ") { n(it) }, dot, "Komponentenweise malnehmen (${v.zip(w).joinToString(", ") { (a, b) -> "${signed(a)} · ${signed(b)}" }}) und addieren.")),
                        "x·x + y·y + z·z",
                    )
                }
            }
            vectors(id, Random(r.nextLong()))
        }
        1 -> {
            val q = r.pick(quadruples)
            val k = if (q[3] > 11) 1L else r.between(1, 3).toLong()
            val comps = q.take(3).map { it * k }.shuffled(r).map { if (r.nextInt(3) == 0) -it else it }
            val len = q[3] * k
            val sq = comps.sumOf { it * it }
            ExtraKit.int(
                id, "norm:${vec(comps)}", Prompt("|${vec(comps)}|", caption = "Betrag (Länge) des Vektors"), len,
                listOf(step(comps.joinToString(" + ") { "${n(kotlin.math.abs(it))} $X ${n(kotlin.math.abs(it))}" }, sq, "Komponenten quadrieren und addieren (Vorzeichen fallen weg)."), step("√${n(sq)}", len)),
                "√(x² + y² + z²)",
                listOf(mistake(comps.sumOf { kotlin.math.abs(it) }, "Komponenten addiert statt Quadratsumme und Wurzel.")),
            )
        }
        else -> {
            repeat(80) {
                val a = r.between(1, 9).toLong()
                val y2 = r.between(1, 4).toLong()
                val s = a * y2
                val x1 = r.between(1, 4).toLong()
                val x2 = r.between(1, 4).toLong()
                val rest = s - x1 * x2
                if (rest <= 0) return@repeat
                val z1 = (1L..9L).filter { rest % it == 0L && rest / it <= 9 }.let { if (it.isEmpty()) return@repeat else r.pick(it) }
                val z2 = rest / z1
                val text = "(${x1}|a|$z1) ⊥ (${x2}|$MINUS$y2|$z2): a = ?"
                return ExtraKit.int(
                    id, "perp:$x1,$z1,$x2,$y2,$z2", Prompt(text, caption = "Senkrecht: Skalarprodukt 0"), a,
                    listOf(step("${n(x1 * x2)} + ${n(z1 * z2)}", s, "Senkrecht heißt Skalarprodukt 0: ${x1 * x2} − ${y2}a + ${z1 * z2} = 0."), step("${n(s)} $DIV $y2", a, "Nach a auflösen.")),
                    "Skalarprodukt gleich null",
                )
            }
            vectors(id, Random(r.nextLong()))
        }
    }

    // --- Integrale -------------------------------------------------------------------------------

    private fun integral(id: String, r: Random): Problem {
        val e = r.between(0, 3)
        val c = (e + 1).toLong() * r.between(1, 3)
        val a = r.between(0, 3).toLong()
        val b = a + r.between(1, 3)
        val anti = c / (e + 1)
        val fb = anti * pow(b, e + 1)
        val fa = anti * pow(a, e + 1)
        val value = fb - fa
        val f = term(c, e)
        val big = term(anti, e + 1)
        return ExtraKit.int(
            id, "int:$f[$a,$b]", Prompt("∫${sub(a.toInt())}${sup(b.toInt())} $f dx", caption = "Bestimmtes Integral"), value,
            listOf(
                step("${n(anti)} $X ${n(pow(b, e + 1))}", fb, "Stammfunktion F(x) = $big (Exponent plus 1, durch den neuen Exponenten teilen). F($b)."),
                step("${n(anti)} $X ${n(pow(a, e + 1))}", fa, "F($a)."),
                step("${n(fb)} $MINUS ${n(fa)}", value, "Obere minus untere Grenze."),
            ),
            "F(b) − F(a)",
            listOf(mistake(c * pow(b, e) - c * pow(a, e), "Die Funktion selbst eingesetzt statt der Stammfunktion.")),
        )
    }

    private val skillList by lazy {
        val b = book
        listOf(
            ExtraKit.skill(
                "w17.log", "Logarithmen", "Exponent suchen",
                "log₂ 64, lg 1.000 und Logarithmengesetze im Kopf.",
                "Der Logarithmus beantwortet die Frage: Mit welchem Exponenten muss ich die Basis potenzieren, um die Zahl zu bekommen? log₂ 64 = 6, weil 2⁶ = 64. lg steht für die Basis 10 – bei Zehnerpotenzen zählst du einfach die Nullen.",
                listOf("Basis erkennen (log₂, log₃, lg = log₁₀, ln = logₑ).", "Fragen: Basis hoch wie viel ergibt die Zahl?", "Bei Produkten: Logarithmen addieren; bei Quotienten: abziehen."),
                listOf("logₐ(aⁿ) = n", "log(u · v) = log u + log v", "2¹⁰ = 1.024 · 3⁵ = 243 · 5⁴ = 625"),
                "Die Zweierpotenzen bis 2¹⁰ = 1.024 lohnen sich auswendig – sie kommen überall vor.",
                ExtraKit.medium, b, 1701, ::log,
            ),
            ExtraKit.skill(
                "w17.growth", "Potenzen und Wachstum", "Wiederholtes Malnehmen",
                "Potenzen, Potenzgesetze, Verdopplung und Halbwertszeit.",
                "Eine Potenz ist wiederholtes Malnehmen. Exponentielles Wachstum heißt: In gleichen Zeitabständen wird mit demselben Faktor malgenommen – bei Verdopplung mit 2, bei einer Halbwertszeit mit ½. Zähle, wie oft das passiert, und nimm den Faktor so oft mal.",
                listOf("Wie oft passt der Zeitabstand in die Gesamtzeit?", "So oft mit dem Faktor malnehmen (2 oder ½).", "Potenzgesetze: gleiche Basis → Exponenten addieren (mal) oder abziehen (geteilt)."),
                listOf("aᵐ · aⁿ = aᵐ⁺ⁿ", "aᵐ ÷ aⁿ = aᵐ⁻ⁿ", "nach n Halbwertszeiten: Menge ÷ 2ⁿ"),
                "Nach 10 Halbwertszeiten ist nur noch etwa ein Tausendstel übrig (2¹⁰ ≈ 1.000).",
                ExtraKit.medium, b, 1702, ::growth,
            ),
            ExtraKit.skill(
                "w17.deriv", "Ableitungen", "Potenzregel",
                "Steigung von Polynomen an einer Stelle.",
                "Die Ableitung gibt die Steigung einer Funktion an. Für Potenzen gilt die Potenzregel: Der Exponent wandert als Faktor nach vorne, der neue Exponent ist eins kleiner. Summen leitest du Glied für Glied ab, Zahlen ohne x fallen weg.",
                listOf("Jedes Glied einzeln ableiten: c·xⁿ wird zu c·n·xⁿ⁻¹.", "Konstanten fallen weg.", "Den x-Wert einsetzen und zusammenzählen."),
                listOf("(xⁿ)' = n · xⁿ⁻¹", "(c · xⁿ)' = c · n · xⁿ⁻¹", "(5x)' = 5 · (7)' = 0"),
                "Bei x = 1 ist es besonders leicht: Alle Potenzen von 1 sind 1, du zählst nur die Faktoren c·n zusammen.",
                ExtraKit.medium, b, 1703, ::derivative,
            ),
            ExtraKit.skill(
                "w17.vertex", "Scheitelpunkt", "x = −b ÷ 2a",
                "Hoch- und Tiefpunkte von Parabeln.",
                "Der Scheitel einer Parabel f(x) = ax² + bx + c liegt bei x = −b ÷ (2a) – dort ist die Ableitung 2ax + b gleich null. Den y-Wert bekommst du durch Einsetzen. Ist a positiv, ist der Scheitel ein Tiefpunkt, sonst ein Hochpunkt.",
                listOf("x-Wert: −b durch 2a teilen.", "y-Wert: den x-Wert in f einsetzen.", "Vorzeichen von a: positiv = Tiefpunkt, negativ = Hochpunkt."),
                listOf("x_S = −b ÷ (2a)", "f(x) = a(x − x_S)² + y_S"),
                "Bei a = 1 ist der x-Wert einfach die Hälfte von −b.",
                ExtraKit.medium, b, 1704, ::vertex,
            ),
            ExtraKit.skill(
                "w17.quad", "Quadratische Gleichungen", "p-q-Formel und Vieta",
                "x² + px + q = 0 im Kopf lösen.",
                "Für x² + px + q = 0 gibt es die p-q-Formel: x = −p/2 ± √((p/2)² − q). Oft geht es mit dem Satz von Vieta noch schneller: Die beiden Lösungen ergeben zusammen −p und malgenommen q. Bei x² − 7x + 12 suchst du zwei Zahlen mit Summe 7 und Produkt 12: 3 und 4.",
                listOf("Vieta probieren: Welche zwei Zahlen haben Produkt q und Summe −p?", "Sonst p-q-Formel: p halbieren, quadrieren, q abziehen, Wurzel ziehen.", "Größere Lösung: Plus vor der Wurzel."),
                listOf("x = −p/2 ± √((p/2)² − q)", "x₁ + x₂ = −p · x₁ · x₂ = q"),
                "Ist q negativ, hat eine Lösung ein Minus – die positive Lösung ist dann die größere.",
                ExtraKit.slow, b, 1705, ::quadratic,
            ),
            ExtraKit.skill(
                "w17.binom", "Binomische Formeln", "a² ± 2ab + b²",
                "Ausmultiplizieren und Faktorisieren mit den drei Formeln.",
                "Die drei binomischen Formeln brauchst du ständig: (a + b)² = a² + 2ab + b², (a − b)² = a² − 2ab + b², (a + b)(a − b) = a² − b². Rückwärts gelesen helfen sie beim Faktorisieren: Ist das mittlere Glied doppelt so groß wie die Wurzel des letzten, steckt eine binomische Formel dahinter.",
                listOf("Ausmultiplizieren: mittleres Glied ist 2 · a · b.", "Faktorisieren: Wurzel aus dem letzten Glied, mittleres Glied prüfen.", "Differenz von Quadraten erkennen: x² − a²."),
                listOf("(a + b)² = a² + 2ab + b²", "(a − b)² = a² − 2ab + b²", "(a + b)(a − b) = a² − b²"),
                "Der häufigste Fehler ist die vergessene 2 im mittleren Glied.",
                ExtraKit.quick, b, 1706, ::binomial,
            ),
            ExtraKit.skill(
                "w17.trig", "Trigonometrie", "Wertetabelle und π = 180°",
                "sin, cos, tan bei besonderen Winkeln und das Bogenmaß.",
                "Die Werte von Sinus und Kosinus bei 0°, 30°, 45°, 60° und 90° kann man sich mit einem Muster merken: sin ist √0/2, √1/2, √2/2, √3/2, √4/2 – cos ist dieselbe Reihe rückwärts. Im Bogenmaß entsprechen 180° genau π.",
                listOf("sin-Reihe: √0/2, √1/2, √2/2, √3/2, √4/2 für 0°, 30°, 45°, 60°, 90°.", "cos: dieselbe Reihe rückwärts. tan = sin ÷ cos.", "Grad → Bogenmaß: durch 180 und mal π; zurück: π durch 180° ersetzen."),
                listOf("sin 30° = 1/2 · cos 60° = 1/2 · tan 45° = 1", "180° = π · 90° = π/2 · 60° = π/3"),
                "√2/2 ≈ 0,71 und √3/2 ≈ 0,87 – gut zum Überschlagen.",
                ExtraKit.quick, b, 1707, ::trig,
            ),
            ExtraKit.skill(
                "w17.pyth", "Satz des Pythagoras", "a² + b² = c²",
                "Seiten im rechtwinkligen Dreieck mit Pythagoras-Tripeln.",
                "Im rechtwinkligen Dreieck gilt a² + b² = c², wobei c die längste Seite (Hypotenuse) ist. Viele Aufgaben benutzen ganzzahlige Tripel wie 3-4-5, 5-12-13 oder 8-15-17 – und deren Vielfache. Wer sie kennt, sieht das Ergebnis oft sofort.",
                listOf("Ist ein bekanntes Tripel (oder ein Vielfaches) versteckt? Dann direkt ablesen.", "Sonst: quadrieren, addieren (oder abziehen), Wurzel ziehen."),
                listOf("3-4-5 · 5-12-13 · 8-15-17 · 7-24-25 · 20-21-29", "6-8-10 = 2 × (3-4-5)"),
                "Die Hypotenuse ist immer die längste Seite – ist dein Ergebnis kleiner als eine Kathete, stimmt etwas nicht.",
                ExtraKit.quick, b, 1708, ::pythagoras,
            ),
            ExtraKit.skill(
                "w17.stats", "Mittelwert, Median, Erwartungswert", "Summe ÷ Anzahl",
                "Kennzahlen von Daten und Zufallsgrößen.",
                "Der Mittelwert ist die Summe aller Werte geteilt durch ihre Anzahl. Der Median ist der Wert in der Mitte der sortierten Liste – bei gerader Anzahl das Mittel der beiden mittleren. Der Erwartungswert ist die Summe aus „Wert mal Wahrscheinlichkeit“.",
                listOf("Mittelwert: addieren, durch die Anzahl teilen.", "Median: sortieren, Mitte nehmen.", "Erwartungswert: jeden Wert mit seiner Wahrscheinlichkeit malnehmen und addieren."),
                listOf("x̄ = (x₁ + … + xₙ) ÷ n", "E(X) = Σ xᵢ · P(xᵢ)"),
                "Trick für den Mittelwert: Einen Bezugswert wählen und nur die Abweichungen addieren.",
                ExtraKit.medium, b, 1709, ::statistics,
            ),
            ExtraKit.skill(
                "w17.prob", "Wahrscheinlichkeit", "Pfadregel und Abzählen",
                "Binomialkoeffizienten, Pfadregel, bedingte Wahrscheinlichkeit, Würfel.",
                "Wahrscheinlichkeiten entlang eines Pfades im Baumdiagramm werden malgenommen. „n über k“ zählt, wie viele Möglichkeiten es gibt, k aus n auszuwählen. Bei bedingten Wahrscheinlichkeiten schränkt die Bedingung die Gesamtheit ein: P(B | A) = P(A und B) ÷ P(A).",
                listOf("„und“ entlang eines Pfades: malnehmen.", "n über k = n · (n−1) · … (k Faktoren) ÷ k!", "Bedingt: nur die Fälle zählen, in denen die Bedingung erfüllt ist.", "Laplace: günstige durch mögliche Fälle."),
                listOf("P(A ∩ B) = P(A) · P(B) bei Unabhängigkeit", "(5 über 2) = 5 · 4 ÷ 2 = 10", "P(B | A) = P(A ∩ B) ÷ P(A)"),
                "n über k ist gleich n über (n − k): 10 über 8 = 10 über 2 = 45.",
                ExtraKit.medium, b, 1710, ::probability,
            ),
            ExtraKit.skill(
                "w17.vector", "Vektoren", "Komponentenweise rechnen",
                "Skalarprodukt, Betrag und senkrechte Vektoren.",
                "Das Skalarprodukt zweier Vektoren ist die Summe der Produkte ihrer Komponenten. Ist es null, stehen die Vektoren senkrecht aufeinander. Den Betrag (die Länge) berechnest du mit Pythagoras im Raum: Wurzel aus der Summe der Quadrate.",
                listOf("Skalarprodukt: x·x + y·y + z·z.", "Senkrecht: Skalarprodukt = 0 setzen und nach der Unbekannten auflösen.", "Betrag: Komponenten quadrieren, addieren, Wurzel."),
                listOf("a · b = a₁b₁ + a₂b₂ + a₃b₃", "|a| = √(a₁² + a₂² + a₃²)", "(1|2|2) hat die Länge 3"),
                "Ganzzahlige Längen gibt es bei Quadrupeln wie 1-2-2-3, 2-3-6-7 oder 1-4-8-9.",
                ExtraKit.medium, b, 1711, ::vectors,
            ),
            ExtraKit.skill(
                "w17.integral", "Integrale", "F(b) − F(a)",
                "Bestimmte Integrale von Potenzfunktionen.",
                "Ein bestimmtes Integral berechnest du mit einer Stammfunktion F: Exponent um 1 erhöhen und durch den neuen Exponenten teilen. Dann F(obere Grenze) − F(untere Grenze). Das Ergebnis ist die Fläche zwischen Graph und x-Achse (wenn der Graph oberhalb liegt).",
                listOf("Stammfunktion bilden: c·xⁿ wird zu c/(n+1) · xⁿ⁺¹.", "Obere Grenze einsetzen.", "Untere Grenze einsetzen und abziehen."),
                listOf("∫ xⁿ dx = xⁿ⁺¹ ÷ (n + 1)", "∫ₐᵇ f(x) dx = F(b) − F(a)", "∫₀³ x² dx = 9 − 0 = 9"),
                "Beginnt das Integral bei 0, fällt die untere Grenze meistens weg.",
                ExtraKit.medium, b, 1712, ::integral,
            ),
        )
    }

    private val mixed by lazy {
        ExtraKit.skill(
            "w17.mixed", "Oberstufe gemischt", "Thema erkennen",
            "Alle Oberstufen-Themen durcheinander – wie in einer Klausur ohne Taschenrechner.",
            "Hier kommen alle Themen dieser Welt durcheinander – ideal für den hilfsmittelfreien Teil im Abitur.",
            listOf("Thema erkennen.", "Passende Regel abrufen.", "Kurz prüfen, ob das Ergebnis plausibel ist."),
            emptyList(),
            "Im hilfsmittelfreien Teil sind die Zahlen fast immer so gewählt, dass glatte Ergebnisse herauskommen.",
            ExtraKit.medium, book, 1713, ExtraKit.mixedOf { skillList },
        )
    }

    val world by lazy {
        World(id = "w17", number = 17, title = "Oberstufe und Mathe-LK", subtitle = "Ergänzung · hilfsmittelfrei im Kopf", skills = skillList + mixed, independent = true)
    }
}
