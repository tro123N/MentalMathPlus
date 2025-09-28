package com.helddertierwelt.mentalmath.data.repository

import com.helddertierwelt.mentalmath.data.dao.GameRecordDao
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            taskResults = emptyList()
        )
    )
    val lastGameRecord: StateFlow<GameRecord> = _lastGameRecord
    fun setLastGameRecord(gameRecord: GameRecord) {
        _lastGameRecord.value = gameRecord
    }

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
            taskResults = emptyList()
        )
    )
    val loadedGameRecord: StateFlow<GameRecord> = _loadedGameRecord
    fun setLoadedGameRecord(gameRecord: GameRecord) {
        _loadedGameRecord.value = gameRecord
    }


    suspend fun insertGameRecord(gameRecord: GameRecord) {
        val newId = gameRecordDao.insert(gameRecord)
        setLastGameRecord(gameRecord.copy(id = newId.toInt()))
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

    suspend fun findBestTotalGameRecords(
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
        return gameRecordDao.findBestTotalGameRecords(
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


    suspend fun deleteAllGameRecords() {
        gameRecordDao.deleteAll()
    }
}