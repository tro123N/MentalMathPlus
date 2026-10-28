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

package com.helddertierwelt.mentalmath.domain.climb.engine

import com.helddertierwelt.mentalmath.domain.climb.math.AnswerCheck
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.random.Random

/** Eine geplante Aufgabe in einer Einheit; [limitMs] überschreibt das Zeitlimit der Einheit. */
data class Planned(
    val problem: Problem,
    val retry: Boolean = false,
    val limitMs: Long? = null,
    /** Nummer in der ursprünglichen Reihenfolge (freies Training, Testsimulation), 0 = ohne. */
    val number: Int = 0,
    /** Testsimulation: Zeit aus früheren Versuchen, bevor die Aufgabe übersprungen wurde. */
    val spentMs: Long = 0,
)

/** Modi des freien Trainings. [selectable]: im freien Training auswählbar. */
enum class FreeMode(val title: String, val selectable: Boolean = true) {
    /** Möglichst viele Aufgaben in einer festen Zeit. */
    ZEIT("Auf Zeit"),

    /** Ohne Ende, bis man aufhört. */
    ENDLOS("Endlos"),

    /** Bis die Leben aufgebraucht sind. */
    UEBERLEBEN("Überleben"),

    /** Feste Zahl von Aufgaben mit Zeitlimit pro Aufgabe, am Ende eine Note. */
    PRUEFUNG("Prüfung"),

    /** Testsimulation: feste Aufgaben, eine Gesamtzeit, keine Rückmeldung bis zum Ende, Überspringen erlaubt. */
    SIMULATION("Testsimulation", selectable = false),
}

/** Regeln einer Einheit im freien Training. */
data class FreeRules(
    val mode: FreeMode,
    /** Sekunden (Zeit), Leben (Überleben) bzw. Aufgaben (Prüfung). */
    val param: Int,
    /** Stufe 1 bis 3; 0 = automatisch (anpassende Schwierigkeit). Bei der Testsimulation: Zahl der Antwortmöglichkeiten. */
    val level: Int,
    /** Bereich, z. B. „ap“ für das TMSnat-Training (Arithmetisches Problemlösen); leer = freies Training. */
    val track: String = "",
) {
    /** Schlüssel für Bestwerte, z. B. „ZEIT_60_2“ oder „ap_SIMULATION_1500_5“. */
    val bestKey: String get() = (if (track.isEmpty()) "" else "${track}_") + "${mode.name}_${param}_$level"

    val isAp: Boolean get() = track == TRACK_AP

    companion object {
        const val TRACK_AP = "ap"
    }
}

/** Was die Oberfläche gerade zeigt. */
enum class Phase {
    /** Eingabe läuft. */
    ANSWERING,

    /** Kurze Rückmeldung (richtig/falsch), danach geht es automatisch weiter. */
    FLASH,

    /** Lösung bzw. Schritt wird gezeigt, weiter mit „Weiter“. */
    REVEAL,

    /** Einheit ist vorbei. */
    FINISHED,
}

/** Rückmeldung zur letzten Eingabe. */
data class Feedback(
    val correct: Boolean,
    val userAnswer: String,
    val correctAnswer: String,
    val note: String? = null,
    val timedOut: Boolean = false,
)

/** Was nach einer Aktion passiert ist (für Töne und Abläufe). */
enum class SessionEvent { NONE, KEY, BACKSPACE, STEP_CORRECT, STEP_WRONG, CORRECT, WRONG, TIMEOUT }

