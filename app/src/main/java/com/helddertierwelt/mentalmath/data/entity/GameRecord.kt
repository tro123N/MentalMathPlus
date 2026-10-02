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

package com.helddertierwelt.mentalmath.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode

@Entity(
    tableName = "game_records",
    indices = [
        Index(
            value = [
                "gameMode",
                "modeLimit",
                "additionRangeStart",
                "additionRangeEnd",
                "subtractionRangeStart",
                "subtractionRangeEnd",
                "multiplicationRangeStart",
                "multiplicationRangeEnd",
                "divisionRangeStart",
                "divisionRangeEnd"
            ]
        )
    ]
)
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val gameMode: GameMode = GameMode.TIMED,
    val modeLimit: Int = 0,

    val additionRangeStart: Int = 0,
    val additionRangeEnd: Int = 0,

    val subtractionRangeStart: Int = 0,
    val subtractionRangeEnd: Int = 0,

    val multiplicationRangeStart: Int = 0,
    val multiplicationRangeEnd: Int = 0,

    val divisionRangeStart: Int = 0,
    val divisionRangeEnd: Int = 0,

    val totalAnswers: Int = 0,
    val correctAnswers: Int = 0,

    val activeTime: Long = 0L,
    val totalTime: Long = 0L,

    val activeScore: Float = 0f,
    val totalScore: Float = 0f,

    val seed: Long = 0L,

    val solvedProblems: List<SolvedProblem> = emptyList()
)