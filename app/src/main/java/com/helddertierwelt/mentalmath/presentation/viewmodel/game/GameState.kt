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

package com.helddertierwelt.mentalmath.presentation.viewmodel.game

import com.helddertierwelt.mentalmath.presentation.viewmodel.game.Operation
import com.helddertierwelt.mentalmath.presentation.viewmodel.game.TaskResult
import com.helddertierwelt.mentalmath.presentation.viewmodel.game.TaskResultConverter
import java.io.Serializable

data class GameState(

    val isModeEnabled: Boolean = false,
    val modeLimit: Int = 1,
    val operand1: Int = 1,
    val operand2: Int = 1,
    val operator: Operation = Operation.ADDITION,
    val input: String = "",
    val tasks: List<TaskResult> = emptyList(),
    val enabledOperators: Map<Operation, Pair<Float, Float>> = mutableMapOf(),
    val isCorrect: Boolean? = null,

    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val skippedAnswers: Int = 0,
    val totalAnswers: Int = 0,

    val isGameStarted: Boolean = false,
    val isTimerRunning: Boolean = false,
    val activeTime: Long = 0,
    val totalTime: Long = 0,
    val startTimeStamp: Long = 0,
    val pausedAt: Long = Long.MAX_VALUE,
    val totalPauseDuration: Long = 0L,
    val seed: Long = 0L,
    val isLinkGame: Boolean = false,

    ) : Serializable {
    companion object {
        fun deserialize(serialized: String): GameState {
            val parts = serialized.split(",")
            val isModeEnabled = parts[0].toBoolean()
            val modeLimit = parts[1].toInt()
            val operand1 = parts[2].toInt()
            val operand2 = parts[3].toInt()
            val operator = Operation.valueOf(parts[4])
            val input = parts[5]
            val tasks = TaskResultConverter().toTaskResultList(parts[6])
            val enabledOperators = mutableMapOf<Operation, Pair<Float, Float>>()
            val isCorrect = parts[8].toBooleanStrictOrNull()
            val correctAnswers = parts[9].toInt()
            val wrongAnswers = parts[10].toInt()
            val skippedAnswers = parts[11].toInt()
            val totalAnswers = parts[12].toInt()
            val isGameStarted = parts[13].toBoolean()
            val isTimerRunning = parts[14].toBoolean()
            val activeTime = parts[15].toLong()
            val totalTime = parts[16].toLong()
            val startTimeStamp = parts[17].toLong()
            val pausedAt = parts[18].toLong()
            val totalPauseDuration = parts[19].toLong()
            val seed = parts[20].toLong()
            val isLinkGame = parts[21].toBoolean()
            return GameState(
                isModeEnabled = isModeEnabled,
                modeLimit = modeLimit,
                operand1 = operand1,
                operand2 = operand2,
                operator = operator,
                input = input,
                tasks = tasks,
                enabledOperators = enabledOperators,
                isCorrect = isCorrect,
                correctAnswers = correctAnswers,
                wrongAnswers = wrongAnswers,
                skippedAnswers = skippedAnswers,
                totalAnswers = totalAnswers,
                isGameStarted = isGameStarted,
                isTimerRunning = isTimerRunning,
                activeTime = activeTime,
                totalTime = totalTime,
                startTimeStamp = startTimeStamp,
                pausedAt = pausedAt,
                totalPauseDuration = totalPauseDuration,
                seed = seed,
                isLinkGame = isLinkGame
            )
        }
    }
}
