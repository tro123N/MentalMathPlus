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

package com.helddertierwelt.mentalmath

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helddertierwelt.mentalmath.data.db.MathProblemDatabase
import com.helddertierwelt.mentalmath.data.entity.MathProblem
import com.helddertierwelt.mentalmath.data.entity.toMathProblem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MathProblemDaoTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: MathProblemDatabase
    private lateinit var tables: List<ProblemTable>

    private val difficulties = 1..9
    private val noLastProblem = -1 to -1

    @Before
    fun openDatabase() {
        database = Room.databaseBuilder(context, MathProblemDatabase::class.java, TEST_DATABASE)
            .createFromAsset("math_problems.db")
            .build()
        tables = problemTablesOf(database)
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun operandsAreAlwaysStoredLargerFirst() {
        tables.forEach { table ->
            assertEquals("${table.name} has rows with operand1 < operand2", 0, countRows(table, "operand1 < operand2"))
        }
    }

    @Test
    fun divisionProblemsHaveWholeNumberResults() {
        assertEquals(0, countRows(divisionTable(), "operand2 = 0 OR operand1 % operand2 <> 0"))
    }

    @Test
    fun everyDifficultyHasAtLeastTwoProblems() = runBlocking {
        tables.forEach { table ->
            difficulties.forEach { difficulty ->
                val count = table.count(difficulty, noLastProblem)
                assertTrue("${table.name}, difficulty $difficulty has only $count problems", count >= 2)
            }
        }
    }

    @Test
    fun lastProblemIsExcludedFromTheCount() = runBlocking {
        tables.forEach { table ->
            difficulties.forEach { difficulty ->
                val lastProblem = table.get(difficulty, 0, noLastProblem)

                val countWithoutLast = table.count(difficulty, lastProblem.operands())

                assertEquals(table.count(difficulty, noLastProblem) - 1, countWithoutLast)
            }
        }
    }

    @Test
    fun lastProblemIsNeverReturnedAgain() = runBlocking {
        val difficulty = 1
        tables.forEach { table ->
            val lastProblem = table.get(difficulty, 0, noLastProblem)
            val count = table.count(difficulty, lastProblem.operands())

            repeat(count) { offset ->
                val nextProblem = table.get(difficulty, offset, lastProblem.operands())
                assertNotEquals(table.name, lastProblem, nextProblem)
            }
        }
    }

    private fun countRows(table: ProblemTable, condition: String): Int {
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM ${table.name} WHERE $condition").use {
            it.moveToFirst()
            return it.getInt(0)
        }
    }

    private fun divisionTable() = tables.single { it.name == "problems_division" }

    private fun MathProblem.operands() = operand1 to operand2

    private class ProblemTable(
        val name: String,
        val count: suspend (difficulty: Int, lastOperands: Pair<Int, Int>) -> Int,
        val get: suspend (difficulty: Int, offset: Int, lastOperands: Pair<Int, Int>) -> MathProblem
    )

    private fun problemTablesOf(database: MathProblemDatabase): List<ProblemTable> {
        val addition = database.additionProblemDao()
        val subtraction = database.subtractionProblemDao()
        val multiplication = database.multiplicationProblemDao()
        val division = database.divisionProblemDao()

        return listOf(
            ProblemTable(
                name = "problems_addition",
                count = { difficulty, (last1, last2) ->
                    addition.countAdditionProblemsByDifficulty(difficulty, last1, last2)
                },
                get = { difficulty, offset, (last1, last2) ->
                    addition.getAdditionProblemsByDifficultyWithOffset(difficulty, offset, last1, last2).toMathProblem()
                }
            ),
            ProblemTable(
                name = "problems_subtraction",
                count = { difficulty, (last1, last2) ->
                    subtraction.countSubtractionProblemsByDifficulty(difficulty, last1, last2)
                },
                get = { difficulty, offset, (last1, last2) ->
                    subtraction.getSubtractionProblemsByDifficultyWithOffset(difficulty, offset, last1, last2).toMathProblem()
                }
            ),
            ProblemTable(
                name = "problems_multiplication",
                count = { difficulty, (last1, last2) ->
                    multiplication.countMultiplicationProblemsByDifficulty(difficulty, last1, last2)
                },
                get = { difficulty, offset, (last1, last2) ->
                    multiplication.getMultiplicationProblemsByDifficultyWithOffset(difficulty, offset, last1, last2).toMathProblem()
                }
            ),
            ProblemTable(
                name = "problems_division",
                count = { difficulty, (last1, last2) ->
                    division.countDivisionProblemsByDifficulty(difficulty, last1, last2)
                },
                get = { difficulty, offset, (last1, last2) ->
                    division.getDivisionProblemsByDifficultyWithOffset(difficulty, offset, last1, last2).toMathProblem()
                }
            )
        )
    }

    private companion object {
        const val TEST_DATABASE = "math_problems_test.db"
    }
}
