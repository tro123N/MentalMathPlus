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

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.climb.ClimbRepository
import com.helddertierwelt.mentalmath.data.climb.PlusSettingsRepository
import com.helddertierwelt.mentalmath.domain.climb.curriculum.Curriculum
import com.helddertierwelt.mentalmath.domain.climb.engine.AdaptiveState
import com.helddertierwelt.mentalmath.domain.climb.engine.ApForecast
import com.helddertierwelt.mentalmath.domain.climb.engine.ApReview
import com.helddertierwelt.mentalmath.domain.climb.engine.FreeMode
import com.helddertierwelt.mentalmath.domain.climb.engine.TrickAdvisor
import com.helddertierwelt.mentalmath.domain.climb.engine.TrickTip
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.engine.FreeRules
import com.helddertierwelt.mentalmath.domain.climb.engine.Phase
import com.helddertierwelt.mentalmath.domain.climb.engine.Placement
import com.helddertierwelt.mentalmath.domain.climb.engine.Progression
import com.helddertierwelt.mentalmath.domain.climb.engine.Session
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionEvent
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionFinisher
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionLogic
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.domain.climb.engine.Summary
import com.helddertierwelt.mentalmath.domain.climb.free.FreeTopic
import com.helddertierwelt.mentalmath.domain.climb.free.FreeTopics
import com.helddertierwelt.mentalmath.domain.climb.model.ApMistakeEntry
import com.helddertierwelt.mentalmath.domain.climb.model.Backup
import com.helddertierwelt.mentalmath.domain.climb.model.BackupCodec
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.Palette
import com.helddertierwelt.mentalmath.domain.climb.model.PlusSettings
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackController
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject
import kotlin.random.Random

/** Steuert den Kletterweg: Fortschritt, laufende Übungseinheit, Zeitmessung und Ergebnis. */
/** Einstellungen des freien Trainings. */
data class FreeConfig(
    val topics: Set<FreeTopic> = setOf(FreeTopic.BASIC),
    val level: Int = 2,
    val mode: FreeMode = FreeMode.ZEIT,
    val param: Int = 60,
) {
    companion object {
        /** Wählbare Werte je Modus: Sekunden, Leben bzw. Aufgaben. */
        fun options(mode: FreeMode): List<Int> = when (mode) {
            FreeMode.ZEIT -> listOf(60, 120, 300)
            FreeMode.UEBERLEBEN -> listOf(1, 3, 5)
            FreeMode.PRUEFUNG -> listOf(10, 20, 30)
            FreeMode.ENDLOS -> listOf(0)
            FreeMode.SIMULATION -> listOf(1_500)
        }
    }
}

