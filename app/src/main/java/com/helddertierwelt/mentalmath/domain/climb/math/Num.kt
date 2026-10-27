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

import kotlin.math.abs

/** Kleine Zahlen-Hilfen für Generatoren und Lösungswege. */
object Num {

    fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val t = x % y
            x = y
            y = t
        }
        return if (x == 0L) 1L else x
    }

    fun pow2(k: Int): Long = 1L shl k

    fun pow10(k: Int): Long {
        var r = 1L
        repeat(k) { r *= 10L }
        return r
    }

    /** Ziffern von links nach rechts. */
    fun digits(n: Long): List<Int> = abs(n).toString().map { it - '0' }

    fun digitSum(n: Long): Int = digits(n).sum()

    /** Mod-Summe (Quersumme bis zur einstelligen Zahl), 9 für Vielfache von 9, 0 nur für 0. */
    fun modSum(n: Long): Int {
        if (n == 0L) return 0
        val r = (abs(n) % 9).toInt()
        return if (r == 0) 9 else r
    }

    /** Zerlegt n in Stellenwerte ungleich 0, z. B. 327 → [300, 20, 7]. */
    fun placeParts(n: Long): List<Long> {
        val d = digits(n)
        val parts = mutableListOf<Long>()
        d.forEachIndexed { i, digit ->
            if (digit != 0) parts += digit * pow10(d.size - 1 - i)
        }
        return parts
    }

    /** Nächstes Vielfaches von [step], das ≥ n ist. */
    fun roundUp(n: Long, step: Long): Long = ((n + step - 1) / step) * step

    /** Nächstes Vielfaches von [step], das ≤ n ist. */
    fun roundDown(n: Long, step: Long): Long = (n / step) * step

    /** Zerlegungen einer Zahl in zwei einstellige Faktoren (2 bis 9), größerer Faktor zuerst. */
    fun singleDigitFactorPairs(n: Long): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()
        for (a in 9L downTo 2L) {
            if (n % a == 0L) {
                val b = n / a
                if (b in 2L..a) result += a to b
            }
        }
        return result
    }

    fun isPrime(n: Long): Boolean {
        if (n < 2) return false
        var i = 2L
        while (i * i <= n) {
            if (n % i == 0L) return false
            i++
        }
        return true
    }
}
