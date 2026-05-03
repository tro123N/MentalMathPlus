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

package com.helddertierwelt.mentalmath.data.repository

import com.helddertierwelt.mentalmath.data.dao.GameRecordDao
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class GameRecordRepository @Inject constructor(
    private val gameRecordDao: GameRecordDao
) {

    private val _lastGameRecord = MutableStateFlow(
        GameRecord(
            id = -1,
            isModeEnabled = false,
            modeLimit = 0,
            additionRangeStart = 0,
            additionRangeEnd = 0,
            subtractionRangeStart = 0,
            subtractionRangeEnd = 0,
            multiplicationRangeStart = 0,
            multiplicationRangeEnd = 0,
            divisionRangeStart = 0,
            divisionRangeEnd = 0,
            totalAnswers = 0,
            correctAnswers = 0,
            activeTime = 0L,
            totalTime = 0L,
            activeScore = 0f,
            totalScore = 0f,
            solvedProblems = emptyList(),
            seed = 0
        )
    )
    val lastGameRecord: StateFlow<GameRecord> = _lastGameRecord.asStateFlow()

    private val _loadedGameRecord = MutableStateFlow(
        GameRecord(
            id = -1,
            isModeEnabled = false,
            modeLimit = 0,
            additionRangeStart = 0,
            additionRangeEnd = 0,
            subtractionRangeStart = 0,
            subtractionRangeEnd = 0,
            multiplicationRangeStart = 0,
            multiplicationRangeEnd = 0,
            divisionRangeStart = 0,
            divisionRangeEnd = 0,
            totalAnswers = 0,
            correctAnswers = 0,
            activeTime = 0L,
            totalTime = 0L,
            activeScore = 0f,
            totalScore = 0f,
            solvedProblems = emptyList(),
            seed = 0
        )
    )
    val loadedGameRecord: StateFlow<GameRecord> = _loadedGameRecord.asStateFlow()

    fun setLastGameRecord(gameRecord: GameRecord) {
        _lastGameRecord.value = gameRecord
    }

    fun setLoadedGameRecord(gameRecord: GameRecord) {
        _loadedGameRecord.value = gameRecord
    }

    suspend fun insertGameRecord(gameRecord: GameRecord): Long {
        val newId = gameRecordDao.insert(gameRecord)
        return newId
    }

    suspend fun findBestActiveGameRecords(
        isModeEnabled: Boolean,
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
        return gameRecordDao.findBestActiveGameRecords(
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
    }

    suspend fun wasAlreadyPlayed(gameRecord: GameRecord): Boolean {
        return gameRecordDao.wasAlreadyPlayed(
            gameRecord.isModeEnabled,
            gameRecord.modeLimit,
            gameRecord.additionRangeStart,
            gameRecord.additionRangeEnd,
            gameRecord.subtractionRangeStart,
            gameRecord.subtractionRangeEnd,
            gameRecord.multiplicationRangeStart,
            gameRecord.multiplicationRangeEnd,
            gameRecord.divisionRangeStart,
            gameRecord.divisionRangeEnd,
            gameRecord.seed
        )
    }

    suspend fun deleteAllGameRecords() {
        gameRecordDao.deleteAll()
    }

    suspend fun findAllGameRecords(): List<GameRecord> {
        return gameRecordDao.findAll()
    }

    suspend fun resetAllGameRecords() {
        gameRecordDao.resetAllRecords()
    }

    fun resetLastGameRecord() {
        _lastGameRecord.value = GameRecord(
            id = -1,
            isModeEnabled = false,
            modeLimit = 0,
            additionRangeStart = 0,
            additionRangeEnd = 0,
            subtractionRangeStart = 0,
            subtractionRangeEnd = 0,
            multiplicationRangeStart = 0,
            multiplicationRangeEnd = 0,
            divisionRangeStart = 0,
            divisionRangeEnd = 0,
            totalAnswers = 0,
            correctAnswers = 0,
            activeTime = 0L,
            totalTime = 0L,
            activeScore = 0f,
            totalScore = 0f,
            solvedProblems = emptyList(),
            seed = 0
        )
    }

    fun resetLoadedGameRecord() {
        _loadedGameRecord.value = GameRecord(
            id = -1,
            isModeEnabled = false,
            modeLimit = 0,
            additionRangeStart = 0,
            additionRangeEnd = 0,
            subtractionRangeStart = 0,
            subtractionRangeEnd = 0,
            multiplicationRangeStart = 0,
            multiplicationRangeEnd = 0,
            divisionRangeStart = 0,
            divisionRangeEnd = 0,
            totalAnswers = 0,
            correctAnswers = 0,
            activeTime = 0L,
            totalTime = 0L,
            activeScore = 0f,
            totalScore = 0f,
            solvedProblems = emptyList(),
            seed = 0
        )
    }

    suspend fun reset() {
        resetAllGameRecords()
        resetLastGameRecord()
        resetLoadedGameRecord()
    }
}