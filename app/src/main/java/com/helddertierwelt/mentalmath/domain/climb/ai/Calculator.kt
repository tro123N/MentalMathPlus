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

package com.helddertierwelt.mentalmath.domain.climb.ai

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.pow
import kotlin.math.sqrt

/** Bruch aus zwei ganzen Zahlen, immer gekürzt, Nenner positiv. */
class Rational private constructor(val num: BigInteger, val den: BigInteger) {

    val isInteger: Boolean get() = den == BigInteger.ONE
    val signum: Int get() = num.signum()

    operator fun plus(o: Rational) = of(num * o.den + o.num * den, den * o.den)
    operator fun minus(o: Rational) = of(num * o.den - o.num * den, den * o.den)
    operator fun times(o: Rational) = of(num * o.num, den * o.den)
    operator fun div(o: Rational): Rational {
        if (o.num.signum() == 0) throw CalcException("Division durch 0 ist nicht erlaubt.")
        return of(num * o.den, den * o.num)
    }

    operator fun unaryMinus() = of(num.negate(), den)

    fun pow(k: Int): Rational = when {
        k == 0 -> ONE
        k > 0 -> of(num.pow(k), den.pow(k))
        else -> ONE / of(num.pow(-k), den.pow(-k))
    }

    fun toDouble(): Double = BigDecimal(num).divide(BigDecimal(den), 20, RoundingMode.HALF_EVEN).toDouble()

    /** Genaue Dezimalzahl, wenn sie mit höchstens [maxScale] Nachkommastellen endet; sonst null. */
    fun toExactDecimal(maxScale: Int): BigDecimal? {
        var d = den
        var twos = 0
        var fives = 0
        val two = BigInteger.valueOf(2)
        val five = BigInteger.valueOf(5)
        while (d.mod(two).signum() == 0) { d /= two; twos++ }
        while (d.mod(five).signum() == 0) { d /= five; fives++ }
        if (d != BigInteger.ONE || maxOf(twos, fives) > maxScale) return null
        return BigDecimal(num).divide(BigDecimal(den), maxOf(twos, fives), RoundingMode.UNNECESSARY)
    }

    fun rounded(scale: Int): BigDecimal = BigDecimal(num).divide(BigDecimal(den), scale, RoundingMode.HALF_UP)

    override fun equals(other: Any?): Boolean = other is Rational && other.num == num && other.den == den
    override fun hashCode(): Int = num.hashCode() * 31 + den.hashCode()
    override fun toString(): String = if (isInteger) num.toString() else "$num/$den"

    companion object {
        val ZERO = Rational(BigInteger.ZERO, BigInteger.ONE)
        val ONE = Rational(BigInteger.ONE, BigInteger.ONE)

        fun of(n: BigInteger, d: BigInteger): Rational {
            if (d.signum() == 0) throw CalcException("Division durch 0 ist nicht erlaubt.")
            val g = n.gcd(d).let { if (it.signum() == 0) BigInteger.ONE else it }
            val sign = if (d.signum() < 0) BigInteger.ONE.negate() else BigInteger.ONE
            return Rational(n / g * sign, d / g * sign)
        }

        fun of(n: Long, d: Long = 1): Rational = of(BigInteger.valueOf(n), BigInteger.valueOf(d))

        fun of(value: BigDecimal): Rational =
            if (value.scale() <= 0) of(value.toBigIntegerExact(), BigInteger.ONE)
            else of(value.unscaledValue(), BigInteger.TEN.pow(value.scale()))
    }
}

/** Fehler beim Rechnen, mit einer Meldung auf Deutsch. */
class CalcException(message: String) : RuntimeException(message)

/**
 * Taschenrechner für die KI. Sprachmodelle verrechnen sich leicht – deshalb rechnet die KI jede
 * Rechnung hiermit nach. Gerechnet wird mit Brüchen, also ohne Rundungsfehler.
 *
 * Erlaubt: + − × · * / : ÷ ^ ( ), Prozent (20 % = 0,2), „von“ (20 % von 150), Wurzel (sqrt, √),
 * Kommazahlen mit Komma oder Punkt. Einheiten wie € oder ml werden überlesen.
 */
