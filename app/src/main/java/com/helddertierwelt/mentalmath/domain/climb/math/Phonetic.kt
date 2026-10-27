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

/**
 * Der phonetische Code aus Kapitel 7: Jede Ziffer steht für einen Konsonanten-Laut,
 * Vokale sowie h, w und y zählen nicht.
 *
 * 1 t, d · 2 n · 3 m · 4 r · 5 l · 6 j, ch, sch · 7 k, g · 8 f, v · 9 p, b · 0 z, s
 *
 * Die Wortlisten hier sind eigene Beispiele (nicht aus dem Buch). Sie enthalten nur Wörter,
 * deren Schreibweise eindeutig ist (keine Wörter mit w, x, q, c, ng, nk, tz, pf, chs oder
 * mit sp/st am Wortanfang).
 */
object Phonetic {

    /** Die Laute zu jeder Ziffer, wie sie in Erklärungen gezeigt werden. */
    val sounds: List<String> = listOf("z, s", "t, d", "n", "m", "r", "l", "j, ch, sch", "k, g", "f, v", "p, b")

    /** Eigene Merkwörter für 0 bis 99 (Index = Zahl). */
    val pegs: List<String> = listOf(
        "Eis", "Hut", "Huhn", "Oma", "Uhr", "Aal", "Asche", "Auge", "Efeu", "Bau",
        "Tasse", "Tüte", "Tanne", "Dame", "Tier", "Teil", "Tasche", "Teig", "Taufe", "Taube",
        "Nuss", "Note", "Nonne", "Nemo", "Narr", "Nil", "Nische", "Neige", "Neffe", "Neubau",
        "Moos", "Mut", "Mine", "Mumm", "Mauer", "Mehl", "Masche", "Mücke", "Mofa", "Mappe",
        "Reis", "Rad", "Rune", "Rahm", "Ruhr", "Rille", "Rausch", "Riege", "Reif", "Raupe",
        "Laus", "Lied", "Lehne", "Lama", "Leier", "Lolli", "Lasche", "Lack", "Lauf", "Laub",
        "Schuss", "Jade", "Schein", "Schaum", "Jahr", "Schule", "Schach", "Scheck", "Schiff", "Schuppe",
        "Kasse", "Kette", "Kanu", "Kamm", "Karre", "Kohle", "Küche", "Geige", "Kaffee", "Kuppe",
        "Fuß", "Fett", "Fahne", "Fama", "Feuer", "Falle", "Fuji", "Feige", "Fifi", "Phobie",
        "Bus", "Bett", "Biene", "Puma", "Bier", "Beil", "Bach", "Bug", "Beef", "Baby",
    )

    /** Weitere Übungswörter mit drei oder mehr Lauten. */
    val longWords: List<String> = listOf(
        "Kamel", "Nadel", "Mond", "Tomate", "Gabel", "Löffel", "Teller", "Messer", "Kerze", "Lampe",
        "Tunnel", "Kanone", "Pirat", "Rakete", "Muschel", "Leiter", "Delfin", "Tiger", "Nagel", "Pudel",
        "Melone", "Paket", "Kamin", "Limonade", "Mantel", "Kuchen", "Tafel", "Möbel", "Koffer", "Teppich",
        "Pokal", "Kabine", "Lineal", "Diamant", "Gitarre", "Pilz", "Salat", "Zelt", "Bagger", "Kegel",
        "Ritter", "Nebel", "Muffin", "Fakir", "Kolibri", "Pelikan", "Papagei", "Schokolade", "Marmelade", "Joghurt",
        "Butter", "Kartoffel", "Paprika", "Melodie", "Komet", "Planet", "Rubin", "Gondel", "Kanal", "Motor",
        "Roboter", "Mikado", "Domino", "Kobra", "Gorilla", "Dackel", "Biber", "Hamster", "Kaktus", "Ameise",
        "Sofa", "Zebra", "Ziege", "Säge", "Rabe", "Dusche", "Hammer", "Kino", "Lupe", "Koala",
    )

