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

import kotlinx.serialization.Serializable

/** Farbthemen von Mental Math Plus. Hell oder dunkel wird getrennt davon gewählt. */
enum class Palette(val title: String, val subtitle: String) {
    STANDARD("Grau", "Wie die Original-App"),
    OZEAN("Ozean", "Kühles Blau"),
    WALD("Wald", "Ruhiges Grün"),
    ABENDROT("Abendrot", "Warmes Orange"),
    LAVENDEL("Lavendel", "Sanftes Lila"),
    SCHWARZ("Tiefschwarz", "Spart Akku im dunklen Modus"),
    ;

    companion object {
        fun of(name: String): Palette = entries.firstOrNull { it.name == name } ?: STANDARD
    }
}

/** Einstellungen, die es nur in Mental Math Plus gibt (gespeichert als JSON-Datei). */
@Serializable
data class PlusSettings(
    /** Name eines [Palette]-Eintrags; als Text gespeichert, damit ältere Versionen neue Werte vertragen. */
    val palette: String = Palette.STANDARD.name,
    val reminderOn: Boolean = false,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    /** Nur erinnern, wenn an diesem Tag noch nicht geübt wurde. */
    val reminderOnlyIfIdle: Boolean = true,
    /** Die Erinnerung der Original-App wurde schon übernommen. */
    val oldReminderMigrated: Boolean = false,
    /** Knopf „Trick-Tipp“ beim Üben zeigen. */
    val trickTips: Boolean = true,
    /** Freies Training: Schwierigkeit passt sich automatisch an. */
    val adaptive: Boolean = false,
    /** KI-Coach: gewähltes Modell (Kennung aus AiModel). */
    val aiModel: String = "gemma4-e4b",
    /** KI-Coach: vor der Antwort gründlich nachdenken (Denkmodus). */
    val aiThinking: Boolean = true,
    /** KI-Coach: schneller schreiben durch Vorausschätzen mehrerer Wörter (wenn das Modell es kann). */
    val aiFast: Boolean = true,
    /** KI-Coach: Modell nur über WLAN herunterladen. */
    val aiWifiOnly: Boolean = true,
) {
    val paletteValue: Palette get() = Palette.of(palette)

    /** Uhrzeit als Text, z. B. „18:30“. */
    val reminderTime: String get() = "$reminderHour:${reminderMinute.toString().padStart(2, '0')}"
}
