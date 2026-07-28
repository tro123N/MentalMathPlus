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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.settings.AppLocale
import com.helddertierwelt.mentalmath.domain.settings.ThemeMode
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsAndStatsCard(
    scoreFontSize: TextUnit,
    statsFontSize: TextUnit,
    scoreIconSize: Dp,
    statsIconSize: Dp,
    modifier: Modifier
) {
    val activity = LocalActivity.current as ViewModelStoreOwner
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(activity)
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
    val lineHeight = statsFontSize / 1.6f
    val themeMode = settingsViewModel.settingsState.collectAsState().value.themeMode

    val statisticsGameRecord =
        if (statisticsViewModel.showLoadedGameRecord.collectAsState().value) {
            statisticsViewModel.loadedGameRecord.collectAsState().value
        } else {
            statisticsViewModel.lastGameRecord.collectAsState().value
        }
    val correctAnswers = statisticsGameRecord.correctAnswers

    LaunchedEffect(Unit) {
        statisticsViewModel.loadBestGameScores()
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
                    text = AppLocale.format(stringResource(R.string.seed), statisticsGameRecord.seed),
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
                    fontSize = statsFontSize,
                    iconSize = statsIconSize,
                    limit = statisticsGameRecord.modeLimit
                )

                val additionRange = statisticsGameRecord.additionRangeStart to statisticsGameRecord.additionRangeEnd
                val additionRangeString = when {
                    additionRange.first <= 0 || additionRange.second <= 0 -> "-"
                    additionRange.first == additionRange.second -> additionRange.first.toString()
                    else -> "${additionRange.first}-${additionRange.second}"
                }
                StatsColumn(
                    fontSize = statsFontSize,
                    statsText = additionRangeString,
                    iconId = R.drawable.addition_24dp,
                    iconSize = statsIconSize,
                    iconColor = green,
                    tooltipText = stringResource(R.string.difficulty_addition)
                )

                val subtractionRange = statisticsGameRecord.subtractionRangeStart to statisticsGameRecord.subtractionRangeEnd
                val subtractionRangeString = when {
                    subtractionRange.first <= 0 || subtractionRange.second <= 0 -> "-"
                    subtractionRange.first == subtractionRange.second -> subtractionRange.first.toString()
                    else -> "${subtractionRange.first}-${subtractionRange.second}"
                }
                StatsColumn(
                    fontSize = statsFontSize,
                    statsText = subtractionRangeString,
                    iconId = R.drawable.subtraction_24dp,
                    iconSize = statsIconSize,
                    iconColor = red,
                    tooltipText = stringResource(R.string.difficulty_subtraction)
                )

                val multiplicationRange = statisticsGameRecord.multiplicationRangeStart to statisticsGameRecord.multiplicationRangeEnd
                val multiplicationRangeString = when {
                    multiplicationRange.first <= 0 || multiplicationRange.second <= 0 -> "-"
                    multiplicationRange.first == multiplicationRange.second -> multiplicationRange.first.toString()
                    else -> "${multiplicationRange.first}-${multiplicationRange.second}"
                }
                StatsColumn(
                    fontSize = statsFontSize,
                    statsText = multiplicationRangeString,
                    iconId = R.drawable.multiplication_24dp,
                    iconSize = statsIconSize,
                    iconColor = yellow,
                    tooltipText = stringResource(R.string.difficulty_multiplication)
                )

                val divisionRange = statisticsGameRecord.divisionRangeStart to statisticsGameRecord.divisionRangeEnd
                val divisionRangeString = when {
                    divisionRange.first <= 0 || divisionRange.second <= 0 -> "-"
                    divisionRange.first == divisionRange.second -> divisionRange.first.toString()
                    else -> "${divisionRange.first}-${divisionRange.second}"
                }
                StatsColumn(
                    fontSize = statsFontSize,
                    statsText = divisionRangeString,
                    iconId = R.drawable.division_24dp,
                    iconSize = statsIconSize,
                    iconColor = blue,
                    tooltipText = stringResource(R.string.difficulty_division)
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
                    fontSize = statsFontSize,
                    iconSize = statsIconSize,
                    totalAnswers = statisticsGameRecord.totalAnswers,
                    activeTime = statisticsGameRecord.activeTime
                )
                StatsColumn(
                    fontSize = statsFontSize,
                    statsText = correctAnswers.toString(),
                    iconId = R.drawable.check_24dp,
                    iconSize = statsIconSize,
                    iconColor = green,
                    tooltipText = stringResource(R.string.correct_answers)
                )
                StatsColumn(
                    fontSize = statsFontSize,
                    statsText = (statisticsGameRecord.totalAnswers - correctAnswers).toString(),
                    iconId = R.drawable.close_24dp,
                    iconSize = statsIconSize,
                    iconColor = red,
                    tooltipText = stringResource(R.string.incorrect_answers)
                )

                val percentage = correctAnswers.toFloat() / statisticsGameRecord.totalAnswers.toFloat() * 100f
                val percentageString = if (percentage.isNaN()) "-" else AppLocale.format("%.1f", percentage)
                StatsColumn(
                    fontSize = statsFontSize,
                    statsText = percentageString,
                    iconId = R.drawable.percent_24dp,
                    iconSize = statsIconSize,
                    iconColor = green,
                    tooltipText = stringResource(R.string.accuracy)
                )

                val timePerCorrectAnswer = if (correctAnswers==0) "-" else formatTimeWithDecimalSeconds(statisticsGameRecord.activeTime / correctAnswers)
                val darkTheme = when (themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                StatsColumn(
                    fontSize = statsFontSize,
                    statsText = timePerCorrectAnswer,
                    iconId = if (darkTheme) R.drawable.timer_dark_24dp else R.drawable.timer_light_24dp,
                    iconSize = statsIconSize,
                    tooltipText = stringResource(R.string.avg_time_per_correct_answer)
                )
            }
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.background
            )
            Spacer(modifier = modifier.height(8.dp))

            val top10 = statisticsViewModel.bestGameScore.collectAsState().value
            val rank = top10.indexOfFirst { it.id == statisticsGameRecord.id }
            val top10Rank = if (rank >= 0) rank + 1 else null
            val activeScore = formatScore(statisticsGameRecord.activeScore)
            ScoreRow(
                score = activeScore,
                rank = top10Rank,
                scoreFontSize = scoreFontSize,
                scoreIconSize = scoreIconSize,
                iconColor = yellow,
                tooltipText = stringResource(R.string.score)
            )
        }
    }
}

fun formatTimeWithDecimalSeconds(ms: Long): String {
    val seconds = ms / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return buildString {
        if (hours > 0) {
            append("$hours:")
            append(minutes.toString().padStart(2, '0'))
            append(":")
            append(remainingSeconds.toString().padStart(2, '0'))
        } else if (minutes > 0) {
            append("$minutes:")
            append(remainingSeconds.toString().padStart(2, '0'))
        } else {
            val secondsWithDecimal = ms / 1000.0
            if (secondsWithDecimal % 1.0 == 0.0) {
                append("${secondsWithDecimal.toInt()}s")
            } else {
                append(AppLocale.format("%.1fs", secondsWithDecimal))
            }
        }
    }
}

fun formatScore(score: Float): String {
    return AppLocale.format("%,d", (score * 100).roundToInt())
}