/** Zustand einer laufenden Übungseinheit. Unveränderlich; jede Aktion liefert einen neuen Zustand. */
data class Session(
    val type: SessionType,
    /** Level der Einheit; null bei gemischter Wiederholung. */
    val skillId: String?,
    val title: String,
    val problems: List<Planned>,
    val index: Int = 0,
    val input: String = "",
    /** Geführtes Üben: aktueller Schritt. */
    val stepIndex: Int = 0,
    /** Geführtes Üben: Fehlversuche im aktuellen Schritt. */
    val stepAttempts: Int = 0,
    /** Geführtes Üben: Fehlversuche und aufgedeckte Schritte in der aktuellen Aufgabe. */
    val hintsUsed: Int = 0,
    /** Geführtes Üben: In der aktuellen Aufgabe wurde mindestens ein Schritt aufgedeckt. */
    val revealedInProblem: Boolean = false,
    val showHint: Boolean = false,
    val phase: Phase = Phase.ANSWERING,
    val feedback: Feedback? = null,
    val results: List<ProblemResult> = emptyList(),
    /** Zeitlimit pro Aufgabe (nur Prüfung und Auffrischung). */
    val timeLimitMs: Long? = null,
    /** Schlüssel der Aufgaben, die schon einmal wiederholt eingeplant wurden. */
    val retried: Set<String> = emptySet(),
    /** Nur im freien Training gesetzt. */
    val free: FreeRules? = null,
    /** Nur beim Einstufungstest gesetzt: die geprüfte Welt. */
    val worldId: String? = null,
    /** Nur im freien Training mit anpassender Schwierigkeit gesetzt. */
    val adaptive: AdaptiveState? = null,
    /** Testsimulation: Zeit, die auf übersprungene Aufgaben verwendet wurde. */
    val skippedMs: Long = 0,
    /** Testsimulation: so oft wurde „Später“ gedrückt. */
    val skips: Int = 0,
) {
    val current: Planned? get() = problems.getOrNull(index)
    val problem: Problem? get() = current?.problem
    val isGuided: Boolean get() = type == SessionType.GUIDED
    val isExam: Boolean
        get() = (type == SessionType.TEST || type == SessionType.REFRESH || type == SessionType.FREE || type == SessionType.PLACEMENT) &&
            !showsSolution

    /** Gemischtes Üben im TMSnat-Training: nach einem Fehler den Lösungsweg zeigen, weiter mit „Weiter“. */
    val showsSolution: Boolean get() = free?.isAp == true && free.mode == FreeMode.ENDLOS

    /** Zeitlimit der aktuellen Aufgabe. */
    val currentLimitMs: Long? get() = current?.limitMs ?: timeLimitMs

    /** Gesamte Rechenzeit der bisherigen Aufgaben (ohne Pausen und Rückmeldungen). */
    val usedMs: Long get() = results.sumOf { it.timeMs } + skippedMs

    /** Testsimulation ohne Rückmeldung nach jeder Aufgabe. */
    val isSimulation: Boolean get() = free?.mode == FreeMode.SIMULATION

    /** Falsche Antworten bisher. */
    val mistakes: Int get() = results.count { !it.correct }

    /** Der Schritt, der im geführten Üben gerade dran ist. */
    val currentStep: Step? get() = if (isGuided) problem?.steps?.getOrNull(stepIndex) else null

    val inputKind: InputKind
        get() = currentStep?.input ?: problem?.input ?: InputKind.INTEGER

    /** Erwartete Antwort für die aktuelle Eingabe. */
    val expected: Expected?
        get() = currentStep?.expected ?: problem?.expected

    val total: Int get() = problems.size
    val done: Int get() = if (phase == Phase.FINISHED) problems.size else index
}

object SessionLogic {

    const val MAX_INPUT = 12

    /** Vor der Erklärung eines typischen Fehlers. */
    const val MISTAKE_PREFIX = "Typischer Fehler: "

    /** Wiederholungen werden so viele Aufgaben später eingeplant. */
    const val RETRY_DISTANCE = 3

    fun create(type: SessionType, skill: Skill, random: Random, avoid: Set<String> = emptySet()): Session {
        val count = when (type) {
            SessionType.GUIDED -> skill.guidedCount
            SessionType.PRACTICE -> skill.practiceCount
            SessionType.TEST -> skill.goals.testSize
            SessionType.REFRESH -> Progression.REFRESH_SIZE
            SessionType.REVIEW -> Progression.REVIEW_PER_SKILL
            SessionType.FREE -> skill.practiceCount
            SessionType.PLACEMENT -> Placement.PER_SKILL
        }
        val problems = SessionPlanner.problems(skill, count, random, avoid).map { Planned(it) }
        val limit = if (type == SessionType.TEST || type == SessionType.REFRESH) (skill.goals.timeLimit * 1000).toLong() else null
        return Session(type = type, skillId = skill.id, title = skill.title, problems = problems, timeLimitMs = limit)
    }

