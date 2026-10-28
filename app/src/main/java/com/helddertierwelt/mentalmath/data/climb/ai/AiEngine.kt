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
import android.os.SystemClock
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.ModelInfo
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.tool
import com.helddertierwelt.mentalmath.domain.climb.ai.AiMessage
import com.helddertierwelt.mentalmath.domain.climb.ai.AiModel
import com.helddertierwelt.mentalmath.domain.climb.ai.AiRole
import com.helddertierwelt.mentalmath.domain.climb.ai.AiToolbox
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmChunk
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmInput
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmSession
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmToolCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** Zustand der KI auf dem Handy. */
sealed interface EngineStatus {
    /** Nicht geladen (spart Arbeitsspeicher). */
    data object Off : EngineStatus

    /** Wird geladen; [step] beschreibt, was gerade passiert. */
    data class Loading(val model: AiModel, val step: String) : EngineStatus

    /** Bereit. [backend] ist „GPU“ (Grafikeinheit) oder „CPU“ (Prozessor). */
    data class Ready(
        val model: AiModel,
        val backend: String,
        val fast: Boolean,
        val thinking: Boolean,
        val tools: Boolean,
        val loadMs: Long,
    ) : EngineStatus

    data class Failed(val model: AiModel, val message: String) : EngineStatus
}

/**
 * Die KI selbst: lädt ein Modell mit LiteRT-LM (zuerst auf der Grafikeinheit, sonst auf dem
 * Prozessor) und führt Gespräche. Läuft komplett auf dem Handy, ohne Internet.
 *
 * Beendet das Laden eines Modells die App (z. B. weil der Arbeitsspeicher nicht reicht), merkt
 * sich die Engine das und versucht es beim nächsten Mal sparsamer bzw. meldet es.
 */
