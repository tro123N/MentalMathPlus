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

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.helddertierwelt.mentalmath.domain.climb.ai.AiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Zustand einer Modelldatei auf dem Handy. */
sealed interface ModelState {
    /** Nicht geladen. */
    data object Missing : ModelState

    /** Wird heruntergeladen. [waiting] sagt, worauf der Download gerade wartet (z. B. WLAN). */
    data class Downloading(val bytes: Long, val total: Long, val waiting: String? = null) : ModelState {
        val fraction: Float get() = if (total <= 0) 0f else (bytes.toFloat() / total).coerceIn(0f, 1f)
    }

    /** Datei ist da und wird geprüft (Prüfsumme). */
    data class Verifying(val fraction: Float) : ModelState

    /** Bereit. [bytes] = belegter Speicher (mit Zwischenspeicher der Grafikeinheit). */
    data class Ready(val bytes: Long) : ModelState

    /** Download oder Prüfung fehlgeschlagen. */
    data class Failed(val message: String) : ModelState
}

/**
 * Lädt die KI-Modelle herunter (über den Download-Manager von Android, der auch bei gesperrtem
 * Bildschirm weiterlädt und Abbrüche fortsetzt), prüft sie und verwaltet den Speicher.
 *
 * Die Dateien liegen im App-Ordner unter Android/data/…/files/models. Sie werden beim
 * Deinstallieren gelöscht und nicht in die Android-Sicherung übernommen.
 */
