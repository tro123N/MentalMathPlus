/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025 HeldDerTierwelt

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
import com.helddertierwelt.mentalmath.data.entity.MathTask
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import com.helddertierwelt.mentalmath.data.repository.MathTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

@HiltViewModel
class GameViewModel @Inject constructor(
    private val mathTaskRepository: MathTaskRepository,
    private val gameRecordRepository: GameRecordRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver {

    private val _gameState = MutableStateFlow(
        savedStateHandle.get<GameState>("game_state") ?: GameState()
    )
    val gamesState = _gameState.asStateFlow()

    private var timerJob: Job? = null
    private var random: Random = Random(System.currentTimeMillis())

    fun appendToInput(number: Int) {
        if (_gameState.value.input.length < 7) {
            if (_gameState.value.input == "0") {
                clearInput()
            }
            saveState(_gameState.value.copy(input = _gameState.value.input + number))
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

    fun generateNewTask() {
        clearInput()
        val enabledOperators = _gameState.value.enabledOperators
        val operationSetting =
            enabledOperators.entries.elementAt(random.nextInt(enabledOperators.size))
        val operation = operationSetting.key
        val difficulty = random.nextInt(
            operationSetting.value.first.toInt(),
            operationSetting.value.second.toInt() + 1
        )
        val lastOperands = max(
            _gameState.value.operand1,
            _gameState.value.operand2
        ) to min(_gameState.value.operand1, _gameState.value.operand2)
        viewModelScope.launch(Dispatchers.IO) {
            val mathTask: MathTask = when (operation) {
                Operation.ADDITION -> mathTaskRepository.getRandomAdditionTaskByDifficulty(
                    difficulty,
                    random,
                    lastOperands
                )

                Operation.SUBTRACTION -> mathTaskRepository.getRandomSubtractionTaskByDifficulty(
                    difficulty,
                    random,
                    lastOperands
                )

                Operation.MULTIPLICATION -> mathTaskRepository.getRandomMultiplicationTaskByDifficulty(
                    difficulty,
                    random,
                    lastOperands
                )

                Operation.DIVISION -> mathTaskRepository.getRandomDivisionTaskByDifficulty(
                    difficulty,
                    random,
                    lastOperands
                )
            }

            val operands = when (operation) {
                Operation.SUBTRACTION -> mathTask.operand1 to mathTask.operand2
                Operation.DIVISION -> (mathTask.operand1 * mathTask.operand2) to mathTask.operand1
                Operation.ADDITION,
                Operation.MULTIPLICATION -> {
                    if (random.nextBoolean())
                        mathTask.operand1 to mathTask.operand2
                    else
                        mathTask.operand2 to mathTask.operand1
                }
            }
            saveState(
                _gameState.value.copy(
                    operand1 = operands.first,
                    operand2 = operands.second,
                    operator = operation,
                    input = "",
                    isCorrect = null
                )
            )
        }
    }

    fun checkAnswerAndCount() {
        val correctResult = calculateResult()
        val userInput = _gameState.value.input.toIntOrNull()
        val newState: GameState
        if (userInput == null) {
            newState = _gameState.value.copy(
                skippedAnswers = _gameState.value.skippedAnswers + 1,
                totalAnswers = _gameState.value.totalAnswers + 1,
                isCorrect = null
            )
        } else if (userInput == correctResult) {
            newState = _gameState.value.copy(
                correctAnswers = _gameState.value.correctAnswers + 1,
                totalAnswers = _gameState.value.totalAnswers + 1,
                isCorrect = true
            )
        } else {
            newState = _gameState.value.copy(
                wrongAnswers = _gameState.value.wrongAnswers + 1,
                totalAnswers = _gameState.value.totalAnswers + 1,
                isCorrect = false
            )
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

    fun addTaskResultToList() {
        val taskResult = TaskResult(
            operand1 = _gameState.value.operand1,
            operand2 = _gameState.value.operand2,
            operator = _gameState.value.operator,
            correctResult = calculateResult(),
            userInput = _gameState.value.input
        )
        saveState(
            _gameState.value.copy(
                tasks = _gameState.value.tasks + taskResult
            )
        )
    }

    fun setSettings(settingsViewModel: SettingsViewModel) {
        val settingsState = settingsViewModel.settingsState.value
        val enabledOperators: MutableMap<Operation, Pair<Float, Float>> = mutableMapOf()
        if (settingsState.isAdditionEnabled) {
            enabledOperators.put(
                Operation.ADDITION,
                settingsViewModel.settingsState.value.additionRange
            )
        }
        if (settingsState.isSubtractionEnabled) {
            enabledOperators.put(
                Operation.SUBTRACTION,
                settingsViewModel.settingsState.value.subtractionRange
            )
        }
        if (settingsState.isMultiplicationEnabled) {
            enabledOperators.put(
                Operation.MULTIPLICATION,
                settingsViewModel.settingsState.value.multiplicationRange
            )
        }
        if (settingsState.isDivisionEnabled) {
            enabledOperators.put(
                Operation.DIVISION,
                settingsViewModel.settingsState.value.divisionRange
            )
        }
        saveState(
            _gameState.value.copy(
                enabledOperators = enabledOperators,
                isModeEnabled = settingsState.isModeEnabled,
                modeLimit = settingsState.limit.toInt()
            )
        )
    }

    fun startGame(seed: Long) {
        random = Random(seed)
        generateNewTask()
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
        val activeMinutes = max(1, (_gameState.value.activeTime / 1000)) / 60.0f
        val totalMinutes = max(1, (_gameState.value.totalTime / 1000)) / 60.0f
        val correctAnswers = _gameState.value.correctAnswers.toFloat()
        val totalAnswers = max(1, _gameState.value.totalAnswers).toFloat()
        val activeScoreValue = 10f * correctAnswers.pow(2f) / (activeMinutes * totalAnswers)
        val activeScore = kotlin.math.round(activeScoreValue * 100) / 100f
        val totalScoreValue = 10f * correctAnswers.pow(2f) / (totalMinutes * totalAnswers)
        val totalScore = kotlin.math.round(totalScoreValue * 100) / 100f

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
            taskResults = _gameState.value.tasks,
            seed = _gameState.value.seed
        )

        gameRecordRepository.setLastGameRecord(lastGame)
        if (0f != activeScore || 0f != totalScore) {
            viewModelScope.launch(Dispatchers.IO) {
                gameRecordRepository.insertGameRecord(lastGame)
            }
        }
    }

    fun resetGame() {
        saveState(GameState())
    }

    fun resetGameData() {
        resetGame()
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
                delay(100)
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
            val totalPauseDuration = _gameState.value.totalPauseDuration + (System.currentTimeMillis() - _gameState.value.pausedAt)
            saveState(_gameState.value.copy(totalPauseDuration = totalPauseDuration))
            startTimer()
        }
    }

    fun endGame() {
        pauseTimer()
        saveGameRecord()
        saveState(_gameState.value.copy(isGameStarted = false))
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        pauseTimer()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        resumeTimer()
    }

    private fun saveState(newState: GameState) {
        _gameState.value = newState
        savedStateHandle["game_state"] = newState
    }
}