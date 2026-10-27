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

package com.helddertierwelt.mentalmath.data.climb

import android.content.Context
import android.util.Log
import com.helddertierwelt.mentalmath.domain.climb.model.PlusSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Speichert die Einstellungen von Mental Math Plus (Farbthema, Erinnerung) als JSON-Datei. */
@Singleton
class PlusSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(PlusSettings())
    val settings: StateFlow<PlusSettings> = _settings.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch {
            mutex.withLock {
                val stored = readStored(context)
                _settings.value = stored
                _loaded.value = true
                // Sicherstellen, dass eine eingeschaltete Erinnerung auch geplant ist.
                try {
                    ReminderScheduler.schedule(context, stored, replace = false)
                } catch (e: Exception) {
                    Log.e(TAG, "Erinnerung konnte nicht geplant werden", e)
                }
            }
        }
    }

    /** Einstellungen ändern und sofort speichern. Die Erinnerung wird passend neu geplant. */
    suspend fun update(transform: (PlusSettings) -> PlusSettings): PlusSettings {
        loaded.first { it }
        return mutex.withLock {
            val before = _settings.value
            val updated = transform(before)
            _settings.value = updated
            withContext(Dispatchers.IO) { write(updated) }
            if (reminderChanged(before, updated)) ReminderScheduler.schedule(context, updated, replace = true)
            updated
        }
    }

    private fun reminderChanged(a: PlusSettings, b: PlusSettings): Boolean =
        a.reminderOn != b.reminderOn || a.reminderHour != b.reminderHour || a.reminderMinute != b.reminderMinute

    private fun write(settings: PlusSettings) {
        try {
            val text = json.encodeToString(PlusSettings.serializer(), settings)
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(File(context.filesDir, FILE_NAME))) {
                File(context.filesDir, FILE_NAME).writeText(text)
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Einstellungen konnten nicht gespeichert werden", e)
        }
    }

    companion object {
        private const val TAG = "PlusSettings"
        const val FILE_NAME = "plus_settings.json"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /** Gespeicherte Einstellungen direkt lesen (auch für die Erinnerung). */
        fun readStored(context: Context): PlusSettings = try {
            val f = File(context.filesDir, FILE_NAME)
            if (f.exists()) json.decodeFromString(PlusSettings.serializer(), f.readText()) else PlusSettings()
        } catch (e: Exception) {
            Log.e(TAG, "Einstellungen konnten nicht gelesen werden", e)
            PlusSettings()
        }
    }
}
