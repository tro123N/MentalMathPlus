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

import com.helddertierwelt.mentalmath.data.dao.AdditionProblemDao
import com.helddertierwelt.mentalmath.data.dao.DivisionProblemDao
import com.helddertierwelt.mentalmath.data.dao.MultiplicationProblemDao
import com.helddertierwelt.mentalmath.data.dao.SubtractionProblemDao
import com.helddertierwelt.mentalmath.data.entity.MathProblem
import com.helddertierwelt.mentalmath.data.entity.toMathProblem
import javax.inject.Inject
import kotlin.random.Random

class MathProblemRepository @Inject constructor(
    private val additionDao: AdditionProblemDao,
    private val subtractionDao: SubtractionProblemDao,
    private val multiplicationDao: MultiplicationProblemDao,
    private val divisionDao: DivisionProblemDao,
) {
    suspend fun getRandomAdditionProblemByDifficulty(
        difficulty: Int,
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathProblem {
        val count = additionDao.countAdditionProblemsByDifficulty(
            difficulty,
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return additionDao.getAdditionProblemsByDifficultyWithOffset(
            difficulty,
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathProblem()
    }

    suspend fun getRandomSubtractionProblemByDifficulty(
        difficulty: Int,
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathProblem {
        val count = subtractionDao.countSubtractionProblemsByDifficulty(
            difficulty,
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return subtractionDao.getSubtractionProblemsByDifficultyWithOffset(
            difficulty,
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathProblem()
    }

    suspend fun getRandomMultiplicationProblemByDifficulty(
        difficulty: Int,
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathProblem {
        val count = multiplicationDao.countMultiplicationProblemsByDifficulty(
            difficulty,
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return multiplicationDao.getMultiplicationProblemsByDifficultyWithOffset(
            difficulty,
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathProblem()
    }

    suspend fun getRandomDivisionProblemByDifficulty(
        difficulty: Int,
        random: Random,
        lastOperands: Pair<Int, Int>
    ): MathProblem {
        val count = divisionDao.countDivisionProblemsByDifficulty(
            difficulty,
            lastOperands.first,
            lastOperands.second
        )
        val offset = random.nextInt(count)
        return divisionDao.getDivisionProblemsByDifficultyWithOffset(
            difficulty,
            offset,
            lastOperands.first,
            lastOperands.second
        ).toMathProblem()
    }
}