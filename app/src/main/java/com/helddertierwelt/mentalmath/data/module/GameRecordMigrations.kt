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

package com.helddertierwelt.mentalmath.data.module

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.helddertierwelt.mentalmath.data.converter.SolvedProblemConverter
import com.helddertierwelt.mentalmath.domain.game.Operation
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object GameRecordMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE game_records_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    isModeEnabled INTEGER NOT NULL,
                    modeLimit INTEGER NOT NULL,
                    additionRangeStart INTEGER NOT NULL,
                    additionRangeEnd INTEGER NOT NULL,
                    subtractionRangeStart INTEGER NOT NULL,
                    subtractionRangeEnd INTEGER NOT NULL,
                    multiplicationRangeStart INTEGER NOT NULL,
                    multiplicationRangeEnd INTEGER NOT NULL,
                    divisionRangeStart INTEGER NOT NULL,
                    divisionRangeEnd INTEGER NOT NULL,
                    totalAnswers INTEGER NOT NULL,
                    correctAnswers INTEGER NOT NULL,
                    activeTime INTEGER NOT NULL,
                    totalTime INTEGER NOT NULL,
                    activeScore REAL NOT NULL,
                    totalScore REAL NOT NULL,
                    seed INTEGER NOT NULL,
                    solvedProblems TEXT NOT NULL
                )
            """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO game_records_new (
                    id, isModeEnabled, modeLimit,
                    additionRangeStart, additionRangeEnd,
                    subtractionRangeStart, subtractionRangeEnd,
                    multiplicationRangeStart, multiplicationRangeEnd,
                    divisionRangeStart, divisionRangeEnd,
                    totalAnswers, correctAnswers,
                    activeTime, totalTime,
                    activeScore, totalScore,
                    seed,
                    solvedProblems
                )
                SELECT
                    id, isModeEnabled, modeLimit,
                    additionRangeStart, additionRangeEnd,
                    subtractionRangeStart, subtractionRangeEnd,
                    multiplicationRangeStart, multiplicationRangeEnd,
                    divisionRangeStart, divisionRangeEnd,
                    totalAnswers, correctAnswers,
                    activeTime, totalTime,
                    activeScore, totalScore,
                    seed,
                    taskResults
                FROM game_records
            """.trimIndent()
            )

            db.execSQL("DROP TABLE game_records")
            db.execSQL("ALTER TABLE game_records_new RENAME TO game_records")

            db.execSQL(
                """
                CREATE INDEX index_game_records_isModeEnabled_modeLimit_additionRangeStart_additionRangeEnd_subtractionRangeStart_subtractionRangeEnd_multiplicationRangeStart_multiplicationRangeEnd_divisionRangeStart_divisionRangeEnd ON game_records (
                    isModeEnabled,
                    modeLimit,
                    additionRangeStart,
                    additionRangeEnd,
                    subtractionRangeStart,
                    subtractionRangeEnd,
                    multiplicationRangeStart,
                    multiplicationRangeEnd,
                    divisionRangeStart,
                    divisionRangeEnd
                )
            """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {

        override fun migrate(
            db: SupportSQLiteDatabase
        ) {
            val oldRecords = mutableListOf<LegacyGameRecord>()

            db.query(
                """
            SELECT
                id,
                isModeEnabled,
                modeLimit,
                additionRangeStart,
                additionRangeEnd,
                subtractionRangeStart,
                subtractionRangeEnd,
                multiplicationRangeStart,
                multiplicationRangeEnd,
                divisionRangeStart,
                divisionRangeEnd,
                totalAnswers,
                correctAnswers,
                activeTime,
                totalTime,
                activeScore,
                totalScore,
                seed,
                solvedProblems
            FROM game_records
            """.trimIndent()
            ).use { cursor ->

                val idIndex = cursor.getColumnIndexOrThrow("id")
                val isModeEnabledIndex = cursor.getColumnIndexOrThrow("isModeEnabled")
                val modeLimitIndex = cursor.getColumnIndexOrThrow("modeLimit")
                val additionRangeStartIndex = cursor.getColumnIndexOrThrow("additionRangeStart")
                val additionRangeEndIndex = cursor.getColumnIndexOrThrow("additionRangeEnd")
                val subtractionRangeStartIndex = cursor.getColumnIndexOrThrow("subtractionRangeStart")
                val subtractionRangeEndIndex = cursor.getColumnIndexOrThrow("subtractionRangeEnd")
                val multiplicationRangeStartIndex = cursor.getColumnIndexOrThrow("multiplicationRangeStart")
                val multiplicationRangeEndIndex = cursor.getColumnIndexOrThrow("multiplicationRangeEnd")
                val divisionRangeStartIndex = cursor.getColumnIndexOrThrow("divisionRangeStart")
                val divisionRangeEndIndex = cursor.getColumnIndexOrThrow("divisionRangeEnd")
                val totalAnswersIndex = cursor.getColumnIndexOrThrow("totalAnswers")
                val correctAnswersIndex = cursor.getColumnIndexOrThrow("correctAnswers")
                val activeTimeIndex = cursor.getColumnIndexOrThrow("activeTime")
                val totalTimeIndex = cursor.getColumnIndexOrThrow("totalTime")
                val activeScoreIndex = cursor.getColumnIndexOrThrow("activeScore")
                val totalScoreIndex = cursor.getColumnIndexOrThrow("totalScore")
                val seedIndex = cursor.getColumnIndexOrThrow("seed")
                val solvedProblemsIndex = cursor.getColumnIndexOrThrow("solvedProblems")

                while (cursor.moveToNext()) {
                    oldRecords += LegacyGameRecord(
                        id = cursor.getInt(idIndex),
                        isModeEnabled = cursor.getInt(isModeEnabledIndex) != 0,
                        modeLimit = cursor.getInt(modeLimitIndex),
                        additionRangeStart = cursor.getInt(additionRangeStartIndex),
                        additionRangeEnd = cursor.getInt(additionRangeEndIndex),
                        subtractionRangeStart = cursor.getInt(subtractionRangeStartIndex),
                        subtractionRangeEnd = cursor.getInt(subtractionRangeEndIndex),
                        multiplicationRangeStart = cursor.getInt(multiplicationRangeStartIndex),
                        multiplicationRangeEnd = cursor.getInt(multiplicationRangeEndIndex),
                        divisionRangeStart = cursor.getInt(divisionRangeStartIndex),
                        divisionRangeEnd = cursor.getInt(divisionRangeEndIndex),
                        totalAnswers = cursor.getInt(totalAnswersIndex),
                        correctAnswers = cursor.getInt(correctAnswersIndex),
                        activeTime = cursor.getLong(activeTimeIndex),
                        totalTime = cursor.getLong(totalTimeIndex),
                        activeScore = cursor.getFloat(activeScoreIndex),
                        totalScore = cursor.getFloat(totalScoreIndex),
                        seed = cursor.getLong(seedIndex),
                        solvedProblems = cursor.getString(solvedProblemsIndex)
                    )
                }
            }

            db.execSQL("DROP TABLE game_records")
            db.execSQL(
                """
            CREATE TABLE game_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                gameMode TEXT NOT NULL,
                modeLimit INTEGER NOT NULL,
                additionRangeStart INTEGER NOT NULL,
                additionRangeEnd INTEGER NOT NULL,
                subtractionRangeStart INTEGER NOT NULL,
                subtractionRangeEnd INTEGER NOT NULL,
                multiplicationRangeStart INTEGER NOT NULL,
                multiplicationRangeEnd INTEGER NOT NULL,
                divisionRangeStart INTEGER NOT NULL,
                divisionRangeEnd INTEGER NOT NULL,
                totalAnswers INTEGER NOT NULL,
                correctAnswers INTEGER NOT NULL,
                activeTime INTEGER NOT NULL,
                totalTime INTEGER NOT NULL,
                activeScore REAL NOT NULL,
                totalScore REAL NOT NULL,
                seed INTEGER NOT NULL,
                solvedProblems TEXT NOT NULL
            )
            """.trimIndent()
            )

            /*
             * 4. Neuen Index erstellen.
             */
            db.execSQL(
                """
            CREATE INDEX index_game_records_gameMode_modeLimit_additionRangeStart_additionRangeEnd_subtractionRangeStart_subtractionRangeEnd_multiplicationRangeStart_multiplicationRangeEnd_divisionRangeStart_divisionRangeEnd
            ON game_records (
                gameMode,
                modeLimit,
                additionRangeStart,
                additionRangeEnd,
                subtractionRangeStart,
                subtractionRangeEnd,
                multiplicationRangeStart,
                multiplicationRangeEnd,
                divisionRangeStart,
                divisionRangeEnd
            )
            """.trimIndent()
            )

            /*
             * 5. Alte Daten transformieren und wieder einfügen.
             */
            oldRecords.forEach { oldRecord ->

                val gameMode =
                    if (oldRecord.isModeEnabled) {
                        GameMode.PROBLEMS
                    } else {
                        GameMode.TIMED
                    }

                val oldSolvedProblems =
                    if (oldRecord.solvedProblems.isBlank()) {
                        emptyList()
                    } else {
                        Json.decodeFromString<List<LegacySolvedProblem>>(oldRecord.solvedProblems)
                    }

                val newSolvedProblems =
                    oldSolvedProblems.map { oldProblem ->

                        SolvedProblem(
                            operand1 = oldProblem.operand1,
                            operator = oldProblem.operator,
                            operand2 = oldProblem.operand2,
                            userInput = oldProblem.userInput,
                            status = determineProblemStatus(
                                operand1 = oldProblem.operand1,
                                operator = oldProblem.operator,
                                operand2 = oldProblem.operand2,
                                userInput = oldProblem.userInput
                            )
                        )
                    }

                val solvedProblemsString = SolvedProblemConverter().fromSolvedProblemList(newSolvedProblems)

                db.execSQL(
                    """
                INSERT INTO game_records (
                    id,
                    gameMode,
                    modeLimit,
                    additionRangeStart,
                    additionRangeEnd,
                    subtractionRangeStart,
                    subtractionRangeEnd,
                    multiplicationRangeStart,
                    multiplicationRangeEnd,
                    divisionRangeStart,
                    divisionRangeEnd,
                    totalAnswers,
                    correctAnswers,
                    activeTime,
                    totalTime,
                    activeScore,
                    totalScore,
                    seed,
                    solvedProblems
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                    arrayOf<Any?>(
                        oldRecord.id,
                        gameMode.name,
                        oldRecord.modeLimit,
                        oldRecord.additionRangeStart,
                        oldRecord.additionRangeEnd,
                        oldRecord.subtractionRangeStart,
                        oldRecord.subtractionRangeEnd,
                        oldRecord.multiplicationRangeStart,
                        oldRecord.multiplicationRangeEnd,
                        oldRecord.divisionRangeStart,
                        oldRecord.divisionRangeEnd,
                        oldRecord.totalAnswers,
                        oldRecord.correctAnswers,
                        oldRecord.activeTime,
                        oldRecord.totalTime,
                        oldRecord.activeScore,
                        oldRecord.totalScore,
                        oldRecord.seed,
                        solvedProblemsString
                    )
                )
            }
        }
    }

    private data class LegacyGameRecord(
        val id: Int,
        val isModeEnabled: Boolean,
        val modeLimit: Int,
        val additionRangeStart: Int,
        val additionRangeEnd: Int,
        val subtractionRangeStart: Int,
        val subtractionRangeEnd: Int,
        val multiplicationRangeStart: Int,
        val multiplicationRangeEnd: Int,
        val divisionRangeStart: Int,
        val divisionRangeEnd: Int,
        val totalAnswers: Int,
        val correctAnswers: Int,
        val activeTime: Long,
        val totalTime: Long,
        val activeScore: Float,
        val totalScore: Float,
        val seed: Long,
        val solvedProblems: String
    )

    private fun determineProblemStatus(
        operand1: Int,
        operator: Operation,
        operand2: Int,
        userInput: String
    ): ProblemStatus {

        if (userInput.isEmpty()) {
            return ProblemStatus.SKIPPED
        }

        val correctResult = when (operator) {
            Operation.ADDITION -> operand1 + operand2
            Operation.SUBTRACTION -> operand1 - operand2
            Operation.MULTIPLICATION -> operand1 * operand2
            Operation.DIVISION -> operand1 / operand2
        }

        return if (userInput.toIntOrNull() == correctResult) {
            ProblemStatus.CORRECT
        } else {
            ProblemStatus.INCORRECT
        }
    }

    @Serializable
    private data class LegacySolvedProblem(
        val operand1: Int,
        val operand2: Int,
        val operator: Operation,
        val correctResult: Int,
        val userInput: String
    )
}