    private val vowels = "aeiouäöüyhw"

    private fun digitOf(c: Char): Int? = when (c) {
        't', 'd' -> 1
        'n' -> 2
        'm' -> 3
        'r' -> 4
        'l' -> 5
        'j' -> 6
        'k', 'g' -> 7
        'f', 'v' -> 8
        'p', 'b' -> 9
        's', 'z', 'ß' -> 0
        else -> null
    }

    /**
     * Zerlegt ein Wort in seine Laute mit Ziffer, z. B. „Kamel“ → [K=7, m=3, l=5].
     * null, wenn das Wort Buchstaben enthält, deren Laut nicht eindeutig ist.
     */
    fun parts(word: String): List<Pair<String, Int>>? {
        val w = word.lowercase()
        if (w.isEmpty() || w.any { !it.isLetter() }) return null
        if (w.contains('w') || w.contains('x') || w.contains('q')) return null
        if (w.contains("ng") || w.contains("nk") || w.contains("tz") || w.contains("pf") || w.contains("chs")) return null
        if (w.startsWith("sp") || w.startsWith("st")) return null
        val result = mutableListOf<Pair<String, Int>>()
        var i = 0
        var lastLetter: Char? = null
        while (i < w.length) {
            val rest = w.substring(i)
            val (group, digit) = when {
                rest.startsWith("sch") -> "sch" to 6
                rest.startsWith("ch") -> "ch" to 6
                rest.startsWith("ck") -> "ck" to 7
                rest.startsWith("ph") -> "ph" to 8
                rest.startsWith("th") -> "th" to 1
                rest[0] == 'c' -> return null
                rest[0] in vowels -> rest.substring(0, 1) to -1
                else -> rest.substring(0, 1) to (digitOf(rest[0]) ?: return null)
            }
            val original = word.substring(i, i + group.length)
            if (digit >= 0) {
                val doubled = group.length == 1 && lastLetter == group[0]
                if (doubled) {
                    // Doppelte Buchstaben (ll, nn, tt …) sind ein einziger Laut.
                    val (prev, d) = result.removeAt(result.lastIndex)
                    result += (prev + original) to d
                } else {
                    result += original to digit
                }
                lastLetter = group.last()
            } else {
                lastLetter = null
            }
            i += group.length
        }
        return result
    }

    /** Die Zahl, die in einem Wort steckt, als Ziffernfolge (z. B. „Kamel“ → „735“). */
    fun code(word: String): String? = parts(word)?.joinToString("") { it.second.toString() }

    /** Erklärung wie „K = 7, m = 3, l = 5“. */
    fun explain(word: String): String = parts(word)?.joinToString(", ") { "${it.first} = ${it.second}" } ?: ""

    private val pseudoConsonants = listOf("s", "t", "n", "m", "r", "l", "sch", "k", "f", "b")
    private val pseudoVowels = listOf("a", "e", "i", "o", "u")

    /** Ein aussprechbares Fantasiewort für eine Ziffernfolge, z. B. 115 → „Tatel“. */
    fun nonsense(digits: String): String {
        val sb = StringBuilder()
        digits.forEachIndexed { i, ch ->
            sb.append(pseudoConsonants[ch - '0'])
            if (i < digits.lastIndex) sb.append(pseudoVowels[i % pseudoVowels.size])
        }
        return sb.toString().replaceFirstChar { it.uppercase() }
    }

    /** Ein Merkwort für die Ziffernfolge: echtes Wort aus den Listen, sonst ein Fantasiewort. */
    fun wordFor(digits: String): String {
        if (digits.length <= 2 && digits.isNotEmpty() && !(digits.length == 2 && digits[0] == '0')) {
            return pegs[digits.toInt()]
        }
        return longWords.firstOrNull { code(it) == digits } ?: nonsense(digits)
    }
}
