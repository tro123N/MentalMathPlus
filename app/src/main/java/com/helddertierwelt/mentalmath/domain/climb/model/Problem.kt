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

package com.helddertierwelt.mentalmath.domain.climb.model

import java.math.BigDecimal

/** Welche Tasten bzw. welche Eingabeart eine Aufgabe braucht. */
enum class InputKind {
    /** Ganze Zahl, nur Ziffern. */
    INTEGER,

    /** Kommazahl, zusätzlich die Komma-Taste. */
    DECIMAL,

    /** Division mit Rest, zusätzlich die Taste „R“ (z. B. 25R4). */
    REMAINDER,

    /** Bruch, zusätzlich die Bruchstrich-Taste (z. B. 3/4). */
    FRACTION,

    /** Auswahl aus mehreren Antworten. */
    CHOICE,

    /** Ziffernfolge, die genau so eingegeben werden muss (führende Nullen zählen). */
    DIGITS,
}

/** Die richtige Antwort einer Aufgabe oder eines Zwischenschritts. */
sealed interface Expected {

    /** Exakte Zahl. [places] legt fest, mit wie vielen Nachkommastellen sie angezeigt wird (z. B. 2 bei Geld). */
    data class Exact(val value: BigDecimal, val places: Int? = null) : Expected

    /** Schätzung: alles innerhalb von [tolerance] um [value] gilt als richtig. */
    data class Approx(val value: BigDecimal, val tolerance: BigDecimal, val places: Int? = null) : Expected

    /** Ganzzahlige Division mit Rest. */
    data class Remainder(val quotient: Long, val remainder: Long) : Expected

    /** Bruch. Mit [requireReduced] muss er vollständig gekürzt sein. */
    data class Frac(val numerator: Long, val denominator: Long, val requireReduced: Boolean = false) : Expected

    /** Index der richtigen Auswahlmöglichkeit. */
    data class Choice(val index: Int) : Expected

    /** Exakte Ziffernfolge. */
    data class Digits(val digits: String) : Expected
}

fun exact(value: Long): Expected.Exact = Expected.Exact(BigDecimal.valueOf(value))
fun exact(value: Int): Expected.Exact = Expected.Exact(BigDecimal.valueOf(value.toLong()))
fun exact(value: BigDecimal, places: Int? = null): Expected.Exact = Expected.Exact(value, places)

/** Was auf dem Bildschirm als Aufgabe steht. */
data class Prompt(
    /** Hauptzeile, groß dargestellt, z. B. „47 × 6“. */
    val text: String,
    /** Kleine Zeile über der Aufgabe, z. B. „Ergänze zu 100“. */
    val caption: String? = null,
    /** Zusätzliche Zeilen (Listen, Kolonnen), werden über der Hauptzeile angezeigt. */
    val lines: List<String> = emptyList(),
    /** Einheit hinter dem Eingabefeld, z. B. „€“. */
    val unit: String? = null,
    /**
     * Merkaufgaben: Die Zeilen verschwinden, sobald getippt wird – spätestens nach so vielen ms.
     * null = immer sichtbar.
     */
    val hideAfterMs: Long? = null,
)

/** Ein Zwischenschritt im geführten Üben. */
data class Step(
    /** Was in diesem Schritt gerechnet wird, z. B. „40 × 6“. */
    val label: String,
    val expected: Expected,
    val input: InputKind = InputKind.INTEGER,
    /** Kurzer Hinweis, der nach einem Fehler und im Lösungsweg erscheint. */
    val hint: String? = null,
)

/** Eine konkrete Aufgabe mit Lösungsweg. */
data class Problem(
    val skillId: String,
    /** Eindeutiger Schlüssel der Aufgabe (zum Vermeiden von Wiederholungen). */
    val key: String,
    val prompt: Prompt,
    val expected: Expected,
    val input: InputKind,
    /** Lösungsweg nach der Methode des Levels; der letzte Schritt liefert das Ergebnis. */
    val steps: List<Step>,
    /** Name der verwendeten Methode, z. B. „Subtraktionsmethode“. */
    val method: String? = null,
    /** Zusätzliche erklärende Zeilen für den Lösungsweg (optional). */
    val notes: List<String> = emptyList(),
    /** Antwortmöglichkeiten bei [InputKind.CHOICE]. */
    val choices: List<String> = emptyList(),
)
