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

package com.helddertierwelt.mentalmath.domain.climb.math

import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import java.math.BigDecimal
import java.math.RoundingMode

/** Zahlen im deutschen Format: Tausenderpunkte, Dezimalkomma, echtes Minuszeichen. */
object Fmt {
    const val MINUS = "−"
    const val TIMES = "×"
    const val DIVIDE = "÷"

    fun int(n: Long): String {
        val digits = kotlin.math.abs(n).toString()
        val grouped = if (digits.length <= 3) digits else group(digits)
        return if (n < 0) MINUS + grouped else grouped
    }

    fun int(n: Int): String = int(n.toLong())

    /** Kommazahl; mit [places] werden genau so viele Nachkommastellen gezeigt. */
    fun dec(value: BigDecimal, places: Int? = null): String {
        val scaled = if (places != null) {
            value.setScale(places, RoundingMode.HALF_UP)
        } else {
            val stripped = value.stripTrailingZeros()
            if (stripped.scale() < 0) stripped.setScale(0) else stripped
        }
        val negative = scaled.signum() < 0
        val plain = scaled.abs().toPlainString()
        val intPart = plain.substringBefore('.')
        val fracPart = if (plain.contains('.')) plain.substringAfter('.') else ""
        val groupedInt = if (intPart.length <= 3) intPart else group(intPart)
        val body = if (fracPart.isEmpty()) groupedInt else "$groupedInt,$fracPart"
        return if (negative) MINUS + body else body
    }

    fun money(value: BigDecimal): String = dec(value, 2) + " €"

    /** „+ 5“ oder „− 5“ für Rechenschritte. */
    fun plusMinus(n: Long): String = if (n < 0) "$MINUS ${int(-n)}" else "+ ${int(n)}"

    fun expected(expected: Expected, choices: List<String> = emptyList()): String = when (expected) {
        is Expected.Exact -> dec(expected.value, expected.places)
        is Expected.Approx -> "≈ " + dec(expected.value, expected.places)
        is Expected.Remainder ->
            if (expected.remainder == 0L) int(expected.quotient)
            else "${int(expected.quotient)} R ${int(expected.remainder)}"
        is Expected.Frac -> "${int(expected.numerator)}/${int(expected.denominator)}"
        is Expected.Choice -> choices.getOrElse(expected.index) { "?" }
        is Expected.Digits -> expected.digits
    }

    /** „Beschriftung = Wert“, bei Schätzungen „Beschriftung ≈ Wert“. */
    fun equation(label: String, expected: Expected, choices: List<String> = emptyList()): String = when (expected) {
        is Expected.Approx -> "$label ≈ ${dec(expected.value, expected.places)}"
        else -> "$label = ${expected(expected, choices)}"
    }

    /** Ergebnis mit Gleichheitszeichen: „= 42“ bzw. „≈ 42“ bei Schätzungen. */
    fun answer(expected: Expected, choices: List<String> = emptyList()): String = when (expected) {
        is Expected.Approx -> expected(expected, choices)
        else -> "= " + expected(expected, choices)
    }

    /** Nur der Wert, ohne „≈“ (wenn das Zeichen schon davor steht). */
    fun value(expected: Expected, choices: List<String> = emptyList()): String = when (expected) {
        is Expected.Approx -> dec(expected.value, expected.places)
        else -> expected(expected, choices)
    }

    /** Für Sätze wie „Richtig wäre …“: bei Schätzungen „etwa 42“. */
    fun spoken(expected: Expected, choices: List<String> = emptyList()): String = when (expected) {
        is Expected.Approx -> "etwa " + dec(expected.value, expected.places)
        else -> expected(expected, choices)
    }

    /** Eingabe so anzeigen, wie sie getippt wurde, aber mit „R“ und Bruchstrich lesbar. */
    fun input(raw: String): String = raw.replace("R", " R ").replace("/", "/")

    private fun group(digits: String): String {
        val sb = StringBuilder()
        val firstGroup = digits.length % 3
        if (firstGroup > 0) sb.append(digits, 0, firstGroup)
        var i = firstGroup
        while (i < digits.length) {
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(digits, i, i + 3)
            i += 3
        }
        return sb.toString()
    }
}
