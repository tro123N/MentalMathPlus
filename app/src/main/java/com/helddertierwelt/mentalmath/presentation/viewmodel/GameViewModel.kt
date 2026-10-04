/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025-2026 HeldDerTierwelt

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

package com.helddertierwelt.mentalmath.presentation.viewmodel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.entity.MathProblem
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import com.helddertierwelt.mentalmath.data.repository.MathProblemRepository
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackController
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.domain.game.GameState
import com.helddertierwelt.mentalmath.domain.game.Operation
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import com.helddertierwelt.mentalmath.domain.settings.SettingsState
import com.helddertierwelt.mentalmath.domain.settings.enums.FormMode
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.domain.settings.enums.InputDirectionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class GameViewModel @Inject constructor(
    private val mathProblemRepository: MathProblemRepository,
    private val gameRecordRepository: GameRecordRepository,
    private val feedbackController: FeedbackController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver {

    private val _gameState = MutableStateFlow(
        savedStateHandle.get<GameState>("game_state") ?: GameState()
    )
    val gameState = _gameState.asStateFlow()

    private var timerJob: Job? = null

    sealed interface GameEvent {
        data object GameFinished : GameEvent
    }

    private val _gameEvents = MutableSharedFlow<GameEvent>()
    val gameEvents = _gameEvents.asSharedFlow()

    private var random: Random = Random(System.currentTimeMillis())
    private val randomMutex = Mutex()

    private val _disableEnterButton = MutableStateFlow(false)
    val disableEnterButton = _disableEnterButton.asStateFlow()

    fun addLastDigit(number: Int) {
        if (_gameState.value.input.length < 7) {
            if (_gameState.value.input == "0") {
                clearInput()
            }
            saveState(_gameState.value.copy(input = _gameState.value.input + number))
        }
    }

    fun addFirstDigit(number: Int) {
        if (_gameState.value.input.length < 7) {
            saveState(_gameState.value.copy(input = number.toString() + _gameState.value.input))
        }
    }

    fun removeFirstDigit() {
        if (_gameState.value.input.isNotEmpty()) {
            saveState(_gameState.value.copy(input = _gameState.value.input.drop(1)))
        }
    }

    fun removeLastDigit() {
        if (_gameState.value.input.isNotEmpty()) {
            saveState(_gameState.value.copy(input = _gameState.value.input.dropLast(1)))
        }
    }

    fun clearInput() {
        saveState(_gameState.value.copy(input = ""))
    }

    fun toggleInputDirection(): InputDirectionMode {
        playFeedback(FeedbackType.TAP)

        val newInputDirectionMode = when (_gameState.value.inputDirectionMode) {
            InputDirectionMode.LEFT_TO_RIGHT -> InputDirectionMode.RIGHT_TO_LEFT
            InputDirectionMode.RIGHT_TO_LEFT -> InputDirectionMode.LEFT_TO_RIGHT
        }

        saveState(
            _gameState.value.copy(inputDirectionMode = newInputDirectionMode)
        )
        return newInputDirectionMode
    }

    fun toggleFormMode(): FormMode {
        playFeedback(FeedbackType.TAP)

        val newFormMode = when (_gameState.value.formMode) {
            FormMode.HORIZONTAL -> FormMode.VERTICAL
            FormMode.VERTICAL -> FormMode.HORIZONTAL
        }

        saveState(
            _gameState.value.copy(formMode = newFormMode)
        )
        return newFormMode
    }

    fun startGame(seed: Long) {
        _disableEnterButton.value = false
        random = Random(seed)
        generateNewProblem()
        setStartTimestamps()
        saveState(
            _gameState.value.copy(
                isGameStarted = true,
                seed = seed
            )
        )
        startTimer()
    }

    private fun finishProblem(
        problemStatus: ProblemStatus,
        userInput: String
    ) {
        _disableEnterButton.value = true

        val state = _gameState.value
        val newState = when (problemStatus) {
            ProblemStatus.CORRECT -> {
                playFeedback(FeedbackType.CORRECT)

                state.copy(
                    correctAnswers = state.correctAnswers + 1,
                    totalAnswers = state.totalAnswers + 1,
                    problemStatus = problemStatus
                )
            }

            ProblemStatus.INCORRECT -> {
                playFeedback(FeedbackType.WRONG)

                state.copy(
                    wrongAnswers = state.wrongAnswers + 1,
                    totalAnswers = state.totalAnswers + 1,
                    problemStatus = problemStatus
                )
            }

            ProblemStatus.SKIPPED -> {
                playFeedback(FeedbackType.SKIPPED)

                state.copy(
                    skippedAnswers = state.skippedAnswers + 1,
                    totalAnswers = state.totalAnswers + 1,
                    problemStatus = problemStatus
                )
            }

            ProblemStatus.TIMEOUT -> {
                playFeedback(FeedbackType.TIMEOUT)

                state.copy(
                    timeoutAnswers = state.timeoutAnswers + 1,
                    totalAnswers = state.totalAnswers + 1,
                    problemStatus = problemStatus
                )
            }
        }

        val solvedProblem = SolvedProblem(
            operand1 = state.operand1,
            operand2 = state.operand2,
            operator = state.operator,
            userInput = userInput,
            status = problemStatus
        )

        saveState(
            newState.copy(
                solvedProblems = state.solvedProblems + solvedProblem
            )
        )
        resetSurvivalProblemStart()
    }

    fun submitAnswer() {
        val state = _gameState.value
        val userInput = state.input.toIntOrNull()
        val correctResult = calculateResult()

        val status = when (userInput) {
            null -> ProblemStatus.SKIPPED
            correctResult -> ProblemStatus.CORRECT
            else -> ProblemStatus.INCORRECT
        }

        val formattedInput = if (state.input.isNotEmpty()) {
            state.input.trimStart('0').ifEmpty { "0" }
        } else {
            ""
        }

        finishProblem(
            problemStatus = status,
            userInput = formattedInput
        )

        if (
            state.gameMode == GameMode.SURVIVAL &&
            status != ProblemStatus.CORRECT
        ) {
            loseLife()
        }

        if (shouldEndGame()) {
            endGame()
        } else {
            generateNewProblem()
        }
    }

    private fun calculateResult(): Int {
        return when (_gameState.value.operator) {
            Operation.ADDITION -> _gameState.value.operand1 + _gameState.value.operand2
            Operation.SUBTRACTION -> _gameState.value.operand1 - _gameState.value.operand2
            Operation.MULTIPLICATION -> _gameState.value.operand1 * _gameState.value.operand2
            Operation.DIVISION -> if (_gameState.value.operand2 != 0) _gameState.value.operand1 / _gameState.value.operand2 else 0
        }
    }

    private fun shouldEndGame(): Boolean {
        val state = _gameState.value

        return when (state.gameMode) {
            GameMode.PROBLEMS -> {
                val totalProblems = state.modeLimit * 10
                state.totalAnswers >= totalProblems
            }

            GameMode.TIMED -> {
                val totalTime = state.modeLimit.toLong() * 60_000L
                state.activeTime >= totalTime
            }

            GameMode.SURVIVAL -> state.lives <= 0 || state.totalAnswers >= 100

        }
    }

    private fun generateNewProblem() {

        viewModelScope.launch(Dispatchers.IO) {

            // lock all calls with the random-object to keep the order of problems deterministic
            val (operation, operands) = randomMutex.withLock {
                val enabledOperators = _gameState.value.enabledOperators
                val operationSetting = enabledOperators.entries.elementAt(random.nextInt(enabledOperators.size))
                val operation = operationSetting.key
                val difficulty =
                    random.nextInt(operationSetting.value.first.toInt(), operationSetting.value.second.toInt() + 1)
                val lastOperands = max(_gameState.value.operand1, _gameState.value.operand2) to min(
                    _gameState.value.operand1,
                    _gameState.value.operand2
                )

                val mathProblem: MathProblem = when (operation) {
                    Operation.ADDITION -> mathProblemRepository.getRandomAdditionProblemByDifficulty(
                        difficulty,
                        random,
                        lastOperands
                    )

                    Operation.SUBTRACTION -> mathProblemRepository.getRandomSubtractionProblemByDifficulty(
                        difficulty,
                        random,
                        lastOperands
                    )

                    Operation.MULTIPLICATION -> mathProblemRepository.getRandomMultiplicationProblemByDifficulty(
                        difficulty,
                        random,
                        lastOperands
                    )

                    Operation.DIVISION -> mathProblemRepository.getRandomDivisionProblemByDifficulty(
                        difficulty,
                        random,
                        lastOperands
                    )
                }

                val operands = when (operation) {
                    Operation.SUBTRACTION -> mathProblem.operand1 to mathProblem.operand2
                    Operation.DIVISION -> mathProblem.operand1 to mathProblem.operand2
                    Operation.ADDITION,
                    Operation.MULTIPLICATION -> {
                        if (random.nextBoolean())
                            mathProblem.operand1 to mathProblem.operand2
                        else
                            mathProblem.operand2 to mathProblem.operand1
                    }
                }
                operation to operands
            }

            // delay to let the ui show if the answer was correct
            delay(80.milliseconds)
            withContext(Dispatchers.Main) {
                saveState(
                    _gameState.value.copy(
                        operand1 = operands.first,
                        operand2 = operands.second,
                        operator = operation,
                        input = "",
                        problemStatus = ProblemStatus.SKIPPED
                    )
                )
            }

            delay(170.milliseconds)
            withContext(Dispatchers.Main) {
                _disableEnterButton.value = false
            }
        }
    }

    fun endGame() {
        pauseTimer()
        saveGameRecord()
        val gameState = _gameState.value
        saveState(gameState.copy(isGameStarted = false))

        if (!gameState.isLinkGame) {
            val correctPercentage = gameState.correctAnswers.toFloat() / gameState.totalAnswers.coerceAtLeast(1)

            val feedbackType = when {
                correctPercentage == 1f -> FeedbackType.RESULT_PERFECT
                correctPercentage >= 0.8f -> FeedbackType.RESULT_GOOD
                correctPercentage >= 0.6f -> FeedbackType.RESULT_OKAY
                else -> FeedbackType.RESULT_BAD
            }
            playDelayedFeedback(feedbackType)
        }

        viewModelScope.launch {
            _gameEvents.emit(GameEvent.GameFinished)
        }
    }

    fun cancelGame() {
        playFeedback(FeedbackType.STOP)
        pauseTimer()
        saveCancelledGameRecord()
        saveState(_gameState.value.copy(isGameStarted = false))
    }

    private fun Long.toTenths(): Float {
        return (this / 100L) / 10f
    }

    private fun saveGameRecord() {
        val state = _gameState.value

        val activeSeconds = max(0.1f, state.activeTime.toTenths())
        val totalSeconds = max(0.1f, state.totalTime.toTenths())
        val activeMinutes = activeSeconds / 60f
        val totalMinutes = totalSeconds / 60f

        val correctAnswers = state.correctAnswers.toFloat()
        val totalAnswers = max(1, state.totalAnswers).toFloat()

        fun standardScore(minutes: Float): Float {
            if (minutes <= 0f || totalAnswers <= 0f) return 0f

            return 10f * correctAnswers.pow(2f) / (minutes * totalAnswers)
        }

        fun survivalScore(seconds: Float): Float {
            if (totalAnswers <= 0f) return 0f

            val timeLimit = gameState.value.modeLimit * 10f
            val timeRatio = seconds / (totalAnswers * timeLimit)

            return (correctAnswers * 10f - timeRatio * 10f).coerceAtLeast(0f)
        }

        val (activeScore, totalScore) = if (state.gameMode == GameMode.SURVIVAL) {
            survivalScore(activeSeconds) to survivalScore(totalSeconds)
        } else {
            standardScore(activeMinutes) to standardScore(totalMinutes)
        }

        val gameRecord = createGameRecord(
            state = state,
            activeScore = activeScore,
            totalScore = totalScore
        )

        viewModelScope.launch(Dispatchers.IO) {
            val newId = gameRecordRepository.insertGameRecord(gameRecord)
            gameRecordRepository.setLastGameRecord(
                gameRecord.copy(id = newId.toInt())
            )
        }
    }

    private fun saveCancelledGameRecord() {
        val cancelledGame = createGameRecord(_gameState.value)
        viewModelScope.launch(Dispatchers.IO) {
            gameRecordRepository.insertGameRecord(cancelledGame)
        }
    }

    private fun createGameRecord(state: GameState, activeScore: Float = 0f, totalScore: Float = 0f): GameRecord {
        return GameRecord(
            gameMode = state.gameMode,
            modeLimit = state.modeLimit,
            additionRangeStart = state.enabledOperators[Operation.ADDITION]?.first?.toInt() ?: 0,
            additionRangeEnd = state.enabledOperators[Operation.ADDITION]?.second?.toInt() ?: 0,
            subtractionRangeStart = state.enabledOperators[Operation.SUBTRACTION]?.first?.toInt() ?: 0,
            subtractionRangeEnd = state.enabledOperators[Operation.SUBTRACTION]?.second?.toInt() ?: 0,
            multiplicationRangeStart = state.enabledOperators[Operation.MULTIPLICATION]?.first?.toInt() ?: 0,
            multiplicationRangeEnd = state.enabledOperators[Operation.MULTIPLICATION]?.second?.toInt() ?: 0,
            divisionRangeStart = state.enabledOperators[Operation.DIVISION]?.first?.toInt() ?: 0,
            divisionRangeEnd = state.enabledOperators[Operation.DIVISION]?.second?.toInt() ?: 0,
            totalAnswers = state.totalAnswers,
            correctAnswers = state.correctAnswers,
            totalTime = state.totalTime,
            activeTime = state.activeTime,
            activeScore = activeScore,
            totalScore = totalScore,
            solvedProblems = state.solvedProblems,
            seed = state.seed
        )
    }

    fun setSettings(settingsState: SettingsState) : Boolean {
        val enabledOperators = buildMap {
            if (settingsState.isAdditionEnabled) {
                put(Operation.ADDITION, settingsState.additionRange)
            }
            if (settingsState.isSubtractionEnabled) {
                put(Operation.SUBTRACTION, settingsState.subtractionRange)
            }
            if (settingsState.isMultiplicationEnabled) {
                put(Operation.MULTIPLICATION, settingsState.multiplicationRange)
            }
            if (settingsState.isDivisionEnabled) {
                put(Operation.DIVISION, settingsState.divisionRange)
            }
        }
        if (enabledOperators.isEmpty()) return false

        saveState(
            _gameState.value.copy(
                enabledOperators = enabledOperators,
                gameMode = settingsState.gameMode,
                modeLimit = settingsState.limit.toInt(),
                inputDirectionMode = settingsState.inputDirectionMode,
                formMode = settingsState.formMode
            )
        )
        return true
    }

    fun setRandomSettings(settingsState: SettingsState) {

        // random mode and limit
        val gameMode = GameMode.entries.toTypedArray().random()
        val limit = (1..5).random().toFloat()

        // random difficultyRanges
        val pairs = (1..9).flatMap { a ->
            (a..9).map { b -> a.toFloat() to b.toFloat() }
        }

        val additionRange = pairs.random()
        val subtractionRange = pairs.random()
        val multiplicationRange = pairs.random()
        val divisionRange = pairs.random()

        // random selection of operations
        val operationSelection = (1..15).random()

        val isAdditionEnabled = ((operationSelection shr 0) and 1) == 1
        val isSubtractionEnabled = ((operationSelection shr 1) and 1) == 1
        val isMultiplicationEnabled = ((operationSelection shr 2) and 1) == 1
        val isDivisionEnabled = ((operationSelection shr 3) and 1) == 1

        val enabledOperators = buildMap {
            if (isAdditionEnabled) {
                put(Operation.ADDITION, additionRange)
            }
            if (isSubtractionEnabled) {
                put(Operation.SUBTRACTION, subtractionRange)
            }
            if (isMultiplicationEnabled) {
                put(Operation.MULTIPLICATION, multiplicationRange)
            }
            if (isDivisionEnabled) {
                put(Operation.DIVISION, divisionRange)
            }
        }

        saveState(
            _gameState.value.copy(
                enabledOperators = enabledOperators,
                gameMode = gameMode,
                modeLimit = limit.toInt(),
                inputDirectionMode = settingsState.inputDirectionMode,
                formMode = settingsState.formMode
            )
        )
    }

    fun setSettingsFromLink(gameRecord: GameRecord, settingsState: SettingsState) {
        val additionRange = gameRecord.additionRangeStart.toFloat() to gameRecord.additionRangeEnd.toFloat()
        val subtractionRange = gameRecord.subtractionRangeStart.toFloat() to gameRecord.subtractionRangeEnd.toFloat()
        val multiplicationRange = gameRecord.multiplicationRangeStart.toFloat() to gameRecord.multiplicationRangeEnd.toFloat()
        val divisionRange = gameRecord.divisionRangeStart.toFloat() to gameRecord.divisionRangeEnd.toFloat()

        val enabledOperators = buildMap {
            if (additionRange.first != 0f && additionRange.second != 0f) {
                put(Operation.ADDITION, additionRange)
            }
            if (subtractionRange.first != 0f && subtractionRange.second != 0f) {
                put(Operation.SUBTRACTION, subtractionRange)
            }
            if (multiplicationRange.first != 0f && multiplicationRange.second != 0f) {
                put(Operation.MULTIPLICATION, multiplicationRange)
            }
            if (divisionRange.first != 0f && divisionRange.second != 0f) {
                put(Operation.DIVISION, divisionRange)
            }
        }
        if (enabledOperators.isEmpty()) return
        saveState(
            _gameState.value.copy(
                enabledOperators = enabledOperators,
                gameMode = gameRecord.gameMode,
                modeLimit = gameRecord.modeLimit,
                inputDirectionMode = settingsState.inputDirectionMode,
                formMode = settingsState.formMode
            )
        )
    }

    fun setIsLinkGame(isLinkGame: Boolean) {
        saveState(
            _gameState.value.copy(
                isLinkGame = isLinkGame
            )
        )
    }

    fun resetGameState() {
        saveState(GameState())
    }

    fun resetGameData() {
        resetGameState()
        viewModelScope.launch(Dispatchers.IO) {
            gameRecordRepository.reset()
        }
    }

    fun playFeedback(feedbackType: FeedbackType) {
        feedbackController.play(feedbackType)
    }

    fun playDelayedFeedback(feedbackType: FeedbackType) {
        viewModelScope.launch {
            delay(400.milliseconds)
            playFeedback(feedbackType)
        }
    }

    private fun setStartTimestamps() {
        saveState(_gameState.value.copy(
            startTimeStamp = System.currentTimeMillis(),
            survivalProblemStartTime = System.currentTimeMillis()
        ))
    }

    private fun startTimer() {
        if (!_gameState.value.isGameStarted) return
        saveState(_gameState.value.copy(isTimerRunning = true))

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_gameState.value.isTimerRunning) {
                updateGameTime()
                delay(100.milliseconds)
            }
        }
    }

    fun startTimerIfNeeded() {
        if (_gameState.value.isTimerRunning && (timerJob == null || timerJob?.isCompleted == true)) {
            startTimer()
        }
    }

    private fun updateGameTime() {
        val state = _gameState.value

        val elapsedTime = System.currentTimeMillis() - state.startTimeStamp
        val activeTime = elapsedTime - state.totalPauseDuration

        saveState(
            state.copy(
                activeTime = activeTime,
                totalTime = elapsedTime
            )
        )

        if (state.gameMode == GameMode.SURVIVAL) {
            updateSurvivalTimer()
            return
        }

        if (shouldEndGame()) {
            endGame()
        }
    }

    private fun loseLife() {
        saveState(
            _gameState.value.copy(
                lives = (_gameState.value.lives - 1).coerceAtLeast(0)
            )
        )
    }

    private fun updateSurvivalTimer() {
        if (getSurvivalTimeLeft() <= 0L) {
            finishProblem(
                problemStatus = ProblemStatus.TIMEOUT,
                userInput = ""
            )
            loseLife()

            if (shouldEndGame()) {
                endGame()
            } else {
                generateNewProblem()
            }
        }
    }

    private fun resetSurvivalProblemStart() {
        if (_gameState.value.gameMode != GameMode.SURVIVAL) return

        saveState(
            _gameState.value.copy(
                survivalProblemStartTime = System.currentTimeMillis()
            )
        )
    }

    private fun getSurvivalTimeLimit(state: GameState): Int {
        val startSeconds = state.modeLimit * 10
        val groups = state.totalAnswers / 5

        val reduction = startSeconds / 10
        val minimumTime = startSeconds / 5

        val percentageTime = startSeconds - groups * reduction

        return percentageTime.coerceAtLeast(minimumTime)
    }

    fun getSurvivalTimeLeft(): Long {
        val state = _gameState.value

        val elapsedTime =
            System.currentTimeMillis() - state.survivalProblemStartTime

        val timeLimit =
            getSurvivalTimeLimit(state) * 1000L

        return (timeLimit - elapsedTime).coerceAtLeast(0L)
    }

    fun getSurvivalProgress(): Float {

        val timeLimit = getSurvivalTimeLimit(_gameState.value) * 1000L
        val timeLeft = getSurvivalTimeLeft()

        return (1f - timeLeft.toFloat() / timeLimit)
            .coerceIn(0f, 1f)
    }

    private fun pauseTimer() {
        if (_gameState.value.isGameStarted && _gameState.value.isTimerRunning) {
            saveState(
                _gameState.value.copy(
                    pausedAt = System.currentTimeMillis(),
                    isTimerRunning = false
                )
            )
            timerJob?.cancel()
        }
    }

    private fun resumeTimer() {
        if (!_gameState.value.isGameStarted ||
            _gameState.value.isTimerRunning
        ) return

        val state = _gameState.value
        val now = System.currentTimeMillis()
        val pauseDuration = now - state.pausedAt

        val newState = state.copy(
            totalPauseDuration = state.totalPauseDuration + pauseDuration,
            survivalProblemStartTime = if (state.gameMode == GameMode.SURVIVAL) {
                state.survivalProblemStartTime + pauseDuration
            } else {
                state.survivalProblemStartTime
            }
        )

        saveState(newState)
        startTimer()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        pauseTimer()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        resumeTimer()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        if (_gameState.value.isGameStarted) {
            cancelGame()
        }
    }

    private fun saveState(newState: GameState) {
        _gameState.value = newState
        savedStateHandle["game_state"] = newState
    }
}