@Singleton
class AiEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: ModelStore,
) {
    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Off)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    /** Hinweis, wenn das Laden beim letzten Mal die App beendet hat. */
    private val _crashNote = MutableStateFlow<String?>(null)
    val crashNote: StateFlow<String?> = _crashNote.asStateFlow()

    private val dispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "ki-engine") }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val busy = AtomicInteger(0)

    private var engine: Engine? = null
    private var engineModel: AiModel? = null
    private var engineFast = false
    private var supportsThinking = true
    private var supportsTools = true
    private var conversation: Conversation? = null
    @Volatile
    private var session: LiteRtSession? = null
    private var releaseJob: Job? = null

    private val marker = File(context.filesDir, "ai_loading.txt")
    private val badFile = File(context.filesDir, "ai_bad.txt")

    init {
        // Lag beim letzten Start ein Ladevermerk, ist die App beim Laden abgestürzt.
        if (marker.exists()) {
            val combo = runCatching { marker.readText().trim() }.getOrDefault("")
            marker.delete()
            if (combo.isNotEmpty()) {
                runCatching { badFile.appendText(combo + "\n") }
                val model = AiModel.of(combo.substringBefore('|'))
                _crashNote.value = "Beim letzten Laden von ${model?.title ?: "der KI"} wurde die App beendet – vermutlich reichte der Arbeitsspeicher nicht. " +
                    "Die App versucht es jetzt sparsamer."
            }
        }
    }

    fun clearCrashNote() {
        _crashNote.value = null
    }

    private fun badCombos(): Set<String> = runCatching { badFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet() }.getOrDefault(emptySet())

    /** Ist ein Modell auf diesem Handy schon auf Grafikeinheit und Prozessor beim Laden abgestürzt? */
    fun crashedEverywhere(model: AiModel): Boolean {
        val bad = badCombos()
        return "${model.id}|GPU|false" in bad && "${model.id}|CPU|false" in bad
    }

    /** Vermerke über Abstürze löschen (z. B. nach einem App-Update). */
    fun forgetCrashes() {
        badFile.delete()
        _crashNote.value = null
    }

    val loadedModel: AiModel? get() = (status.value as? EngineStatus.Ready)?.model

    /** Modell laden, falls es nicht schon mit diesen Einstellungen geladen ist. */
    suspend fun load(model: AiModel, fast: Boolean): Boolean = mutex.withLock {
        cancelRelease()
        if (engine != null && engineModel == model && engineFast == fast && status.value is EngineStatus.Ready) return@withLock true
        withContext(dispatcher) { loadLocked(model, fast) }
    }

    @OptIn(ExperimentalApi::class)
    private fun loadLocked(model: AiModel, fast: Boolean): Boolean {
        closeLocked()
        val file = store.file(model)
        if (file == null || !store.isReady(model)) {
            _status.value = EngineStatus.Failed(model, "Das Modell ist noch nicht geladen.")
            return false
        }
        _status.value = EngineStatus.Loading(model, "Modell wird gelesen …")
        val path = file.absolutePath
        var mtp = false
        try {
            ModelInfo.from(path).use { info ->
                (info as? ModelInfo.Llm)?.let {
                    mtp = it.hasSpeculativeDecodingSupport()
                    supportsThinking = it.supportsThinking()
                    supportsTools = it.supportsFunctionCalling()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Modellinfo nicht lesbar", t)
            supportsThinking = true
            supportsTools = true
        }
        val bad = badCombos()
        data class Attempt(val backend: String, val mtp: Boolean)
        val attempts = buildList {
            if (!forceCpu) {
                if (fast && mtp) add(Attempt("GPU", true))
                add(Attempt("GPU", false))
            }
            if (fast && mtp) add(Attempt("CPU", true))
            add(Attempt("CPU", false))
        }.filter { "${model.id}|${it.backend}|${it.mtp}" !in bad }
        if (attempts.isEmpty()) {
            _status.value = EngineStatus.Failed(model, "${model.title} lässt sich auf diesem Handy nicht laden – beim Laden wurde die App mehrfach beendet. Nimm ein kleineres Modell.")
            return false
        }
        var lastError: Throwable? = null
        for (a in attempts) {
            _status.value = EngineStatus.Loading(
                model,
                if (a.backend == "GPU") "Wird auf die Grafikeinheit geladen …" else "Wird auf den Prozessor geladen …",
            )
            val started = SystemClock.elapsedRealtime()
            runCatching { marker.writeText("${model.id}|${a.backend}|${a.mtp}") }
            try {
                ExperimentalFlags.enableSpeculativeDecoding = a.mtp
                val e = Engine(
                    EngineConfig(
                        modelPath = path,
                        backend = if (a.backend == "GPU") Backend.GPU() else Backend.CPU(),
                        maxNumTokens = contextOverride ?: model.contextTokens,
                    ),
                )
                e.initialize()
                engine = e
                engineModel = model
                engineFast = fast
                _status.value = EngineStatus.Ready(model, a.backend, a.mtp, supportsThinking, supportsTools, SystemClock.elapsedRealtime() - started)
                Log.i(TAG, "${model.id} geladen: ${a.backend}, MTP=${a.mtp}, ${SystemClock.elapsedRealtime() - started} ms")
                return true
            } catch (t: Throwable) {
                lastError = t
                Log.w(TAG, "Laden mit ${a.backend} (MTP=${a.mtp}) fehlgeschlagen", t)
            } finally {
                ExperimentalFlags.enableSpeculativeDecoding = null
                marker.delete()
            }
        }
        _status.value = EngineStatus.Failed(model, "Die KI konnte nicht geladen werden (${lastError?.message?.take(160) ?: lastError?.javaClass?.simpleName ?: "unbekannt"}).")
        return false
    }

    /** Neues Gespräch mit Systemanweisung und den bisherigen Nachrichten (für das Gedächtnis). */
    suspend fun newSession(system: String, history: List<AiMessage>): LlmSession = mutex.withLock {
        withContext(dispatcher) {
            val e = engine ?: throw IllegalStateException("Es ist kein Modell geladen.")
            val model = engineModel!!
            waitIdle()
            session = null
            conversation?.let { runCatching { it.close() } }
            conversation = null
            val tools = if (supportsTools) AiToolbox.specs.map { spec ->
                val json = spec.json().toString()
                tool(
                    object : OpenApiTool {
                        override fun getToolDescriptionJsonString(): String = json

                        // Werkzeuge führt die App selbst aus (automaticToolCalling = false).
                        override fun execute(paramsJsonString: String): String = "{}"
                    },
                )
            } else emptyList()
            val c = e.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(system),
                    initialMessages = history.map { m -> if (m.role == AiRole.USER) Message.user(m.modelText) else Message.model(m.modelText) },
                    tools = tools,
                    samplerConfig = if (deterministic) SamplerConfig(topK = 1, topP = 1.0, temperature = 1.0, seed = 1) else SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7),
                    automaticToolCalling = false,
                    maxOutputToken = model.thinkingBudget + ANSWER_TOKENS,
                ),
            )
            conversation = c
            LiteRtSession(c, model.thinkingBudget + ANSWER_TOKENS, model.thinkingBudget, supportsThinking, busy).also { session = it }
        }
    }

    /** Gehört diese Sitzung noch zum geladenen Modell? */
    fun isCurrent(s: LlmSession?): Boolean = s != null && s === session && conversation?.isAlive == true

    /** Platz, der für eine Antwort (mit Denken und Werkzeugen) frei bleiben muss, in Tokens. */
    fun reserveTokens(model: AiModel): Int = model.thinkingBudget + ANSWER_TOKENS + 600

    /** Größe des Gesprächsgedächtnisses des geladenen Modells. */
    fun contextTokens(model: AiModel): Int = contextOverride ?: model.contextTokens

    private fun waitIdle() {
        if (busy.get() == 0) return
        runCatching { conversation?.cancelProcess() }
        val until = SystemClock.elapsedRealtime() + 3_000
        while (busy.get() > 0 && SystemClock.elapsedRealtime() < until) Thread.sleep(20)
    }

    /** Nach [delayMs] freigeben, wenn sie bis dahin nicht gebraucht wird. */
    fun scheduleRelease(delayMs: Long) {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            delay(delayMs)
            while (busy.get() > 0) delay(1_000)
            release()
        }
    }

    fun cancelRelease() {
        releaseJob?.cancel()
        releaseJob = null
    }

    /** Modell aus dem Arbeitsspeicher entfernen. */
    suspend fun release() = mutex.withLock {
        withContext(dispatcher) { closeLocked() }
        _status.value = EngineStatus.Off
    }

    private fun closeLocked() {
        waitIdle()
        session = null
        conversation?.let { runCatching { it.close() } }
        conversation = null
        engine?.let { runCatching { it.close() } }
        engine = null
        engineModel = null
    }

    companion object {
        private const val TAG = "AiEngine"

        /** So viele Tokens darf die eigentliche Antwort (ohne Denken) lang werden. */
        const val ANSWER_TOKENS = 1_536

        /** Für Tests: immer das wahrscheinlichste Wort nehmen (gleiche Frage → gleiche Antwort). */
        @Volatile
        var deterministic: Boolean = false

        /** Für Tests im Emulator: nur den Prozessor benutzen. */
        @Volatile
        var forceCpu: Boolean = false

        /** Für Tests: kleineres Gesprächsgedächtnis (spart Arbeitsspeicher im Emulator). */
        @Volatile
        var contextOverride: Int? = null
    }
}