    fun createReview(skills: List<Skill>, random: Random, title: String): Session {
        val problems = SessionPlanner.review(skills, Progression.REVIEW_PER_SKILL, random).map { Planned(it) }
        return Session(type = SessionType.REVIEW, skillId = null, title = title, problems = problems)
    }

    /** Freies Training: Aufgaben werden vorab erzeugt, das Ende bestimmen die Regeln. */
    fun createFree(problems: List<Problem>, rules: FreeRules, title: String, timeLimitMs: Long?): Session =
        Session(
            type = SessionType.FREE,
            skillId = null,
            title = title,
            problems = problems.mapIndexed { i, p -> Planned(p, number = i + 1) },
            timeLimitMs = timeLimitMs,
            free = rules,
        )

    /**
     * Anpassende Schwierigkeit: nach einer Antwort die Stufe nachführen. Ändert sie sich, werden
     * die noch kommenden Aufgaben mit [regenerate] (Stufe, Anzahl) neu erzeugt.
     */
    fun adapt(s: Session, regenerate: (level: Int, count: Int) -> List<Problem>): Session {
        val state = s.adaptive ?: return s
        val last = s.results.lastOrNull() ?: return s
        val next = Adaptive.update(state, last.correct, last.timeMs, last.problem.skillId)
        if (next.level == state.level) return s.copy(adaptive = next)
        val keep = s.problems.take(s.index + 1)
        val count = s.problems.size - keep.size
        val fresh = if (count > 0) regenerate(next.level, count).map { Planned(it) } else emptyList()
        return s.copy(adaptive = next, problems = keep + fresh)
    }

    /** Ist eine Einheit im freien Training nach ihren Regeln vorbei? */
    fun freeOver(s: Session, extraMs: Long = 0): Boolean {
        val rules = s.free ?: return false
        return when (rules.mode) {
            FreeMode.ZEIT, FreeMode.SIMULATION -> s.usedMs + extraMs >= rules.param * 1000L
            FreeMode.UEBERLEBEN -> s.mistakes >= rules.param
            FreeMode.ENDLOS, FreeMode.PRUEFUNG -> false
        }
    }

    /**
     * Einheit sofort beenden (Zeit um, Leben weg oder vom Nutzer beendet).
     * [elapsedMs]: Zeit, die schon für die aktuelle, nicht beantwortete Aufgabe vergangen ist.
     */
    fun finishNow(s: Session, elapsedMs: Long = 0): Session {
        if (s.isSimulation) {
            // Nicht bearbeitete Aufgaben zählen wie im Test als falsch.
            val currentIndex = s.results.size
            val open = s.problems.drop(currentIndex).mapIndexed { i, planned ->
                val extra = planned.spentMs + if (i == 0 && s.phase == Phase.ANSWERING) elapsedMs else 0L
                ProblemResult(planned.problem, "", false, 0, timedOut = true, number = planned.number, extraMs = extra)
            }
            val results = s.results + open
            val counted = if (s.phase == Phase.ANSWERING && open.isNotEmpty()) elapsedMs else 0L
            return s.copy(
                results = results,
                index = s.problems.size,
                phase = Phase.FINISHED,
                input = "",
                feedback = null,
                skippedMs = s.skippedMs + counted,
            )
        }
        val done = s.results.size
        return s.copy(problems = s.problems.take(done), index = done, phase = Phase.FINISHED, input = "", feedback = null)
    }

    /** Testsimulation: aktuelle Aufgabe ans Ende stellen und später bearbeiten. */
    fun skip(s: Session, elapsedMs: Long): Session {
        if (!s.isSimulation || s.phase != Phase.ANSWERING) return s
        val current = s.current ?: return s
        if (s.index >= s.problems.lastIndex) return s
        val list = s.problems.toMutableList()
        list.removeAt(s.index)
        list.add(current.copy(spentMs = current.spentMs + elapsedMs))
        return s.copy(problems = list, input = "", showHint = false, skippedMs = s.skippedMs + elapsedMs, skips = s.skips + 1)
    }

