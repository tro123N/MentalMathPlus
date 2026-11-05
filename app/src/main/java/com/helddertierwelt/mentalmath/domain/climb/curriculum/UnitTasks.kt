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

import com.helddertierwelt.mentalmath.domain.climb.engine.SessionLogic
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Mistake
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import java.math.BigDecimal
import java.math.MathContext
import kotlin.random.Random

/**
 * Aufgaben zum Einheiten-Umrechnen (Welt 14 und freies Training): Vorsilben und Zehnerpotenzen,
 * Länge, Masse, Volumen, Fläche, Zeit, Geschwindigkeit, Konzentration, Physik und die
 * wissenschaftliche Schreibweise. Alle Zahlen werden zufällig erzeugt – es gibt unbegrenzt viele Aufgaben.
 */
object UnitTasks {

    const val MU = "µ"
    private const val DOT = "·"

    /** Eine Einheit: Symbol und Wert in der Grundeinheit ihrer Größe. [linear]: zugehörige Längeneinheit bei Fläche/Volumen. */
    data class Unit(val symbol: String, val factor: BigDecimal, val linear: String? = null)

    /** Eine physikalische Größe mit ihren Einheiten (von groß nach klein). [power]: 1 Länge, 2 Fläche, 3 Volumen. */
    class Dim(val id: String, val caption: String, val units: List<Unit>, val power: Int = 1) {
        fun unit(symbol: String): Unit = units.first { it.symbol == symbol }
    }

    private fun u(symbol: String, factor: String, linear: String? = null) = Unit(symbol, BigDecimal(factor), linear)

    /** Längeneinheiten in Meter – für Flächen- und Volumenfaktoren. */
    private val lengthInMeters = mapOf("km" to "1000", "m" to "1", "dm" to "0.1", "cm" to "0.01", "mm" to "0.001").mapValues { BigDecimal(it.value) }

    val LENGTH = Dim(
        "length", "Längen umrechnen",
        listOf(u("km", "1000"), u("m", "1"), u("dm", "0.1"), u("cm", "0.01"), u("mm", "0.001"), u("${MU}m", "0.000001"), u("nm", "0.000000001")),
    )
    val MASS = Dim(
        "mass", "Massen umrechnen",
        listOf(u("t", "1000000"), u("kg", "1000"), u("g", "1"), u("mg", "0.001"), u("${MU}g", "0.000001"), u("ng", "0.000000001")),
    )
    val VOLUME = Dim(
        "volume", "Volumen umrechnen",
        listOf(
            u("m³", "1000", "m"), u("hl", "100"), u("l", "1", "dm"), u("dm³", "1", "dm"), u("dl", "0.1"), u("cl", "0.01"),
            u("ml", "0.001", "cm"), u("cm³", "0.001", "cm"), u("mm³", "0.000001", "mm"), u("${MU}l", "0.000001", "mm"),
        ),
        power = 3,
    )
    val AREA = Dim(
        "area", "Flächen umrechnen",
        listOf(u("km²", "1000000", "km"), u("ha", "10000"), u("a", "100"), u("m²", "1", "m"), u("dm²", "0.01", "dm"), u("cm²", "0.0001", "cm"), u("mm²", "0.000001", "mm")),
        power = 2,
    )
    val TIME = Dim(
        "time", "Zeit umrechnen",
        listOf(u("d", "86400"), u("h", "3600"), u("min", "60"), u("s", "1"), u("ms", "0.001"), u("${MU}s", "0.000001")),
    )
    val CONC = Dim(
        "conc", "Konzentrationen umrechnen",
        listOf(u("g/ml", "1000"), u("g/dl", "10"), u("g/l", "1"), u("mg/ml", "1"), u("mg/dl", "0.01"), u("mg/l", "0.001"), u("${MU}g/ml", "0.001"), u("${MU}g/l", "0.000001")),
    )
    val MOLAR = Dim(
        "molar", "Stoffmengen-Konzentration",
        listOf(u("mol/l", "1"), u("mmol/l", "0.001"), u("${MU}mol/l", "0.000001"), u("nmol/l", "0.000000001")),
    )
    val PRESSURE = Dim("pressure", "Druck umrechnen", listOf(u("MPa", "1000000"), u("bar", "100000"), u("kPa", "1000"), u("hPa", "100"), u("mbar", "100"), u("Pa", "1")))
    val ENERGY = Dim("energy", "Energie umrechnen", listOf(u("kWh", "3600000"), u("MJ", "1000000"), u("kJ", "1000"), u("Wh", "3600"), u("J", "1")))
    val POWER = Dim("power", "Leistung umrechnen", listOf(u("GW", "1000000000"), u("MW", "1000000"), u("kW", "1000"), u("W", "1"), u("mW", "0.001")))
    val CURRENT = Dim("current", "Stromstärke umrechnen", listOf(u("A", "1"), u("mA", "0.001"), u("${MU}A", "0.000001"), u("nA", "0.000000001")))
    val VOLTAGE = Dim("voltage", "Spannung umrechnen", listOf(u("kV", "1000"), u("V", "1"), u("mV", "0.001"), u("${MU}V", "0.000001")))
    val FREQUENCY = Dim("frequency", "Frequenz umrechnen", listOf(u("GHz", "1000000000"), u("MHz", "1000000"), u("kHz", "1000"), u("Hz", "1")))
    val AMOUNT = Dim("amount", "Stoffmenge umrechnen", listOf(u("mol", "1"), u("mmol", "0.001"), u("${MU}mol", "0.000001"), u("nmol", "0.000000001")))

    /** Vorsilben mit Zehnerpotenz und Namen. */
    data class Prefix(val symbol: String, val exponent: Int, val name: String)

    val PREFIXES = listOf(
        Prefix("T", 12, "Tera"), Prefix("G", 9, "Giga"), Prefix("M", 6, "Mega"), Prefix("k", 3, "Kilo"), Prefix("h", 2, "Hekto"),
        Prefix("d", -1, "Dezi"), Prefix("c", -2, "Zenti"), Prefix("m", -3, "Milli"), Prefix(MU, -6, "Mikro"), Prefix("n", -9, "Nano"), Prefix("p", -12, "Piko"),
    )

    /** Grundeinheiten und die Vorsilben, mit denen man sie wirklich antrifft. */
    private val prefixUse = listOf(
        "m" to "k,,d,c,m,$MU,n,p",
        "g" to "k,,m,$MU,n,p",
        "l" to "h,,d,c,m,$MU",
        "s" to ",m,$MU,n",
        "W" to "G,M,k,,m",
        "Hz" to "G,M,k,",
        "J" to "M,k,",
        "Pa" to "M,k,h,",
        "mol" to ",m,$MU,n,p",
        "A" to ",m,$MU,n",
        "V" to "k,,m,$MU",
    ).map { (base, list) -> base to list.split(',') }

