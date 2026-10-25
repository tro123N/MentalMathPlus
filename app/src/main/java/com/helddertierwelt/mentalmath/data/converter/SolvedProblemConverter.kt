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

package com.helddertierwelt.mentalmath.data.converter

import androidx.room.TypeConverter
import com.helddertierwelt.mentalmath.domain.game.Operation
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem

class SolvedProblemConverter {

    companion object {
        private const val FORMAT_VERSION = "1"
    }

    @TypeConverter
    fun fromSolvedProblemList(problems: List<SolvedProblem>): String {
        if (problems.isEmpty()) {
            return "$FORMAT_VERSION|"
        }

        val encodedProblems = problems.joinToString(";") { problem ->
            listOf(
                problem.operand1.toString(),
                problem.operator.symbol,
                problem.operand2.toString(),
                problem.userInput,
                problem.status.name
            ).joinToString(",")
        }

        return "$FORMAT_VERSION|$encodedProblems"
    }

    @TypeConverter
    fun toSolvedProblemList(value: String): List<SolvedProblem> {
        // version 29 wrote the old JSON "[]" when the app was reset
        if (value.isBlank() || value == "[]") {
            return emptyList()
        }

        val separatorIndex = value.indexOf('|')

        require(separatorIndex > 0) {
            "Invalid SolvedProblem format"
        }

        val version = value.substring(0, separatorIndex)

        require(version == FORMAT_VERSION) {
            "Unsupported SolvedProblem format version: $version"
        }

        val data = value.substring(separatorIndex + 1)

        if (data.isBlank()) {
            return emptyList()
        }

        return data.split(";").map { encodedProblem ->
            decodeProblem(encodedProblem)
        }
    }

    private fun decodeProblem(value: String): SolvedProblem {
        val fields = value.split(",", limit = 5)

        require(fields.size == 5) {
            "Invalid SolvedProblem: $value"
        }

        return SolvedProblem(
            operand1 = fields[0].toInt(),
            operator = operationFromSymbol(fields[1]),
            operand2 = fields[2].toInt(),
            userInput = fields[3],
            status = ProblemStatus.valueOf(fields[4])
        )
    }

    private fun operationFromSymbol(symbol: String): Operation {
        return Operation.entries.firstOrNull {
            it.symbol == symbol
        } ?: error("Unknown operation symbol: $symbol")
    }
}