    /** Wie viele Aufgaben der Testsimulation sind noch offen (einschließlich der aktuellen)? */
    fun open(s: Session): Int = (s.problems.size - s.results.size).coerceAtLeast(0)

    /** Eine Taste tippen: Ziffer, ',' bei Kommazahlen, 'R' bei Rest, '/' bei Brüchen. */
    fun type(s: Session, key: Char): Pair<Session, SessionEvent> {
        if (s.phase != Phase.ANSWERING || s.problem == null) return s to SessionEvent.NONE
        if (s.input.length >= MAX_INPUT) return s to SessionEvent.NONE
        val kind = s.inputKind
        val allowed = when {
            key.isDigit() -> kind != InputKind.CHOICE
            key == ',' -> kind == InputKind.DECIMAL && !s.input.contains(',')
            key == 'R' -> kind == InputKind.REMAINDER && s.input.isNotEmpty() && !s.input.contains('R')
            key == '/' -> kind == InputKind.FRACTION && s.input.isNotEmpty() && !s.input.contains('/')
            else -> false
        }
        if (!allowed) return s to SessionEvent.NONE
        val newInput = if (key.isDigit() && s.input == "0" && kind != InputKind.DIGITS) key.toString() else s.input + key
        return s.copy(input = newInput) to SessionEvent.KEY
    }

    fun backspace(s: Session): Pair<Session, SessionEvent> {
        if (s.phase != Phase.ANSWERING || s.input.isEmpty()) return s to SessionEvent.NONE
        return s.copy(input = s.input.dropLast(1)) to SessionEvent.BACKSPACE
    }

    fun toggleHint(s: Session): Session = if (s.phase == Phase.ANSWERING) s.copy(showHint = !s.showHint) else s

    /** Antwort abgeben. [elapsedMs] ist die Zeit, die für die aktuelle Aufgabe vergangen ist. */
    fun submit(s: Session, elapsedMs: Long): Pair<Session, SessionEvent> {
        if (s.phase != Phase.ANSWERING) return s to SessionEvent.NONE
        val planned = s.current ?: return s to SessionEvent.NONE
        val problem = planned.problem
        val expected = s.expected ?: return s to SessionEvent.NONE

        if (s.isGuided) return submitGuided(s, elapsedMs)

        val input = s.input
        val verdict = AnswerCheck.check(expected, input)
        val display = if (input.isEmpty()) "" else displayInput(input, s.inputKind, problem.choices)
        // Passt eine falsche Antwort zu einem typischen Fehler, wird erklärt, was schiefging.
        val mistake = if (verdict.correct) null else problem.mistakeFor(input)
        val feedback = Feedback(
            correct = verdict.correct,
            userAnswer = display,
            correctAnswer = Fmt.spoken(problem.expected, problem.choices),
            note = verdict.note ?: mistake?.let { "$MISTAKE_PREFIX$it" },
        )
        val result = ProblemResult(
            problem,
            display,
            verdict.correct,
            elapsedMs,
            retry = planned.retry,
            number = planned.number,
            extraMs = planned.spentMs,
            note = mistake,
        )
        var next = s.copy(results = s.results + result, feedback = feedback)

        return if (s.isExam) {
            next.copy(phase = Phase.FLASH) to (if (verdict.correct) SessionEvent.CORRECT else SessionEvent.WRONG)
        } else if (verdict.correct) {
            next.copy(phase = Phase.FLASH) to SessionEvent.CORRECT
        } else {
            // Im freien Training keine Wiederholung derselben Aufgabe (bei Antworten A–E würde man sich nur den Buchstaben merken).
            if (s.free == null && problem.key !in s.retried) {
                val at = minOf(s.index + 1 + RETRY_DISTANCE, s.problems.size)
                val list = s.problems.toMutableList()
                list.add(at, Planned(problem, retry = true))
                next = next.copy(problems = list, retried = s.retried + problem.key)
            }
            next.copy(phase = Phase.REVEAL) to SessionEvent.WRONG
        }
    }

