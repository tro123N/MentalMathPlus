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

/** Prüft eine Eingabe gegen die erwartete Antwort. */
object AnswerCheck {

    data class Verdict(val correct: Boolean, val note: String? = null)

    fun parseDecimal(input: String): BigDecimal? {
        val cleaned = input.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned == ".") return null
        if (cleaned.count { it == '.' } > 1) return null
        if (!cleaned.all { it.isDigit() || it == '.' }) return null
        val normalized = when {
            cleaned.startsWith('.') -> "0$cleaned"
            cleaned.endsWith('.') -> cleaned.dropLast(1)
            else -> cleaned
        }
        return normalized.toBigDecimalOrNull()
    }

    fun check(expected: Expected, input: String): Verdict {
        val raw = input.trim()
        if (raw.isEmpty()) return Verdict(false)
        return when (expected) {
            is Expected.Exact -> {
                val value = parseDecimal(raw) ?: return Verdict(false)
                Verdict(value.compareTo(expected.value) == 0)
            }

            is Expected.Approx -> {
                val value = parseDecimal(raw) ?: return Verdict(false)
                val diff = (value - expected.value).abs()
                if (diff <= expected.tolerance) {
                    Verdict(true, "Genau wären es ${Fmt.dec(expected.value, expected.places)}.")
                } else {
                    Verdict(false)
                }
            }

            is Expected.Remainder -> {
                val parts = raw.split('R')
                if (parts.size > 2) return Verdict(false)
                val q = parts[0].toLongOrNull() ?: return Verdict(false)
                val r = if (parts.size == 2) (parts[1].ifEmpty { "0" }).toLongOrNull() ?: return Verdict(false) else 0L
                when {
                    q == expected.quotient && r == expected.remainder -> Verdict(true)
                    q == expected.quotient -> Verdict(false, "Das Ergebnis stimmt, der Rest nicht.")
                    else -> Verdict(false)
                }
            }

            is Expected.Frac -> {
                val parts = raw.split('/')
                if (parts.size > 2) return Verdict(false)
                val num = parts[0].toLongOrNull() ?: return Verdict(false)
                val den = if (parts.size == 2) parts[1].toLongOrNull() ?: return Verdict(false) else 1L
                if (den == 0L) return Verdict(false)
                val equivalent = num * expected.denominator == expected.numerator * den
                when {
                    !equivalent -> Verdict(false)
                    expected.requireReduced && Num.gcd(num, den) != 1L ->
                        Verdict(false, "Der Wert stimmt, aber der Bruch lässt sich noch kürzen.")
                    else -> Verdict(true)
                }
            }

            is Expected.Choice -> Verdict(raw.toIntOrNull() == expected.index)
            is Expected.Digits -> Verdict(raw == expected.digits)
        }
    }
}
