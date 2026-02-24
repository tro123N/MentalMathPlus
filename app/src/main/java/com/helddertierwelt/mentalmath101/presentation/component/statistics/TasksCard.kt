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

package com.helddertierwelt.mentalmath101.presentation.component.statistics

import androidx.activity.compose.LocalActivity
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
import com.helddertierwelt.mentalmath101.R
import com.helddertierwelt.mentalmath101.presentation.theme.green
import com.helddertierwelt.mentalmath101.presentation.theme.red
import com.helddertierwelt.mentalmath101.presentation.theme.yellow
import com.helddertierwelt.mentalmath101.presentation.viewmodel.statistics.StatisticsViewModel

@Composable
fun TasksCard(
    resultFontSize: TextUnit,
    iconSize: Dp
) {
    val activity = LocalActivity.current as ViewModelStoreOwner
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(activity)
    Card(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        TaskCardRow(
            modifier = Modifier
                .padding(16.dp, 4.dp),
        ) {
            Text(
                text = stringResource(R.string.solution),
                fontSize = resultFontSize,
            )
            Text(
                text = stringResource(R.string.input),
                fontSize = resultFontSize,
            )
        }
        val statisticsGameRecord =
            if (statisticsViewModel.showLoadedGameRecord.collectAsState().value) {
                statisticsViewModel.loadedGameRecord.collectAsState().value.taskResults
            } else {
                statisticsViewModel.lastGameRecord.collectAsState().value.taskResults
            }
        statisticsGameRecord
            .forEach { task ->
                TaskCardRow(
                    modifier = Modifier
                        .padding(16.dp, 4.dp),
                ) {
                    val exercise = "%d %s %d = %d".format(
                        task.operand1,
                        stringResource(task.operator.stringId),
                        task.operand2,
                        task.correctResult
                    )
                    Row {
                        Text(
                            text = exercise,
                            fontSize = resultFontSize,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    val isCorrect =
                        if (!task.userInput.isEmpty()) task.correctResult == task.userInput.toInt() else null

                    when (isCorrect) {

                        true -> Row {
                            Icon(
                                painter = painterResource(R.drawable.check_circle_24dp),
                                contentDescription = "Correct",
                                modifier = Modifier
                                    .size(iconSize)
                                    .align(Alignment.CenterVertically),
                                tint = green
                            )
                        }

                        false -> Row {
                            Text(
                                text = task.userInput,
                                color = red,
                                fontSize = resultFontSize,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.cancel_24dp),
                                contentDescription = "Incorrect",
                                modifier = Modifier
                                    .size(iconSize)
                                    .align(Alignment.CenterVertically),
                                tint = red
                            )
                        }

                        null -> Row {
                            Icon(
                                painter = painterResource(R.drawable.skipped_24dp),
                                contentDescription = "Skipped",
                                modifier = Modifier
                                    .size(iconSize)
                                    .align(Alignment.CenterVertically),
                                tint = yellow
                            )
                        }
                    }
                }
            }
        Spacer(modifier = Modifier.height(12.dp))
    }
}