    private fun submitGuided(s: Session, elapsedMs: Long): Pair<Session, SessionEvent> {
        val problem = s.problem ?: return s to SessionEvent.NONE
        val step = s.currentStep ?: return s to SessionEvent.NONE
        if (s.input.isEmpty()) return s.copy(showHint = true) to SessionEvent.NONE
        val verdict = AnswerCheck.check(step.expected, s.input)
        val display = displayInput(s.input, s.inputKind, problem.choices)
        val correctText = Fmt.spoken(step.expected, problem.choices)
        return if (verdict.correct) {
            val feedback = Feedback(true, display, correctText, verdict.note)
            if (s.stepIndex >= problem.steps.lastIndex) {
                val result = ProblemResult(problem, display, !s.revealedInProblem, elapsedMs, hintsUsed = s.hintsUsed, retry = s.current?.retry == true)
                s.copy(results = s.results + result, feedback = feedback, phase = Phase.FLASH, input = "") to SessionEvent.CORRECT
            } else {
                s.copy(stepIndex = s.stepIndex + 1, stepAttempts = 0, input = "", showHint = false, feedback = feedback) to SessionEvent.STEP_CORRECT
            }
        } else {
            val attempts = s.stepAttempts + 1
            val feedback = Feedback(false, display, correctText, verdict.note)
            val base = s.copy(stepAttempts = attempts, hintsUsed = s.hintsUsed + 1, input = "", showHint = true, feedback = feedback)
            if (attempts >= 2) {
                base.copy(phase = Phase.REVEAL, revealedInProblem = true) to SessionEvent.STEP_WRONG
            } else {
                base to SessionEvent.STEP_WRONG
            }
        }
    }

    /** Geführtes Üben: aktuellen Schritt aufdecken. */
    fun revealStep(s: Session): Session {
        if (!s.isGuided || s.phase != Phase.ANSWERING) return s
        val step = s.currentStep ?: return s
        val problem = s.problem ?: return s
        return s.copy(
            phase = Phase.REVEAL,
            revealedInProblem = true,
            hintsUsed = s.hintsUsed + 1,
            input = "",
            feedback = Feedback(false, "", Fmt.spoken(step.expected, problem.choices)),
        )
    }

    /** Auswahl antippen (bei Auswahlaufgaben). */
    fun choose(s: Session, index: Int, elapsedMs: Long): Pair<Session, SessionEvent> {
        if (s.phase != Phase.ANSWERING) return s to SessionEvent.NONE
        return submit(s.copy(input = index.toString()), elapsedMs)
    }

    /** Zeit abgelaufen (nur in Prüfungen). */
    fun timeout(s: Session, elapsedMs: Long): Pair<Session, SessionEvent> {
        if (s.phase != Phase.ANSWERING || !s.isExam) return s to SessionEvent.NONE
        val planned = s.current ?: return s to SessionEvent.NONE
        val problem = planned.problem
        val result = ProblemResult(problem, "", false, elapsedMs, timedOut = true, retry = planned.retry)
        val feedback = Feedback(false, "", Fmt.spoken(problem.expected, problem.choices), timedOut = true)
        return s.copy(results = s.results + result, feedback = feedback, phase = Phase.FLASH) to SessionEvent.TIMEOUT
    }

    /** Weiter zur nächsten Aufgabe (oder zum nächsten Schritt nach einem aufgedeckten Schritt). */
    fun advance(s: Session, elapsedMs: Long = 0): Session {
        if (s.phase == Phase.ANSWERING || s.phase == Phase.FINISHED) return s
        if (s.free != null && freeOver(s)) return finishNow(s)
        if (s.isGuided && s.phase == Phase.REVEAL) {
            val problem = s.problem ?: return s
            if (s.stepIndex < problem.steps.lastIndex) {
                return s.copy(stepIndex = s.stepIndex + 1, stepAttempts = 0, phase = Phase.ANSWERING, input = "", showHint = false, feedback = null)
            }
            val result = ProblemResult(problem, "", false, elapsedMs, hintsUsed = s.hintsUsed, retry = s.current?.retry == true)
            return nextProblem(s.copy(results = s.results + result))
        }
        return nextProblem(s)
    }