object Calculator {

    /** Ergebnis einer Rechnung. */
    sealed interface Result {
        data class Ok(
            /** Exakter Wert, null bei Wurzeln und gebrochenen Hochzahlen. */
            val exact: Rational?,
            /** Näherungswert (immer vorhanden). */
            val approx: Double,
            /** Ergebnis im deutschen Format, z. B. „120“, „0,375“ oder „1/3“. */
            val text: String,
            /** Bruch, wenn das Ergebnis keine abbrechende Dezimalzahl ist, z. B. „1/3“. */
            val fraction: String?,
            /** Gerundeter Wert (6 Nachkommastellen), wenn [text] nicht schon die genaue Dezimalzahl ist. */
            val rounded: String?,
            /** Auf 2 Nachkommastellen gerundet (für Geldbeträge), wenn es mehr Stellen gibt. */
            val rounded2: String?,
        ) : Result

        data class Error(val message: String) : Result
    }

    private const val MAX_EXPONENT = 400
    private const val MAX_BITS = 6_000
    private const val MAX_SCALE = 12

    fun evaluate(expression: String): Result = try {
        val tokens = tokenize(normalize(expression))
        if (tokens.isEmpty()) throw CalcException("Der Ausdruck ist leer.")
        val parser = Parser(tokens)
        val value = parser.parseAll()
        format(value)
    } catch (e: CalcException) {
        Result.Error(e.message ?: "Das kann ich nicht ausrechnen.")
    } catch (e: ArithmeticException) {
        Result.Error("Das kann ich nicht ausrechnen.")
    }

    // --- Wert: exakt (Bruch) oder genähert ---------------------------------------------------

    private sealed interface Value {
        data class Exact(val r: Rational) : Value
        data class Approx(val d: Double) : Value
    }

    private fun Value.toDouble(): Double = when (this) {
        is Value.Exact -> r.toDouble()
        is Value.Approx -> d
    }

    private fun check(r: Rational): Rational {
        if (r.num.bitLength() > MAX_BITS || r.den.bitLength() > MAX_BITS) throw CalcException("Die Zahl wird zu groß.")
        return r
    }

    private fun approx(d: Double): Value {
        if (d.isNaN() || d.isInfinite()) throw CalcException("Das Ergebnis ist keine gültige Zahl.")
        return Value.Approx(d)
    }

    private fun combine(a: Value, b: Value, exact: (Rational, Rational) -> Rational, inexact: (Double, Double) -> Double): Value =
        if (a is Value.Exact && b is Value.Exact) Value.Exact(check(exact(a.r, b.r)))
        else approx(inexact(a.toDouble(), b.toDouble()))

    // --- Vorbereitung des Textes ---------------------------------------------------------------

    private val wordOps = listOf(
        Regex("\\bgeteilt\\s+durch\\b") to "/",
        Regex("\\bdividiert\\s+durch\\b") to "/",
        Regex("\\bmultipliziert\\s+mit\\b") to "*",
        Regex("\\bmal\\b") to "*",
        Regex("\\btimes\\b") to "*",
        Regex("\\bvon\\b") to "*",
        Regex("\\bof\\b") to "*",
        Regex("\\bplus\\b") to "+",
        Regex("\\bminus\\b") to "-",
        Regex("\\bhoch\\b") to "^",
        Regex("\\bprozent\\b") to "%",
        Regex("\\bwurzel\\s*aus\\b") to "sqrt",
        Regex("\\bwurzel\\b") to "sqrt",
    )

    /** Einheiten mit Schrägstrich, die nicht als Division gelesen werden dürfen. */
    private val slashUnits = Regex("(km/h|m/s|mg/kg|µg/kg|mcg/kg|ml/h|ml/min|mg/ml|g/l|mg/l|tropfen/min|tr/min|€/kg|€/l|€/stück)")

