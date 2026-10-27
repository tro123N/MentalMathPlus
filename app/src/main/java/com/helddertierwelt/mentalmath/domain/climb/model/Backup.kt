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
import kotlinx.serialization.json.Json

/** Sicherungsdatei: Fortschritt und Einstellungen, um sie auf ein anderes Handy zu übertragen. */
@Serializable
data class Backup(
    val app: String,
    val format: Int,
    /** Zeitpunkt der Sicherung in ms. */
    val createdAt: Long,
    val progress: ClimbProgress,
    val settings: PlusSettings? = null,
) {
    companion object {
        const val APP = "MentalMathPlus"
        const val FORMAT = 1
    }
}

/** Schreibt und liest Sicherungsdateien. */
object BackupCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
    }

    fun create(progress: ClimbProgress, settings: PlusSettings?, now: Long): Backup =
        Backup(app = Backup.APP, format = Backup.FORMAT, createdAt = now, progress = progress, settings = settings)

    fun encode(backup: Backup): String = json.encodeToString(Backup.serializer(), backup)

    /** Liest eine Sicherung. Bei einem Fehler steht in der Ausnahme eine verständliche Meldung. */
    fun decode(text: String): Result<Backup> {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{")) return Result.failure(IllegalArgumentException(NOT_A_BACKUP))
        val backup = try {
            json.decodeFromString(Backup.serializer(), trimmed)
        } catch (e: Exception) {
            // Vielleicht die reine Fortschrittsdatei der App (climb_progress.json).
            return try {
                val progress = json.decodeFromString(ClimbProgress.serializer(), trimmed)
                if (progress.skills.isEmpty() && progress.totalSolved == 0L) {
                    Result.failure(IllegalArgumentException(NOT_A_BACKUP))
                } else {
                    Result.success(create(progress, null, 0L))
                }
            } catch (e2: Exception) {
                Result.failure(IllegalArgumentException(NOT_A_BACKUP))
            }
        }
        if (backup.app != Backup.APP) return Result.failure(IllegalArgumentException(NOT_A_BACKUP))
        if (backup.format > Backup.FORMAT) return Result.failure(IllegalArgumentException(TOO_NEW))
        return Result.success(backup)
    }

    const val NOT_A_BACKUP = "Diese Datei ist keine Sicherung von Mental Math Plus."
    const val TOO_NEW = "Die Sicherung stammt aus einer neueren Version. Bitte zuerst die App aktualisieren."
}