    private fun nextProblem(s: Session): Session {
        val nextIndex = s.index + 1
        return if (nextIndex >= s.problems.size) {
            s.copy(index = s.problems.size, phase = Phase.FINISHED, input = "", feedback = null)
        } else {
            s.copy(
                index = nextIndex,
                input = "",
                stepIndex = 0,
                stepAttempts = 0,
                hintsUsed = 0,
                revealedInProblem = false,
                showHint = false,
                phase = Phase.ANSWERING,
                feedback = null,
            )
        }
    }

    /** Eingabe in Anzeigeform: Brüche und Reste lesbar machen, Auswahl als Text. */
    fun displayInput(input: String, kind: InputKind, choices: List<String>): String {
        if (kind == InputKind.CHOICE) {
            return input.toIntOrNull()?.let { choices.getOrNull(it) } ?: input
        }
        return Fmt.input(input)
    }
}

/** Ergebnis einer abgeschlossenen Einheit für die Ergebnisseite. */
data class Summary(
    val type: SessionType,
    val skillId: String?,
    val title: String,
    val results: List<ProblemResult>,
    val outcome: Outcome?,
    val starsBefore: Int,
    val starsAfter: Int,
    /** Das Level, das durch diese Einheit neu freigeschaltet wurde. */
    val unlocked: Skill?,
    /** Das Level, das nach zwei verpatzten Prüfungen aufgefrischt werden muss. */
    val sentBackTo: Skill?,
    /** Levels, die in der Wiederholung eingerostet sind. */
    val becameRusty: List<Skill>,
    /** Die Auffrischung wurde bestanden. */
    val refreshed: Boolean,
    /** Freies Training: Regeln, Punktzahl und bisheriger Bestwert. */
    val free: FreeRules? = null,
    val freeScore: Int = 0,
    val freeBestBefore: Int = 0,
    /** Einstufungstest: Auswertung. */
    val placement: PlacementOutcome? = null,
    /** Freies Training mit anpassender Schwierigkeit: Stufe am Ende (1 bis 3). */
    val freeEndLevel: Int? = null,
    /** Testsimulation: das gespeicherte Ergebnis. */
    val apRun: com.helddertierwelt.mentalmath.domain.climb.model.ApRun? = null,
)

object SessionFinisher {

    fun finish(
        worlds: List<World>,
        before: ClimbProgress,
        session: Session,
        now: Long,
        today: Long,
    ): Pair<ClimbProgress, Summary> {
        val (p, summary) = finishSession(worlds, before, session, now, today)
        // Fehlertraining: falsch gelöste Aufgabentypen des TMSnat-Trainings merken (nicht beim geführten Üben).
        val reviewed = if (session.type == SessionType.GUIDED) p else ApReview.update(p, session.results, now)
        return reviewed to summary
    }

    private fun finishSession(
        worlds: List<World>,
        before: ClimbProgress,
        session: Session,
        now: Long,
        today: Long,
    ): Pair<ClimbProgress, Summary> {
        session.free?.let { rules -> return finishFree(before, session, rules, now, today) }
        val skill = session.skillId?.let { Progression.skill(worlds, it) }
        val nextBefore = Progression.nextSkill(worlds, before)
        var p = Progression.record(before, session.results, now, today, skillDays = session.type != SessionType.GUIDED)
        var outcome: Outcome? = null
        var refreshed = false
        var placement: PlacementOutcome? = null
        when (session.type) {
            SessionType.TEST -> if (skill != null) {
                outcome = Evaluation.outcome(skill.goals, session.results)
                p = Progression.afterTest(worlds, p, skill.id, outcome, now)
            }

            SessionType.REFRESH -> if (skill != null) {
                outcome = Evaluation.outcome(skill.goals, session.results)
                p = Progression.afterRefresh(p, skill.id, outcome, now)
                refreshed = outcome.passed
            }

            SessionType.REVIEW -> p = Progression.afterReview(worlds, p, session.results.filter { !it.retry }, now)
            SessionType.PLACEMENT -> {
                val world = worlds.firstOrNull { it.id == session.worldId }
                if (world != null) {
                    val result = Placement.evaluate(world, session.results)
                    val newly = if (result.passed) world.skills.count { !p.of(it.id).mastered } else 0
                    if (result.passed) p = Progression.applyPlacement(world, p, now)
                    placement = result.copy(newlyMastered = newly)
                }
            }

            SessionType.GUIDED, SessionType.PRACTICE, SessionType.FREE -> Unit
        }
        if (session.results.isNotEmpty()) p = Progression.touchStreak(p, today)

        val nextAfter = Progression.nextSkill(worlds, p)
        val unlocked = if (nextAfter != null && nextAfter.id != nextBefore?.id && Progression.skillUnlocked(worlds, p, nextAfter.id)) nextAfter else null
        val sentBackTo = if (skill != null && session.type == SessionType.TEST && outcome?.passed == false) {
            Progression.prerequisite(worlds, skill.id)?.takeIf { pre ->
                p.of(pre.id).refreshPending && !before.of(pre.id).refreshPending
            }
        } else null
        val becameRusty = worlds.flatMap { it.skills }.filter { p.of(it.id).rusty && !before.of(it.id).rusty }

        val summary = Summary(
            type = session.type,
            skillId = session.skillId,
            title = session.title,
            results = session.results,
            outcome = outcome,
            starsBefore = skill?.let { before.of(it.id).stars } ?: 0,
            starsAfter = skill?.let { p.of(it.id).stars } ?: 0,
            unlocked = unlocked,
            sentBackTo = sentBackTo,
            becameRusty = becameRusty,
            refreshed = refreshed,
            placement = placement,
        )
        return p to summary
    }

