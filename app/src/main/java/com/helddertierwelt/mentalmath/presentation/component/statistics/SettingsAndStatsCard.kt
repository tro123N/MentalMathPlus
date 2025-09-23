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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import java.util.Locale
import kotlin.math.pow

@Composable
fun SettingsAndStatsCard(
    settingsViewModel: SettingsViewModel,
    operatorSize: TextUnit,
    fontSize: TextUnit,
    iconSize: Dp,
    modifier: Modifier
) {
    val gameViewModel: GameViewModel = viewModel()
    val gameState = gameViewModel.gamesState.value
    val totalTime = gameState.totalTime
    val percentage = gameState.correctAnswers.toFloat() /
            gameState.totalAnswers.toFloat() * 100f
    val percentageString = if (percentage.isNaN()) "-" else String.format(
        Locale.getDefault(),
        "%.1f",
        percentage
    )
    val correctPerMinute = String.format(
        Locale.getDefault(),
        "%.1f",
        gameState.correctAnswers.toFloat() /
                (gameState.activeTime.toFloat() / 60000f)
    )
    val activeScoreValue = 10f * gameState.correctAnswers.toFloat().pow(2f) /
            ((gameState.activeTime.toFloat() / 60000f) * maxOf(
                1f,
                gameState.totalAnswers.toFloat()
            ))
    val activeScore = String.format(
        Locale.getDefault(), "%s:  %.2f", stringResource(R.string.active_score),
        activeScoreValue
    )
    val totalScoreValue = 10f * gameState.correctAnswers.toFloat().pow(2f) /
            ((totalTime.toFloat() / 60000f) * maxOf(1f, gameState.totalAnswers.toFloat()))
    val totalScore = String.format(
        Locale.getDefault(), "%s:  %.2f", stringResource(R.string.total_score),
        totalScoreValue
    )
    Card(
        shape = RoundedCornerShape(32.dp),
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
    ) {
        Column(
            modifier = modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SettingsModeColumn(
                    modeEnabled = settingsViewModel.settingsState.value.isModeEnabled,
                    fontSize = fontSize,
                    iconSize = iconSize,
                    limit = settingsViewModel.settingsState.value.limit.toInt()
                )
                SettingsOperatorColumn(
                    operatorSize = operatorSize,
                    fontSize = fontSize,
                    operatorEnabled = settingsViewModel.settingsState.value.isAdditionEnabled,
                    difficultyRange = settingsViewModel.settingsState.value.additionRange,
                    operatorId = R.string.add,
                    operatorColor = green
                )
                SettingsOperatorColumn(
                    operatorSize = operatorSize,
                    fontSize = fontSize,
                    operatorEnabled = settingsViewModel.settingsState.value.isSubtractionEnabled,
                    difficultyRange = settingsViewModel.settingsState.value.subtractionRange,
                    operatorId = R.string.subtract,
                    operatorColor = red
                )
                SettingsOperatorColumn(
                    operatorSize = operatorSize,
                    fontSize = fontSize,
                    operatorEnabled = settingsViewModel.settingsState.value.isMultiplicationEnabled,
                    difficultyRange = settingsViewModel.settingsState.value.multiplicationRange,
                    operatorId = R.string.multiply,
                    operatorColor = yellow
                )
                SettingsOperatorColumn(
                    operatorSize = operatorSize,
                    fontSize = fontSize,
                    operatorEnabled = settingsViewModel.settingsState.value.isDivisionEnabled,
                    difficultyRange = settingsViewModel.settingsState.value.divisionRange,
                    operatorId = R.string.divide,
                    operatorColor = blue
                )
            }
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.background
            )
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatsProgressColumn(
                    modeEnabled = settingsViewModel.settingsState.value.isModeEnabled,
                    fontSize = fontSize,
                    iconSize = iconSize,
                    totalAnswers = gameState.totalAnswers,
                    activeTime = gameState.activeTime
                )
                StatsColumn(
                    fontSize = fontSize,
                    statsText = gameState.correctAnswers.toString(),
                    iconId = R.drawable.check_24dp,
                    iconSize = iconSize,
                    iconColor = green
                )

                StatsColumn(
                    fontSize = fontSize,
                    statsText = percentageString,
                    iconId = R.drawable.percent_24dp,
                    iconSize = iconSize,
                    iconColor = green
                )

                StatsCorrectPerMinuteColumn(
                    iconId = R.drawable.check_24dp,
                    iconSize = iconSize,
                    iconColor = green,
                    iconText = "min",
                    iconFontSize = fontSize / 2,
                    fontSize = fontSize,
                    text = correctPerMinute
                )

                StatsColumn(
                    fontSize = fontSize,
                    statsText = formatTime(totalTime),
                    iconId = R.drawable.access_time_24dp,
                    iconSize = iconSize,
                    iconColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.background
            )
            Spacer(modifier = modifier.height(8.dp))
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeScore,
                    fontSize = fontSize,
                    modifier = modifier.padding(horizontal = 16.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.star_24dp),
                    contentDescription = "activeSoreIcon",
                    modifier = modifier.size(iconSize),
                    tint = yellow
                )
            }
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = totalScore,
                    fontSize = fontSize,
                    modifier = modifier.padding(horizontal = 16.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.star_24dp),
                    contentDescription = "totalScoreIcon",
                    modifier = modifier.size(iconSize),
                    tint = green
                )
            }
        }
    }
}
