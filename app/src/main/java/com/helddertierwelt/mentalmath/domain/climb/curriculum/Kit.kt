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

import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.math.Num
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import com.helddertierwelt.mentalmath.domain.climb.model.exact
import java.math.BigDecimal
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Kleine Hilfen für alle Welten
// ---------------------------------------------------------------------------

internal const val X = Fmt.TIMES
internal const val MINUS = Fmt.MINUS
internal const val DIV = Fmt.DIVIDE

internal fun n(x: Long): String = Fmt.int(x)
internal fun n(x: Int): String = Fmt.int(x)
internal fun d(x: BigDecimal, places: Int? = null): String = Fmt.dec(x, places)

internal fun Random.between(from: Int, to: Int): Int = nextInt(from, to + 1)
internal fun Random.betweenL(from: Long, to: Long): Long = nextLong(from, to + 1)
internal fun <T> Random.pick(list: List<T>): T = list[nextInt(list.size)]

/** Zahl mit Nachkommastellen aus ganzer Zahl: dec(602, 1) = 60,2. */
internal fun dec(unscaled: Long, scale: Int): BigDecimal = BigDecimal.valueOf(unscaled, scale)

internal fun step(label: String, value: Long, hint: String? = null): Step =
    Step(label, exact(value), InputKind.INTEGER, hint)

internal fun decStep(label: String, value: BigDecimal, places: Int? = null, hint: String? = null): Step =
    Step(label, exact(value, places), InputKind.DECIMAL, hint)

internal fun intProblem(
    skillId: String,
    key: String,
    prompt: Prompt,
    value: Long,
    steps: List<Step>,
    method: String? = null,
    notes: List<String> = emptyList(),
): Problem = Problem(
    skillId = skillId,
    key = key,
    prompt = prompt,
    expected = exact(value),
    input = InputKind.INTEGER,
    steps = steps,
    method = method,
    notes = notes,
)

internal fun decProblem(
    skillId: String,
    key: String,
    prompt: Prompt,
    value: BigDecimal,
    places: Int?,
    steps: List<Step>,
    method: String? = null,
    notes: List<String> = emptyList(),
): Problem = Problem(
    skillId = skillId,
    key = key,
    prompt = prompt,
    expected = Expected.Exact(value, places),
    input = InputKind.DECIMAL,
    steps = steps,
    method = method,
    notes = notes,
)

// ---------------------------------------------------------------------------
// Bausteine der Rechenmethoden. Jede Funktion liefert die Zwischenschritte,
// der letzte Schritt ist immer das Ergebnis.
// ---------------------------------------------------------------------------

internal object Methods {

    /**
     * a + b von links nach rechts: b wird in Stellenwerte zerlegt und Stück für Stück addiert.
     * Hat die Zwischensumme an den restlichen Stellen nur Nullen, wird der Rest in einem Schritt angehängt.
     */
    fun addLeftToRight(a: Long, b: Long): List<Step> {
        val parts = Num.placeParts(b)
        val steps = mutableListOf<Step>()
        var current = a
        var i = 0
        while (i < parts.size) {
            val rest = parts.drop(i).sum()
            val restDigits = rest.toString().length
            if (parts.size - i > 1 && current % Num.pow10(restDigits) == 0L) {
                steps += step("${n(current)} + ${n(rest)}", current + rest, "Hier stehen in ${n(current)} nur Nullen – ${n(rest)} einfach einsetzen.")
                return steps
            }
            val part = parts[i]
            val next = current + part
            val hint = when {
                parts.size == 1 -> null
                i == 0 -> "Zuerst die größte Stelle: ${n(part)} dazu."
                i == parts.lastIndex -> "Zum Schluss die Einer."
                else -> "Dann ${n(part)} dazu."
            }
            steps += step("${n(current)} + ${n(part)}", next, hint)
            current = next
            i++
        }
        return steps
    }

