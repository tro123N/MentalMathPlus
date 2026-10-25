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

package com.helddertierwelt.mentalmath.fake

import com.helddertierwelt.mentalmath.data.dao.AdditionProblemDao
import com.helddertierwelt.mentalmath.data.dao.DivisionProblemDao
import com.helddertierwelt.mentalmath.data.dao.MultiplicationProblemDao
import com.helddertierwelt.mentalmath.data.dao.SubtractionProblemDao
import com.helddertierwelt.mentalmath.data.entity.AdditionProblem
import com.helddertierwelt.mentalmath.data.entity.DivisionProblem
import com.helddertierwelt.mentalmath.data.entity.MathProblem
import com.helddertierwelt.mentalmath.data.entity.MultiplicationProblem
import com.helddertierwelt.mentalmath.data.entity.SubtractionProblem

class FakeMathProblemDao(
    private val problems: List<MathProblem>
) : AdditionProblemDao, SubtractionProblemDao, MultiplicationProblemDao, DivisionProblemDao {

    private fun findProblems(difficulty: Int, lastOperand1: Int, lastOperand2: Int): List<MathProblem> {
        return problems.filter {
            it.difficulty == difficulty && (it.operand1 != lastOperand1 || it.operand2 != lastOperand2)
        }
    }

    private fun findProblem(difficulty: Int, offset: Int, lastOperand1: Int, lastOperand2: Int): MathProblem {
        return findProblems(difficulty, lastOperand1, lastOperand2)[offset]
    }

    override suspend fun countAdditionProblemsByDifficulty(difficulty: Int, lastOperand1: Int, lastOperand2: Int) =
        findProblems(difficulty, lastOperand1, lastOperand2).size

    override suspend fun countSubtractionProblemsByDifficulty(difficulty: Int, lastOperand1: Int, lastOperand2: Int) =
        findProblems(difficulty, lastOperand1, lastOperand2).size

    override suspend fun countMultiplicationProblemsByDifficulty(difficulty: Int, lastOperand1: Int, lastOperand2: Int) =
        findProblems(difficulty, lastOperand1, lastOperand2).size

    override suspend fun countDivisionProblemsByDifficulty(difficulty: Int, lastOperand1: Int, lastOperand2: Int) =
        findProblems(difficulty, lastOperand1, lastOperand2).size

    override suspend fun getAdditionProblemsByDifficultyWithOffset(
        difficulty: Int,
        offset: Int,
        lastOperand1: Int,
        lastOperand2: Int
    ) = findProblem(difficulty, offset, lastOperand1, lastOperand2).let {
        AdditionProblem(difficulty = it.difficulty, operand1 = it.operand1, operand2 = it.operand2)
    }

    override suspend fun getSubtractionProblemsByDifficultyWithOffset(
        difficulty: Int,
        offset: Int,
        lastOperand1: Int,
        lastOperand2: Int
    ) = findProblem(difficulty, offset, lastOperand1, lastOperand2).let {
        SubtractionProblem(difficulty = it.difficulty, operand1 = it.operand1, operand2 = it.operand2)
    }

    override suspend fun getMultiplicationProblemsByDifficultyWithOffset(
        difficulty: Int,
        offset: Int,
        lastOperand1: Int,
        lastOperand2: Int
    ) = findProblem(difficulty, offset, lastOperand1, lastOperand2).let {
        MultiplicationProblem(difficulty = it.difficulty, operand1 = it.operand1, operand2 = it.operand2)
    }

    override suspend fun getDivisionProblemsByDifficultyWithOffset(
        difficulty: Int,
        offset: Int,
        lastOperand1: Int,
        lastOperand2: Int
    ) = findProblem(difficulty, offset, lastOperand1, lastOperand2).let {
        DivisionProblem(difficulty = it.difficulty, operand1 = it.operand1, operand2 = it.operand2)
    }
}
