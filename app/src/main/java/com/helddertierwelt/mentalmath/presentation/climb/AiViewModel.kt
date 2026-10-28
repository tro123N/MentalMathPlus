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

package com.helddertierwelt.mentalmath.presentation.climb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.climb.ClimbRepository
import com.helddertierwelt.mentalmath.data.climb.PlusSettingsRepository
import com.helddertierwelt.mentalmath.data.climb.ai.AiChatRepository
import com.helddertierwelt.mentalmath.data.climb.ai.AiEngine
import com.helddertierwelt.mentalmath.data.climb.ai.EngineStatus
import com.helddertierwelt.mentalmath.data.climb.ai.ModelState
import com.helddertierwelt.mentalmath.data.climb.ai.ModelStore
import com.helddertierwelt.mentalmath.domain.climb.ai.AgentEvent
import com.helddertierwelt.mentalmath.domain.climb.ai.AiAgent
import com.helddertierwelt.mentalmath.domain.climb.ai.AiContext
import com.helddertierwelt.mentalmath.domain.climb.ai.AiConversation
import com.helddertierwelt.mentalmath.domain.climb.ai.AiHistory
import com.helddertierwelt.mentalmath.domain.climb.ai.AiLookup
import com.helddertierwelt.mentalmath.domain.climb.curriculum.Curriculum
import com.helddertierwelt.mentalmath.domain.climb.ai.AiMessage
import com.helddertierwelt.mentalmath.domain.climb.ai.AiModel
import com.helddertierwelt.mentalmath.domain.climb.ai.AiPrompt
import com.helddertierwelt.mentalmath.domain.climb.ai.AiQuiz
import com.helddertierwelt.mentalmath.domain.climb.ai.AiRole
import com.helddertierwelt.mentalmath.domain.climb.ai.AiStatus
import com.helddertierwelt.mentalmath.domain.climb.ai.AiTaskContext
import com.helddertierwelt.mentalmath.domain.climb.ai.AiText
import com.helddertierwelt.mentalmath.domain.climb.ai.AiToolUse
import com.helddertierwelt.mentalmath.domain.climb.ai.AiToolbox
import com.helddertierwelt.mentalmath.domain.climb.ai.LlmSession
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackController
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TimeZone
import javax.inject.Inject
import kotlin.random.Random