@HiltViewModel
class ClimbViewModel @Inject constructor(
    private val repository: ClimbRepository,
    private val settingsRepository: PlusSettingsRepository,
    private val feedbackController: FeedbackController,
) : ViewModel(), DefaultLifecycleObserver {

    val worlds: List<World> = Curriculum.worlds
    val progress: StateFlow<ClimbProgress> = repository.progress
    val loaded: StateFlow<Boolean> = repository.loaded

    /** Einstellungen von Mental Math Plus (Farbthema, Erinnerung). */
    val plusSettings: StateFlow<PlusSettings> = settingsRepository.settings
    val plusSettingsLoaded: StateFlow<Boolean> = settingsRepository.loaded

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    /** Vergangene Zeit für die aktuelle Aufgabe in ms. */
    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    /** Auswertung der zuletzt beendeten Einheit. */
    private val _summary = MutableStateFlow<Summary?>(null)
    val summary: StateFlow<Summary?> = _summary.asStateFlow()

    private val _freeConfig = MutableStateFlow(FreeConfig())
    val freeConfig: StateFlow<FreeConfig> = _freeConfig.asStateFlow()

    private val random = Random(System.nanoTime())

    /** Erzeugt bei anpassender Schwierigkeit neue Aufgaben (Stufe, Anzahl); null = freies Training. */
    private var freeGenerator: ((Int, Int) -> List<Problem>)? = null

    /** Startet die zuletzt gestartete Einheit des freien Trainings bzw. des TMSnat-Trainings neu. */
    private var lastFreeStart: (() -> Boolean)? = null
    private var tickJob: Job? = null
    private var advanceJob: Job? = null
    private var problemStart = 0L
    private var pausedAt: Long? = null

    // --- Abfragen für die Oberfläche ----------------------------------------------------------

    fun skill(id: String): Skill? = Curriculum.skill(id)

    fun now(): Long = System.currentTimeMillis()

    fun today(): Long {
        val n = now()
        return (n + TimeZone.getDefault().getOffset(n)) / Progression.DAY_MS
    }

    fun dueSkills(p: ClimbProgress): List<Skill> = Progression.dueSkills(worlds, p, now())

    fun refreshSkills(p: ClimbProgress): List<Skill> = Progression.needsRefresh(worlds, p)

    fun isUnlocked(p: ClimbProgress, skillId: String): Boolean = Progression.skillUnlocked(worlds, p, skillId)

    fun isWorldUnlocked(p: ClimbProgress, worldIndex: Int): Boolean = Progression.worldUnlocked(worlds, p, worldIndex)

    fun blockers(p: ClimbProgress, skillId: String): List<Skill> = Progression.blockers(worlds, p, skillId)

    fun nextSkill(p: ClimbProgress): Skill? = Progression.nextSkill(worlds, p)

    fun streak(p: ClimbProgress): Int = Progression.visibleStreak(p, today())

    // --- Einheiten starten --------------------------------------------------------------------

    fun startSession(type: SessionType, skillId: String): Boolean {
        val skill = Curriculum.skill(skillId) ?: return false
        begin(SessionLogic.create(type, skill, random))
        return true
    }

    /** Wiederholung der fälligen Levels; ist nichts fällig, gemischtes Training aus gelernten Levels. */
    fun startReview(): Boolean {
        val p = progress.value
        val due = Progression.dueSkills(worlds, p, now()).take(Progression.REVIEW_MAX_SKILLS)
        if (due.isNotEmpty()) {
            begin(SessionLogic.createReview(due, random, "Wiederholung"))
            return true
        }
        val mastered = worlds.flatMap { it.skills }
            .filter { p.of(it.id).mastered && !p.of(it.id).needsAttention }
            .shuffled(random)
            .take(Progression.REVIEW_MAX_SKILLS)
        if (mastered.isEmpty()) return false
        begin(SessionLogic.createReview(mastered, random, "Gemischtes Training").copy(type = SessionType.PRACTICE))
        return true
    }

    /** Einstufungstest für eine Welt starten. */
    fun startPlacement(worldId: String): Boolean {
        val world = worlds.firstOrNull { it.id == worldId } ?: return false
        begin(Placement.create(world, random))
        return true
    }

    fun setFreeConfig(config: FreeConfig) {
        _freeConfig.value = config
    }

    /** Aufgaben des freien Trainings für die gewählten Aufgabenarten. */
    private fun freeProblems(topics: List<FreeTopic>, level: Int, count: Int): List<Problem> =
        List(count) { FreeTopics.generate(topics[random.nextInt(topics.size)], level, random) }

    /** Freies Training mit den aktuellen Einstellungen starten. */
    fun startFree(): Boolean {
        freeGenerator = null
        lastFreeStart = { startFree() }
        val c = _freeConfig.value
        val topics = c.topics.toList().ifEmpty { return false }
        val adaptive = plusSettings.value.adaptive
        val count = if (c.mode == FreeMode.PRUEFUNG) c.param else 400
        val problems = freeProblems(topics, c.level, count)
        val limit = if (c.mode == FreeMode.PRUEFUNG) 5_000L + 7_000L * (if (adaptive) 3 else c.level) else null
        val title = if (topics.size == 1) topics.first().title else "${topics.size} Aufgabenarten"
        val rules = FreeRules(c.mode, c.param, if (adaptive) 0 else c.level)
        val session = SessionLogic.createFree(problems, rules, title, limit)
        begin(if (adaptive) session.copy(adaptive = AdaptiveState(c.level)) else session)
        return true
    }

    /**
     * Testsimulation des TMSnat-Untertests „Arithmetisches Problemlösen“: [count] Aufgaben,
     * [seconds] Sekunden Gesamtzeit (im Test 16 Aufgaben in 25 Minuten), [options] Antwortmöglichkeiten (A–E).
     */
    fun startApSimulation(seconds: Int, count: Int = 16, options: Int = 5): Boolean {
        freeGenerator = null
        lastFreeStart = { startApSimulation(seconds, count, options) }
        val problems = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks.simulation(count, options, random)
        val title = com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks.simulationName(count, seconds)
        begin(SessionLogic.createFree(problems, FreeRules(FreeMode.SIMULATION, seconds, options, FreeRules.TRACK_AP), title, null))
        return true
    }

    /** Gemischtes Üben „Arithmetisches Problemlösen“ ohne Ende, mit Rückmeldung und Lösungsweg. */
    fun startApPractice(categories: Set<com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory> = emptySet()): Boolean {
        val cats = categories.ifEmpty { com.helddertierwelt.mentalmath.domain.climb.curriculum.ApCategory.entries.toSet() }.toList()
        val generator = { level: Int, count: Int ->
            List(count) { com.helddertierwelt.mentalmath.domain.climb.curriculum.ApTasks.mixed(5, level, random, cats) }
        }
        freeGenerator = generator
        lastFreeStart = { startApPractice(categories) }
        val adaptive = plusSettings.value.adaptive
        val start = 2
        val title = if (cats.size == 1) cats.first().title else "Gemischt üben"
        val session = SessionLogic.createFree(generator(start, 400), FreeRules(FreeMode.ENDLOS, 0, if (adaptive) 0 else start, FreeRules.TRACK_AP), title, null)
        begin(if (adaptive) session.copy(adaptive = AdaptiveState(start)) else session)
        return true
    }

    /** Fällige Aufgabentypen im Fehlertraining. */
    fun apReviewDue(p: ClimbProgress): List<ApMistakeEntry> = ApReview.due(p, now())

    /** Wann der nächste Aufgabentyp im Fehlertraining fällig wird (null: keiner mehr in der Wiederholung). */
    fun apReviewNext(p: ClimbProgress): Long? = ApReview.nextDue(p, now())

    /** Fehlertraining: zu jedem fälligen Aufgabentyp eine neue Aufgabe, nach Fehlern mit Lösungsweg. */
    fun startApReview(): Boolean {
        val problems = ApReview.problems(progress.value, now(), random)
        if (problems.isEmpty()) return false
        freeGenerator = null
        lastFreeStart = { startApReview() }
        begin(SessionLogic.createFree(problems, FreeRules(FreeMode.ENDLOS, 0, 2, FreeRules.TRACK_AP), REVIEW_TITLE, null))
        return true
    }

    /** Punkt-Prognose für den TMSnat (rechnet im Hintergrund 10.000 Tests durch). */
    suspend fun apForecast(p: ClimbProgress): ApForecast.Outcome = withContext(Dispatchers.Default) { ApForecast.forecast(p) }

    /** „Nochmal“ nach dem freien Training oder dem TMSnat-Training. */
    fun restartFree(): Boolean = lastFreeStart?.invoke() ?: startFree()

    /** Testsimulation: aktuelle Aufgabe überspringen und später bearbeiten. */
    fun onSkip() {
        val s = _session.value ?: return
        if (!s.isSimulation || s.phase != Phase.ANSWERING) return
        feedbackController.play(FeedbackType.TAP)
        _session.value = SessionLogic.skip(s, currentElapsed())
        restartProblemClock()
    }

    /** Trick-Tipp zur Aufgabe. */
    fun trickTip(problem: Problem): TrickTip = TrickAdvisor.tip(problem, worlds)

    fun setTrickTips(on: Boolean) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch { settingsRepository.update { it.copy(trickTips = on) } }
    }

    fun setAdaptive(on: Boolean) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch { settingsRepository.update { it.copy(adaptive = on) } }
    }

    /** Freies Training beenden und auswerten (Endlos-Modus oder vorzeitig). */
    fun endFree() {
        val s = _session.value ?: return
        if (s.results.isEmpty()) {
            quitSession()
            return
        }
        advanceJob?.cancel()
        val done = SessionLogic.finishNow(s, if (s.phase == Phase.ANSWERING) currentElapsed() else 0L)
        _session.value = done
        restartProblemClock()
        finish(done)
    }

    private fun begin(s: Session) {
        advanceJob?.cancel()
        _summary.value = null
        _session.value = s
        feedbackController.play(FeedbackType.START)
        restartProblemClock()
        startTicking()
    }

    // --- Eingaben -------------------------------------------------------------------------------

    fun onKey(key: Char) {
        val s = _session.value ?: return
        apply(SessionLogic.type(s, key))
    }

    fun onBackspace() {
        val s = _session.value ?: return
        apply(SessionLogic.backspace(s))
    }

    fun onSubmit() {
        val s = _session.value ?: return
        if (s.phase != Phase.ANSWERING) return
        apply(SessionLogic.submit(s, currentElapsed()))
    }

    fun onChoice(index: Int) {
        val s = _session.value ?: return
        apply(SessionLogic.choose(s, index, currentElapsed()))
    }

    fun onToggleHint() {
        val s = _session.value ?: return
        feedbackController.play(FeedbackType.TAP)
        _session.value = SessionLogic.toggleHint(s)
    }

    fun onRevealStep() {
        val s = _session.value ?: return
        feedbackController.play(FeedbackType.TAP)
        _session.value = SessionLogic.revealStep(s)
    }

    /** „Weiter“ nach einer gezeigten Lösung oder während der kurzen Rückmeldung. */
    fun onContinue() {
        advanceJob?.cancel()
        feedbackController.play(FeedbackType.TAP)
        doAdvance()
    }

    fun quitSession() {
        tickJob?.cancel()
        advanceJob?.cancel()
        _session.value = null
        feedbackController.play(FeedbackType.STOP)
    }

    fun clearSummary() {
        _summary.value = null
        _session.value = null
    }

    fun resetProgress() {
        tickJob?.cancel()
        advanceJob?.cancel()
        _session.value = null
        _summary.value = null
        viewModelScope.launch { repository.reset() }
    }

    /** Nur für den automatischen Oberflächentest: eine fertig gerechnete Einheit samt Auswertung anzeigen. */
    fun showFinishedForTest(session: Session, summary: Summary) {
        tickJob?.cancel()
        advanceJob?.cancel()
        _session.value = session
        _summary.value = summary
    }

    /** Fortschritt komplett ersetzen (für den automatischen Oberflächentest und beim Laden einer Sicherung). */
    fun importProgress(p: ClimbProgress) {
        tickJob?.cancel()
        advanceJob?.cancel()
        _session.value = null
        _summary.value = null
        viewModelScope.launch { repository.update { p } }
    }

    // --- Einstellungen und Sicherung -------------------------------------------------------------

    fun setPalette(palette: Palette) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch { settingsRepository.update { it.copy(palette = palette.name) } }
    }

    fun setReminder(on: Boolean, hour: Int = plusSettings.value.reminderHour, minute: Int = plusSettings.value.reminderMinute) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(reminderOn = on, reminderHour = hour.mod(24), reminderMinute = minute.mod(60)) }
        }
    }

    fun setReminderOnlyIfIdle(value: Boolean) {
        feedbackController.play(FeedbackType.TAP)
        viewModelScope.launch { settingsRepository.update { it.copy(reminderOnlyIfIdle = value) } }
    }

    /** Die Erinnerung der Original-App wurde übernommen (oder war aus). */
    fun markOldReminderMigrated(enableReminder: Boolean) {
        viewModelScope.launch {
            settingsRepository.update {
                if (enableReminder) it.copy(oldReminderMigrated = true, reminderOn = true) else it.copy(oldReminderMigrated = true)
            }
        }
    }

    /** Einstellungen von Mental Math Plus zurücksetzen (beim Zurücksetzen der ganzen App). */
    fun resetPlusSettings() {
        viewModelScope.launch { settingsRepository.update { PlusSettings(oldReminderMigrated = true) } }
    }

    /** Sicherung als Text (JSON) mit Fortschritt und Einstellungen. */
    fun backupText(): String = BackupCodec.encode(BackupCodec.create(progress.value, plusSettings.value, now()))

    /** Dateiname für die Sicherung, z. B. „MentalMathPlus-Sicherung-2026-10-27.json“. */
    fun backupFileName(): String {
        val c = java.util.Calendar.getInstance()
        val date = String.format(
            java.util.Locale.ROOT,
            "%04d-%02d-%02d",
            c.get(java.util.Calendar.YEAR),
            c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH),
        )
        return "MentalMathPlus-Sicherung-$date.json"
    }

    fun readBackup(text: String): Result<Backup> = BackupCodec.decode(text)

    /** Sicherung übernehmen: Fortschritt ersetzen, Farbthema und Erinnerung übernehmen. */
    fun restoreBackup(backup: Backup) {
        importProgress(backup.progress)
        val s = backup.settings ?: return
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(
                    palette = s.palette,
                    reminderOn = s.reminderOn,
                    reminderHour = s.reminderHour,
                    reminderMinute = s.reminderMinute,
                    reminderOnlyIfIdle = s.reminderOnlyIfIdle,
                    trickTips = s.trickTips,
                    adaptive = s.adaptive,
                )
            }
        }
    }

    fun playTap() = feedbackController.play(FeedbackType.TAP)

    // --- Ablauf ----------------------------------------------------------------------------------

    private fun apply(result: Pair<Session, SessionEvent>) {
        var (s, event) = result
        if (s.adaptive != null && (event == SessionEvent.CORRECT || event == SessionEvent.WRONG || event == SessionEvent.TIMEOUT)) {
            val topics = _freeConfig.value.topics.toList().ifEmpty { listOf(FreeTopic.BASIC) }
            val generator = freeGenerator ?: { level: Int, count: Int -> freeProblems(topics, level, count) }
            s = SessionLogic.adapt(s, generator)
        }
        _session.value = s
        if (s.isSimulation && s.phase == Phase.FLASH) {
            // Wie im echten Test: keine Rückmeldung, sofort zur nächsten Aufgabe.
            feedbackController.play(FeedbackType.TAP)
            scheduleAdvance(0)
            return
        }
        when (event) {
            SessionEvent.KEY -> feedbackController.play(FeedbackType.NUMPAD)
            SessionEvent.BACKSPACE -> feedbackController.play(FeedbackType.BACKSPACE)
            SessionEvent.STEP_CORRECT, SessionEvent.CORRECT -> feedbackController.play(FeedbackType.CORRECT)
            SessionEvent.STEP_WRONG, SessionEvent.WRONG -> feedbackController.play(FeedbackType.WRONG)
            SessionEvent.TIMEOUT -> feedbackController.play(FeedbackType.TIMEOUT)
            SessionEvent.NONE -> Unit
        }
        if (s.phase == Phase.FLASH) {
            val wait = if (s.feedback?.correct == true) 450L else 1300L
            scheduleAdvance(wait)
        }
    }

    private fun scheduleAdvance(delayMs: Long) {
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(delayMs)
            doAdvance()
        }
    }

    private fun doAdvance() {
        val s = _session.value ?: return
        if (s.phase == Phase.ANSWERING || s.phase == Phase.FINISHED) return
        val before = s.index
        val next = SessionLogic.advance(s, currentElapsed())
        _session.value = next
        if (next.index != before || next.phase == Phase.FINISHED) restartProblemClock()
        if (next.phase == Phase.FINISHED) finish(next)
    }

    private fun finish(s: Session) {
        tickJob?.cancel()
        viewModelScope.launch {
            var summary: Summary? = null
            repository.update { p ->
                val (updated, sum) = SessionFinisher.finish(worlds, p, s, now(), today())
                summary = sum
                updated
            }
            _summary.value = summary
            val results = s.results
            val accuracy = if (results.isEmpty()) 0.0 else results.count { it.correct }.toDouble() / results.size
            val sound = when {
                summary?.outcome?.passed == false -> FeedbackType.RESULT_BAD
                accuracy >= 0.999 -> FeedbackType.RESULT_PERFECT
                accuracy >= 0.8 -> FeedbackType.RESULT_GOOD
                accuracy >= 0.6 -> FeedbackType.RESULT_OKAY
                else -> FeedbackType.RESULT_BAD
            }
            delay(300)
            feedbackController.play(sound)
        }
    }

    // --- Zeitmessung ---------------------------------------------------------------------------

    private fun restartProblemClock() {
        problemStart = SystemClock.elapsedRealtime()
        if (pausedAt != null) pausedAt = problemStart
        _elapsedMs.value = 0L
    }

    private fun currentElapsed(): Long = ((pausedAt ?: SystemClock.elapsedRealtime()) - problemStart).coerceAtLeast(0L)

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(100)
                val s = _session.value ?: break
                if (s.phase == Phase.FINISHED) break
                if (pausedAt != null || s.phase != Phase.ANSWERING) continue
                val elapsed = currentElapsed()
                _elapsedMs.value = elapsed
                val limit = s.currentLimitMs
                if (s.free != null && SessionLogic.freeOver(s, elapsed)) {
                    val done = SessionLogic.finishNow(s, elapsed)
                    _session.value = done
                    finish(done)
                    break
                }
                if (limit != null && elapsed >= limit) {
                    apply(SessionLogic.timeout(s, limit))
                }
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (_session.value != null && pausedAt == null) pausedAt = SystemClock.elapsedRealtime()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        val p = pausedAt ?: return
        problemStart += SystemClock.elapsedRealtime() - p
        pausedAt = null
    }

    companion object {
        /** Titel der Einheit Fehlertraining. */
        const val REVIEW_TITLE = "Fehlertraining"
    }
}