    private fun normalize(raw: String): String {
        var s = raw.lowercase()
            .replace('×', '*').replace('·', '*').replace('⋅', '*').replace('∙', '*')
            .replace('÷', '/').replace(':', '/')
            .replace('−', '-').replace('–', '-').replace('—', '-')
            .replace("**", "^")
            .replace("²", "^2").replace("³", "^3")
            .replace("√", "sqrt")
            .replace('[', '(').replace(']', ')').replace('{', '(').replace('}', ')')
            .replace("=", " ")
        s = slashUnits.replace(s, " ")
        // „3 x 4“ als Malzeichen lesen.
        s = Regex("(?<=[\\d)])\\s*x\\s*(?=[\\d(])").replace(s, "*")
        for ((regex, op) in wordOps) s = regex.replace(s, " $op ")
        // Satzzeichen ohne folgende Ziffer (z. B. ein Punkt am Ende) überlesen.
        s = Regex("[.,](?!\\d)").replace(s, " ")
        // Zeichen nach einem „=“ (z. B. „3 * 4 = ?“) und Fragezeichen ignorieren.
        s = s.replace("?", " ")
        // Einheiten und andere Wörter überlesen (sqrt bleibt erhalten).
        s = Regex("[a-zäöüßµ€\\$]+").replace(s) { m -> if (m.value == "sqrt") " sqrt " else " " }
        return s
    }

    // --- Zerlegen in Bausteine -------------------------------------------------------------------

    private sealed interface Token {
        data class Number(val value: Rational) : Token
        data class Op(val c: Char) : Token
        data object Sqrt : Token
    }

    private val numberRegex = Regex("\\d+(?:[.,]\\d+)*|[.,]\\d+")

    private fun parseNumber(text: String): Rational {
        val t = if (text.startsWith(".") || text.startsWith(",")) "0$text" else text
        val dots = t.count { it == '.' }
        val commas = t.count { it == ',' }
        val plain = when {
            dots > 0 && commas > 0 -> {
                // Das letzte Trennzeichen ist das Komma der Kommazahl, die anderen trennen Tausender.
                val decimal = if (t.lastIndexOf('.') > t.lastIndexOf(',')) '.' else ','
                val thousands = if (decimal == '.') ',' else '.'
                t.replace(thousands.toString(), "").replace(decimal, '.')
            }
            dots > 1 -> t.replace(".", "")
            commas > 1 -> t.replace(",", "")
            else -> t.replace(',', '.')
        }
        return Rational.of(BigDecimal(plain))
    }

