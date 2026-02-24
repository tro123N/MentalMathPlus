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

package com.helddertierwelt.mentalmath101.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.helddertierwelt.mentalmath101.presentation.viewmodel.game.TaskResult

@Entity(
    tableName = "game_records",
    indices = [
        Index(
            value = ["isModeEnabled", "modeLimit", "isAdditionEnabled", "isSubtractionEnabled",
                "isMultiplicationEnabled", "isDivisionEnabled"]
        )
    ]
)
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val isModeEnabled: Boolean = false,
    val modeLimit: Int = 0,

    val isAdditionEnabled: Boolean = false,
    val isSubtractionEnabled: Boolean = false,
    val isMultiplicationEnabled: Boolean = false,
    val isDivisionEnabled: Boolean = false,

    val totalAnswers: Int = 0,
    val correctAnswers: Int = 0,

    val activeTime: Long = 0L,
    val totalTime: Long = 0L,

    val activeScore: Float = 0f,
    val totalScore: Float = 0f,

    val seed: Long = 0L,

    val taskResults: List<TaskResult> = emptyList()
)