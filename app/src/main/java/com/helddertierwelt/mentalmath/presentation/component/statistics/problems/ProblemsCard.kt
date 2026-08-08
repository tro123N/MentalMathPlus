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

package com.helddertierwelt.mentalmath.presentation.component.statistics.problems

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel

@Composable
fun ProblemsCard(
    resultFontSize: TextUnit,
    resultIconSize: Dp,
    iconSize: Dp
) {

    val activity = LocalActivity.current as ViewModelStoreOwner
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(activity)
    val solvedProblems =
        if (statisticsViewModel.showLoadedGameRecord.collectAsState().value) {
            statisticsViewModel.loadedGameRecord.collectAsState().value.solvedProblems
        } else {
            statisticsViewModel.lastGameRecord.collectAsState().value.solvedProblems
        }

    Card(
        shape = RoundedCornerShape(32.dp),
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
                    fontSize = resultFontSize,
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
                        .size(iconSize)
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
                    fontSize = resultFontSize,
                )
                Text(
                    text = stringResource(R.string.status),
                    fontSize = resultFontSize,
                )
            }
            solvedProblems.forEach { problem ->

                val status = when {
                    problem.userInput.isEmpty() -> ProblemStatus.SKIPPED
                    problem.correctResult == problem.userInput.toInt() -> ProblemStatus.CORRECT
                    else -> ProblemStatus.INCORRECT
                }

                ProblemRow(
                    modifier = Modifier.padding(16.dp, 4.dp),
                ) {
                    ProblemResult(
                        problem = problem,
                        status = status, fontSize = resultFontSize
                    )
                    ProblemStatusIcon(
                        status = status,
                        iconSize = resultIconSize
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}