@Singleton
class ModelStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("ai_downloads", Context.MODE_PRIVATE)
    private val downloads = context.getSystemService(DownloadManager::class.java)
    private val verifyMutex = Mutex()

    private val _states = MutableStateFlow<Map<AiModel, ModelState>>(AiModel.entries.associateWith { ModelState.Missing })
    val states: StateFlow<Map<AiModel, ModelState>> = _states.asStateFlow()

    /** Ordner der Modelle; null, wenn der App-Speicher gerade nicht verfügbar ist. */
    fun dir(): File? = context.getExternalFilesDir(DIR)?.also { it.mkdirs() }

    fun file(model: AiModel): File? = dir()?.let { File(it, model.file) }

    private fun partFile(model: AiModel): File? = dir()?.let { File(it, model.file + PART) }

    private fun okFile(model: AiModel): File? = dir()?.let { File(it, model.file + OK) }

    /** Geprüft in diesem Programmlauf (falls der Prüfvermerk nicht geschrieben werden konnte). */
    private val verifiedInMemory: MutableSet<AiModel> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /** Fertig geladen und geprüft. */
    fun isReady(model: AiModel): Boolean {
        val f = file(model) ?: return false
        return f.exists() && f.length() == model.bytes && (model in verifiedInMemory || okFile(model)?.exists() == true)
    }

    /** Freier Speicher im Modellordner in Bytes. */
    fun freeBytes(): Long = dir()?.usableSpace ?: 0L

    /** Arbeitsspeicher des Handys in Bytes. */
    fun deviceRamBytes(): Long {
        val am = context.getSystemService(ActivityManager::class.java) ?: return 0L
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem
    }

    /** Arbeitsspeicher in GB, wie ihn der Hersteller angibt (z. B. 15,2 → 16). */
    fun deviceRamGb(): Int {
        val gb = deviceRamBytes() / 1e9
        return listOf(2, 3, 4, 6, 8, 12, 16, 18, 24, 32).firstOrNull { gb <= it * 1.02 } ?: gb.toInt()
    }

    /** Die KI braucht Android 12 oder neuer (ältere Geräte haben ohnehin zu wenig Speicher). */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Belegter Speicher eines Modells (Datei und Zwischenspeicher daneben). */
    private fun usedBytes(model: AiModel): Long =
        dir()?.listFiles()?.filter { it.name.startsWith(model.file) && !it.name.endsWith(PART) }?.sumOf { it.length() } ?: 0L

    private fun set(model: AiModel, state: ModelState) = _states.update { it + (model to state) }

    /** Zustände neu bestimmen (beim Öffnen und regelmäßig während eines Downloads). */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        for (model in AiModel.entries) refreshOne(model)
    }

    private suspend fun refreshOne(model: AiModel) {
        val id = prefs.getLong(model.id, -1L)
        if (id >= 0) {
            val q = query(id)
            when (q?.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    finishDownload(model, id, q.localPath)
                    return
                }
                DownloadManager.STATUS_FAILED -> {
                    forget(model, removeDownload = true)
                    partFile(model)?.delete()
                    set(model, ModelState.Failed(failureText(q.reason)))
                    return
                }
                DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {
                    val waiting = when {
                        q.status == DownloadManager.STATUS_PENDING -> "Wartet auf den Start …"
                        q.status != DownloadManager.STATUS_PAUSED -> null
                        q.reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Wartet auf WLAN"
                        q.reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Wartet auf Internet"
                        q.reason == DownloadManager.PAUSED_WAITING_TO_RETRY -> "Verbindung unterbrochen – versucht es gleich wieder"
                        else -> "Pausiert"
                    }
                    set(model, ModelState.Downloading(q.bytes, if (q.total > 0) q.total else model.bytes, waiting))
                    return
                }
                null -> forget(model, removeDownload = false)
            }
        }
        val current = _states.value[model]
        if (current is ModelState.Verifying) return
        val f = file(model)
        when {
            f == null -> set(model, ModelState.Missing)
            isReady(model) -> set(model, ModelState.Ready(usedBytes(model)))
            // Datei ohne Prüfvermerk (z. B. per Kabel kopiert): einmal prüfen.
            f.exists() && f.length() == model.bytes -> verify(model, f)
            else -> if (current !is ModelState.Failed) set(model, ModelState.Missing)
        }
    }

    private data class Query(val status: Int, val reason: Int, val bytes: Long, val total: Long, val localPath: String?)

    private fun query(id: Long): Query? = try {
        downloads?.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (!c.moveToFirst()) return@use null
            val uri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            Query(
                status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                bytes = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                localPath = uri?.let { Uri.parse(it).path },
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Download-Abfrage fehlgeschlagen", e)
        null
    }

    private fun failureText(reason: Int): String = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Nicht genug Speicherplatz auf dem Handy."
        DownloadManager.ERROR_HTTP_DATA_ERROR, DownloadManager.ERROR_CANNOT_RESUME -> "Die Verbindung ist abgebrochen. Bitte noch einmal starten."
        DownloadManager.ERROR_TOO_MANY_REDIRECTS, DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Der Server hat den Download abgelehnt. Bitte später noch einmal versuchen."
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Der Speicher des Handys ist gerade nicht erreichbar."
        in 400..599 -> "Der Server antwortet mit Fehler $reason. Bitte später noch einmal versuchen."
        else -> "Der Download hat nicht geklappt (Fehler $reason). Bitte noch einmal starten."
    }

    private fun forget(model: AiModel, removeDownload: Boolean) {
        val id = prefs.getLong(model.id, -1L)
        prefs.edit().remove(model.id).apply()
        if (removeDownload && id >= 0) runCatching { downloads?.remove(id) }
    }

    private suspend fun finishDownload(model: AiModel, id: Long, localPath: String?) {
        val target = file(model) ?: return
        val part = localPath?.let { File(it) }?.takeIf { it.exists() } ?: partFile(model)
        if (part == null || !part.exists()) {
            forget(model, removeDownload = true)
            set(model, ModelState.Failed("Die geladene Datei fehlt. Bitte noch einmal laden."))
            return
        }
        target.delete()
        okFile(model)?.delete()
        if (!part.renameTo(target)) {
            set(model, ModelState.Failed("Die Datei konnte nicht gespeichert werden."))
            return
        }
        // Erst nach dem Umbenennen austragen – sonst löscht der Download-Manager die Datei.
        forget(model, removeDownload = true)
        verify(model, target)
    }

    /** Größe und Prüfsumme kontrollieren; eine beschädigte Datei wird gelöscht. */
    private suspend fun verify(model: AiModel, f: File) = verifyMutex.withLock {
        if ((model in verifiedInMemory || okFile(model)?.exists() == true) && f.length() == model.bytes) {
            set(model, ModelState.Ready(usedBytes(model)))
            return@withLock
        }
        set(model, ModelState.Verifying(0f))
        val ok = withContext(Dispatchers.IO) {
            if (f.length() != model.bytes) return@withContext false
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8 * 1024 * 1024)
            var done = 0L
            var lastShown = 0f
            f.inputStream().use { input ->
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                    done += n
                    val fraction = done.toFloat() / model.bytes
                    if (fraction - lastShown >= 0.02f) {
                        lastShown = fraction
                        set(model, ModelState.Verifying(fraction))
                    }
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) } == model.sha256
        }
        if (!ok) verifiedInMemory -= model
        if (ok) {
            val marked = runCatching { okFile(model)?.writeText(model.sha256) }.isSuccess
            if (!marked) Log.w(TAG, "Prüfvermerk für ${model.id} konnte nicht geschrieben werden")
            verifiedInMemory += model
            set(model, ModelState.Ready(usedBytes(model)))
        } else {
            f.delete()
            okFile(model)?.delete()
            set(model, ModelState.Failed("Die Datei war beschädigt und wurde gelöscht. Bitte noch einmal laden."))
        }
    }

    /** Download starten. Gibt eine Fehlermeldung zurück oder null, wenn er läuft. */
    suspend fun download(model: AiModel, wifiOnly: Boolean): String? = withContext(Dispatchers.IO) {
        if (!supported) return@withContext "Die KI braucht Android 12 oder neuer."
        val dm = downloads ?: return@withContext "Der Download-Manager von Android ist nicht verfügbar."
        dir() ?: return@withContext "Der Speicher des Handys ist gerade nicht erreichbar."
        val needed = model.bytes + 300_000_000L - (partFile(model)?.length() ?: 0L)
        if (freeBytes() < needed) {
            return@withContext "Zu wenig freier Speicher: Es werden ${AiModel.gigabytes(needed)} gebraucht, frei sind ${AiModel.gigabytes(freeBytes())}."
        }
        if (prefs.getLong(model.id, -1L) >= 0) return@withContext null
        partFile(model)?.delete()
        val request = DownloadManager.Request(Uri.parse(model.url))
            .setTitle("KI-Modell ${model.title}")
            .setDescription("Für den KI-Coach von Mental Math Plus · ${model.sizeLabel}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, DIR, model.file + PART)
            .setAllowedOverMetered(!wifiOnly)
            .setAllowedOverRoaming(false)
        return@withContext try {
            val id = dm.enqueue(request)
            prefs.edit().putLong(model.id, id).apply()
            set(model, ModelState.Downloading(0, model.bytes))
            null
        } catch (e: Exception) {
            Log.e(TAG, "Download konnte nicht gestartet werden", e)
            "Der Download konnte nicht gestartet werden: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    /** Laufenden Download abbrechen. */
    suspend fun cancel(model: AiModel) = withContext(Dispatchers.IO) {
        forget(model, removeDownload = true)
        partFile(model)?.delete()
        set(model, ModelState.Missing)
    }

    /** Modell und seinen Zwischenspeicher löschen. */
    suspend fun delete(model: AiModel) = withContext(Dispatchers.IO) {
        verifiedInMemory -= model
        forget(model, removeDownload = true)
        dir()?.listFiles()?.filter { it.name.startsWith(model.file) }?.forEach { it.delete() }
        set(model, ModelState.Missing)
    }

    /** Fehlermeldung zurücksetzen (z. B. nach dem Lesen). */
    fun clearFailure(model: AiModel) {
        if (_states.value[model] is ModelState.Failed) set(model, ModelState.Missing)
    }

    companion object {
        private const val TAG = "ModelStore"
        const val DIR = "models"
        const val PART = ".download"
        const val OK = ".ok"
    }
}
