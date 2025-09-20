package com.helddertierwelt.mentalmath.data.repository

import com.helddertierwelt.mentalmath.data.dao.AdditionTaskDao
import com.helddertierwelt.mentalmath.data.dao.DivisionTaskDao
import com.helddertierwelt.mentalmath.data.dao.MultiplicationTaskDao
import com.helddertierwelt.mentalmath.data.dao.SubtractionTaskDao
import com.helddertierwelt.mentalmath.data.entity.MathTask
import com.helddertierwelt.mentalmath.data.entity.toMathTask
import kotlin.random.Random

class MathTaskRepository(
    private val additionDao: AdditionTaskDao,
    private val subtractionDao: SubtractionTaskDao,
    private val multiplicationDao: MultiplicationTaskDao,
    private val divisionDao: DivisionTaskDao,
) {
    suspend fun getRandomAdditionTaskByDifficulty(difficulty: Int, random: Random, lastOperands: Pair<Int,Int>): MathTask {
        val count = additionDao.countAdditionTasksByDifficulty(difficulty, lastOperands.first, lastOperands.second)
        val offset = random.nextInt(count)
        return additionDao.getAdditionTaskByDifficultyWithOffset(difficulty, offset, lastOperands.first, lastOperands.second).toMathTask()
    }

    suspend fun getRandomSubtractionTaskByDifficulty(difficulty: Int, random: Random, lastOperands: Pair<Int,Int>): MathTask {
        val count = subtractionDao.countSubtractionTasksByDifficulty(difficulty, lastOperands.first, lastOperands.second)
        val offset = random.nextInt(count)
        return subtractionDao.getSubtractionTaskByDifficultyWithOffset(difficulty, offset, lastOperands.first, lastOperands.second).toMathTask()
    }

    suspend fun getRandomMultiplicationTaskByDifficulty(difficulty: Int, random: Random, lastOperands: Pair<Int,Int>): MathTask {
        val count = multiplicationDao.countMultiplicationTasksByDifficulty(difficulty, lastOperands.first, lastOperands.second)
        val offset = random.nextInt(count)
        return multiplicationDao.getMultiplicationTaskByDifficultyWithOffset(difficulty, offset, lastOperands.first, lastOperands.second).toMathTask()
    }

    suspend fun getRandomDivisionTaskByDifficulty(difficulty: Int, random: Random, lastOperands: Pair<Int,Int>): MathTask {
        val lastOperand1 = lastOperands.second
        val lastOperand2 = lastOperands.first / lastOperands.second
        val count = divisionDao.countDivisionTasksByDifficulty(difficulty, lastOperands.second, lastOperand2)
        val offset = random.nextInt(count)
        return divisionDao.getDivisionTaskByDifficultyWithOffset(difficulty, offset, lastOperand1, lastOperand2).toMathTask()
    }
}