/** Ein Gespräch mit LiteRT-LM. Werkzeugaufrufe werden an die App zurückgegeben. */
private class LiteRtSession(
    private val conversation: Conversation,
    private val maxOutput: Int,
    private val thinkingBudget: Int,
    private val thinkingSupported: Boolean,
    private val busy: AtomicInteger,
) : LlmSession {

    override fun send(input: LlmInput, thinking: Boolean): Flow<LlmChunk> = callbackFlow {
        val finished = AtomicBoolean(false)
        val think = thinking && thinkingSupported
        val message = when (input) {
            is LlmInput.User -> Message.user(input.text)
            is LlmInput.ToolResults -> Message.tool(
                Contents.of(input.results.map { (name, json) -> Content.ToolResponse(name, jsonToKotlin(parse(json))) }),
            )
        }
        val callback = object : MessageCallback {
            override fun onMessage(message: Message) {
                message.channels[THOUGHT]?.takeIf { it.isNotEmpty() }?.let { trySend(LlmChunk.Thought(it)) }
                val text = message.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
                if (text.isNotEmpty()) trySend(LlmChunk.Text(text))
                if (message.toolCalls.isNotEmpty()) {
                    trySend(LlmChunk.ToolCalls(message.toolCalls.map { LlmToolCall(it.name, kotlinToJson(it.arguments).toString()) }))
                }
            }

            override fun onDone() {
                if (finished.compareAndSet(false, true)) busy.decrementAndGet()
                close()
            }

            override fun onError(throwable: Throwable) {
                if (finished.compareAndSet(false, true)) busy.decrementAndGet()
                if (throwable is java.util.concurrent.CancellationException) close() else close(throwable)
            }
        }
        busy.incrementAndGet()
        try {
            conversation.sendMessageAsync(
                message,
                callback,
                extraContext = mapOf("enable_thinking" to think),
                maxOutputToken = maxOutput,
                thinkingConfig = ThinkingConfig(enableThinking = think, thinkingTokenBudget = if (think) thinkingBudget else -1),
            )
        } catch (t: Throwable) {
            if (finished.compareAndSet(false, true)) busy.decrementAndGet()
            close(t)
        }
        awaitClose {
            if (!finished.get()) runCatching { conversation.cancelProcess() }
        }
    }.buffer(Channel.UNLIMITED)

    override fun cancel() {
        runCatching { conversation.cancelProcess() }
    }

    override fun usedTokens(): Int = runCatching { conversation.getTokenCount() }.getOrDefault(-1)

    companion object {
        /** Kanal, in dem Gemma 4 seine Gedanken schreibt. */
        const val THOUGHT = "thought"

        fun parse(json: String): JsonElement = runCatching { Json.parseToJsonElement(json) }.getOrElse { JsonPrimitive(json) }

        /** JSON in Kotlin-Werte (Map, List, Text, Zahl), wie LiteRT-LM sie erwartet. */
        fun jsonToKotlin(e: JsonElement): Any? = when (e) {
            is JsonNull -> null
            is JsonObject -> e.mapValues { jsonToKotlin(it.value) }
            is JsonArray -> e.map { jsonToKotlin(it) }
            is JsonPrimitive -> when {
                e.isString -> e.content
                e.booleanOrNull != null -> e.booleanOrNull
                e.longOrNull != null -> e.longOrNull
                e.doubleOrNull != null -> e.doubleOrNull
                else -> e.content
            }
        }

        /** Argumente eines Werkzeugaufrufs (aus LiteRT-LM) als JSON. */
        fun kotlinToJson(v: Any?): JsonElement = when (v) {
            null -> JsonNull
            is JsonElement -> v
            is String -> JsonPrimitive(v)
            is Boolean -> JsonPrimitive(v)
            is Number -> v.toString().toBigDecimalOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(v.toString())
            is Map<*, *> -> JsonObject(v.entries.associate { it.key.toString() to kotlinToJson(it.value) })
            is List<*> -> JsonArray(v.map { kotlinToJson(it) })
            else -> JsonPrimitive(v.toString())
        }
    }
}
