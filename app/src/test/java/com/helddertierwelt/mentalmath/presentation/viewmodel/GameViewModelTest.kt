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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.entity.MathProblem
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import com.helddertierwelt.mentalmath.data.repository.MathProblemRepository
import com.helddertierwelt.mentalmath.domain.game.GameState
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import com.helddertierwelt.mentalmath.domain.settings.SettingsState
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.fake.FakeGameRecordDao
import com.helddertierwelt.mentalmath.fake.FakeMathProblemDao
import com.helddertierwelt.mentalmath.testutil.FakeLifecycleOwner
import com.helddertierwelt.mentalmath.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val scheduler get() = mainDispatcherRule.testDispatcher.scheduler

    private val additionProblems = (1..9).flatMap { a ->
        (1..a).map { b -> MathProblem(operand1 = a, operand2 = b, difficulty = 1) }
    }
    private val gameRecordDao = FakeGameRecordDao()
    private val savedStateHandle = SavedStateHandle()

    private var viewModel = createViewModel(savedStateHandle = savedStateHandle)
    private val state: GameState get() = viewModel.gameState.value

    @Test
    fun `correct answer is counted as correct`() = runGameTest {
        startGame(GameMode.PROBLEMS)

        answerCorrectly()

        assertEquals(1, state.correctAnswers)
        assertEquals(ProblemStatus.CORRECT, state.solvedProblems.single().status)
    }

    @Test
    fun `wrong answer is counted as incorrect`() = runGameTest {
        startGame(GameMode.PROBLEMS)

        answerWrong()

        assertEquals(1, state.wrongAnswers)
        assertEquals(ProblemStatus.INCORRECT, state.solvedProblems.single().status)
    }

    @Test
    fun `empty answer is counted as skipped`() = runGameTest {
        startGame(GameMode.PROBLEMS)

        skipProblem()

        assertEquals(1, state.skippedAnswers)
        assertEquals(ProblemStatus.SKIPPED, state.solvedProblems.single().status)
    }

    @Test
    fun `the same problem never appears twice in a row`() = runGameTest {
        val twoProblems = listOf(
            MathProblem(operand1 = 2, operand2 = 1, difficulty = 1),
            MathProblem(operand1 = 3, operand2 = 1, difficulty = 1)
        )
        viewModel = createViewModel(problems = twoProblems)
        startGame(GameMode.PROBLEMS, limit = 5)

        repeat(50) { answerCorrectly() }

        state.solvedProblems.zipWithNext().forEach { (previous, next) ->
            assertNotEquals(previous.sortedOperands(), next.sortedOperands())
        }
    }

    @Test
    fun `problems mode ends after ten problems per limit`() = runGameTest {
        startGame(GameMode.PROBLEMS, limit = 1)

        repeat(9) { answerCorrectly() }
        assertTrue(state.isGameStarted)

        answerCorrectly()
        assertFalse(state.isGameStarted)
    }

    @Test
    fun `finished game is saved as game record`() = runGameTest {
        startGame(GameMode.PROBLEMS, limit = 1)

        repeat(7) { answerCorrectly() }
        repeat(3) { answerWrong() }

        val record = gameRecordDao.records.single()
        assertEquals(10, record.totalAnswers)
        assertEquals(7, record.correctAnswers)
        assertEquals(state.solvedProblems, record.solvedProblems)
    }

    @Test
    fun `timed mode ends after its time limit`() = runGameTest {
        startGame(GameMode.TIMED, limit = 1)

        advanceTime(59.seconds)
        assertTrue(state.isGameStarted)

        advanceTime(1.seconds)
        assertFalse(state.isGameStarted)
    }

    @Test
    fun `survival - running out of time costs a life`() = runGameTest {
        startGame(GameMode.SURVIVAL, limit = 1)

        advanceTime(20.seconds)

        assertEquals(2, state.lives)
        assertEquals(ProblemStatus.TIMEOUT, state.solvedProblems.single().status)
    }

    @Test
    fun `survival - wrong answer costs a life`() = runGameTest {
        startGame(GameMode.SURVIVAL)

        answerWrong()

        assertEquals(2, state.lives)
    }

    @Test
    fun `survival - game ends when all lives are lost`() = runGameTest {
        startGame(GameMode.SURVIVAL)

        repeat(3) { answerWrong() }

        assertEquals(0, state.lives)
        assertFalse(state.isGameStarted)
    }

    @Test
    fun `survival - answering resets the problem timer`() = runGameTest {
        startGame(GameMode.SURVIVAL, limit = 1)

        advanceTime(15.seconds)
        answerCorrectly()
        advanceTime(15.seconds)

        assertEquals(3, state.lives)
    }

    @Test
    fun `survival - time limit shrinks after every five answers`() = runGameTest {
        startGame(GameMode.SURVIVAL, limit = 1)

        repeat(5) { answerCorrectly() }
        advanceTime(18.seconds)

        assertEquals(2, state.lives)
    }

    @Test
    fun `time in the background is not counted`() = runGameTest {
        startGame(GameMode.TIMED, limit = 1)

        leaveApp()
        advanceTime(5.minutes)
        returnToApp()
        advanceTime(1.seconds)

        assertTrue(state.isGameStarted)
        assertTrue(state.activeTime.milliseconds < 2.seconds)
    }

    @Test
    fun `survival timer does not run in the background`() = runGameTest {
        startGame(GameMode.SURVIVAL, limit = 1)

        leaveApp()
        advanceTime(1.minutes)
        returnToApp()
        advanceTime(1.seconds)

        assertEquals(3, state.lives)
    }

    @Test
    fun `game is restored after the app was killed in the background`() = runGameTest {
        startGame(GameMode.PROBLEMS)
        repeat(3) { answerCorrectly() }
        leaveApp()
        val stateBeforeKill = state

        restoreAfterAppWasKilled()

        assertEquals(stateBeforeKill, state)
    }

    @Test
    fun `timer keeps running after the app was restored`() = runGameTest {
        startGame(GameMode.TIMED, limit = 1)
        leaveApp()

        restoreAfterAppWasKilled()
        returnToApp()
        advanceTime(1.minutes)

        assertFalse(state.isGameStarted)
    }

    @Test
    fun `closing the app during a game saves it as cancelled game`() = runGameTest {
        startGame(GameMode.PROBLEMS)
        repeat(3) { answerCorrectly() }

        closeApp()

        val record = gameRecordDao.records.single()
        assertFalse(state.isGameStarted)
        assertEquals(3, record.correctAnswers)
        assertEquals(0f, record.activeScore)
    }

    @Test
    fun `same seed produces the same problems`() = runGameTest {
        val firstProblems = solveTenProblems(seed = 7)

        viewModel = createViewModel()
        val secondProblems = solveTenProblems(seed = 7)

        assertEquals(firstProblems, secondProblems)
    }

    @Test
    fun `different seeds produce different problems`() = runGameTest {
        val firstProblems = solveTenProblems(seed = 7)

        viewModel = createViewModel()
        val secondProblems = solveTenProblems(seed = 8)

        assertNotEquals(firstProblems, secondProblems)
    }

    @Test
    fun `going to the background does not change the problems`() = runGameTest {
        val uninterruptedProblems = solveTenProblems(seed = 7)

        viewModel = createViewModel()
        startGame(GameMode.PROBLEMS, limit = 1, seed = 7)
        repeat(5) { answerCorrectly() }
        leaveApp()
        advanceTime(1.minutes)
        returnToApp()
        repeat(5) { answerCorrectly() }

        assertEquals(uninterruptedProblems, state.solvedProblems)
    }

    @Test
    fun `link game replays the problems of the shared game`() = runGameTest {
        val sharedProblems = solveTenProblems(seed = 7)
        val sharedGame = gameRecordDao.records.single()

        viewModel = createViewModel()
        startLinkGame(sharedGame)
        repeat(10) { answerCorrectly() }

        assertEquals(sharedProblems, state.solvedProblems)
    }

    @Test
    fun `resetting the app clears the game and the records`() = runGameTest {
        solveTenProblems(seed = 7)

        viewModel.resetGameData()
        scheduler.runCurrent()

        val record = gameRecordDao.records.single()
        assertEquals(GameState(), state)
        assertEquals(0, record.totalAnswers)
        assertEquals(emptyList<SolvedProblem>(), record.solvedProblems)
    }

    private fun runGameTest(testBody: () -> Unit) = runTest {
        testBody()
        // the game timer loops endlessly, runTest would wait for it forever
        viewModel.viewModelScope.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createViewModel(
        problems: List<MathProblem> = additionProblems,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): GameViewModel {
        val problemDao = FakeMathProblemDao(problems)
        return GameViewModel(
            mathProblemRepository = MathProblemRepository(problemDao, problemDao, problemDao, problemDao),
            gameRecordRepository = GameRecordRepository(gameRecordDao),
            feedbackPlayer = {},
            timeProvider = { scheduler.currentTime },
            savedStateHandle = savedStateHandle
        )
    }

    private fun startGame(gameMode: GameMode, limit: Int = 1, seed: Long = 42) {
        val settings = SettingsState(
            gameMode = gameMode,
            limit = limit.toFloat(),
            isAdditionEnabled = true,
            additionRange = 1f to 1f
        )
        viewModel.setSettings(settings)
        viewModel.startGame(seed)
        waitForNextProblem()
    }

    private fun startLinkGame(sharedGame: GameRecord) {
        viewModel.setIsLinkGame(true)
        viewModel.setSettingsFromLink(sharedGame, SettingsState())
        viewModel.startGame(sharedGame.seed)
        waitForNextProblem()
    }

    private fun solveTenProblems(seed: Long): List<SolvedProblem> {
        startGame(GameMode.PROBLEMS, limit = 1, seed = seed)
        repeat(10) { answerCorrectly() }
        return state.solvedProblems
    }

    private fun answerCorrectly() = answer(state.operand1 + state.operand2)

    private fun answerWrong() = answer(state.operand1 + state.operand2 + 1)

    private fun answer(result: Int) {
        result.toString().forEach { digit -> viewModel.addLastDigit(digit.digitToInt()) }
        viewModel.submitAnswer()
        waitForNextProblem()
    }

    private fun skipProblem() {
        viewModel.submitAnswer()
        waitForNextProblem()
    }

    private fun leaveApp() = viewModel.onPause(FakeLifecycleOwner)

    private fun returnToApp() = viewModel.onResume(FakeLifecycleOwner)

    private fun closeApp() {
        viewModel.onDestroy(FakeLifecycleOwner)
        scheduler.runCurrent()
    }

    private fun restoreAfterAppWasKilled() {
        viewModel.viewModelScope.cancel()
        viewModel = createViewModel(savedStateHandle = savedStateHandle)
    }

    private fun waitForNextProblem() = advanceTime(250.milliseconds)

    private fun advanceTime(duration: Duration) {
        scheduler.advanceTimeBy(duration)
        scheduler.runCurrent()
    }

    private fun SolvedProblem.sortedOperands() = listOf(operand1, operand2).sorted()
}
