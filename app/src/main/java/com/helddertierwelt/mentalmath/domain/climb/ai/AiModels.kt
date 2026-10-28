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

import java.util.Locale

/**
 * Die KI-Modelle, die direkt auf dem Handy laufen (Format LiteRT-LM, Lizenz Apache 2.0).
 *
 * Die Dateien kommen von Hugging Face (Konto „litert-community“ von Google). Die Adresse ist auf
 * einen festen Stand (Commit) gepinnt, damit Größe und Prüfsumme immer zur Datei passen.
 */
enum class AiModel(
    /** Stabile Kennung, unter der die Wahl gespeichert wird. Nie umbenennen. */
    val id: String,
    val title: String,
    /** Kurzes Etikett in der Auswahl. */
    val badge: String,
    /** Ein Satz zur Einordnung. */
    val summary: String,
    val repo: String,
    val revision: String,
    val file: String,
    val bytes: Long,
    val sha256: String,
    /** Empfohlener Arbeitsspeicher des Handys in GB. */
    val minRamGb: Int,
    /** Größe des Gesprächsgedächtnisses (Tokens). */
    val contextTokens: Int,
    /** So viele Tokens darf das Modell im Denkmodus höchstens nachdenken. */
    val thinkingBudget: Int,
    /** Noch nicht auf Handys erprobt: Die App fällt bei Problemen auf ein kleineres Modell zurück. */
    val experimental: Boolean = false,
) {
    GEMMA4_E4B(
        id = "gemma4-e4b",
        title = "Gemma 4 E4B",
        badge = "Empfohlen",
        summary = "Kluges Modell von Google, schnell auf der Grafikeinheit deines Handys. Denkt bei Bedarf gründlich nach und kann die App bedienen.",
        repo = "litert-community/gemma-4-E4B-it-litert-lm",
        revision = "2eee7ac325f20eb8c9ac1d0e972f7c84663062da",
        file = "gemma-4-E4B-it.litertlm",
        bytes = 3_659_530_240L,
        sha256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
        minRamGb = 12,
        contextTokens = 16_384,
        thinkingBudget = 1_536,
    ),
    GEMMA4_12B(
        id = "gemma4-12b",
        title = "Gemma 4 12B",
        badge = "Experiment · am klügsten",
        summary = "Das größte Gemma-4-Modell, das auf ein Handy passt: deutlich klüger, aber langsamer und sehr speicherhungrig. Braucht ein Handy mit 16 GB Arbeitsspeicher.",
        repo = "litert-community/gemma-4-12B-it-litert-lm",
        revision = "7a0b1ce0ea821bcd01c5f72af84155e02191152f",
        file = "gemma-4-12B-it.litertlm",
        bytes = 6_883_278_368L,
        sha256 = "58fd31b778ca2c21c80d634fb34fc5a89d11d563a38dfd3cbf1b40dbf252a8b6",
        minRamGb = 16,
        contextTokens = 8_192,
        thinkingBudget = 1_024,
        experimental = true,
    ),
    GEMMA4_E2B(
        id = "gemma4-e2b",
        title = "Gemma 4 E2B",
        badge = "Klein und schnell",
        summary = "Die kleine Ausgabe: antwortet am schnellsten und braucht am wenigsten Platz, ist aber weniger gründlich.",
        repo = "litert-community/gemma-4-E2B-it-litert-lm",
        revision = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1",
        file = "gemma-4-E2B-it.litertlm",
        bytes = 2_588_147_712L,
        sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
        minRamGb = 8,
        contextTokens = 16_384,
        thinkingBudget = 1_024,
    ),
    ;

    /** Download-Adresse der Modelldatei. */
    val url: String get() = "https://huggingface.co/$repo/resolve/$revision/$file?download=true"

    /** Seite des Modells (Lizenz, Beschreibung). */
    val page: String get() = "https://huggingface.co/$repo"

    /** Größe für Menschen, z. B. „3,7 GB“. */
    val sizeLabel: String get() = gigabytes(bytes)

    companion object {
        val DEFAULT = GEMMA4_E4B

        fun of(id: String?): AiModel? = entries.firstOrNull { it.id == id }

        /** Reihenfolge, in der bei Problemen auf ein anderes, schon geladenes Modell ausgewichen wird. */
        val fallbackOrder: List<AiModel> = listOf(GEMMA4_E4B, GEMMA4_E2B, GEMMA4_12B)

        /** Bytes als Gigabyte mit einer Nachkommastelle, z. B. „6,9 GB“ (wie das Handy Speicher anzeigt). */
        fun gigabytes(bytes: Long): String = String.format(Locale.GERMANY, "%.1f GB", bytes / 1e9)
    }
}
