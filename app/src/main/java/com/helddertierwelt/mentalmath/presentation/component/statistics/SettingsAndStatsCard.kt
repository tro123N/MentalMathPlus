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

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.helddertierwelt.mentalmath.presentation.theme.ThemeMode
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.settings.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.statistics.StatisticsViewModel
import com.helddertierwelt.mentalmath.util.AppLocale
import kotlin.math.max

@Composable
fun SettingsAndStatsCard(
    fontSize: TextUnit,
    iconSize: Dp,
    modifier: Modifier
) {
    val activity = LocalActivity.current as ViewModelStoreOwner
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(activity)
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
    val lineHeight = fontSize / 1.6f
    val themeMode = settingsViewModel.settingsState.collectAsState().value.themeMode

    val statisticsGameRecord =
        if (statisticsViewModel.showLoadedGameRecord.collectAsState().value) {
            statisticsViewModel.loadedGameRecord.collectAsState().value
        } else {
            statisticsViewModel.lastGameRecord.collectAsState().value
        }

    Card(
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth(),
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
                    .padding(16.dp, 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seed: ",
                    fontSize = lineHeight,
                    color = MaterialTheme.colorScheme.onPrimary,
                    lineHeight = lineHeight
                )
                Text(
                    text = "${statisticsGameRecord.seed}",
                    fontSize = lineHeight,
                    color = MaterialTheme.colorScheme.onPrimary,
                    lineHeight = lineHeight
                )
            }
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.background
            )
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SettingsModeColumn(
                    modeEnabled = statisticsGameRecord.isModeEnabled,
                    fontSize = fontSize,
                    iconSize = iconSize,
                    limit = statisticsGameRecord.modeLimit
                )

                val additionRange = statisticsGameRecord.additionRangeStart to statisticsGameRecord.additionRangeEnd
                val additionRangeString = when {
                    additionRange.first <= 0 || additionRange.second <= 0 -> "-"
                    additionRange.first == additionRange.second -> additionRange.first.toString()
                    else -> "${additionRange.first}-${additionRange.second}"
                }
                StatsColumn(
                    fontSize = fontSize,
                    statsText = additionRangeString,
                    iconId = R.drawable.addition_24dp,
                    iconSize = iconSize,
                    iconColor = green
                )

                val subtractionRange = statisticsGameRecord.subtractionRangeStart to statisticsGameRecord.subtractionRangeEnd
                val subtractionRangeString = when {
                    subtractionRange.first <= 0 || subtractionRange.second <= 0 -> "-"
                    subtractionRange.first == subtractionRange.second -> subtractionRange.first.toString()
                    else -> "${subtractionRange.first}-${subtractionRange.second}"
                }
                StatsColumn(
                    fontSize = fontSize,
                    statsText = subtractionRangeString,
                    iconId = R.drawable.subtraction_24dp,
                    iconSize = iconSize,
                    iconColor = red
                )

                val multiplicationRange = statisticsGameRecord.multiplicationRangeStart to statisticsGameRecord.multiplicationRangeEnd
                val multiplicationRangeString = when {
                    multiplicationRange.first <= 0 || multiplicationRange.second <= 0 -> "-"
                    multiplicationRange.first == multiplicationRange.second -> multiplicationRange.first.toString()
                    else -> "${multiplicationRange.first}-${multiplicationRange.second}"
                }
                StatsColumn(
                    fontSize = fontSize,
                    statsText = multiplicationRangeString,
                    iconId = R.drawable.multiplication_24dp,
                    iconSize = iconSize,
                    iconColor = yellow
                )

                val divisionRange = statisticsGameRecord.divisionRangeStart to statisticsGameRecord.divisionRangeEnd
                val divisionRangeString = when {
                    divisionRange.first <= 0 || divisionRange.second <= 0 -> "-"
                    divisionRange.first == divisionRange.second -> divisionRange.first.toString()
                    else -> "${divisionRange.first}-${divisionRange.second}"
                }
                StatsColumn(
                    fontSize = fontSize,
                    statsText = divisionRangeString,
                    iconId = R.drawable.division_24dp,
                    iconSize = iconSize,
                    iconColor = blue
                )
            }
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.background
            )
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatsProgressColumn(
                    modeEnabled = statisticsGameRecord.isModeEnabled,
                    fontSize = fontSize,
                    iconSize = iconSize,
                    totalAnswers = statisticsGameRecord.totalAnswers,
                    activeTime = statisticsGameRecord.activeTime
                )
                StatsColumn(
                    fontSize = fontSize,
                    statsText = statisticsGameRecord.correctAnswers.toString(),
                    iconId = R.drawable.check_24dp,
                    iconSize = iconSize,
                    iconColor = green
                )

                val percentage = statisticsGameRecord.correctAnswers.toFloat() / statisticsGameRecord.totalAnswers.toFloat() * 100f
                val percentageString = if (percentage.isNaN()) "-" else AppLocale.format("%.1f", percentage)
                StatsColumn(
                    fontSize = fontSize,
                    statsText = percentageString,
                    iconId = R.drawable.percent_24dp,
                    iconSize = iconSize,
                    iconColor = green
                )

                val activeMinutes = max(1, (statisticsGameRecord.activeTime / 1000)) / 60.0f
                val correctPerMinute = AppLocale.format("%.1f", statisticsGameRecord.correctAnswers.toFloat() / activeMinutes)
                val darkTheme = when (themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                StatsColumn(
                    fontSize = fontSize,
                    statsText = correctPerMinute,
                    iconId = if (darkTheme) R.drawable.check_min_dark_24dp else R.drawable.check_min_light_24dp,
                    iconSize = iconSize
                )

                StatsColumn(
                    fontSize = fontSize,
                    statsText = formatTime(statisticsGameRecord.totalTime),
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

            val activeScore = AppLocale.format("%.2f", statisticsGameRecord.activeScore)
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp, 16.dp, 3.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.active),
                    fontSize = fontSize,
                    modifier = modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = activeScore,
                    fontSize = fontSize,
                    modifier = modifier.padding(end = 16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Icon(
                    painter = painterResource(R.drawable.star_24dp),
                    contentDescription = "activeScoreIcon",
                    modifier = modifier.size(iconSize),
                    tint = green
                )
            }

            val totalScore = AppLocale.format("%.2f", statisticsGameRecord.totalScore)
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 3.dp, 16.dp, 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total),
                    fontSize = fontSize,
                    modifier = modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = totalScore,
                    fontSize = fontSize,
                    modifier = modifier.padding(end = 16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Icon(
                    painter = painterResource(R.drawable.star_24dp),
                    contentDescription = "totalScoreIcon",
                    modifier = modifier.size(iconSize),
                    tint = yellow
                )
            }
        }
    }
}