    /** a + b, wobei b knapp unter einem Vielfachen von [base] liegt: erst aufgerundet addieren, dann abziehen. */
    fun addRoundingUp(a: Long, b: Long, base: Long): List<Step> {
        val up = Num.roundUp(b, base)
        val diff = up - b
        return listOf(
            step("${n(a)} + ${n(up)}", a + up, "${n(b)} ist fast ${n(up)}. Also erst ${n(up)} addieren."),
            step("${n(a + up)} $MINUS ${n(diff)}", a + b, "Das waren ${n(diff)} zu viel – wieder abziehen."),
        )
    }

    /** a − b Stelle für Stelle von links nach rechts (nur ohne Borgen sinnvoll). */
    fun subtractLeftToRight(a: Long, b: Long): List<Step> {
        val parts = Num.placeParts(b)
        val steps = mutableListOf<Step>()
        var current = a
        parts.forEachIndexed { i, part ->
            val next = current - part
            val hint = when {
                parts.size == 1 -> null
                i == 0 -> "Zuerst ${n(part)} abziehen."
                i == parts.lastIndex -> "Zum Schluss die Einer."
                else -> "Dann ${n(part)} abziehen."
            }
            steps += step("${n(current)} $MINUS ${n(part)}", next, hint)
            current = next
        }
        return steps
    }

    /** a − b mit Aufrunden von b auf den nächsten Zehner: zu viel abziehen, Differenz wieder addieren. */
    fun subtractRoundingUp(a: Long, b: Long, base: Long = 10): List<Step> {
        val up = Num.roundUp(b, base)
        val diff = up - b
        return listOf(
            step("${n(a)} $MINUS ${n(up)}", a - up, "${n(b)} auf ${n(up)} aufrunden und abziehen."),
            step("${n(a - up)} + ${n(diff)}", a - b, "Das waren ${n(diff)} zu viel abgezogen – wieder dazuzählen."),
        )
    }

    /** a − b mit Komplement: b auf den nächsten Hunderter aufrunden, abziehen, Komplement addieren. */
    fun subtractWithComplement(a: Long, b: Long): List<Step> {
        val up = Num.roundUp(b, 100)
        val gap = up - b
        val lastTwo = b % 100
        val gapHint = if (lastTwo != 0L) {
            "Abstand von ${n(b)} bis ${n(up)} = Komplement von ${n(lastTwo)} (Ziffern ergänzen zu 9 und 10)."
        } else null
        return listOf(
            step("${n(a)} $MINUS ${n(up)}", a - up, "${n(b)} auf ${n(up)} aufrunden und abziehen."),
            step("${n(up)} $MINUS ${n(b)}", gap, gapHint),
            step("${n(a - up)} + ${n(gap)}", a - b, "Zu viel abgezogen – das Komplement wieder addieren."),
        )
    }

    /** Komplement zu 100 von links nach rechts: Zehner ergänzen zu 9, Einer ergänzen zu 10. */
    fun complement100(x: Long): List<Step> {
        val tens = x / 10
        val ones = x % 10
        return if (ones == 0L) {
            listOf(step("10 $MINUS ${n(tens)}", 10 - tens, "Bei glatten Zehnern einfach die Zehner zu 10 ergänzen."), step("100 $MINUS ${n(x)}", 100 - x))
        } else {
            listOf(
                step("9 $MINUS ${n(tens)}", 9 - tens, "Die Zehnerziffer ergänzt man zu 9."),
                step("10 $MINUS ${n(ones)}", 10 - ones, "Die Einerziffer ergänzt man zu 10."),
                step("100 $MINUS ${n(x)}", 100 - x, "Beide Ziffern nebeneinander schreiben."),
            )
        }
    }

