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

import com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World

/** Nachschlagen für die KI: Kategorien, Levels und Erklärungen finden. */
object AiLookup {

    /** Text vereinfachen: klein, Umlaute ausgeschrieben, nur Buchstaben und Ziffern. */
    fun simplify(text: String): String = text.lowercase()
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private val stopWords = setOf(
        "der", "die", "das", "den", "dem", "des", "und", "oder", "mit", "wie", "ich", "ein", "eine", "einen",
        "einer", "fuer", "bei", "von", "zum", "zur", "auf", "aus", "ist", "sind", "was", "man", "mir", "mich",
        "bitte", "erklaere", "erklaer", "erklaerung", "methode", "trick", "tricks", "aufgabe", "aufgaben",
        "rechnen", "rechne", "level", "ueben", "uebung", "wenn", "dann", "kann", "soll", "geht", "schnell",
    )

    private fun words(text: String): List<String> = simplify(text).split(' ').filter { it.isNotEmpty() }

    private fun queryWords(query: String): List<String> =
        words(query).filter { (it.length >= 3 || it.any(Char::isDigit)) && it !in stopWords }.distinct()

    /**
     * Passt ein Suchwort zu einem Wort im Text? Zahlen müssen genau stimmen, kurze Wörter am
     * Wortanfang; längere Wörter, wenn die ersten fünf Buchstaben gleich sind (Prozente – Prozentsatz).
     */
    private fun matches(q: String, w: String): Boolean = when {
        q.any(Char::isDigit) -> q == w
        q.length <= 4 -> w.startsWith(q)
        else -> w.length >= 5 && w.regionMatches(0, q, 0, 5)
    }

    // --- Kategorien des TMSnat-Trainings -----------------------------------------------------

    /** Stichwörter, an denen eine Kategorie erkannt wird (zusätzlich zu Titel und Methode). */
    private val categoryWords: Map<ApCategory, List<String>> = mapOf(
        ApCategory.RATIO to listOf("dreisatz", "proportional", "antiproportional", "verhaeltnis"),
        ApCategory.PERCENT to listOf("prozent", "prozente", "rabatt", "grundwert", "prozentwert", "prozentsatz", "mehrwertsteuer", "zinsen"),
        ApCategory.RELATIONS to listOf("beziehung", "beziehungen", "beziehungskette", "kette", "doppelt"),
        ApCategory.FRACTIONS to listOf("anteil", "anteile", "rest", "restmenge", "bruchteil", "drittel", "viertel"),
        ApCategory.UNITS to listOf("einheit", "einheiten", "dosis", "dosierung", "infusion", "tropfen", "medikament", "mg", "ml"),
        ApCategory.SPEED to listOf("tempo", "geschwindigkeit", "weg", "strecke", "kmh", "fahrt", "einholen", "begegnung"),
        ApCategory.WORK to listOf("arbeit", "arbeiten", "gemeinsam", "zusammen", "pumpe", "pumpen", "zulauf", "abfluss"),
        ApCategory.MIX to listOf("mischen", "mischung", "verduennen", "verduennung", "konzentration", "loesung", "mischungskreuz"),
        ApCategory.GROWTH to listOf("wachstum", "halbwertszeit", "verdopplung", "verdoppeln", "zerfall", "bakterien", "exponentiell"),
        ApCategory.EQUATIONS to listOf("gleichung", "gleichungen", "unbekannte", "alter", "altersaufgabe", "gleichungssystem"),
        ApCategory.COMBI to listOf("kombinatorik", "anordnen", "anordnung", "reihenfolge", "reihenfolgen", "zaehlen", "kombination", "kombinationen", "moeglichkeiten"),
        ApCategory.PROB to listOf("wahrscheinlichkeit", "wahrscheinlichkeiten", "zufall", "wuerfel", "ziehen", "chance"),
    )