    /** Punktzahl im freien Training: richtige Antworten. */
    fun freeScore(session: Session): Int = session.results.count { it.correct }

    private fun finishFree(before: ClimbProgress, session: Session, rules: FreeRules, now: Long, today: Long): Pair<ClimbProgress, Summary> {
        // Nicht bearbeitete Aufgaben einer Testsimulation zählen für die Punkte, aber nicht als gelöst.
        val answered = session.results.filter { !(it.timedOut && it.answer.isEmpty() && it.timeMs == 0L) }
        var stats = before.free
        answered.groupBy { it.problem.skillId }.forEach { (topic, list) ->
            val old = stats[topic] ?: com.helddertierwelt.mentalmath.domain.climb.model.FreeStat()
            stats = stats + (topic to old.copy(
                solved = old.solved + list.size,
                correct = old.correct + list.count { it.correct },
                totalMs = old.totalMs + list.sumOf { it.timeMs },
            ))
        }
        val score = freeScore(session)
        val bestBefore = before.freeBest[rules.bestKey] ?: 0
        var p = if (rules.isAp) {
            // TMSnat-Training: Ergebnisse zählen auch für die Sicherheit der Kategorien (Levels von Welt 13).
            Progression.record(before, answered, now, today)
        } else {
            Progression.recordDay(
                before.copy(
                    totalSolved = before.totalSolved + answered.size,
                    totalCorrect = before.totalCorrect + answered.count { it.correct },
                ),
                answered,
                today,
            )
        }
        p = p.copy(
            free = stats,
            freeBest = if (score > bestBefore) p.freeBest + (rules.bestKey to score) else p.freeBest,
        )
        var run: com.helddertierwelt.mentalmath.domain.climb.model.ApRun? = null
        if (rules.mode == FreeMode.SIMULATION) {
            run = com.helddertierwelt.mentalmath.domain.climb.model.ApRun(
                at = now,
                score = score,
                total = session.results.size,
                usedMs = minOf(session.usedMs, rules.param * 1000L),
                seconds = rules.param,
                options = rules.level,
            )
            p = p.copy(apRuns = (p.apRuns + run).takeLast(ClimbProgress.AP_RUNS_KEPT))
        }
        if (answered.isNotEmpty()) p = Progression.touchStreak(p, today)
        val summary = Summary(
            type = SessionType.FREE,
            skillId = null,
            title = session.title,
            results = session.results,
            outcome = null,
            starsBefore = 0,
            starsAfter = 0,
            unlocked = null,
            sentBackTo = null,
            becameRusty = emptyList(),
            refreshed = false,
            free = rules,
            freeScore = score,
            freeBestBefore = bestBefore,
            freeEndLevel = session.adaptive?.level,
            apRun = run,
        )
        return p to summary
    }
}
