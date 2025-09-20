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
import androidx.room.Query
import com.helddertierwelt.mentalmath.data.entity.SubtractionTask

@Dao
interface SubtractionTaskDao {

    @Query("SELECT COUNT(*) FROM tasks_subtraction WHERE difficulty = :difficulty AND (operand1 <> :lastOperand1 OR operand2 <> :lastOperand2)")
    suspend fun countSubtractionTasksByDifficulty(difficulty: Int, lastOperand1: Int, lastOperand2: Int): Int

    @Query("SELECT * FROM tasks_subtraction WHERE difficulty = :difficulty  AND (operand1 <> :lastOperand1 OR operand2 <> :lastOperand2) LIMIT 1 OFFSET :offset")
    suspend fun getSubtractionTaskByDifficultyWithOffset(difficulty: Int, offset: Int, lastOperand1: Int, lastOperand2: Int): SubtractionTask
}

