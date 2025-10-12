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

package com.helddertierwelt.mentalmath.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.helddertierwelt.mentalmath.data.entity.GameRecord

@Dao
interface GameRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gameScore: GameRecord): Long

    @Query(
        """SELECT * FROM game_records WHERE
        isModeEnabled = :isModeEnabled AND
        modeLimit = :modeLimit AND
        additionRangeStart = :additionRangeStart AND
        additionRangeEnd = :additionRangeEnd AND
        subtractionRangeStart = :subtractionRangeStart AND
        subtractionRangeEnd = :subtractionRangeEnd AND
        multiplicationRangeStart = :multiplicationRangeStart AND
        multiplicationRangeEnd = :multiplicationRangeEnd AND
        divisionRangeStart = :divisionRangeStart AND
        divisionRangeEnd = :divisionRangeEnd
        ORDER BY activeScore DESC
        LIMIT 10
        """
    )
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
    ): List<GameRecord>

    @Query(
        """SELECT * FROM game_records WHERE
        isModeEnabled = :isModeEnabled AND
        modeLimit = :modeLimit AND
        additionRangeStart = :additionRangeStart AND
        additionRangeEnd = :additionRangeEnd AND
        subtractionRangeStart = :subtractionRangeStart AND
        subtractionRangeEnd = :subtractionRangeEnd AND
        multiplicationRangeStart = :multiplicationRangeStart AND
        multiplicationRangeEnd = :multiplicationRangeEnd AND
        divisionRangeStart = :divisionRangeStart AND
        divisionRangeEnd = :divisionRangeEnd
        ORDER BY totalScore DESC
        LIMIT 10
        """
    )
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
    ): List<GameRecord>

    @Query(
        """SELECT EXISTS(SELECT 1 FROM game_records WHERE
        isModeEnabled = :isModeEnabled AND
        modeLimit = :modeLimit AND
        additionRangeStart = :additionRangeStart AND
        additionRangeEnd = :additionRangeEnd AND
        subtractionRangeStart = :subtractionRangeStart AND
        subtractionRangeEnd = :subtractionRangeEnd AND
        multiplicationRangeStart = :multiplicationRangeStart AND
        multiplicationRangeEnd = :multiplicationRangeEnd AND
        divisionRangeStart = :divisionRangeStart AND
        divisionRangeEnd = :divisionRangeEnd AND
        seed = :seed
        LIMIT 1)"""
    )
    suspend fun wasAlreadyPlayed(
        isModeEnabled: Boolean,
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
    ): Boolean


    @Query("DELETE FROM game_records")
    suspend fun deleteAll()
}