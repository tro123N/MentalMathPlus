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

package com.helddertierwelt.mentalmath.data.repository

import com.helddertierwelt.mentalmath.data.dao.AdditionTaskDao
import com.helddertierwelt.mentalmath.data.dao.DivisionTaskDao
import com.helddertierwelt.mentalmath.data.dao.MultiplicationTaskDao
import com.helddertierwelt.mentalmath.data.dao.SubtractionTaskDao
import com.helddertierwelt.mentalmath.data.entity.MathTask
import com.helddertierwelt.mentalmath.data.entity.toMathTask
import javax.inject.Inject
import kotlin.random.Random

class MathTaskRepository @Inject constructor(
    private val additionDao: AdditionTaskDao,
    private val subtractionDao: SubtractionTaskDao,
    private val multiplicationDao: MultiplicationTaskDao,
    private val divisionDao: DivisionTaskDao,
) {
    suspend fun getRandomAdditionTaskByDifficulty(
        difficulty: Int,
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathTask {
        val count = additionDao.countAdditionTasksByDifficulty(
            difficulty,
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return additionDao.getAdditionTaskByDifficultyWithOffset(
            difficulty,
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathTask()
    }

    suspend fun getRandomSubtractionTaskByDifficulty(
        difficulty: Int,
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathTask {
        val count = subtractionDao.countSubtractionTasksByDifficulty(
            difficulty,
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return subtractionDao.getSubtractionTaskByDifficultyWithOffset(
            difficulty,
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathTask()
    }

    suspend fun getRandomMultiplicationTaskByDifficulty(
        difficulty: Int,
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathTask {
        val count = multiplicationDao.countMultiplicationTasksByDifficulty(
            difficulty,
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return multiplicationDao.getMultiplicationTaskByDifficultyWithOffset(
            difficulty,
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathTask()
    }

    suspend fun getRandomDivisionTaskByDifficulty(
        difficulty: Int,
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathTask {
        val lastOperand1 = lastOperands.second
        val lastOperand2 = lastOperands.first / lastOperands.second
        val count = divisionDao.countDivisionTasksByDifficulty(
            difficulty,
            lastOperands.second,
            lastOperand2
        )
        val offset = random.nextInt(count)
        return divisionDao.getDivisionTaskByDifficultyWithOffset(
            difficulty,
            offset,
            lastOperand1,
            lastOperand2
        ).toMathTask()
    }
}