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

import kotlin.random.Random

/** Verweis auf die Stelle im Buch „Mathe-Magie“, an der die Methode erklärt wird. */
data class BookRef(
    /** Kapitelnummer im Buch (0 bis 9); null, wenn das Level eine Ergänzung ist. */
    val chapter: Int?,
    /** Abschnitt im Kapitel. */
    val section: String,
) {
    val label: String
        get() = if (chapter == null) section else "Kapitel $chapter · $section"

    /** Stammt die Methode aus dem Buch? */
    val fromBook: Boolean get() = chapter != null

    /** Kurze Herkunftsangabe für Markierungen: „Buch · Kap. 3“ oder „Ergänzung“. */
    val tag: String get() = if (chapter == null) "Ergänzung" else "Buch · Kap. $chapter"

    /** Ausführliche Herkunft für Level- und Lernseite. */
    val origin: String
        get() = if (chapter == null) {
            "Nicht aus dem Buch – ${section.removePrefix("Ergänzung: ").removePrefix("Ergänzung ")}"
        } else {
            "Aus dem Buch: Kapitel $chapter · $section"
        }
}

/**
 * Ziele eines Levels.
 * Die Sternzeiten sind Durchschnittssekunden pro Aufgabe in der Prüfung.
 */
data class Goals(
    val testSize: Int = 10,
    val minAccuracy: Double = 0.9,
    val star1: Double,
    val star2: Double,
    val star3: Double,
    /** Zeitlimit pro Aufgabe in der Prüfung (Sekunden). */
    val timeLimit: Double = star1 * 2.5,
) {
    fun secondsFor(stars: Int): Double = when (stars) {
        1 -> star1
        2 -> star2
        else -> star3
    }
}

/** Baustein eines Erklärungstextes. */
sealed interface Block {
    /** Normaler Absatz. */
    data class Text(val text: String) : Block

    /** Nummerierte Schritte der Methode. */
    data class Steps(val items: List<String>) : Block

    /** Durchgerechnetes Beispiel mit Lösungsweg. */
    data class Example(val problem: Problem) : Block

    /** Hervorgehobener Tipp. */
    data class Tip(val text: String) : Block

    /** Kurze Formel oder Rechnung, hervorgehoben. */
    data class Formula(val text: String) : Block
}

/** Ein Level des Kletterwegs. */
class Skill(
    /** Stabile ID, unter der der Fortschritt gespeichert wird. Nie umbenennen. */
    val id: String,
    val title: String,
    /** Kurzer Name der Methode. */
    val method: String,
    val book: BookRef,
    /** Ein Satz: worum es geht. */
    val summary: String,
    val explanation: List<Block>,
    val goals: Goals,
    /** Anzahl Aufgaben im geführten Üben. */
    val guidedCount: Int = 4,
    /** Anzahl Aufgaben im freien Üben. */
    val practiceCount: Int = 10,
    private val generator: (Random) -> Problem,
) {
    fun generate(random: Random): Problem = generator(random)
}

/** Eine Welt des Kletterwegs, meist ein Kapitel des Buchs. */
class World(
    val id: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    val skills: List<Skill>,
    /**
     * Eigenständige Welt (z. B. Vorbereitung auf einen Test): immer offen, alle Levels frei,
     * nicht Teil des Hauptwegs („Weiterklettern“).
     */
    val independent: Boolean = false,
) {
    /** Levels, deren Methode aus dem Buch stammt. */
    val bookSkills: Int get() = skills.count { it.book.fromBook }

    /** Ganze Welt stammt nicht aus dem Buch. */
    val supplement: Boolean get() = bookSkills == 0

    /** Kapitel des Buchs, die diese Welt abdeckt. */
    val chapters: List<Int> get() = skills.mapNotNull { it.book.chapter }.distinct().sorted()

    /**
     * Ergänzungswelten sind immer offen; diese Welt des Hauptwegs sollte vorher geschafft sein,
     * weil die Aufgaben darauf aufbauen (nur Empfehlung, nichts wird gesperrt).
     */
    val recommendedAfter: Int?
        get() = when (id) {
            "w13" -> 8 // Prozent, Dreisatz und Überschlagen (Kapitel 4 und 5)
            "w14" -> 5 // Kommazahlen und Zehnerpotenzen
            "w15" -> 6 // baut auf Kapitel 2 und 3 auf (Quadrate, Faktorisieren)
            "w16" -> 8 // Prozent und Überschlagen (Kapitel 5)
            "w17" -> 7 // Brüche (Kapitel 4) und Schulwissen der Oberstufe
            else -> null
        }
}
