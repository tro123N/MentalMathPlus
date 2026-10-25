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

package com.helddertierwelt.mentalmath

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helddertierwelt.mentalmath.data.dao.GameRecordDao
import com.helddertierwelt.mentalmath.data.db.GameRecordDatabase
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.entity.GameScore
import com.helddertierwelt.mentalmath.domain.game.Operation
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameRecordDaoTest {

    private lateinit var database: GameRecordDatabase
    private lateinit var dao: GameRecordDao

    private val game = GameRecord(
        gameMode = GameMode.PROBLEMS,
        modeLimit = 2,
        additionRangeStart = 1,
        additionRangeEnd = 3,
        multiplicationRangeStart = 2,
        multiplicationRangeEnd = 5,
        totalAnswers = 20,
        correctAnswers = 18,
        activeTime = 60_000,
        totalTime = 65_000,
        activeScore = 10f,
        totalScore = 9f,
        seed = 42,
        solvedProblems = listOf(
            SolvedProblem(3, Operation.ADDITION, 2, userInput = "5", status = ProblemStatus.CORRECT),
            SolvedProblem(4, Operation.MULTIPLICATION, 5, userInput = "", status = ProblemStatus.SKIPPED)
        )
    )

    private val gameWithOtherSettings = game.copy(modeLimit = 3)

    @Before
    fun openDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GameRecordDatabase::class.java).build()
        dao = database.gameRecordDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savedGameIsLoadedUnchanged() = runBlocking {
        val id = dao.insert(game).toInt()

        assertEquals(game.copy(id = id), dao.findById(id))
    }

    @Test
    fun unknownIdReturnsNothing() = runBlocking {
        assertNull(dao.findById(99))
    }

    @Test
    fun historyContainsOnlyScoredGamesNewestFirst() = runBlocking {
        val firstId = dao.insert(game).toInt()
        dao.insert(game.copy(activeScore = 0f))
        val thirdId = dao.insert(game).toInt()

        assertEquals(listOf(thirdId, firstId), dao.findAll().map { it.id })
    }

    @Test
    fun gameWithSameSettingsAndSeedWasAlreadyPlayed() = runBlocking {
        dao.insert(game)

        assertTrue(wasAlreadyPlayed(game))
        assertFalse(wasAlreadyPlayed(game.copy(seed = 43)))
        assertFalse(wasAlreadyPlayed(gameWithOtherSettings))
    }

    @Test
    fun gamesWithoutScoreDoNotCountAsScored() = runBlocking {
        dao.insert(game.copy(activeScore = 0f))
        dao.insert(gameWithOtherSettings)

        assertFalse(hasGamesWithScore(game))
    }

    @Test
    fun gameWithScoreIsFoundForItsSettings() = runBlocking {
        dao.insert(game)

        assertTrue(hasGamesWithScore(game))
        assertFalse(hasGamesWithScore(gameWithOtherSettings))
    }

    @Test
    fun bestScoresAreSortedHighestFirst() = runBlocking {
        val lowId = dao.insert(game.copy(activeScore = 5f)).toInt()
        val highId = dao.insert(game.copy(activeScore = 20f)).toInt()
        val middleId = dao.insert(game.copy(activeScore = 10f)).toInt()

        val expectedScores = listOf(
            GameScore(highId, 20f),
            GameScore(middleId, 10f),
            GameScore(lowId, 5f)
        )
        assertEquals(expectedScores, findBestGameScores(game))
    }

    @Test
    fun equalBestScoresShowTheNewerGameFirst() = runBlocking {
        val olderId = dao.insert(game).toInt()
        val newerId = dao.insert(game).toInt()

        assertEquals(listOf(newerId, olderId), findBestGameScores(game).map { it.id })
    }

    @Test
    fun bestScoresContainOnlyTheTopTenWithScoreAndSameSettings() = runBlocking {
        repeat(12) { index -> dao.insert(game.copy(activeScore = index + 1f)) }
        dao.insert(game.copy(activeScore = 0f))
        dao.insert(gameWithOtherSettings.copy(activeScore = 100f))

        val bestScores = findBestGameScores(game).map { it.activeScore }

        assertEquals((12 downTo 3).map { it.toFloat() }, bestScores)
    }

    @Test
    fun resetClearsResultsButKeepsGames() = runBlocking {
        val id = dao.insert(game).toInt()

        dao.resetAllRecords()

        val resetGame = game.copy(
            id = id,
            totalAnswers = 0,
            correctAnswers = 0,
            activeTime = 0,
            totalTime = 0,
            activeScore = 0f,
            totalScore = 0f,
            solvedProblems = emptyList()
        )
        assertEquals(resetGame, dao.findById(id))
        assertTrue(dao.findAll().isEmpty())
        assertFalse(hasGamesWithScore(game))
    }

    @Test
    fun linkGameStaysPlayedAfterReset() = runBlocking {
        dao.insert(game)

        dao.resetAllRecords()

        assertTrue(wasAlreadyPlayed(game))
    }

    @Test
    fun deleteAllRemovesEveryGame() = runBlocking {
        val id = dao.insert(game).toInt()

        dao.deleteAll()

        assertNull(dao.findById(id))
        assertFalse(wasAlreadyPlayed(game))
    }

    private suspend fun wasAlreadyPlayed(game: GameRecord) = dao.wasAlreadyPlayed(
        game.gameMode, game.modeLimit,
        game.additionRangeStart, game.additionRangeEnd,
        game.subtractionRangeStart, game.subtractionRangeEnd,
        game.multiplicationRangeStart, game.multiplicationRangeEnd,
        game.divisionRangeStart, game.divisionRangeEnd,
        game.seed
    )

    private suspend fun hasGamesWithScore(settings: GameRecord) = dao.hasGamesWithScore(
        settings.gameMode, settings.modeLimit,
        settings.additionRangeStart, settings.additionRangeEnd,
        settings.subtractionRangeStart, settings.subtractionRangeEnd,
        settings.multiplicationRangeStart, settings.multiplicationRangeEnd,
        settings.divisionRangeStart, settings.divisionRangeEnd
    )

    private suspend fun findBestGameScores(settings: GameRecord) = dao.findBestGameScores(
        settings.gameMode, settings.modeLimit,
        settings.additionRangeStart, settings.additionRangeEnd,
        settings.subtractionRangeStart, settings.subtractionRangeEnd,
        settings.multiplicationRangeStart, settings.multiplicationRangeEnd,
        settings.divisionRangeStart, settings.divisionRangeEnd
    )
}
