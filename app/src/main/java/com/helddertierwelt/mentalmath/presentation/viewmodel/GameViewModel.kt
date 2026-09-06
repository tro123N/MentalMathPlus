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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
    private var random: Random = Random(System.currentTimeMillis())
    private val randomMutex = Mutex()

    private val _isGeneratingProblem = MutableStateFlow(false)
    val isGeneratingProblem = _isGeneratingProblem.asStateFlow()


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

    fun toggleIsRightToLeft(): Boolean {
        playSound(FeedbackType.TAP)
        val newValue = !_gameState.value.isRightToLeft
        saveState(_gameState.value.copy(isRightToLeft = newValue))
        return newValue
    }

    fun toggleIsVerticalForm(): Boolean {
        playSound(FeedbackType.TAP)
        val newValue = !_gameState.value.isVerticalForm
        saveState(_gameState.value.copy(isVerticalForm = newValue))
        return newValue
    }

    fun generateNewProblem() {

        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingProblem.value = true

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
                _isGeneratingProblem.value = false
            }
        }
    }

    fun checkAnswerAndCount() {
        val correctResult = calculateResult()
        val userInput = _gameState.value.input.toIntOrNull()
        val newState: GameState
        when (userInput) {
            null -> {
                playSound(FeedbackType.SKIPPED)
                newState = _gameState.value.copy(
                    skippedAnswers = _gameState.value.skippedAnswers + 1,
                    totalAnswers = _gameState.value.totalAnswers + 1,
                    problemStatus = ProblemStatus.SKIPPED
                )
            }

            correctResult -> {
                playSound(FeedbackType.CORRECT)
                newState = _gameState.value.copy(
                    correctAnswers = _gameState.value.correctAnswers + 1,
                    totalAnswers = _gameState.value.totalAnswers + 1,
                    problemStatus = ProblemStatus.CORRECT
                )
            }

            else -> {
                playSound(FeedbackType.WRONG)
                newState = _gameState.value.copy(
                    wrongAnswers = _gameState.value.wrongAnswers + 1,
                    totalAnswers = _gameState.value.totalAnswers + 1,
                    problemStatus = ProblemStatus.INCORRECT
                )
            }
        }
        saveState(newState)
    }

    private fun calculateResult(): Int {
        return when (_gameState.value.operator) {
            Operation.ADDITION -> _gameState.value.operand1 + _gameState.value.operand2
            Operation.SUBTRACTION -> _gameState.value.operand1 - _gameState.value.operand2
            Operation.MULTIPLICATION -> _gameState.value.operand1 * _gameState.value.operand2
            Operation.DIVISION -> if (_gameState.value.operand2 != 0) _gameState.value.operand1 / _gameState.value.operand2 else 0
        }
    }

    fun addProblemToList() {
        val solvedProblem = SolvedProblem(
            operand1 = _gameState.value.operand1,
            operand2 = _gameState.value.operand2,
            operator = _gameState.value.operator,
            correctResult = calculateResult(),
            userInput = if (_gameState.value.input.isNotEmpty()) {
                _gameState.value.input.trimStart('0').ifEmpty { "0" }
            } else {
                ""
            }
        )
        saveState(
            _gameState.value.copy(
                solvedProblems = _gameState.value.solvedProblems + solvedProblem
            )
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
                isModeEnabled = settingsState.isModeEnabled,
                modeLimit = settingsState.limit.toInt(),
                isRightToLeft = settingsState.isRightToLeftDefault,
                isVerticalForm = settingsState.isVerticalFormDefault
            )
        )
        return true
    }

    fun setRandomSettings(settingsState: SettingsState) {

        // random mode and limit
        val isModeEnabled = listOf(true, false).random()
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
                isModeEnabled = isModeEnabled,
                modeLimit = limit.toInt(),
                isRightToLeft = settingsState.isRightToLeftDefault,
                isVerticalForm = settingsState.isVerticalFormDefault
            )
        )
    }

    fun setSettingsFromLink(
        gameRecord: GameRecord,
        settingsState: SettingsState
    ) {
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
                isModeEnabled = gameRecord.isModeEnabled,
                modeLimit = gameRecord.modeLimit,
                isRightToLeft = settingsState.isRightToLeftDefault,
                isVerticalForm = settingsState.isVerticalFormDefault
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

    fun startGame(seed: Long) {
        random = Random(seed)
        generateNewProblem()
        setStartTimestamp()
        saveState(
            _gameState.value.copy(
                isGameStarted = true,
                seed = seed
            )
        )
        startTimer()
    }

    fun saveGameRecord() {
        val activeMinutes = max(1f / 60f, _gameState.value.activeTime / 60000f)
        val totalMinutes = max(1f / 60f, _gameState.value.totalTime / 60000f)
        val correctAnswers = _gameState.value.correctAnswers.toFloat()
        val totalAnswers = max(1, _gameState.value.totalAnswers).toFloat()
        val activeScore = 10f * correctAnswers.pow(2f) / (activeMinutes * totalAnswers)
        val totalScore = 10f * correctAnswers.pow(2f) / (totalMinutes * totalAnswers)

        val lastGame = GameRecord(
            isModeEnabled = _gameState.value.isModeEnabled,
            modeLimit = _gameState.value.modeLimit,
            additionRangeStart = _gameState.value.enabledOperators[Operation.ADDITION]?.first?.toInt() ?: 0,
            additionRangeEnd = _gameState.value.enabledOperators[Operation.ADDITION]?.second?.toInt() ?: 0,
            subtractionRangeStart = _gameState.value.enabledOperators[Operation.SUBTRACTION]?.first?.toInt() ?: 0,
            subtractionRangeEnd = _gameState.value.enabledOperators[Operation.SUBTRACTION]?.second?.toInt() ?: 0,
            multiplicationRangeStart = _gameState.value.enabledOperators[Operation.MULTIPLICATION]?.first?.toInt() ?: 0,
            multiplicationRangeEnd = _gameState.value.enabledOperators[Operation.MULTIPLICATION]?.second?.toInt() ?: 0,
            divisionRangeStart = _gameState.value.enabledOperators[Operation.DIVISION]?.first?.toInt() ?: 0,
            divisionRangeEnd = _gameState.value.enabledOperators[Operation.DIVISION]?.second?.toInt() ?: 0,
            totalAnswers = _gameState.value.totalAnswers,
            correctAnswers = _gameState.value.correctAnswers,
            totalTime = _gameState.value.totalTime,
            activeTime = _gameState.value.activeTime,
            activeScore = activeScore,
            totalScore = totalScore,
            solvedProblems = _gameState.value.solvedProblems,
            seed = _gameState.value.seed
        )

        viewModelScope.launch(Dispatchers.IO) {
            val newId = gameRecordRepository.insertGameRecord(lastGame)
            gameRecordRepository.setLastGameRecord(lastGame.copy(id = newId.toInt()))
        }
    }

    fun saveCancelledGameRecord() {
        val cancelledGame = GameRecord(
            isModeEnabled = _gameState.value.isModeEnabled,
            modeLimit = _gameState.value.modeLimit,
            additionRangeStart = _gameState.value.enabledOperators[Operation.ADDITION]?.first?.toInt() ?: 0,
            additionRangeEnd = _gameState.value.enabledOperators[Operation.ADDITION]?.second?.toInt() ?: 0,
            subtractionRangeStart = _gameState.value.enabledOperators[Operation.SUBTRACTION]?.first?.toInt() ?: 0,
            subtractionRangeEnd = _gameState.value.enabledOperators[Operation.SUBTRACTION]?.second?.toInt() ?: 0,
            multiplicationRangeStart = _gameState.value.enabledOperators[Operation.MULTIPLICATION]?.first?.toInt() ?: 0,
            multiplicationRangeEnd = _gameState.value.enabledOperators[Operation.MULTIPLICATION]?.second?.toInt() ?: 0,
            divisionRangeStart = _gameState.value.enabledOperators[Operation.DIVISION]?.first?.toInt() ?: 0,
            divisionRangeEnd = _gameState.value.enabledOperators[Operation.DIVISION]?.second?.toInt() ?: 0,
            seed = _gameState.value.seed
        )

        viewModelScope.launch(Dispatchers.IO) {
            gameRecordRepository.insertGameRecord(cancelledGame)
        }
    }

    fun reset() {
        saveState(GameState())
    }

    fun resetGameData() {
        reset()
        viewModelScope.launch(Dispatchers.IO) {
            gameRecordRepository.reset()
        }
    }

    fun setStartTimestamp() {
        saveState(_gameState.value.copy(startTimeStamp = System.currentTimeMillis()))
    }

    fun startTimer() {
        if (!_gameState.value.isGameStarted) return
        saveState(_gameState.value.copy(isTimerRunning = true))
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_gameState.value.isTimerRunning) {
                val elapsedTime = System.currentTimeMillis() - _gameState.value.startTimeStamp
                saveState(
                    _gameState.value.copy(
                        activeTime = elapsedTime - _gameState.value.totalPauseDuration,
                        totalTime = elapsedTime
                    )
                )
                delay(100.milliseconds)
            }
        }
    }

    fun pauseTimer() {
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

    fun startTimerIfNeeded() {
        if (_gameState.value.isTimerRunning && (timerJob == null || timerJob?.isCompleted == true)) {
            startTimer()
        }
    }

    fun resumeTimer() {
        if (_gameState.value.isGameStarted && !_gameState.value.isTimerRunning) {
            val totalPauseDuration =
                _gameState.value.totalPauseDuration + (System.currentTimeMillis() - _gameState.value.pausedAt)
            saveState(_gameState.value.copy(totalPauseDuration = totalPauseDuration))
            startTimer()
        }
    }

    fun endGame() {
        pauseTimer()
        saveGameRecord()
        val gameState = _gameState.value
        saveState(gameState.copy(isGameStarted = false))

        if (!gameState.isLinkGame) {
            val correctPercentage =
                if (gameState.totalAnswers > 0) {
                    gameState.correctAnswers.toFloat() / gameState.totalAnswers
                } else {
                    0f
                }

            val sound = when {
                correctPercentage >= 0.9f -> FeedbackType.BEST
                correctPercentage >= 0.8f -> FeedbackType.GOOD
                correctPercentage >= 0.5f -> FeedbackType.OKAY
                else -> FeedbackType.BAD
            }
            playSound(sound)
        }
    }

    fun cancelGame() {
        playSound(FeedbackType.STOP)
        pauseTimer()
        saveCancelledGameRecord()
        saveState(_gameState.value.copy(isGameStarted = false))
    }

    fun playSound(sound: FeedbackType) {
        feedbackController.play(sound)
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