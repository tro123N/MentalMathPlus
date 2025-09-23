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

package com.helddertierwelt.mentalmath.presentation.component.statistics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import java.util.Locale

@Composable
fun TasksCard(
    resultFontSize: androidx.compose.ui.unit.TextUnit,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val gameViewModel: GameViewModel = viewModel()
    var isExpanded by remember { mutableStateOf(true) }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),

                text = stringResource(R.string.exercises),
                fontSize = resultFontSize
            )
            Icon(
                painter = painterResource(if (isExpanded) R.drawable.expand_less_24dp else R.drawable.expand_more_24dp),
                contentDescription = "Expand",
                modifier = Modifier
                    .size(iconSize)
                    .clickable { isExpanded = !isExpanded }
            )
        }
        if (isExpanded) {
            gameViewModel.gamesState.value.tasks.forEach { task ->
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val exercise = String.format(
                        Locale.getDefault(),
                        "%d %s %d = %d",
                        task.operand1,
                        stringResource(task.operator.stringId),
                        task.operand2,
                        task.correctResult
                    )
                    Text(
                        text = exercise,
                        fontSize = resultFontSize,
                    )

                    val isCorrect =
                        if (!task.userInput.isEmpty()) task.correctResult == task.userInput.toInt() else null
                    when (isCorrect) {

                        true -> Icon(
                            painter = painterResource(R.drawable.check_circle_24dp),
                            contentDescription = "Correct",
                            modifier = Modifier
                                .size(iconSize)
                                .align(Alignment.CenterVertically),
                            tint = green
                        )

                        false -> {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = task.userInput,
                                color = red,
                                fontSize = resultFontSize,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.cancel_24dp),
                                contentDescription = "Skipped",
                                modifier = Modifier
                                    .size(iconSize)
                                    .align(Alignment.CenterVertically),
                                tint = red
                            )
                        }


                        null -> Icon(
                            painter = painterResource(R.drawable.cancel_24dp),
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