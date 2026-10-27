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
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
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

/** Speichert den Fortschritt im Kletterweg als JSON-Datei im App-Speicher. */
@Singleton
class ClimbRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progress = MutableStateFlow(ClimbProgress())
    val progress: StateFlow<ClimbProgress> = _progress.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val file: File get() = File(context.filesDir, FILE_NAME)

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        mutex.withLock {
            val stored = try {
                val f = file
                if (f.exists()) json.decodeFromString(ClimbProgress.serializer(), f.readText()) else ClimbProgress()
            } catch (e: Exception) {
                Log.e(TAG, "Fortschritt konnte nicht gelesen werden", e)
                ClimbProgress()
            }
            _progress.value = stored
            _loaded.value = true
        }
    }

    /** Fortschritt ändern und sofort speichern. */
    suspend fun update(transform: (ClimbProgress) -> ClimbProgress): ClimbProgress {
        loaded.first { it }
        return mutex.withLock {
            val updated = transform(_progress.value)
            _progress.value = updated
            withContext(Dispatchers.IO) { write(updated) }
            updated
        }
    }

    suspend fun reset() {
        update { ClimbProgress() }
    }

    private fun write(progress: ClimbProgress) {
        try {
            val text = json.encodeToString(ClimbProgress.serializer(), progress)
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) {
                file.writeText(text)
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fortschritt konnte nicht gespeichert werden", e)
        }
    }

    companion object {
        private const val TAG = "ClimbRepository"
        const val FILE_NAME = "climb_progress.json"
    }
}