    // --- Zahlen und Formatierung ---------------------------------------------------------------

    private fun bd(x: Long) = BigDecimal.valueOf(x)
    private fun pow10(e: Int): BigDecimal = if (e >= 0) BigDecimal.ONE.scaleByPowerOfTen(e).setScale(0) else BigDecimal.ONE.scaleByPowerOfTen(e)

    private fun plain(x: BigDecimal): String = x.stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }.toPlainString()

    /** Passt die Zahl in das Eingabefeld (höchstens 12 Zeichen, nicht zu viele Ziffern)? */
    private fun typeable(x: BigDecimal, max: Int = SessionLogic.MAX_INPUT - 1): Boolean = plain(x).length <= max && x.signum() > 0

    private val superscript = mapOf('0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '-' to '⁻')

    /** „10⁻⁶“. */
    fun power(e: Int): String = "10" + e.toString().map { superscript.getValue(it) }.joinToString("")

    /** „4,2 · 10⁻⁴“. */
    fun sci(mantissa: BigDecimal, e: Int): String = "${d(mantissa)} $DOT ${power(e)}"

    /** Wie viele Stellen das Komma bei einem Faktor 10ⁿ wandert (null, wenn kein Zehnerfaktor). */
    private fun places(factor: BigDecimal): Int? {
        val s = factor.stripTrailingZeros()
        if (s.unscaledValue() != java.math.BigInteger.ONE) return null
        return -s.scale()
    }

    private fun placesText(k: Int): String = if (k == 1) "eine Stelle" else "$k Stellen"

    /** Eine „schöne“ Zahl wie 7, 35, 250, 4,2 oder 1,25. */
    private fun nice(r: Random): BigDecimal = when (r.nextInt(6)) {
        0 -> bd(r.between(1, 9).toLong())
        1 -> bd(r.between(11, 99).toLong())
        2 -> bd(r.pick(listOf(120L, 125L, 150L, 175L, 200L, 225L, 250L, 300L, 375L, 400L, 450L, 500L, 600L, 750L, 800L, 900L)))
        3 -> dec(r.between(11, 99).toLong(), 1)
        4 -> dec(r.between(101, 999).toLong(), 2)
        else -> bd(r.between(101, 999).toLong())
    }.stripTrailingZeros()

    private fun decimalProblem(skillId: String, key: String, prompt: Prompt, value: BigDecimal, steps: List<Step>, method: String, mistakes: List<Mistake> = emptyList()) =
        Problem(
            skillId = skillId,
            key = key,
            prompt = prompt,
            expected = Expected.Exact(value.stripTrailingZeros()),
            input = InputKind.DECIMAL,
            steps = steps,
            method = method,
            mistakes = mistakes.filter { m -> (m.answer as? Expected.Exact)?.value?.compareTo(value) != 0 && (m.answer as? Expected.Exact)?.value?.signum() != 0 }
                .distinctBy { (it.answer as Expected.Exact).value.stripTrailingZeros() },
        )

    /** Typischer Fehler; unendliche Kommazahlen (z. B. ÷ 3,6) auf zwei Stellen gerundet, wie man sie eintippen würde. */
    private fun mistake(value: BigDecimal, why: String): Mistake {
        val v = value.stripTrailingZeros().let { if (it.scale() > 9) it.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros() else it }
        return Mistake(Expected.Exact(v), why)
    }

    /** Ganze Zahl mit echtem Minuszeichen, negative in Klammern: „(−3)“. */
    private fun signed(x: Int): String = if (x < 0) "($MINUS${-x})" else "$x"

    private fun s(label: String, value: BigDecimal, hint: String? = null) = Step(label, Expected.Exact(value.stripTrailingZeros()), InputKind.DECIMAL, hint)

    // --- Umrechnen innerhalb einer Größe -------------------------------------------------------

    /**
     * Ein Rechenschritt „Zahl × Faktor“ bzw. „Zahl ÷ Faktor“ mit Erklärung. [ratio] ist der Wert von
     * 1 Quell-Einheit in Ziel-Einheiten.
     */
    private fun factorStep(v: BigDecimal, ratio: BigDecimal, from: String, to: String): Step {
        val result = v.multiply(ratio)
        if (ratio.compareTo(BigDecimal.ONE) == 0) {
            return s("${d(v)} $X 1", result, "1 $from = 1 $to: Beide Einheiten sind gleich groß – die Zahl bleibt.")
        }
        val inverse = try {
            BigDecimal.ONE.divide(ratio).stripTrailingZeros()
        } catch (e: ArithmeticException) {
            null
        }
        if (ratio < BigDecimal.ONE && inverse == null) {
            // Kein glatter Kehrwert (z. B. Wh → MJ): mit dem Kommafaktor malnehmen.
            return s("${d(v)} $X ${d(ratio)}", result, "1 $from = ${d(ratio)} $to – also mal ${d(ratio)}.")
        }
        return if (ratio >= BigDecimal.ONE) {
            val k = places(ratio)
            val move = k?.let { " – das Komma rückt ${placesText(it)} nach rechts" } ?: ""
            s("${d(v)} $X ${d(ratio)}", result, "1 $from = ${d(ratio)} $to. In die kleinere Einheit wird die Zahl größer: mal ${d(ratio)}$move.")
        } else {
            val q = inverse!!
            val k = places(q)
            val move = k?.let { " – das Komma rückt ${placesText(it)} nach links" } ?: ""
            s("${d(v)} $DIV ${d(q)}", result, "1 $to = ${d(q)} $from. In die größere Einheit wird die Zahl kleiner: geteilt durch ${d(q)}$move.")
        }
    }

    /** Faktor von [from] nach [to] (1 from = ratio to), exakt. */
    private fun ratio(from: Unit, to: Unit): BigDecimal? = try {
        from.factor.divide(to.factor).stripTrailingZeros()
    } catch (e: ArithmeticException) {
        null
    }

    /** Passender Wert für die Umrechnung oder null, wenn keiner in das Eingabefeld passt. */
    private fun valueFor(ratio: BigDecimal, r: Random, small: Boolean = false): BigDecimal? {
        repeat(40) {
            val e = if (small) 0 else r.between(-2, 3)
            val v = nice(r).scaleByPowerOfTen(e).stripTrailingZeros()
            val res = v.multiply(ratio).stripTrailingZeros()
            if (typeable(v, 9) && typeable(res) && res.scale() <= 9 && v >= BigDecimal("0.001")) return v
        }
        return null
    }

    /**
     * Umrechnung in einer Größe, z. B. „3,5 km = ? m“. [maxGap]: wie weit die Einheiten in der Liste
     * höchstens auseinanderliegen.
     */
    fun convert(skillId: String, dim: Dim, r: Random, maxGap: Int = 3, value: BigDecimal? = null, units: Pair<Unit, Unit>? = null): Problem {
        repeat(60) {
            val (from, to) = units ?: run {
                val i = r.nextInt(dim.units.size)
                val j = (i + r.pick((-maxGap..maxGap).filter { it != 0 })).coerceIn(0, dim.units.lastIndex)
                dim.units[i] to dim.units[j]
            }
            if (from.symbol == to.symbol) return@repeat
            val ratio = ratio(from, to) ?: return@repeat
            if (ratio.compareTo(BigDecimal.ONE) == 0 && units == null && r.nextInt(3) > 0) return@repeat
            val v = value ?: valueFor(ratio, r) ?: return@repeat
            val res = v.multiply(ratio).stripTrailingZeros()
            if (!typeable(res)) return@repeat
            val steps = mutableListOf<Step>()
            val fromLin = from.linear?.let { lengthInMeters[it] }
            val toLin = to.linear?.let { lengthInMeters[it] }
            if (dim.power > 1 && fromLin != null && toLin != null && ratio.compareTo(BigDecimal.ONE) != 0) {
                // Flächen und Volumen: Längenfaktor hoch 2 bzw. 3.
                val lin = fromLin.divide(toLin).stripTrailingZeros()
                val big = lin >= BigDecimal.ONE
                val l = if (big) lin else BigDecimal.ONE.divide(lin).stripTrailingZeros()
                val f = if (big) ratio else BigDecimal.ONE.divide(ratio).stripTrailingZeros()
                val (a, b) = if (big) from.linear!! to to.linear!! else to.linear!! to from.linear!!
                val what = if (dim.power == 2) "der Fläche zählt Länge mal Breite" else "dem Volumen zählt Länge mal Breite mal Höhe"
                steps += s(List(dim.power) { d(l) }.joinToString(" $X "), f, "1 $a = ${d(l)} $b. Bei $what – der Faktor kommt ${if (dim.power == 2) "zweimal" else "dreimal"} vor.")
            }
            steps += factorStep(v, ratio, from.symbol, to.symbol)
            val mistakes = mutableListOf<Mistake>()
            val backwards = if (ratio >= BigDecimal.ONE) v.divide(ratio, MathContext.DECIMAL128) else v.multiply(BigDecimal.ONE.divide(ratio, MathContext.DECIMAL128))
            if (ratio.compareTo(BigDecimal.ONE) != 0) {
                mistakes += mistake(
                    backwards,
                    if (ratio >= BigDecimal.ONE) "Falsche Richtung: ${from.symbol} ist die größere Einheit – in ${to.symbol} muss die Zahl größer werden, also mal ${d(ratio)}."
                    else "Falsche Richtung: ${from.symbol} ist die kleinere Einheit – in ${to.symbol} muss die Zahl kleiner werden, also geteilt.",
                )
            }
            if (dim.power > 1 && fromLin != null && toLin != null) {
                val lin = fromLin.divide(toLin).stripTrailingZeros()
                if (lin.compareTo(ratio) != 0) {
                    mistakes += mistake(
                        v.multiply(lin),
                        "Nur mit dem Längenfaktor ${d(if (lin >= BigDecimal.ONE) lin else BigDecimal.ONE.divide(lin))} gerechnet – bei ${if (dim.power == 2) "Flächen zählt er zweimal" else "Volumen zählt er dreimal"}.",
                    )
                }
            }
            if (dim === TIME && from.factor >= BigDecimal(60) && to.factor >= BigDecimal.ONE) {
                // Typischer Zeitfehler: mit 100 statt 60 rechnen.
                val decimalRatio = when {
                    ratio.compareTo(BigDecimal(60)) == 0 -> BigDecimal(100)
                    ratio.compareTo(BigDecimal(3600)) == 0 -> BigDecimal(10000)
                    else -> null
                }
                decimalRatio?.let { mistakes += mistake(v.multiply(it), "Eine Stunde hat 60 Minuten und eine Minute 60 Sekunden – nicht 100.") }
            }
            return decimalProblem(
                skillId,
                "${dim.id}:${plain(v)}${from.symbol}>${to.symbol}",
                Prompt("${d(v)} ${from.symbol} = ? ${to.symbol}", caption = dim.caption),
                res,
                steps,
                if (dim.power > 1) "Längenfaktor hoch ${dim.power}" else "Faktor zwischen den Einheiten",
                mistakes,
            )
        }
        // Rückfall: die einfachste Umrechnung der Größe.
        return convert(skillId, dim, Random(r.nextLong()), maxGap = 1)
    }

    /** Zwei Angaben in verschiedenen Einheiten zusammenzählen, z. B. „1,2 km + 350 m = ? m“. */
    fun sum(skillId: String, dim: Dim, r: Random): Problem {
        repeat(60) {
            val i = r.nextInt(dim.units.size - 1)
            val big = dim.units[i]
            val small = dim.units[i + 1 + r.nextInt(minOf(2, dim.units.size - i - 1))]
            val ratio = ratio(big, small) ?: return@repeat
            if (ratio <= BigDecimal.ONE || ratio > BigDecimal(10000)) return@repeat
            val a = dec(r.between(11, 99).toLong(), 1).stripTrailingZeros()
            val b = bd(r.between(1, maxOf(2, ratio.toInt() - 1).coerceAtMost(999)).toLong())
            val aConv = a.multiply(ratio).stripTrailingZeros()
            val res = aConv + b
            if (!typeable(res) || !typeable(aConv)) return@repeat
            return decimalProblem(
                skillId,
                "${dim.id}:sum:${plain(a)}${big.symbol}+${plain(b)}${small.symbol}",
                Prompt("${d(a)} ${big.symbol} + ${d(b)} ${small.symbol} = ? ${small.symbol}", caption = dim.caption),
                res,
                listOf(
                    factorStep(a, ratio, big.symbol, small.symbol),
                    s("${d(aConv)} + ${d(b)}", res, "Erst wenn beide Angaben dieselbe Einheit haben, wird zusammengezählt."),
                ),
                "Erst angleichen, dann rechnen",
                listOf(mistake(a + b, "Zahlen mit verschiedenen Einheiten einfach zusammengezählt – erst ${big.symbol} in ${small.symbol} umrechnen.")),
            )
        }
        return convert(skillId, dim, r)
    }

    // --- Vorsilben und Zehnerpotenzen ----------------------------------------------------------

    private fun choiceProblem(skillId: String, key: String, prompt: Prompt, correct: String, wrong: List<String>, label: String, hint: String, method: String, r: Random, whyFor: Map<String, String> = emptyMap()): Problem {
        val options = (listOf(correct) + wrong.filter { it != correct }.distinct().take(4)).shuffled(r)
        val index = options.indexOf(correct)
        return Problem(
            skillId = skillId,
            key = key,
            prompt = prompt,
            expected = Expected.Choice(index),
            input = InputKind.CHOICE,
            steps = listOf(Step(label, Expected.Choice(index), InputKind.CHOICE, hint)),
            method = method,
            choices = options,
            mistakes = options.mapIndexedNotNull { i, o -> whyFor[o]?.takeIf { i != index }?.let { Mistake(Expected.Choice(i), it) } },
        )
    }

    private fun prefixLabel(p: Prefix) = "${p.name} (${p.symbol})"

    /** Vorsilbe ↔ Zehnerpotenz als Auswahl. */
    fun prefixChoice(skillId: String, r: Random): Problem {
        val p = r.pick(PREFIXES)
        val others = PREFIXES.filter { it != p }.shuffled(r)
        return if (r.nextBoolean()) {
            // Welche Zehnerpotenz?
            val near = listOf(-p.exponent, p.exponent + 3, p.exponent - 3, p.exponent * 2).filter { it != p.exponent && it in -15..15 && it != 0 }
            val wrong = (near + others.map { it.exponent }).distinct().filter { it != p.exponent }.take(4).map { power(it) }
            choiceProblem(
                skillId, "prefix:${p.name}>exp", Prompt("${prefixLabel(p)} = ?", caption = "Welche Zehnerpotenz?"),
                power(p.exponent), wrong, "${p.name} als Zehnerpotenz",
                "${p.name} steht für ${power(p.exponent)} = ${d(pow10(p.exponent))}.", "Vorsilben-Tabelle", r,
                mapOf(power(-p.exponent) to "Vorzeichen vertauscht: Vorsilben für große Zahlen (k, M, G, T) haben positive, für kleine (m, µ, n, p) negative Exponenten."),
            )
        } else {
            val wrong = others.sortedBy { kotlin.math.abs(it.exponent - p.exponent) }.take(4).map { prefixLabel(it) }
            choiceProblem(
                skillId, "prefix:exp>${p.name}", Prompt("${power(p.exponent)} = ?", caption = "Welche Vorsilbe?"),
                prefixLabel(p), wrong, "Vorsilbe zu ${power(p.exponent)}",
                "${power(p.exponent)} heißt ${p.name} (${p.symbol}).", "Vorsilben-Tabelle", r,
            )
        }
    }

    /** „1 kW = ? mW“ oder „2,5 GHz = ? MHz“: Vorsilben über den Exponentenabstand. */
    fun prefixConvert(skillId: String, r: Random): Problem {
        repeat(80) {
            val (base, allowed) = r.pick(prefixUse)
            val a = r.pick(allowed)
            val b = r.pick(allowed)
            if (a == b) return@repeat
            val ea = PREFIXES.firstOrNull { it.symbol == a }?.exponent ?: 0
            val eb = PREFIXES.firstOrNull { it.symbol == b }?.exponent ?: 0
            val diff = ea - eb
            if (kotlin.math.abs(diff) > 9) return@repeat
            val v = if (r.nextInt(3) == 0) BigDecimal.ONE else nice(r)
            val res = v.multiply(pow10(diff)).stripTrailingZeros()
            if (!typeable(res) || !typeable(v, 9)) return@repeat
            val from = a + base
            val to = b + base
            // Abstand der Exponenten ohne negative Zwischenwerte.
            val distLabel = when {
                ea >= 0 && eb >= 0 -> "${maxOf(ea, eb)} $MINUS ${minOf(ea, eb)}"
                ea <= 0 && eb <= 0 -> "${maxOf(-ea, -eb)} $MINUS ${minOf(-ea, -eb)}"
                else -> "${maxOf(ea, eb)} + ${-minOf(ea, eb)}"
            }
            fun name(sym: String) = PREFIXES.firstOrNull { it.symbol == sym }?.let { "${it.name} = ${power(it.exponent)}" } ?: "ohne Vorsilbe = ${power(0)}"
            val dist = kotlin.math.abs(diff)
            val factor = pow10(dist)
            val last = if (diff > 0) s("${d(v)} $X ${d(factor)}", res, "$from ist größer als $to: mal ${d(factor)}, das Komma rückt ${placesText(dist)} nach rechts.")
            else s("${d(v)} $DIV ${d(factor)}", res, "$from ist kleiner als $to: geteilt durch ${d(factor)}, das Komma rückt ${placesText(dist)} nach links.")
            val wrongDir = if (diff > 0) v.divide(factor) else v.multiply(factor)
            return decimalProblem(
                skillId,
                "prefix:${plain(v)}$from>$to",
                Prompt("${d(v)} $from = ? $to", caption = "Vorsilben umrechnen"),
                res,
                listOf(
                    s(distLabel, bd(dist.toLong()), "${name(a)}, ${name(b)}: Die Exponenten liegen $dist auseinander."),
                    last,
                ),
                "Exponenten-Abstand der Vorsilben",
                listOf(mistake(wrongDir, "Falsche Richtung: Die Zahl wird größer, wenn man in die kleinere Einheit umrechnet.")),
            )
        }
        return prefixChoice(skillId, r)
    }

    // --- Zeit ----------------------------------------------------------------------------------

    /** Zeitangaben: Dezimalstunden, Stunden und Minuten, Sekunden, Tage, Zeitspannen. */
    fun time(skillId: String, r: Random): Problem {
        val caption = TIME.caption
        return when (r.nextInt(8)) {
            0 -> {
                // Dezimalstunden in Minuten: 2,25 h = 135 min
                val den = r.pick(listOf(2L, 4L, 5L, 10L))
                val hours = bd(r.betweenL(den + 1, den * 8)).divide(bd(den)).stripTrailingZeros()
                val min = hours.multiply(bd(60)).stripTrailingZeros()
                decimalProblem(
                    skillId, "time:${plain(hours)}h>min", Prompt("${d(hours)} h = ? min", caption = caption), min,
                    listOf(s("${d(hours)} $X 60", min, "1 h = 60 min – die Nachkommastellen sind Teile einer Stunde, keine Minuten.")),
                    "Stunden mal 60",
                    listOf(mistake(hours.multiply(bd(100)), "Mit 100 statt 60 gerechnet: ${d(hours)} h sind nicht ${d(hours.multiply(bd(100)))} min.")),
                )
            }
            1 -> {
                // Minuten in Dezimalstunden: 135 min = 2,25 h
                val hours = dec(r.between(3, 40).toLong() * r.pick(listOf(25L, 50L, 20L, 10L)), 2).stripTrailingZeros()
                val min = hours.multiply(bd(60)).stripTrailingZeros()
                val wrong = min.divide(bd(100))
                decimalProblem(
                    skillId, "time:${plain(min)}min>h", Prompt("${d(min)} min = ? h", caption = caption), hours,
                    listOf(s("${d(min)} $DIV 60", hours, "60 min = 1 h, also durch 60 teilen.")),
                    "Minuten durch 60",
                    listOf(mistake(wrong, "Durch 100 statt durch 60 geteilt – eine Stunde hat 60 Minuten.")),
                )
            }
            2 -> {
                // Stunden und Minuten in Minuten: 2 h 15 min = 135 min
                val h = r.between(1, 9).toLong()
                val m = r.between(1, 59).toLong()
                decimalProblem(
                    skillId, "time:${h}h${m}min", Prompt("$h h $m min = ? min", caption = caption), bd(h * 60 + m),
                    listOf(s("$h $X 60", bd(h * 60), "Die Stunden in Minuten umrechnen."), s("${n(h * 60)} + $m", bd(h * 60 + m), "Die Minuten dazuzählen.")),
                    "Stunden mal 60, Minuten dazu",
                    listOf(mistake(bd(h * 100 + m), "Mit 100 Minuten pro Stunde gerechnet.")),
                )
            }
            3 -> {
                // Sekunden in Stunden oder Stunden in Sekunden (zwei Schritte über Minuten)
                val hours = dec(r.between(2, 16).toLong() * 25, 2).stripTrailingZeros()
                val min = hours.multiply(bd(60)).stripTrailingZeros()
                val sec = min.multiply(bd(60)).stripTrailingZeros()
                if (r.nextBoolean()) {
                    decimalProblem(
                        skillId, "time:${plain(hours)}h>s", Prompt("${d(hours)} h = ? s", caption = caption), sec,
                        listOf(s("${d(hours)} $X 60", min, "Erst in Minuten …"), s("${d(min)} $X 60", sec, "… dann in Sekunden: 1 h = 3.600 s.")),
                        "Zweimal mal 60",
                        listOf(mistake(min, "Nur einmal mit 60 malgenommen – das sind erst die Minuten.")),
                    )
                } else {
                    decimalProblem(
                        skillId, "time:${plain(sec)}s>h", Prompt("${d(sec)} s = ? h", caption = caption), hours,
                        listOf(s("${d(sec)} $DIV 60", min, "Erst in Minuten …"), s("${d(min)} $DIV 60", hours, "… dann in Stunden: 3.600 s = 1 h.")),
                        "Zweimal durch 60",
                        listOf(mistake(min, "Nur einmal durch 60 geteilt – das sind erst die Minuten.")),
                    )
                }
            }
            4 -> {
                // Tage und Stunden
                val days = dec(r.between(2, 28).toLong() * 25, 2).stripTrailingZeros()
                val hours = days.multiply(bd(24)).stripTrailingZeros()
                if (hours.scale() > 0 || r.nextBoolean()) {
                    val d0 = bd(r.between(1, 14).toLong())
                    decimalProblem(
                        skillId, "time:${plain(d0)}d>h", Prompt("${d(d0)} d = ? h", caption = caption), d0.multiply(bd(24)),
                        listOf(s("${d(d0)} $X 24", d0.multiply(bd(24)), "1 Tag (d) = 24 h.")), "Tage mal 24",
                    )
                } else {
                    decimalProblem(
                        skillId, "time:${plain(hours)}h>d", Prompt("${d(hours)} h = ? d", caption = caption), days,
                        listOf(s("${d(hours)} $DIV 24", days, "24 h = 1 Tag (d).")), "Stunden durch 24",
                    )
                }
            }
            5 -> {
                // Zeitspanne zwischen zwei Uhrzeiten in Minuten
                val h1 = r.between(6, 17)
                val m1 = r.between(1, 11) * 5
                val h2 = h1 + r.between(1, 4)
                val m2 = r.between(0, 11) * 5
                val toFull = 60 - m1
                val full = h2 - (h1 + 1)
                val total = toFull + full * 60 + m2
                val steps = mutableListOf(s("60 $MINUS $m1", bd(toFull.toLong()), "Bis zur vollen Stunde ${h1 + 1}:00."))
                if (full > 0) steps += s("$full $X 60", bd(full * 60L), "Volle Stunden von ${h1 + 1}:00 bis $h2:00.")
                steps += s(listOfNotNull("$toFull", if (full > 0) n(full * 60L) else null, "$m2").joinToString(" + "), bd(total.toLong()), "Alles zusammenzählen.")
                decimalProblem(
                    skillId, "time:span:$h1:$m1-$h2:$m2", Prompt("$h1:${"%02d".format(m1)} bis $h2:${"%02d".format(m2)} = ? min", caption = "Zeitspanne in Minuten"),
                    bd(total.toLong()), steps, "Bis zur vollen Stunde, dann weiter",
                    listOf(mistake(bd(((h2 * 100 + m2) - (h1 * 100 + m1)).toLong()), "Uhrzeiten wie Dezimalzahlen abgezogen – eine Stunde hat aber nur 60 Minuten.")),
                )
            }
            6 -> sum(skillId, TIME, r)
            else -> convert(skillId, TIME, r, maxGap = 2)
        }
    }

    // --- Geschwindigkeit -----------------------------------------------------------------------

    fun speed(skillId: String, r: Random): Problem {
        val caption = "Geschwindigkeit umrechnen"
        return when (r.nextInt(4)) {
            0 -> {
                val ms = dec(r.between(2, 120).toLong() * 5, 1).stripTrailingZeros()
                val kmh = ms.multiply(BigDecimal("3.6")).stripTrailingZeros()
                decimalProblem(
                    skillId, "speed:${plain(kmh)}kmh>ms", Prompt("${d(kmh)} km/h = ? m/s", caption = caption), ms,
                    listOf(s("${d(kmh)} $DIV 3,6", ms, "1 m/s = 3,6 km/h (3.600 s pro Stunde, 1.000 m pro km). Also durch 3,6.")),
                    "Durch 3,6",
                    listOf(mistake(kmh.multiply(BigDecimal("3.6")), "Falsche Richtung: m/s sind die größere Einheit, die Zahl muss kleiner werden.")),
                )
            }
            1 -> {
                val ms = dec(r.between(2, 160).toLong() * 5, 1).stripTrailingZeros()
                val kmh = ms.multiply(BigDecimal("3.6")).stripTrailingZeros()
                decimalProblem(
                    skillId, "speed:${plain(ms)}ms>kmh", Prompt("${d(ms)} m/s = ? km/h", caption = caption), kmh,
                    listOf(s("${d(ms)} $X 3,6", kmh, "1 m/s = 3,6 km/h – also mal 3,6.")),
                    "Mal 3,6",
                    listOf(mistake(ms.divide(BigDecimal("3.6"), MathContext.DECIMAL128), "Falsche Richtung: In km/h wird die Zahl größer.")),
                )
            }
            2 -> {
                // m/min in km/h
                val kmh = bd(r.between(3, 45).toLong() * 6)
                val mmin = kmh.multiply(bd(1000)).divide(bd(60)).stripTrailingZeros()
                val perHour = mmin.multiply(bd(60))
                decimalProblem(
                    skillId, "speed:${plain(mmin)}mmin>kmh", Prompt("${d(mmin)} m/min = ? km/h", caption = caption), kmh,
                    listOf(s("${d(mmin)} $X 60", perHour, "Meter pro Stunde: mal 60."), s("${d(perHour)} $DIV 1.000", kmh, "In Kilometer: durch 1.000.")),
                    "Mal 60, durch 1.000",
                )
            }
            else -> {
                // km/h in km/min bzw. Weg in einer Anzahl Minuten
                val kmh = bd(r.pick(listOf(30L, 36L, 42L, 45L, 48L, 54L, 60L, 66L, 72L, 75L, 78L, 84L, 90L, 96L, 108L, 120L, 150L)))
                val min = r.pick(listOf(10L, 15L, 20L, 25L, 30L, 40L, 45L, 50L))
                val km = kmh.multiply(bd(min)).divide(bd(60), MathContext.DECIMAL128).stripTrailingZeros()
                val perMin = kmh.divide(bd(60), MathContext.DECIMAL128).stripTrailingZeros()
                if (km.scale() > 2 || perMin.scale() > 3) return convert(skillId, LENGTH, r)
                decimalProblem(
                    skillId, "speed:${plain(kmh)}kmh*${min}min",
                    Prompt("${d(kmh)} km/h für $min min = ? km", caption = "Weg aus Tempo und Minuten"), km,
                    listOf(s("${d(kmh)} $DIV 60", perMin, "Kilometer pro Minute."), s("${d(perMin)} $X $min", km, "Mal die Minuten.")),
                    "Tempo pro Minute",
                    listOf(mistake(kmh.multiply(bd(min)), "Minuten nicht in Stunden umgerechnet: ${d(kmh)} km/h × $min wäre $min Stunden lang.")),
                )
            }
        }
    }

    // --- Konzentration, Dichte, Tropfen --------------------------------------------------------

    fun concentration(skillId: String, r: Random): Problem = when (r.nextInt(8)) {
        0, 1 -> convert(skillId, CONC, r, maxGap = 3)
        2 -> convert(skillId, MOLAR, r, maxGap = 2)
        3 -> {
            // Prozent (g pro 100 ml) in mg/ml bzw. zurück
            val pct = r.pick(listOf("0.1", "0.2", "0.5", "0.9", "1", "1.5", "2", "2.5", "3", "4", "5", "8", "10", "20", "40")).let { BigDecimal(it) }
            val mgml = pct.multiply(bd(10)).stripTrailingZeros()
            if (r.nextBoolean()) {
                decimalProblem(
                    skillId, "conc:${plain(pct)}%>mgml", Prompt("${d(pct)} % = ? mg/ml", caption = "Prozent: Gramm pro 100 ml"), mgml,
                    listOf(s("${d(pct)} $X 10", mgml, "1 % = 1 g in 100 ml = 1.000 mg in 100 ml = 10 mg/ml.")),
                    "1 % = 10 mg/ml",
                    listOf(mistake(pct.multiply(bd(1000)), "1 % heißt 1 g pro 100 ml – nicht pro ml.")),
                )
            } else {
                decimalProblem(
                    skillId, "conc:${plain(mgml)}gl>%", Prompt("${d(mgml)} g/l = ? %", caption = "Prozent: Gramm pro 100 ml"), pct,
                    listOf(s("${d(mgml)} $DIV 10", pct, "1 % = 1 g pro 100 ml = 10 g pro Liter.")),
                    "10 g/l = 1 %",
                )
            }
        }
        4 -> {
            // Wirkstoffmenge in einer Lösung: 250 ml einer 2-%-Lösung
            val ml = r.pick(listOf(50L, 100L, 150L, 200L, 250L, 300L, 400L, 500L, 750L, 1000L))
            val pct = r.pick(listOf("0.5", "0.9", "1", "2", "2.5", "4", "5", "10", "20")).let { BigDecimal(it) }
            val parts = bd(ml).divide(bd(100)).stripTrailingZeros()
            val g = pct.multiply(parts).stripTrailingZeros()
            decimalProblem(
                skillId, "conc:${ml}ml*${plain(pct)}%", Prompt("${n(ml)} ml einer ${d(pct)}-%-Lösung: ? g Wirkstoff", caption = "Wirkstoff in Gramm"), g,
                listOf(s("${n(ml)} $DIV 100", parts, "So oft passen 100 ml hinein."), s("${d(pct)} $X ${d(parts)}", g, "In je 100 ml sind ${d(pct)} g.")),
                "Prozent = Gramm pro 100 ml",
                listOf(mistake(pct.multiply(bd(ml)), "Prozent als Gramm pro ml gerechnet – gemeint ist Gramm pro 100 ml.")),
            )
        }
        5 -> {
            // Tropfen: 20 Tropfen = 1 ml
            val ml = dec(r.between(1, 40).toLong() * 25, 2).stripTrailingZeros()
            val drops = ml.multiply(bd(20)).stripTrailingZeros()
            if (r.nextBoolean()) {
                decimalProblem(
                    skillId, "drops:${plain(drops)}>ml", Prompt("${d(drops)} Tropfen = ? ml", caption = "20 Tropfen = 1 ml"), ml,
                    listOf(s("${d(drops)} $DIV 20", ml, "20 Tropfen ergeben 1 ml.")), "Durch 20",
                )
            } else {
                decimalProblem(
                    skillId, "drops:${plain(ml)}ml>drops", Prompt("${d(ml)} ml = ? Tropfen", caption = "20 Tropfen = 1 ml"), drops,
                    listOf(s("${d(ml)} $X 20", drops, "Jeder Milliliter sind 20 Tropfen.")), "Mal 20",
                )
            }
        }
        6 -> {
            // Dichte: Masse = Dichte × Volumen bzw. Volumen = Masse ÷ Dichte
            val rho = BigDecimal(r.pick(listOf("0.8", "0.9", "1", "1.1", "1.2", "1.25", "1.5", "2", "2.5")))
            val ml = bd(r.between(2, 40).toLong() * 10)
            val g = rho.multiply(ml).stripTrailingZeros()
            if (r.nextBoolean()) {
                decimalProblem(
                    skillId, "density:${plain(rho)}*${plain(ml)}", Prompt("${d(ml)} ml bei ${d(rho)} g/ml = ? g", caption = "Dichte: Masse = Dichte × Volumen"), g,
                    listOf(s("${d(ml)} $X ${d(rho)}", g, "Jeder Milliliter wiegt ${d(rho)} g.")), "Masse = Dichte × Volumen",
                )
            } else {
                decimalProblem(
                    skillId, "density:${plain(g)}/${plain(rho)}", Prompt("${d(g)} g bei ${d(rho)} g/ml = ? ml", caption = "Dichte: Volumen = Masse ÷ Dichte"), ml,
                    listOf(s("${d(g)} $DIV ${d(rho)}", ml, "So viele Milliliter braucht es für ${d(g)} g.")), "Volumen = Masse ÷ Dichte",
                    listOf(mistake(g.multiply(rho), "Mit der Dichte malgenommen statt geteilt.")),
                )
            }
        }
        else -> {
            // Konzentration aus Masse und Volumen in Prozent: 5 g in 250 ml = 2 %
            val ml = r.pick(listOf(50L, 100L, 200L, 250L, 400L, 500L, 1000L))
            val pct = r.pick(listOf("0.5", "1", "2", "4", "5", "10", "20")).let { BigDecimal(it) }
            val g = pct.multiply(bd(ml)).divide(bd(100)).stripTrailingZeros()
            val per = g.divide(bd(ml), MathContext.DECIMAL128).stripTrailingZeros()
            decimalProblem(
                skillId, "conc:${plain(g)}g/${ml}ml", Prompt("${d(g)} g in ${n(ml)} ml = ? %", caption = "Konzentration in Prozent"), pct,
                listOf(s("${d(g)} $DIV ${n(ml)}", per, "Gramm pro Milliliter."), s("${d(per)} $X 100", pct, "Prozent = Gramm pro 100 ml.")),
                "Gramm pro 100 ml",
            )
        }
    }

    // --- Physik: Druck, Energie, Leistung, Strom, Spannung, Frequenz, Stoffmenge, Temperatur ---

    fun physics(skillId: String, r: Random): Problem = when (r.nextInt(10)) {
        0 -> convert(skillId, PRESSURE, r, maxGap = 3)
        1 -> convert(skillId, ENERGY, r, maxGap = 2)
        2 -> convert(skillId, POWER, r, maxGap = 2)
        3 -> convert(skillId, r.pick(listOf(CURRENT, VOLTAGE)), r, maxGap = 2)
        4 -> convert(skillId, r.pick(listOf(FREQUENCY, AMOUNT)), r, maxGap = 2)
        5 -> {
            // Temperatur: °C ↔ K
            if (r.nextBoolean()) {
                val c = bd(r.between(0, 120).toLong())
                val k = c + BigDecimal("273.15")
                decimalProblem(
                    skillId, "temp:${plain(c)}C>K", Prompt("${d(c)} °C = ? K", caption = "Temperatur umrechnen"), k,
                    listOf(s("${d(c)} + 273,15", k, "Kelvin = Grad Celsius + 273,15.")), "Plus 273,15",
                    listOf(mistake(c + bd(273), "Mit 273 statt 273,15 gerechnet.")),
                )
            } else {
                val k = bd(r.between(274, 400).toLong())
                val c = k - BigDecimal("273.15")
                decimalProblem(
                    skillId, "temp:${plain(k)}K>C", Prompt("${d(k)} K = ? °C", caption = "Temperatur umrechnen"), c,
                    listOf(s("${d(k)} $MINUS 273,15", c, "Grad Celsius = Kelvin − 273,15.")), "Minus 273,15",
                )
            }
        }
        6 -> {
            // Kilowattstunden in Megajoule: 1 kWh = 3,6 MJ
            val kwh = dec(r.between(1, 40).toLong() * 5, 1).stripTrailingZeros()
            val mj = kwh.multiply(BigDecimal("3.6")).stripTrailingZeros()
            decimalProblem(
                skillId, "energy:${plain(kwh)}kWh>MJ", Prompt("${d(kwh)} kWh = ? MJ", caption = "Energie umrechnen"), mj,
                listOf(s("${d(kwh)} $X 3,6", mj, "1 kWh = 1.000 W × 3.600 s = 3.600.000 J = 3,6 MJ.")), "1 kWh = 3,6 MJ",
            )
        }
        7 -> {
            // Leistung mal Zeit: 2.000 W für 30 min = 1 kWh
            val w = r.pick(listOf(500L, 800L, 1000L, 1200L, 1500L, 2000L, 2400L, 3000L))
            val min = r.pick(listOf(15L, 30L, 45L, 60L, 90L, 120L))
            val kw = bd(w).divide(bd(1000)).stripTrailingZeros()
            val h = bd(min).divide(bd(60)).stripTrailingZeros()
            val kwh = kw.multiply(h).stripTrailingZeros()
            decimalProblem(
                skillId, "energy:${w}W*${min}min", Prompt("${n(w)} W für $min min = ? kWh", caption = "Energie = Leistung × Zeit"), kwh,
                listOf(
                    s("${n(w)} $DIV 1.000", kw, "Watt in Kilowatt."),
                    s("$min $DIV 60", h, "Minuten in Stunden."),
                    s("${d(kw)} $X ${d(h)}", kwh, "Kilowatt mal Stunden ergibt Kilowattstunden."),
                ),
                "Erst kW und h, dann malnehmen",
                listOf(mistake(kw.multiply(bd(min)), "Minuten nicht in Stunden umgerechnet.")),
            )
        }
        8 -> {
            // Kilokalorien in Kilojoule: 1 kcal = 4,184 kJ
            val kcal = bd(r.pick(listOf(10L, 20L, 50L, 100L, 150L, 200L, 250L, 500L)))
            val kj = kcal.multiply(BigDecimal("4.184")).stripTrailingZeros()
            decimalProblem(
                skillId, "energy:${plain(kcal)}kcal>kJ", Prompt("${d(kcal)} kcal = ? kJ", caption = "1 kcal = 4,184 kJ"), kj,
                listOf(s("${d(kcal)} $X 4,184", kj, "Eine Kilokalorie sind 4,184 Kilojoule.")), "Mal 4,184",
            )
        }
        else -> prefixConvert(skillId, r)
    }

    // --- Wissenschaftliche Schreibweise --------------------------------------------------------

    fun scientific(skillId: String, r: Random): Problem = when (r.nextInt(5)) {
        0 -> {
            // Kleine oder große Zahl → Exponent als Auswahl: 0,00042 = 4,2 · 10^?
            val mant = dec(r.between(11, 99).toLong(), 1).stripTrailingZeros()
            val e = r.pick(listOf(-7, -6, -5, -4, -3, -2, 2, 3, 4, 5, 6, 7))
            val number = mant.scaleByPowerOfTen(e).stripTrailingZeros()
            val wrong = listOf(-e, e + 1, e - 1, e + 2, e - 2).filter { it != e && it != 0 }.map { power(it) }
            val dir = if (e < 0) "nach rechts" else "nach links"
            choiceProblem(
                skillId, "sci:${plain(number)}>exp", Prompt("${d(number)} = ${d(mant)} $DOT 10^?", caption = "Welcher Exponent?"),
                power(e), wrong, "Exponent",
                "Das Komma rückt ${placesText(kotlin.math.abs(e))} $dir, bis eine Ziffer davor steht – also ${power(e)}.",
                "Kommastellen zählen", r,
                mapOf(power(-e) to "Vorzeichen vertauscht: Zahlen kleiner als 1 haben einen negativen Exponenten, große Zahlen einen positiven."),
            )
        }
        1 -> {
            // Wissenschaftliche Schreibweise → normale Zahl
            val mant = dec(r.between(11, 99).toLong(), 1).stripTrailingZeros()
            val e = r.pick(listOf(-6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6))
            val number = mant.scaleByPowerOfTen(e).stripTrailingZeros()
            val factor = pow10(kotlin.math.abs(e))
            val step = if (e > 0) s("${d(mant)} $X ${d(factor)}", number, "${power(e)}: Komma ${placesText(e)} nach rechts.")
            else s("${d(mant)} $DIV ${d(factor)}", number, "${power(e)}: Komma ${placesText(-e)} nach links.")
            decimalProblem(
                skillId, "sci:${plain(mant)}e$e", Prompt(sci(mant, e), caption = "Als normale Zahl schreiben"), number, listOf(step), "Komma verschieben",
                listOf(mistake(mant.scaleByPowerOfTen(-e), "Falsche Richtung: Ein ${if (e > 0) "positiver" else "negativer"} Exponent macht die Zahl ${if (e > 0) "größer" else "kleiner"}.")),
            )
        }
        2 -> {
            // Zehnerpotenzen malnehmen oder teilen
            val a = r.between(-9, 9)
            val b = r.between(-9, 9)
            val multiply = r.nextBoolean()
            val e = if (multiply) a + b else a - b
            if (a == 0 || b == 0) return scientific(skillId, r)
            val wrongs = mutableListOf(if (multiply) a - b else a + b, a * b, -e, e + 1, e - 1).filter { it != e }
            val op = if (multiply) DOT else DIV
            val bText = power(b)
            choiceProblem(
                skillId, "sci:pow:$a$op$b", Prompt("${power(a)} $op $bText = ?", caption = "Zehnerpotenzen"),
                power(e), wrongs.map { power(it) }, "Exponent",
                if (multiply) "Beim Malnehmen werden die Exponenten addiert: ${signed(a)} + ${signed(b)} = ${signed(e)}." else "Beim Teilen werden die Exponenten abgezogen: ${signed(a)} $MINUS ${signed(b)} = ${signed(e)}.",
                if (multiply) "Exponenten addieren" else "Exponenten abziehen", r,
                mapOf(power(a * b) to "Die Exponenten wurden malgenommen – beim ${if (multiply) "Malnehmen addiert" else "Teilen subtrahiert"} man sie."),
            )
        }
        else -> {
            // (a · 10^m) · (b · 10^n) oder (a · 10^m) ÷ (b · 10^n)
            val multiply = r.nextBoolean()
            val m = r.pick((-6..8).filter { it != 0 })
            val nn = r.pick((-6..6).filter { it != 0 })
            val (a, b) = if (multiply) r.between(2, 9).toLong() to r.between(2, 9).toLong() else {
                val q = r.between(2, 9).toLong(); val bb = r.between(2, 4).toLong(); (q * bb) to bb
            }
            if (!multiply && a > 9) return scientific(skillId, r)
            val raw = if (multiply) a * b else a / b
            var mant = bd(raw)
            var e = if (multiply) m + nn else m - nn
            if (mant >= bd(10)) {
                mant = mant.divide(bd(10)).stripTrailingZeros(); e += 1
            }
            val correct = sci(mant, e)
            val op = if (multiply) X else DIV
            val rawE = if (multiply) m + nn else m - nn
            val wrong = listOf(
                sci(mant, if (multiply) m * nn else m + nn),
                sci(mant, e + 1),
                sci(mant, e - 1),
                sci(bd(if (multiply) a + b else a - b).max(BigDecimal.ONE), rawE),
                sci(mant, -e),
            )
            val normalize = if (raw >= 10) " ${n(raw)} $DOT ${power(rawE)} = ${correct} (eine Stelle in den Exponenten)." else ""
            choiceProblem(
                skillId, "sci:$a e$m $op $b e$nn", Prompt("(${n(a)} $DOT ${power(m)}) $op (${n(b)} $DOT ${power(nn)}) = ?", caption = "Wissenschaftliche Schreibweise"),
                correct, wrong, "Ergebnis",
                "Zahlen ${if (multiply) "malnehmen" else "teilen"}: ${n(a)} $op ${n(b)} = ${n(raw)}. Exponenten ${if (multiply) "addieren" else "abziehen"}: ${power(rawE)}.$normalize",
                "Zahlen und Exponenten getrennt", r,
            )
        }
    }

    // --- Für Welt 14 und das freie Training ----------------------------------------------------

    /** Alle Aufgabenarten zufällig gemischt. */
    fun mixed(skillId: String, r: Random): Problem = when (r.nextInt(12)) {
        0 -> prefixConvert(skillId, r)
        1 -> prefixChoice(skillId, r)
        2 -> convert(skillId, LENGTH, r)
        3 -> convert(skillId, MASS, r)
        4 -> convert(skillId, VOLUME, r)
        5 -> convert(skillId, AREA, r, maxGap = 2)
        6 -> time(skillId, r)
        7 -> speed(skillId, r)
        8 -> concentration(skillId, r)
        9 -> physics(skillId, r)
        10 -> scientific(skillId, r)
        else -> sum(skillId, r.pick(listOf(LENGTH, MASS, VOLUME)), r)
    }

    /** Freies Training: Stufe 1 Grundgrößen, 2 dazu Fläche, Tempo, Konzentration, 3 alles. */
    fun free(skillId: String, level: Int, r: Random): Problem = when (level) {
        1 -> when (r.nextInt(5)) {
            0 -> convert(skillId, LENGTH, r, maxGap = 1)
            1 -> convert(skillId, MASS, r, maxGap = 1)
            2 -> convert(skillId, VOLUME, r, maxGap = 1)
            3 -> time(skillId, r)
            else -> prefixChoice(skillId, r)
        }
        2 -> when (r.nextInt(7)) {
            0 -> convert(skillId, LENGTH, r)
            1 -> convert(skillId, MASS, r)
            2 -> convert(skillId, VOLUME, r, maxGap = 2)
            3 -> convert(skillId, AREA, r, maxGap = 1)
            4 -> time(skillId, r)
            5 -> speed(skillId, r)
            else -> prefixConvert(skillId, r)
        }
        else -> mixed(skillId, r)
    }
}
