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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.game.SolvedProblem
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue

@Composable
fun ProblemsCard(
    solvedProblems: List<SolvedProblem>,
) {
    val dimens = MentalMathTheme.dimens

    Card(
        shape = RoundedCornerShape(dimens.components.cardCornerRadius),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
    ) {
        if (solvedProblems.isEmpty()) {
            Row(
                modifier = Modifier
                    .padding(16.dp, 16.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.no_problems),
                    fontSize = dimens.fonts.resultText,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )
                Icon(
                    painter = painterResource(R.drawable.hourglass_bottom_24dp),
                    contentDescription = "time is up",
                    tint = blue,
                    modifier = Modifier
                        .size(dimens.icons.cardHeader)
                        .align(Alignment.CenterVertically)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            ProblemRow(
                modifier = Modifier.padding(16.dp, 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.problem),
                    fontSize = dimens.fonts.resultText,
                )
                Text(
                    text = stringResource(R.string.status),
                    fontSize = dimens.fonts.resultText,
                )
            }
            solvedProblems.forEach { problem ->

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    ProblemResult(
                        problem = problem,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProblemStatusIcon(
                        status = problem.status,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}