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

package com.helddertierwelt.mentalmath.presentation.component.result.problems

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow


@Composable
fun ProblemResult(
    problem: SolvedProblem,
    status: ProblemStatus,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val text = buildAnnotatedString {
        append("${problem.operand1} ${problem.operator.symbol} ${problem.operand2} = ")

        when (status) {
            ProblemStatus.CORRECT -> {
                append(problem.correctResult.toString())
            }

            ProblemStatus.INCORRECT -> {
                withStyle(
                    SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        color = red
                    )
                ) {
                    append(problem.userInput)
                }
                append(" ${problem.correctResult}")
            }

            ProblemStatus.SKIPPED -> {
                withStyle(SpanStyle(color = yellow)) {
                    append("? ")
                }
                append(problem.correctResult.toString())
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Text(
            text = text,
            modifier = modifier,
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}