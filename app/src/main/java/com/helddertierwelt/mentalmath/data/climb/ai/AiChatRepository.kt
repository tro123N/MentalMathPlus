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

package com.helddertierwelt.mentalmath.data.climb.ai

import android.content.Context
import android.util.Log
import com.helddertierwelt.mentalmath.domain.climb.ai.AiConversation
import com.helddertierwelt.mentalmath.domain.climb.ai.AiStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Speichert das letzte Gespräch mit dem KI-Coach (bleibt auch nach einem Neustart der App). */
@Singleton
class AiChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    suspend fun load(): AiConversation = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (!file.exists()) return@withLock AiConversation()
                val c = json.decodeFromString(AiConversation.serializer(), file.readText())
                // Eine Antwort, die beim Beenden der App noch lief, gilt als abgebrochen.
                c.copy(messages = c.messages.map { if (it.status == AiStatus.STREAMING) it.copy(status = AiStatus.STOPPED) else it })
            } catch (e: Exception) {
                Log.e(TAG, "Gespräch konnte nicht gelesen werden", e)
                AiConversation()
            }
        }
    }

    suspend fun save(c: AiConversation) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val trimmed = c.copy(messages = c.messages.takeLast(AiConversation.KEPT))
                val tmp = File(context.filesDir, "$FILE_NAME.tmp")
                tmp.writeText(json.encodeToString(AiConversation.serializer(), trimmed))
                if (!tmp.renameTo(file)) {
                    file.writeText(tmp.readText())
                    tmp.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gespräch konnte nicht gespeichert werden", e)
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock { file.delete() }
    }

    companion object {
        private const val TAG = "AiChat"
        const val FILE_NAME = "ai_chat.json"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
    }
}
