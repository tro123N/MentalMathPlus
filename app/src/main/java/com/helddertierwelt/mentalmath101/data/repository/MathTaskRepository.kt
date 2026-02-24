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

package com.helddertierwelt.mentalmath101.data.repository

import com.helddertierwelt.mentalmath101.data.dao.AdditionTaskDao
import com.helddertierwelt.mentalmath101.data.dao.DivisionTaskDao
import com.helddertierwelt.mentalmath101.data.dao.MultiplicationTaskDao
import com.helddertierwelt.mentalmath101.data.dao.SubtractionTaskDao
import com.helddertierwelt.mentalmath101.data.entity.MathTask
import com.helddertierwelt.mentalmath101.data.entity.toMathTask
import javax.inject.Inject
import kotlin.random.Random

class MathTaskRepository @Inject constructor(
    private val additionDao: AdditionTaskDao,
    private val subtractionDao: SubtractionTaskDao,
    private val multiplicationDao: MultiplicationTaskDao,
    private val divisionDao: DivisionTaskDao,
) {
    suspend fun getRandomAdditionTaskByDifficulty(
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathTask {
        val count = additionDao.countAdditionTasksByDifficulty(
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return additionDao.getAdditionTaskByDifficultyWithOffset(
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathTask()
    }

    suspend fun getRandomSubtractionTaskByDifficulty(
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathTask {
        val count = subtractionDao.countSubtractionTasksByDifficulty(
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return subtractionDao.getSubtractionTaskByDifficultyWithOffset(
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathTask()
    }

    suspend fun getRandomMultiplicationTaskByDifficulty(
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathTask {
        val count = multiplicationDao.countMultiplicationTasksByDifficulty(
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return multiplicationDao.getMultiplicationTaskByDifficultyWithOffset(
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathTask()
    }

    suspend fun getRandomDivisionTaskByDifficulty(
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathTask {
        val count = divisionDao.countDivisionTasksByDifficulty(
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return divisionDao.getDivisionTaskByDifficultyWithOffset(
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathTask()
    }
}