    private fun tokenize(s: String): List<Token> {
        val compact = s.replace(Regex("(?<=\\d)\\s+(?=\\d{3}(?!\\d))"), "")
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < compact.length) {
            val c = compact[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || ((c == '.' || c == ',') && i + 1 < compact.length && compact[i + 1].isDigit()) -> {
                    val m = numberRegex.find(compact, i) ?: throw CalcException("Unklare Zahl.")
                    tokens += Token.Number(parseNumber(m.value))
                    i = m.range.last + 1
                }
                compact.startsWith("sqrt", i) -> {
                    tokens += Token.Sqrt
                    i += 4
                }
                c in "+-*/^()%!" -> {
                    tokens += Token.Op(c)
                    i++
                }
                c == ',' || c == ';' -> throw CalcException("Bitte nur eine Rechnung auf einmal schicken.")
                else -> throw CalcException("Das Zeichen „$c“ kenne ich nicht.")
            }
        }
        return tokens
    }

    // --- Rechnen (rekursiver Abstieg) ------------------------------------------------------------

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun isOp(c: Char): Boolean = (peek() as? Token.Op)?.c == c

        fun parseAll(): Value {
            val v = expr()
            if (pos < tokens.size) {
                val t = tokens[pos]
                throw CalcException(if (t is Token.Op && t.c == ')') "Eine Klammer zu viel." else "Der Ausdruck ist unvollständig oder doppelt.")
            }
            return v
        }

        private fun expr(): Value {
            var v = term()
            while (true) {
                v = when {
                    isOp('+') -> { pos++; combine(v, term(), { a, b -> a + b }, { a, b -> a + b }) }
                    isOp('-') -> { pos++; combine(v, term(), { a, b -> a - b }, { a, b -> a - b }) }
                    else -> return v
                }
            }
        }

        /**
         * Weggelassener Malpunkt: „2(3 + 4)“, „(1 + 2)(3 + 4)“, „(3 + 4)2“ oder „2 sqrt(9)“.
         * Zwei Zahlen direkt hintereinander sind dagegen ein Fehler.
         */
        private fun implicitTimes(): Boolean {
            val next = peek()
            val prev = tokens.getOrNull(pos - 1)
            val afterClose = prev is Token.Op && (prev.c == ')' || prev.c == '%' || prev.c == '!')
            return when {
                next is Token.Sqrt -> true
                next is Token.Op && next.c == '(' -> true
                next is Token.Number && afterClose -> true
                next is Token.Number -> throw CalcException("Zwischen zwei Zahlen fehlt ein Rechenzeichen.")
                else -> false
            }
        }

        private fun term(): Value {
            var v = unary()
            while (true) {
                v = when {
                    isOp('*') -> { pos++; combine(v, unary(), { a, b -> a * b }, { a, b -> a * b }) }
                    isOp('/') -> {
                        pos++
                        val d = unary()
                        if (d is Value.Approx && d.d == 0.0) throw CalcException("Division durch 0 ist nicht erlaubt.")
                        combine(v, d, { a, b -> a / b }, { a, b -> a / b })
                    }
                    // Malpunkt weggelassen: „2(3 + 4)“ oder „(1 + 2)(3 + 4)“.
                    implicitTimes() -> combine(v, unary(), { a, b -> a * b }, { a, b -> a * b })
                    else -> return v
                }
            }
        }

        private fun unary(): Value = when {
            isOp('-') -> {
                pos++
                when (val v = unary()) {
                    is Value.Exact -> Value.Exact(-v.r)
                    is Value.Approx -> Value.Approx(-v.d)
                }
            }
            isOp('+') -> { pos++; unary() }
            else -> power()
        }

        private fun power(): Value {
            val base = postfix()
            if (!isOp('^')) return base
            pos++
            val exp = unary()
            if (exp is Value.Exact && exp.r.isInteger) {
                val k = exp.r.num
                if (k.abs() > BigInteger.valueOf(MAX_EXPONENT.toLong())) throw CalcException("Die Hochzahl ist zu groß.")
                return when (base) {
                    is Value.Exact -> {
                        if (base.r.signum == 0 && k.signum() < 0) throw CalcException("Division durch 0 ist nicht erlaubt.")
                        val bits = maxOf(base.r.num.bitLength(), base.r.den.bitLength()).toLong() * k.abs().toLong()
                        if (bits > MAX_BITS) throw CalcException("Die Zahl wird zu groß.")
                        Value.Exact(check(base.r.pow(k.toInt())))
                    }
                    is Value.Approx -> approx(base.d.pow(k.toInt()))
                }
            }
            val b = base.toDouble()
            if (b < 0) throw CalcException("Gebrochene Hochzahlen gehen nur bei positiven Zahlen.")
            return approx(b.pow(exp.toDouble()))
        }

        private fun postfix(): Value {
            var v = primary()
            while (isOp('%') || isOp('!')) {
                val op = (peek() as Token.Op).c
                pos++
                v = if (op == '%') {
                    when (v) {
                        is Value.Exact -> Value.Exact(v.r / Rational.of(100))
                        is Value.Approx -> Value.Approx(v.d / 100)
                    }
                } else {
                    factorial(v)
                }
            }
            return v
        }

        private fun factorial(v: Value): Value {
            val r = (v as? Value.Exact)?.r
            if (r == null || !r.isInteger || r.signum < 0) throw CalcException("Fakultät (!) gibt es nur für ganze Zahlen ab 0.")
            if (r.num > BigInteger.valueOf(200)) throw CalcException("Die Zahl wird zu groß.")
            var f = BigInteger.ONE
            for (i in 2..r.num.toInt()) f *= BigInteger.valueOf(i.toLong())
            return Value.Exact(check(Rational.of(f, BigInteger.ONE)))
        }

        private fun primary(): Value {
            return when (val t = peek()) {
                is Token.Number -> { pos++; Value.Exact(t.value) }
                is Token.Sqrt -> {
                    pos++
                    val arg = if (isOp('(')) primary() else postfix()
                    sqrtOf(arg)
                }
                is Token.Op -> {
                    if (t.c != '(') throw CalcException(if (t.c == ')') "Eine Klammer zu viel." else "Vor „${t.c}“ fehlt eine Zahl.")
                    pos++
                    val v = expr()
                    if (!isOp(')')) throw CalcException("Eine Klammer fehlt.")
                    pos++
                    v
                }
                null -> throw CalcException("Der Ausdruck ist unvollständig.")
            }
        }

        private fun sqrtOf(v: Value): Value {
            if (v is Value.Exact) {
                if (v.r.signum < 0) throw CalcException("Aus negativen Zahlen gibt es keine Wurzel.")
                val n = isqrt(v.r.num)
                val d = isqrt(v.r.den)
                if (n * n == v.r.num && d * d == v.r.den) return Value.Exact(Rational.of(n, d))
            }
            val x = v.toDouble()
            if (x < 0) throw CalcException("Aus negativen Zahlen gibt es keine Wurzel.")
            return approx(sqrt(x))
        }
    }

    /** Ganzzahlige Wurzel (abgerundet), ohne BigInteger.sqrt (gibt es erst ab Android 13). */
    private fun isqrt(n: BigInteger): BigInteger {
        if (n.signum() <= 0) return BigInteger.ZERO
        var x = BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2)
        while (true) {
            val y = (x + n / x).shiftRight(1)
            if (y >= x) return x
            x = y
        }
    }

    // --- Ausgabe ---------------------------------------------------------------------------------

    /** Dezimalzahl im deutschen Format, ohne Tausenderpunkte (damit die KI sie wieder eingeben kann). */
    fun german(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val plain = (if (stripped.scale() < 0) stripped.setScale(0) else stripped).toPlainString()
        return plain.replace('.', ',')
    }

    private fun format(v: Value): Result.Ok = when (v) {
        is Value.Exact -> {
            val r = v.r
            val exactDecimal = r.toExactDecimal(MAX_SCALE)
            if (exactDecimal != null) {
                val more = exactDecimal.stripTrailingZeros().scale() > 2
                Result.Ok(
                    exact = r,
                    approx = r.toDouble(),
                    text = german(exactDecimal),
                    fraction = null,
                    rounded = null,
                    rounded2 = if (more) german(exactDecimal.setScale(2, RoundingMode.HALF_UP)) else null,
                )
            } else {
                Result.Ok(
                    exact = r,
                    approx = r.toDouble(),
                    text = "${r.num}/${r.den}",
                    fraction = "${r.num}/${r.den}",
                    rounded = german(r.rounded(6)),
                    rounded2 = german(r.rounded(2)),
                )
            }
        }
        is Value.Approx -> {
            val bd = BigDecimal(v.d).setScale(6, RoundingMode.HALF_UP)
            Result.Ok(
                exact = null,
                approx = v.d,
                text = "≈ " + german(bd),
                fraction = null,
                rounded = german(bd),
                rounded2 = german(BigDecimal(v.d).setScale(2, RoundingMode.HALF_UP)),
            )
        }
    }
}
