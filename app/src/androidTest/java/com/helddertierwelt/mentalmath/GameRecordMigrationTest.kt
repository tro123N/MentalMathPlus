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
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.helddertierwelt.mentalmath.data.dao.GameRecordDao
import com.helddertierwelt.mentalmath.data.db.GameRecordDatabase
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.module.GameRecordMigrations.MIGRATION_1_2
import com.helddertierwelt.mentalmath.data.module.GameRecordMigrations.MIGRATION_2_3
import com.helddertierwelt.mentalmath.domain.game.Operation
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameRecordMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GameRecordDatabase::class.java
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val legacySolvedProblems = """
        [
            {"operand1":7,"operand2":3,"operator":"SUBTRACTION","correctResult":4,"userInput":"4"},
            {"operand1":6,"operand2":2,"operator":"MULTIPLICATION","correctResult":12,"userInput":"13"},
            {"operand1":9,"operand2":3,"operator":"DIVISION","correctResult":3,"userInput":""}
        ]
    """.trimIndent()

    private val migratedSolvedProblems = listOf(
        SolvedProblem(7, Operation.SUBTRACTION, 3, userInput = "4", status = ProblemStatus.CORRECT),
        SolvedProblem(6, Operation.MULTIPLICATION, 2, userInput = "13", status = ProblemStatus.INCORRECT),
        SolvedProblem(9, Operation.DIVISION, 3, userInput = "", status = ProblemStatus.SKIPPED)
    )

    @After
    fun deleteDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun gamesFromVersion1AreMigrated() {
        migrationHelper.createDatabase(TEST_DATABASE, 1).use { database ->
            database.insertLegacyGame(id = 1, isModeEnabled = true, problemsColumn = "taskResults", legacySolvedProblems)
            database.insertLegacyGame(id = 2, isModeEnabled = false, problemsColumn = "taskResults", "[]")
        }

        migrateToLatestVersion()

        assertEquals(
            listOf(
                expectedGame(id = 1, gameMode = GameMode.PROBLEMS, solvedProblems = migratedSolvedProblems),
                expectedGame(id = 2, gameMode = GameMode.TIMED, solvedProblems = emptyList())
            ),
            readAllGames()
        )
    }

    @Test
    fun gamesFromVersion2AreMigrated() {
        migrationHelper.createDatabase(TEST_DATABASE, 2).use { database ->
            database.insertLegacyGame(id = 1, isModeEnabled = true, problemsColumn = "solvedProblems", legacySolvedProblems)
            database.insertLegacyGame(id = 2, isModeEnabled = false, problemsColumn = "solvedProblems", "[]")
        }

        migrateToLatestVersion()

        assertEquals(
            listOf(
                expectedGame(id = 1, gameMode = GameMode.PROBLEMS, solvedProblems = migratedSolvedProblems),
                expectedGame(id = 2, gameMode = GameMode.TIMED, solvedProblems = emptyList())
            ),
            readAllGames()
        )
    }

    @Test
    fun newGamesGetNewIdsAfterMigration() {
        migrationHelper.createDatabase(TEST_DATABASE, 1).use { database ->
            database.insertLegacyGame(id = 5, isModeEnabled = true, problemsColumn = "taskResults", "[]")
        }
        migrateToLatestVersion()

        val newId = withMigratedDao { it.insert(GameRecord()) }

        assertEquals(6L, newId)
    }

    private fun migrateToLatestVersion() {
        migrationHelper.runMigrationsAndValidate(TEST_DATABASE, 3, true, MIGRATION_1_2, MIGRATION_2_3).close()
    }

    private fun readAllGames(): List<GameRecord> {
        return withMigratedDao { it.findAll() }.sortedBy { it.id }
    }

    private fun <T> withMigratedDao(block: suspend (GameRecordDao) -> T): T {
        val database = Room.databaseBuilder(context, GameRecordDatabase::class.java, TEST_DATABASE)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        try {
            return runBlocking { block(database.gameRecordDao()) }
        } finally {
            database.close()
        }
    }

    private fun SupportSQLiteDatabase.insertLegacyGame(
        id: Int,
        isModeEnabled: Boolean,
        problemsColumn: String,
        solvedProblems: String
    ) {
        execSQL(
            """
            INSERT INTO game_records (
                id, isModeEnabled, modeLimit,
                additionRangeStart, additionRangeEnd, subtractionRangeStart, subtractionRangeEnd,
                multiplicationRangeStart, multiplicationRangeEnd, divisionRangeStart, divisionRangeEnd,
                totalAnswers, correctAnswers, activeTime, totalTime, activeScore, totalScore, seed,
                $problemsColumn
            )
            VALUES (?, ?, 2, 1, 3, 2, 4, 3, 5, 4, 6, 3, 1, 45000, 50000, 12.5, 11.25, 987654321, ?)
            """.trimIndent(),
            arrayOf<Any?>(id, isModeEnabled, solvedProblems)
        )
    }

    private fun expectedGame(id: Int, gameMode: GameMode, solvedProblems: List<SolvedProblem>) = GameRecord(
        id = id,
        gameMode = gameMode,
        modeLimit = 2,
        additionRangeStart = 1,
        additionRangeEnd = 3,
        subtractionRangeStart = 2,
        subtractionRangeEnd = 4,
        multiplicationRangeStart = 3,
        multiplicationRangeEnd = 5,
        divisionRangeStart = 4,
        divisionRangeEnd = 6,
        totalAnswers = 3,
        correctAnswers = 1,
        activeTime = 45000,
        totalTime = 50000,
        activeScore = 12.5f,
        totalScore = 11.25f,
        seed = 987654321,
        solvedProblems = solvedProblems
    )

    private companion object {
        const val TEST_DATABASE = "game_record_migration_test.db"
    }
}
