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
        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE game_records_new (
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

            db.execSQL(
                """
                INSERT INTO game_records_new (
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
                SELECT
                    id,
                    CASE
                        WHEN isModeEnabled = 1 THEN 'PROBLEMS'
                        ELSE 'TIMED'
                    END,
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
            )

            db.execSQL("DROP TABLE game_records")
            db.execSQL("ALTER TABLE game_records_new RENAME TO game_records")

            db.execSQL(
                """
                CREATE INDEX index_game_records_gameMode_modeLimit_additionRangeStart_additionRangeEnd_subtractionRangeStart_subtractionRangeEnd_multiplicationRangeStart_multiplicationRangeEnd_divisionRangeStart_divisionRangeEnd ON game_records (
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
        }
    }
}