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
    suspend fun getRandomAdditionTaskByDifficulty(difficulty: Int, random: Random): MathTask {
        val count = additionDao.countAdditionTasksByDifficulty(difficulty)
        val offset = random.nextInt(count)
        return additionDao.getAdditionTaskByDifficultyWithOffset(difficulty, offset).toMathTask()
    }

    suspend fun getRandomSubtractionTaskByDifficulty(difficulty: Int, random: Random): MathTask {
        val count = subtractionDao.countSubtractionTasksByDifficulty(difficulty)
        val offset = random.nextInt(count)
        return subtractionDao.getSubtractionTaskByDifficultyWithOffset(difficulty, offset).toMathTask()
    }

    suspend fun getRandomMultiplicationTaskByDifficulty(difficulty: Int, random: Random): MathTask {
        val count = multiplicationDao.countMultiplicationTasksByDifficulty(difficulty)
        val offset = random.nextInt(count)
        return multiplicationDao.getMultiplicationTaskByDifficultyWithOffset(difficulty, offset).toMathTask()
    }

    suspend fun getRandomDivisionTaskByDifficulty(difficulty: Int, random: Random): MathTask {
        val count = divisionDao.countDivisionTasksByDifficulty(difficulty)
        val offset = random.nextInt(count)
        return divisionDao.getDivisionTaskByDifficultyWithOffset(difficulty, offset).toMathTask()
    }
}