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

package com.helddertierwelt.mentalmath.fake

import com.helddertierwelt.mentalmath.data.dao.GameRecordDao
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.entity.GameScore
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode

class FakeGameRecordDao : GameRecordDao {

    val records = mutableListOf<GameRecord>()

    override suspend fun insert(gameScore: GameRecord): Long {
        val record = gameScore.copy(id = records.size + 1)
        records += record
        return record.id.toLong()
    }

    override suspend fun findById(id: Int): GameRecord? {
        return records.find { it.id == id }
    }

    override suspend fun findAll(): List<GameRecord> {
        return records.filter { it.activeScore > 0 }.sortedByDescending { it.id }
    }

    override suspend fun deleteAll() {
        records.clear()
    }

    override suspend fun resetAllRecords() {
        records.replaceAll {
            it.copy(
                totalAnswers = 0,
                correctAnswers = 0,
                activeTime = 0,
                totalTime = 0,
                activeScore = 0f,
                totalScore = 0f,
                solvedProblems = emptyList()
            )
        }
    }

    override suspend fun wasAlreadyPlayed(
        gameMode: GameMode,
        modeLimit: Int,
        additionRangeStart: Int,
        additionRangeEnd: Int,
        subtractionRangeStart: Int,
        subtractionRangeEnd: Int,
        multiplicationRangeStart: Int,
        multiplicationRangeEnd: Int,
        divisionRangeStart: Int,
        divisionRangeEnd: Int,
        seed: Long
    ): Boolean {
        return findRecordsWithSettings(
            gameMode, modeLimit,
            additionRangeStart, additionRangeEnd,
            subtractionRangeStart, subtractionRangeEnd,
            multiplicationRangeStart, multiplicationRangeEnd,
            divisionRangeStart, divisionRangeEnd
        ).any { it.seed == seed }
    }

    override suspend fun hasGamesWithScore(
        gameMode: GameMode,
        modeLimit: Int,
        additionRangeStart: Int,
        additionRangeEnd: Int,
        subtractionRangeStart: Int,
        subtractionRangeEnd: Int,
        multiplicationRangeStart: Int,
        multiplicationRangeEnd: Int,
        divisionRangeStart: Int,
        divisionRangeEnd: Int
    ): Boolean {
        return findRecordsWithSettings(
            gameMode, modeLimit,
            additionRangeStart, additionRangeEnd,
            subtractionRangeStart, subtractionRangeEnd,
            multiplicationRangeStart, multiplicationRangeEnd,
            divisionRangeStart, divisionRangeEnd
        ).any { it.activeScore > 0 }
    }

    override suspend fun findBestGameScores(
        gameMode: GameMode,
        modeLimit: Int,
        additionRangeStart: Int,
        additionRangeEnd: Int,
        subtractionRangeStart: Int,
        subtractionRangeEnd: Int,
        multiplicationRangeStart: Int,
        multiplicationRangeEnd: Int,
        divisionRangeStart: Int,
        divisionRangeEnd: Int
    ): List<GameScore> {
        return findRecordsWithSettings(
            gameMode, modeLimit,
            additionRangeStart, additionRangeEnd,
            subtractionRangeStart, subtractionRangeEnd,
            multiplicationRangeStart, multiplicationRangeEnd,
            divisionRangeStart, divisionRangeEnd
        )
            .filter { it.activeScore > 0 }
            .sortedWith(compareByDescending<GameRecord> { it.activeScore }.thenByDescending { it.id })
            .take(10)
            .map { GameScore(id = it.id, activeScore = it.activeScore) }
    }

    private fun findRecordsWithSettings(
        gameMode: GameMode,
        modeLimit: Int,
        additionRangeStart: Int,
        additionRangeEnd: Int,
        subtractionRangeStart: Int,
        subtractionRangeEnd: Int,
        multiplicationRangeStart: Int,
        multiplicationRangeEnd: Int,
        divisionRangeStart: Int,
        divisionRangeEnd: Int
    ): List<GameRecord> {
        return records.filter {
            it.gameMode == gameMode &&
                    it.modeLimit == modeLimit &&
                    it.additionRangeStart == additionRangeStart &&
                    it.additionRangeEnd == additionRangeEnd &&
                    it.subtractionRangeStart == subtractionRangeStart &&
                    it.subtractionRangeEnd == subtractionRangeEnd &&
                    it.multiplicationRangeStart == multiplicationRangeStart &&
                    it.multiplicationRangeEnd == multiplicationRangeEnd &&
                    it.divisionRangeStart == divisionRangeStart &&
                    it.divisionRangeEnd == divisionRangeEnd
        }
    }
}
