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

package com.helddertierwelt.mentalmath.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.helddertierwelt.mentalmath.presentation.viewmodel.TaskResult

@Entity(
    tableName = "game_records",
    indices = [
        Index(
            value = ["isModeEnabled", "modeLimit", "additionRangeStart", "additionRangeEnd",
                "subtractionRangeStart", "subtractionRangeEnd",
                "multiplicationRangeStart", "multiplicationRangeEnd",
                "divisionRangeStart", "divisionRangeEnd"]
        )
    ]
)
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

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

    val taskResults: List<TaskResult>
)