/** Steuert den KI-Coach: Gespräch, Modell, Download und die Vorschläge der KI. */
@HiltViewModel
class AiViewModel @Inject constructor(
    private val engine: AiEngine,
    private val store: ModelStore,
    private val chats: AiChatRepository,
    private val repository: ClimbRepository,
    private val settingsRepository: PlusSettingsRepository,
    private val feedbackController: FeedbackController,
) : ViewModel() {

    val engineStatus: StateFlow<EngineStatus> = engine.status
    val crashNote: StateFlow<String?> = engine.crashNote
    val models: StateFlow<Map<AiModel, ModelState>> = store.states
    val settings = settingsRepository.settings
    val progress = repository.progress

    private val _chat = MutableStateFlow(AiConversation())
    val chat: StateFlow<AiConversation> = _chat.asStateFlow()

    private val _chatLoaded = MutableStateFlow(false)
    val chatLoaded: StateFlow<Boolean> = _chatLoaded.asStateFlow()

    /** Aufgabe aus der App, über die gesprochen wird. */
    private val _focus = MutableStateFlow<AiTaskContext?>(null)
    val focus: StateFlow<AiTaskContext?> = _focus.asStateFlow()
    private var focusSent = false

    /** Level, aus dem heraus gefragt wird (ID), null = kein bestimmtes. */
    private val _topic = MutableStateFlow<String?>(null)
    val topic: StateFlow<String?> = _topic.asStateFlow()

    /** Die KI schreibt gerade. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Was die KI gerade tut, z. B. „Rechnet nach …“ (null = schreibt bzw. nichts). */
    private val _activity = MutableStateFlow<String?>(null)
    val activity: StateFlow<String?> = _activity.asStateFlow()

    /** Kurze Meldung für die Person (z. B. Fehler beim Download). */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var session: LlmSession? = null
    private var sessionModel: AiModel? = null
    private val sessionMutex = Mutex()
    private var job: Job? = null
    private var pollJob: Job? = null
    private var lastProgress: ClimbProgress? = null
    private val random = Random(System.nanoTime())

    init {
        viewModelScope.launch {
            _chat.value = chats.load()
            _chatLoaded.value = true
        }
        viewModelScope.launch { store.refresh() }
    }

    // --- Modell ----------------------------------------------------------------------------------

    val supported: Boolean get() = store.supported

    fun deviceRamGb(): Int = store.deviceRamGb()

    fun freeBytes(): Long = store.freeBytes()

    fun crashedEverywhere(model: AiModel): Boolean = engine.crashedEverywhere(model)

    /** Gewähltes Modell aus den Einstellungen. */
    val selectedModel: AiModel get() = AiModel.chosen(settings.value.aiModel)

    /**
     * Das Modell, mit dem gesprochen wird: das gewählte, wenn es geladen ist und auf dem Handy
     * läuft – sonst ein anderes, schon geladenes Modell.
     */
    fun activeModel(): AiModel? {
        val selected = selectedModel
        fun usable(m: AiModel) = m.phone && store.isReady(m) && !engine.crashedEverywhere(m) && m !in failedThisRun
        if (usable(selected)) return selected
        return AiModel.fallbackOrder.firstOrNull { usable(it) }
    }

    /** Modelle, die sich in diesem Programmlauf nicht laden ließen (nicht bei jeder Nachricht neu versuchen). */
    private val failedThisRun = mutableSetOf<AiModel>()

    fun select(model: AiModel) {
        if (!model.phone) return
        failedThisRun -= model
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch { settingsRepository.update { it.copy(aiModel = model.id) } }
    }

    fun download(model: AiModel) {
        if (!model.phone) return
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch {
            settingsRepository.update { it.copy(aiModel = model.id) }
            store.download(model, settings.value.aiWifiOnly)?.let { _notice.value = it }
            startPolling()
        }
    }

    fun cancelDownload(model: AiModel) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch { store.cancel(model) }
    }

    fun delete(model: AiModel) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch {
            if (engine.loadedModel == model) {
                session = null
                engine.release()
            }
            store.delete(model)
        }
    }

    fun clearFailure(model: AiModel) = store.clearFailure(model)

    fun setThinking(on: Boolean) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch { settingsRepository.update { it.copy(aiThinking = on) } }
    }

    fun setFast(on: Boolean) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch {
            settingsRepository.update { it.copy(aiFast = on) }
            // Wird beim nächsten Laden übernommen.
            if (!_busy.value) {
                session = null
                engine.release()
            }
        }
    }

    fun setWifiOnly(on: Boolean) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch { settingsRepository.update { it.copy(aiWifiOnly = on) } }
    }

    fun clearNotice() {
        _notice.value = null
    }

    fun clearCrashNote() = engine.clearCrashNote()

    /** Abstürze vergessen und alles noch einmal versuchen. */
    fun resetEngine() {
        feedbackController.play(FeedbackType.TAP)
        failedThisRun.clear()
        engine.forgetCrashes()
        viewModelScope.launch {
            session = null
            engine.release()
        }
    }

    /** Während Downloads und Prüfung regelmäßig nachsehen. */
    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                store.refresh()
                val active = store.states.value.values.any { it is ModelState.Downloading || it is ModelState.Verifying }
                if (!active) break
                delay(1_000)
            }
        }
    }

    // --- Sichtbarkeit (Speicher sparen) ----------------------------------------------------------

    /**
     * Der KI-Bildschirm ist sichtbar: Modell im Speicher behalten und Downloads verfolgen.
     * Mit [preload] wird das Modell schon geladen, damit die erste Antwort schneller kommt.
     */
    fun onVisible(preload: Boolean = true) {
        engine.cancelRelease()
        wantsPreload = preload
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            store.refresh()
            startPolling()
            if (!preload) return@launch
            // Läuft gerade ein Download oder die Prüfung der Datei (z. B. gleich nach dem App-Start),
            // erst danach laden – sonst bliebe die KI bis zur ersten Frage ungeladen.
            store.states.first { states -> states.values.none { it is ModelState.Downloading || it is ModelState.Verifying } }
            val model = activeModel() ?: return@launch
            if (wantsPreload && engine.status.value !is EngineStatus.Loading) {
                if (engine.load(model, settings.value.aiFast)) prepareSession(model)
            }
        }
    }

    /** Der KI-Bildschirm ist verdeckt: das Modell nach einer Weile freigeben (es braucht viel Speicher). */
    fun onHidden(delayMs: Long = RELEASE_DELAY_MS) {
        wantsPreload = false
        preloadJob?.cancel()
        engine.scheduleRelease(delayMs)
    }

    /** Soll das Modell vorab geladen werden (Gesprächsbildschirm sichtbar)? */
    @Volatile
    private var wantsPreload = false
    private var preloadJob: Job? = null

    // --- Gespräch --------------------------------------------------------------------------------

    private fun localToday(now: Long): Long = (now + TimeZone.getDefault().getOffset(now)) / Progression.DAY_MS

    /** Aktueller Stand der App für die Werkzeuge. */
    fun context(): AiContext {
        val now = System.currentTimeMillis()
        return AiContext(
            progress = repository.progress.value,
            settings = settingsRepository.settings.value,
            now = now,
            today = localToday(now),
            offsetMs = TimeZone.getDefault().getOffset(now).toLong(),
            focus = _focus.value,
            quiz = _chat.value.quiz,
            random = random,
        )
    }

    fun suggestions(): List<String> {
        val found = _topic.value?.let { AiLookup.locate(Curriculum.worlds, it) }
        return if (found != null && _focus.value == null) AiPrompt.topicSuggestions(found) else AiPrompt.suggestions(context())
    }

    /** Gespräch über eine Aufgabe aus der App beginnen (null = allgemeines Gespräch). */
    fun setFocus(task: AiTaskContext?) {
        _focus.value = task
        focusSent = false
        _topic.value = null
    }

    /** Gespräch zu einem Level beginnen (aus dem Level-Bildschirm). */
    fun setTopic(skillId: String?) {
        _topic.value = skillId
        _focus.value = null
        focusSent = false
    }

    /** Name des Levels, aus dem gefragt wird, z. B. „Welt 6, Level 1: Additionsmethode“. */
    fun topicName(skillId: String): String? = AiLookup.locate(Curriculum.worlds, skillId)?.name

    private fun historyBudget(model: AiModel): Int =
        (engine.contextTokens(model) - engine.reserveTokens(model) - SYSTEM_TOKENS).coerceIn(400, 6_000)

    /** Gespräch im Modell anlegen (mit dem bisherigen Verlauf als Gedächtnis). */
    private suspend fun prepareSession(model: AiModel, exclude: Set<Long> = emptySet()): LlmSession = sessionMutex.withLock {
        val current = session
        val used = current?.usedTokens() ?: -1
        val full = used >= 0 && used > engine.contextTokens(model) - engine.reserveTokens(model)
        if (current != null && engine.isCurrent(current) && sessionModel == model && !full) return@withLock current
        val history = AiHistory.recent(_chat.value.messages.filter { it.id !in exclude }, historyBudget(model))
        val s = engine.newSession(AiPrompt.system(context()), history)
        session = s
        sessionModel = model
        lastProgress = repository.progress.value
        s
    }

    private fun updateMessage(id: Long, change: (AiMessage) -> AiMessage) {
        _chat.update { c -> c.copy(messages = c.messages.map { if (it.id == id) change(it) else it }) }
    }

    private fun save() {
        val c = _chat.value
        viewModelScope.launch { chats.save(c) }
    }

    private class UserFacing(message: String) : Exception(message)

    /** Nachricht der Person an die KI schicken. */
    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || _busy.value) return
        feedbackController.play(FeedbackType.TAP)
        val focus = _focus.value
        val withTask = focus != null && !focusSent
        if (withTask) focusSent = true
        val before = _chat.value
        val userId = before.nextId
        val replyId = before.nextId + 1
        val topic = _topic.value?.takeIf { !withTask }?.let { AiLookup.locate(Curriculum.worlds, it) }
        if (topic != null) _topic.value = null
        val question = when {
            withTask -> AiPrompt.taskQuestion(focus!!, text)
            topic != null -> AiPrompt.topicNote(topic) + "\n\n" + text
            else -> text
        }
        val card = when {
            withTask -> focus!!.card
            topic != null -> topic.name
            else -> null
        }
        val user = AiMessage(userId, AiRole.USER, text, prompt = question.takeIf { it != text }, task = card)
        val reply = AiMessage(replyId, AiRole.ASSISTANT, "", status = AiStatus.STREAMING)
        _chat.value = before.copy(messages = before.messages + user + reply, nextId = before.nextId + 2)
        // Gleich speichern: Wird die App im Hintergrund beendet, bleibt die Frage erhalten.
        save()
        run(user, replyId)
    }

    private fun run(user: AiMessage, replyId: Long) {
        _busy.value = true
        job = viewModelScope.launch {
            val text = StringBuilder()
            val thought = StringBuilder()
            try {
                val model = loadWithFallback() ?: throw UserFacing(
                    (engine.status.value as? EngineStatus.Failed)?.message ?: "Es ist noch kein KI-Modell auf dem Handy. Lade zuerst eins herunter.",
                )
                if (!engine.isCurrent(session)) _activity.value = "Bereitet das Gespräch vor …"
                var s = prepareSession(model, exclude = setOf(user.id, replyId))
                // Hat die Person seit der letzten Antwort weiter geübt, bekommt die KI einen kurzen Hinweis.
                val now = repository.progress.value
                val note = lastProgress?.let { AiPrompt.progressNote(it, now) }
                lastProgress = now
                val prompt = if (note != null) note + "\n\n" + user.modelText else user.modelText
                val ready = engine.status.value as? EngineStatus.Ready
                val thinking = settings.value.aiThinking && ready?.thinking != false
                var attempt = 0
                while (true) {
                    _activity.value = if (thinking) "Denkt nach …" else "Schreibt …"
                    try {
                        collectAnswer(s, prompt, thinking, replyId, text, thought)
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        // Steht noch kein Text da (z. B. war ein Werkzeugaufruf des Modells nicht lesbar),
                        // einmal still mit einem frischen Gespräch neu versuchen – mit neuem Startwert
                        // kommt dabei eine andere Antwort heraus.
                        if (attempt > 0 || text.isNotEmpty()) throw e
                        attempt++
                        session = null
                        thought.setLength(0)
                        updateMessage(replyId) { it.copy(thought = "", tools = emptyList(), actions = emptyList()) }
                        _activity.value = "Versucht es noch einmal …"
                        s = prepareSession(model, exclude = setOf(user.id, replyId))
                    }
                }
                val final = AiText.clean(text.toString())
                val quiz = _chat.value.quiz
                updateMessage(replyId) { m ->
                    val shown = final.ifBlank {
                        when {
                            m.quiz.isNotEmpty() -> "Hier ist deine Aufgabe:"
                            m.actions.isNotEmpty() -> "Ich habe dir etwas vorbereitet:"
                            else -> "Dazu fällt mir gerade nichts ein. Frag mich bitte noch einmal anders."
                        }
                    }
                    // Fürs Gedächtnis des Modells (z. B. nach einem Neustart): welche Aufgaben als Karte dastanden.
                    val notes = m.quiz.mapNotNull { id -> quiz.firstOrNull { it.id == id }?.let { AiQuiz.card(it) }?.let { AiQuiz.note(it) } }
                    m.copy(
                        text = shown,
                        prompt = if (notes.isEmpty()) m.prompt else shown + "\n\n" + notes.joinToString("\n"),
                        thought = AiText.clean(thought.toString()),
                        status = AiStatus.DONE,
                    )
                }
            } catch (e: CancellationException) {
                updateMessage(replyId) { it.copy(text = AiText.clean(text.toString()), thought = AiText.clean(thought.toString()), status = AiStatus.STOPPED) }
                throw e
            } catch (e: UserFacing) {
                updateMessage(replyId) { it.copy(status = AiStatus.ERROR, error = e.message) }
            } catch (e: Throwable) {
                // Nach einem Fehler beim nächsten Mal ein frisches Gespräch im Modell anlegen.
                session = null
                updateMessage(replyId) {
                    it.copy(
                        text = AiText.clean(text.toString()),
                        status = AiStatus.ERROR,
                        error = "Die KI ist ins Stocken geraten (${e.message?.take(120) ?: e.javaClass.simpleName}). Tippe auf „Nochmal“.",
                    )
                }
            } finally {
                _busy.value = false
                _activity.value = null
                save()
            }
        }
    }

    /** Eine Antwort des Modells (mit Werkzeugen) abholen und laufend in die Nachricht [replyId] schreiben. */
    private suspend fun collectAnswer(s: LlmSession, prompt: String, thinking: Boolean, replyId: Long, text: StringBuilder, thought: StringBuilder) {
        var lastSave = System.currentTimeMillis()
        AiAgent.answer(s, prompt, thinking) { context() }.collect { e ->
            // Alle paar Sekunden speichern, damit eine halbe Antwort nicht verloren geht.
            val now = System.currentTimeMillis()
            if (now - lastSave > 3_000) {
                lastSave = now
                save()
            }
            when (e) {
                is AgentEvent.Text -> {
                    text.append(e.text)
                    _activity.value = null
                    updateMessage(replyId) { it.copy(text = text.toString()) }
                }
                is AgentEvent.Thought -> {
                    thought.append(e.text)
                    updateMessage(replyId) { it.copy(thought = thought.toString()) }
                }
                is AgentEvent.ToolStarted -> _activity.value = toolActivity(e.name)
                is AgentEvent.ToolFinished -> {
                    val o = e.outcome
                    o.quiz?.let { q -> _chat.update { it.copy(quiz = q) } }
                    // Neue Aufgabe: erscheint als Karte unter der Antwort (statt als kleiner Hinweis).
                    val posed = if (o.name == AiToolbox.CREATE_TASK && o.ok) o.quiz?.lastOrNull()?.id else null
                    updateMessage(replyId) { m ->
                        m.copy(
                            tools = if (o.label.isEmpty() || posed != null) m.tools else m.tools + AiToolUse(o.name, o.label, o.ok),
                            actions = o.action?.let { a -> if (a in m.actions) m.actions else m.actions + a } ?: m.actions,
                            quiz = if (posed != null && posed !in m.quiz) m.quiz + posed else m.quiz,
                        )
                    }
                    // Nach dem Werkzeug geht es mit Denken oder Schreiben weiter.
                    if (text.isEmpty()) _activity.value = if (thinking) "Denkt nach …" else "Schreibt …"
                }
            }
        }
    }

    /**
     * Gewähltes Modell laden. Klappt das nicht (z. B. reicht beim 12B-Modell der Speicher nicht),
     * wird ein anderes, schon geladenes Modell genommen. Null, wenn keins geht.
     */
    private suspend fun loadWithFallback(): AiModel? {
        val first = activeModel() ?: return null
        if (engine.status.value !is EngineStatus.Ready || engine.loadedModel != first) _activity.value = "Lädt die KI …"
        if (engine.load(first, settings.value.aiFast)) return first
        failedThisRun += first
        val reason = (engine.status.value as? EngineStatus.Failed)?.message
        for (other in AiModel.fallbackOrder) {
            if (other == first || !store.isReady(other) || engine.crashedEverywhere(other)) continue
            _activity.value = "Lädt ${other.title} …"
            if (engine.load(other, settings.value.aiFast)) {
                _notice.value = "${first.title} ließ sich nicht laden" + (reason?.let { " ($it)" } ?: "") + ". Ich antworte mit ${other.title}."
                return other
            }
            failedThisRun += other
        }
        return null
    }

    /** Laufende Antwort abbrechen. */
    fun stop() {
        feedbackController.play(FeedbackType.TAP)
        session?.cancel()
        job?.cancel()
    }

    /** Letzte Antwort neu erzeugen (nach Fehler oder Abbruch). */
    fun retry() {
        if (_busy.value) return
        val messages = _chat.value.messages
        val lastUser = messages.lastOrNull { it.role == AiRole.USER } ?: return
        val after = messages.dropWhile { it.id != lastUser.id }.drop(1)
        val replyId = _chat.value.nextId
        val reply = AiMessage(replyId, AiRole.ASSISTANT, "", status = AiStatus.STREAMING)
        _chat.update { c -> c.copy(messages = c.messages.filter { m -> after.none { it.id == m.id } } + reply, nextId = c.nextId + 1) }
        // Abgebrochene Antworten stecken noch im Gedächtnis des Modells: frisch anfangen.
        session = null
        save()
        run(lastUser, replyId)
    }

    /** Neues Gespräch: Verlauf löschen. */
    fun newChat() {
        feedbackController.play(FeedbackType.TAP)
        stop()
        session = null
        _focus.value = null
        _topic.value = null
        focusSent = false
        _chat.value = AiConversation()
        viewModelScope.launch { chats.clear() }
    }

    /** Ein Vorschlag wurde ausgeführt: Knopf als erledigt zeigen. */
    fun markDone(messageId: Long, index: Int) {
        updateMessage(messageId) { if (index in it.done) it else it.copy(done = it.done + index) }
        save()
    }

    fun playTap() = feedbackController.play(FeedbackType.TAP)

    override fun onCleared() {
        engine.scheduleRelease(0)
        super.onCleared()
    }

    companion object {
        /** So lange bleibt das Modell nach dem Verlassen des KI-Bildschirms geladen. */
        const val RELEASE_DELAY_MS = 180_000L

        /** Ungefähre Größe von Systemanweisung und Werkzeugbeschreibungen in Tokens. */
        const val SYSTEM_TOKENS = 2_400

        fun toolActivity(name: String): String = when (name) {
            AiToolbox.CALCULATE -> "Rechnet nach …"
            AiToolbox.TMSNAT_STATUS -> "Schaut in deinen TMSnat-Lernstand …"
            AiToolbox.CLIMB_STATUS -> "Schaut in deinen Kletterweg …"
            AiToolbox.SEARCH_METHODS -> "Schlägt im Handbuch nach …"
            AiToolbox.CREATE_TASK -> "Denkt sich eine Aufgabe aus …"
            AiToolbox.CHECK_ANSWER -> "Prüft deine Antwort …"
            AiToolbox.GET_HINT -> "Sucht einen Tipp …"
            AiToolbox.CHANGE_SETTING -> "Bereitet die Einstellung vor …"
            else -> "Bereitet eine Übung vor …"
        }
    }
}
