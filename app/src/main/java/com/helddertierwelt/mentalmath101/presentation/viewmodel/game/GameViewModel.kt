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

package com.helddertierwelt.mentalmath101.presentation.viewmodel.game

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath101.data.entity.GameRecord
import com.helddertierwelt.mentalmath101.data.entity.MathTask
import com.helddertierwelt.mentalmath101.data.repository.GameRecordRepository
import com.helddertierwelt.mentalmath101.data.repository.MathTaskRepository
import com.helddertierwelt.mentalmath101.presentation.viewmodel.settings.SettingsViewModel
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
import kotlin.math.pow
import kotlin.math.round
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
    val gameState = _gameState.asStateFlow()

    private var timerJob: Job? = null
    private var random: Random = Random(System.currentTimeMillis())
    private val randomMutex = Mutex()

    private val _isGeneratingTask = MutableStateFlow(false)
    val isGeneratingTask = _isGeneratingTask.asStateFlow()


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

    fun generateNewTask() {

        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingTask.value = true

            // lock all calls with the random-object to keep the order of tasks deterministic
            val (operation, operands) = randomMutex.withLock {
                val enabledOperators = _gameState.value.enabledOperators.filter { (key, value) -> value }
                val operationSetting = enabledOperators.entries.elementAt(random.nextInt(enabledOperators.size))
                val operation = operationSetting.key
                val lastOperands = gameState.value.operand1 to _gameState.value.operand2

                val mathTask: MathTask = when (operation) {
                    Operation.ADDITION -> mathTaskRepository.getRandomAdditionTaskByDifficulty(random, lastOperands)
                    Operation.SUBTRACTION -> mathTaskRepository.getRandomSubtractionTaskByDifficulty(random, lastOperands)
                    Operation.MULTIPLICATION -> mathTaskRepository.getRandomMultiplicationTaskByDifficulty(random, lastOperands)
                    Operation.DIVISION -> mathTaskRepository.getRandomDivisionTaskByDifficulty(random, lastOperands)
                }

                val operands = mathTask.operand1 to mathTask.operand2
                operation to operands
            }

            // delay to let the ui show if the answer was correct
            delay(80)
            withContext(Dispatchers.Main) {
                saveState(
                    _gameState.value.copy(
                        operand1 = operands.first,
                        operand2 = operands.second,
                        operator = operation,
                        input = "",
                        isCorrect = null
                    )
                )
                _isGeneratingTask.value = false
            }
        }
    }

    fun checkAnswerAndCount() {
        val correctResult = calculateResult()
        val userInput = _gameState.value.input.toIntOrNull()
        val newState: GameState
        when (userInput) {
            null -> {
                newState = _gameState.value.copy(
                    skippedAnswers = _gameState.value.skippedAnswers + 1,
                    totalAnswers = _gameState.value.totalAnswers + 1,
                    isCorrect = null
                )
            }

            correctResult -> {
                newState = _gameState.value.copy(
                    correctAnswers = _gameState.value.correctAnswers + 1,
                    totalAnswers = _gameState.value.totalAnswers + 1,
                    isCorrect = true
                )
            }

            else -> {
                newState = _gameState.value.copy(
                    wrongAnswers = _gameState.value.wrongAnswers + 1,
                    totalAnswers = _gameState.value.totalAnswers + 1,
                    isCorrect = false
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

    fun addTaskResultToList() {
        val taskResult = TaskResult(
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
                tasks = _gameState.value.tasks + taskResult
            )
        )
    }

    fun setSettings(settingsViewModel: SettingsViewModel) {
        val settingsState = settingsViewModel.settingsState.value
        val enabledOperators: MutableMap<Operation, Boolean> = mutableMapOf()
        enabledOperators[Operation.ADDITION] =settingsState.isAdditionEnabled
        enabledOperators[Operation.SUBTRACTION] = settingsState.isSubtractionEnabled
        enabledOperators[Operation.MULTIPLICATION] = settingsState.isMultiplicationEnabled
        enabledOperators[Operation.DIVISION] = settingsState.isDivisionEnabled
        saveState(
            _gameState.value.copy(
                enabledOperators = enabledOperators,
                isModeEnabled = settingsState.isModeEnabled,
                modeLimit = settingsState.limit.toInt()
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
        val activeScore = round(activeScoreValue * 100) / 100f
        val totalScoreValue = 10f * correctAnswers.pow(2f) / (totalMinutes * totalAnswers)
        val totalScore = round(totalScoreValue * 100) / 100f

        val lastGame = GameRecord(
            isModeEnabled = _gameState.value.isModeEnabled,
            modeLimit = _gameState.value.modeLimit,
            isAdditionEnabled = _gameState.value.enabledOperators[Operation.ADDITION] == true,
            isSubtractionEnabled = _gameState.value.enabledOperators[Operation.SUBTRACTION] == true,
            isMultiplicationEnabled = _gameState.value.enabledOperators[Operation.MULTIPLICATION] == true,
            isDivisionEnabled = _gameState.value.enabledOperators[Operation.DIVISION] == true,
            totalAnswers = _gameState.value.totalAnswers,
            correctAnswers = _gameState.value.correctAnswers,
            totalTime = _gameState.value.totalTime,
            activeTime = _gameState.value.activeTime,
            activeScore = activeScore,
            totalScore = totalScore,
            taskResults = _gameState.value.tasks,
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
            isAdditionEnabled = _gameState.value.enabledOperators[Operation.ADDITION] == true,
            isSubtractionEnabled = _gameState.value.enabledOperators[Operation.SUBTRACTION] == true,
            isMultiplicationEnabled = _gameState.value.enabledOperators[Operation.MULTIPLICATION] == true,
            isDivisionEnabled = _gameState.value.enabledOperators[Operation.DIVISION] == true,
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
            val totalPauseDuration =
                _gameState.value.totalPauseDuration + (System.currentTimeMillis() - _gameState.value.pausedAt)
            saveState(_gameState.value.copy(totalPauseDuration = totalPauseDuration))
            startTimer()
        }
    }

    fun endGame() {
        pauseTimer()
        saveGameRecord()
        saveState(_gameState.value.copy(isGameStarted = false))
    }

    fun cancelGame() {
        pauseTimer()
        saveCancelledGameRecord()
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