    /** n × f von links nach rechts (f einstellig): Stellenwerte einzeln multiplizieren und addieren. */
    fun multiplyLeftToRight(number: Long, factor: Long): List<Step> {
        val parts = Num.placeParts(number)
        val steps = mutableListOf<Step>()
        var sum = 0L
        parts.forEachIndexed { i, part ->
            val product = part * factor
            val hint = if (i == 0 && part >= 10) {
                val lead = part / Num.pow10(part.toString().length - 1)
                val zeros = part.toString().length - 1
                "${n(lead)} $X ${n(factor)} = ${n(lead * factor)}, dann ${if (zeros == 1) "eine 0" else "$zeros Nullen"} anhängen."
            } else null
            steps += step("${n(part)} $X ${n(factor)}", product, hint)
            if (i > 0) {
                steps += step("${n(sum)} + ${n(product)}", sum + product, "Addieren – von links nach rechts.")
            }
            sum += product
        }
        return steps
    }

    /** n × f mit Aufrunden (n endet auf 8 oder 9): auf den nächsten Zehner rechnen, zu viel abziehen. */
    fun multiplyRoundingUp(number: Long, factor: Long): List<Step> {
        val up = Num.roundUp(number, 10)
        val diff = up - number
        return listOf(
            step("${n(up)} $X ${n(factor)}", up * factor, "${n(number)} auf ${n(up)} aufrunden."),
            step("${n(diff)} $X ${n(factor)}", diff * factor, "So viel wurde zu viel gerechnet."),
            step("${n(up * factor)} $MINUS ${n(diff * factor)}", number * factor, "Den Überschuss abziehen."),
        )
    }

    /** Rundungsabstand zum nächsten Vielfachen von [base] nach der Buchregel. */
    fun squareDistance(a: Long, base: Long): Long {
        val r = a % base
        return if (r * 2 >= base) base - r else r
    }

    /**
     * Quadrat durch Auf- und Abrunden: a² = (a + d)(a − d) + d².
     * [base] = 10 für zweistellige, 100 für dreistellige, 1000 für vierstellige Zahlen.
     */
    fun square(a: Long, base: Long, distanceSquare: List<Step>? = null): List<Step> {
        val r = a % base
        val roundUp = r * 2 >= base
        val dist = if (roundUp) base - r else r
        val round = if (roundUp) a + dist else a - dist
        val other = if (roundUp) a - dist else a + dist
        val roundLabel = if (roundUp) "${n(a)} + ${n(dist)}" else "${n(a)} $MINUS ${n(dist)}"
        val otherLabel = if (roundUp) "${n(a)} $MINUS ${n(dist)}" else "${n(a)} + ${n(dist)}"
        val productHint = zerosHint(round, other)
        val steps = mutableListOf(
            step(roundLabel, round, "Auf die nächste runde Zahl ${if (roundUp) "auf" else "ab"}runden."),
            step(otherLabel, other, "Um denselben Abstand in die andere Richtung."),
            step("${n(round)} $X ${n(other)}", round * other, productHint),
        )
        if (distanceSquare != null) {
            steps += distanceSquare
        } else {
            steps += step("${n(dist)}²", dist * dist, "Das Quadrat des Abstands.")
        }
        steps += step("${n(round * other)} + ${n(dist * dist)}", a * a, "Quadrat des Abstands addieren.")
        return steps
    }

    /** 2er-Block für zweistellige Ergebnisse: 9 → „09“. */
    fun twoDigits(x: Long): String = if (x < 10) "0$x" else x.toString()

    /** Hinweis für „runde Zahl × andere Zahl“, z. B. 80 × 74: „8 × 74 = 592, dann eine 0 anhängen.“ */
    fun zerosHint(round: Long, other: Long): String {
        var lead = round
        var zeros = 0
        while (lead % 10 == 0L && lead != 0L) {
            lead /= 10
            zeros++
        }
        val zerosText = if (zeros == 1) "eine 0" else "$zeros Nullen"
        return if (lead == 1L) {
            "${n(other)} und $zerosText dahinter: ${n(round * other)}."
        } else {
            "${n(lead)} $X ${n(other)} = ${n(lead * other)}, dann $zerosText anhängen."
        }
    }
}