    /** Kategorie aus einem Namen oder Stichwort, z. B. „Prozente“, „PERCENT“ oder „Dosierung“. */
    fun category(name: String): ApCategory? {
        val raw = name.trim()
        ApCategory.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }?.let { return it }
        val simple = simplify(raw)
        if (simple.isEmpty()) return null
        ApCategory.entries.firstOrNull { simplify(it.title) == simple }?.let { return it }
        val q = queryWords(raw).ifEmpty { words(raw) }
        var best: ApCategory? = null
        var bestScore = 0
        for (c in ApCategory.entries) {
            val titleWords = words(c.title) + words(c.method)
            val keys = categoryWords.getValue(c)
            var score = 0
            for (w in q) {
                if (keys.any { matches(w, it) }) score += 3
                if (titleWords.any { matches(w, it) }) score += 2
            }
            if (score > bestScore) {
                best = c
                bestScore = score
            }
        }
        return best
    }

    // --- Levels ----------------------------------------------------------------------------------

    /** Ein Level mit Welt und Nummer in der Welt. */
    data class Found(val skill: Skill, val world: World, val number: Int, val score: Int) {
        /** „Welt 3, Level 4: Zweistellig minus zweistellig“. */
        val name: String get() = "Welt ${world.number}, Level $number: ${skill.title}"
    }

    fun locate(worlds: List<World>, skillId: String): Found? {
        for (w in worlds) {
            val i = w.skills.indexOfFirst { it.id == skillId }
            if (i >= 0) return Found(w.skills[i], w, i + 1, 0)
        }
        return null
    }

    /** Levels zu einer Suche, die besten zuerst. */
    fun search(worlds: List<World>, query: String, limit: Int = 3): List<Found> {
        val trimmed = query.trim()
        // Direkt über die ID („w3.add2“) oder „Welt 3 Level 2“ / „3.2“.
        locate(worlds, trimmed)?.let { return listOf(it.copy(score = 100)) }
        Regex("(?:welt\\s*)?(\\d{1,2})\\s*[.,/]\\s*(?:level\\s*)?(\\d{1,2})|welt\\s*(\\d{1,2})\\D+level\\s*(\\d{1,2})", RegexOption.IGNORE_CASE)
            .find(trimmed)?.let { m ->
                val wn = (m.groupValues[1].ifEmpty { m.groupValues[3] }).toIntOrNull()
                val ln = (m.groupValues[2].ifEmpty { m.groupValues[4] }).toIntOrNull()
                val w = worlds.firstOrNull { it.number == wn }
                if (w != null && ln != null && ln in 1..w.skills.size) return listOf(Found(w.skills[ln - 1], w, ln, 100))
            }
        val q = queryWords(trimmed)
        if (q.isEmpty()) return emptyList()
        val phrase = simplify(trimmed)
        val found = mutableListOf<Found>()
        for (w in worlds) {
            w.skills.forEachIndexed { i, s ->
                val fields = listOf(
                    words(s.title) to 6,
                    words(s.method) to 4,
                    words(s.summary) to 2,
                    words(w.title + " " + w.subtitle) to 1,
                    words(explanationText(s, examples = false)) to 1,
                )
                var score = 0
                for (word in q) {
                    for ((fieldWords, weight) in fields) {
                        if (fieldWords.any { matches(word, it) }) score += weight
                    }
                }
                if (phrase.length >= 5 && simplify(s.title).contains(phrase)) score += 10
                if (phrase.length >= 5 && simplify(s.method).contains(phrase)) score += 8
                if (score > 0) found += Found(s, w, i + 1, score)
            }
        }
        return found.sortedByDescending { it.score }.take(limit)
    }

    /** Erklärung eines Levels als Text (aus den eigenen Texten der App). */
    fun explanationText(skill: Skill, examples: Boolean = true, maxChars: Int = 1_400): String {
        val sb = StringBuilder()
        for (b in skill.explanation) {
            when (b) {
                is Block.Text -> sb.append(b.text).append('\n')
                is Block.Steps -> b.items.forEachIndexed { i, s -> sb.append("${i + 1}. ").append(s).append('\n') }
                is Block.Tip -> sb.append("Tipp: ").append(b.text).append('\n')
                is Block.Formula -> sb.append(b.text).append('\n')
                is Block.Example -> if (examples) sb.append("Beispiel: ").append(solutionText(b.problem)).append('\n')
            }
            if (sb.length > maxChars) break
        }
        val text = sb.toString().trim()
        return if (text.length > maxChars) text.take(maxChars - 2).trimEnd() + " …" else text
    }

    /** Aufgabentext, wie er auf dem Bildschirm steht. */
    fun taskText(problem: Problem): String {
        val p = problem.prompt
        val parts = mutableListOf<String>()
        p.caption?.let { parts += it }
        if (p.lines.isNotEmpty()) parts += p.lines.joinToString(" | ")
        val main = p.text.trim()
        if (main.isNotEmpty() && !main.startsWith("?")) parts += main
        if (main.startsWith("?") && p.unit == null && main.length > 1) parts += "Gesucht in ${main.removePrefix("?").trim()}"
        p.unit?.let { parts += "Antwort in $it" }
        return parts.joinToString("\n")
    }

    /** Lösungsweg als Zeilen „Schritt = Ergebnis“. */
    fun solutionLines(problem: Problem): List<String> = problem.steps.mapIndexed { i, s ->
        val value = Fmt.value(s.expected, problem.choices)
        val line = if (s.label.contains("?")) "${s.label} → $value" else "${s.label} = $value"
        "${i + 1}. " + line + (s.hint?.let { " ($it)" } ?: "")
    } + problem.notes

    /** Aufgabe mit Lösungsweg in einer Zeile (für Beispiele in Erklärungen). */
    fun solutionText(problem: Problem): String =
        taskText(problem).replace('\n', ' ') + " – " + problem.steps.joinToString("; ") { s -> "${s.label} = ${Fmt.value(s.expected, problem.choices)}" }

    // --- Datum -----------------------------------------------------------------------------------

    private val weekdays = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    private val months = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember",
    )

    /** Jahr, Monat (1–12) und Tag zu einem Tag seit dem 1.1.1970. */
    fun civil(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719_468
        val era = Math.floorDiv(z, 146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        return Triple((if (m <= 2) y + 1 else y).toInt(), m.toInt(), d.toInt())
    }

    /** Tag seit dem 1.1.1970 zu einem Datum. */
    fun epochDay(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1L else year.toLong()
        val era = Math.floorDiv(y, 400L)
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    /** „Mittwoch, 28. Oktober 2026“. */
    fun germanDate(epochDay: Long): String {
        val (y, m, d) = civil(epochDay)
        val wd = weekdays[Math.floorMod(epochDay + 3, 7L).toInt()]
        return "$wd, $d. ${months[m - 1]} $y"
    }

    /** „12.10.“ */
    fun shortDate(epochDay: Long): String {
        val (_, m, d) = civil(epochDay)
        return "$d.$m."
    }

    /** Erster TMSnat-Termin (geplant am 8./9. Mai 2027). */
    val TMSNAT_DAY: Long = epochDay(2027, 5, 8)
}
