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

package com.helddertierwelt.mentalmath.presentation.component.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.presentation.component.result.summary.formatScore
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme

@Composable
fun HistoryEntriesCard(
    gameRecords: List<GameRecord>,
    onClick: (Int) -> Unit,
    isEnabled: Boolean = true,
    currentSortMode: SortMode,
) {
    val dimens = MentalMathTheme.dimens
    fun sortGameRecords(records: List<GameRecord>, ascending: Boolean): List<GameRecord> {
        return records.sortedWith { record1, record2 ->
            compareValuesBy(
                record1, record2,
                GameRecord::gameMode,
                GameRecord::modeLimit,
                GameRecord::additionRangeStart,
                GameRecord::additionRangeEnd,
                GameRecord::subtractionRangeStart,
                GameRecord::subtractionRangeEnd,
                GameRecord::multiplicationRangeStart,
                GameRecord::multiplicationRangeEnd,
                GameRecord::divisionRangeStart,
                GameRecord::divisionRangeEnd,
                GameRecord::activeScore
            )
        }.let { if (ascending) it else it.asReversed() }
    }

    fun formatRange(rangeStart: Int, rangeEnd: Int): String {
        return when {
            rangeStart <= 0 || rangeEnd <= 0 -> "-"
            rangeStart == rangeEnd -> "$rangeStart"
            else -> "$rangeStart-$rangeEnd"
        }
    }

    val gameRecordsForHistory = when (currentSortMode) {
        SortMode.TIME_DESCENDING -> gameRecords
        SortMode.TIME_ASCENDING -> gameRecords.asReversed()
        SortMode.ALPHA_DESCENDING -> sortGameRecords(gameRecords, ascending = false)
        SortMode.ALPHA_ASCENDING -> sortGameRecords(gameRecords, ascending = true)
    }
    Card(
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (gameRecords.isEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_games),
                        fontSize = dimens.fonts.historyEntry,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                gameRecordsForHistory.forEachIndexed { index, game ->
                    val limit = when (game.gameMode) {
                        GameMode.PROBLEMS -> "${game.modeLimit}0"
                        GameMode.TIMED -> "${game.modeLimit}:00"
                        GameMode.SURVIVAL -> "${game.modeLimit*20}${stringResource(R.string.second)}"
                    }
                    val rangeStrings = listOf(
                        formatRange(game.additionRangeStart, game.additionRangeEnd),
                        formatRange(game.subtractionRangeStart, game.subtractionRangeEnd),
                        formatRange(game.multiplicationRangeStart, game.multiplicationRangeEnd),
                        formatRange(game.divisionRangeStart, game.divisionRangeEnd)
                    )
                    val scoreString = formatScore(game.activeScore)
                    val allStrings = listOf(limit) + rangeStrings + scoreString

                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = isEnabled) { onClick(game.id) }
                    ) {
                        allStrings.forEach { textContent ->
                            Text(
                                text = textContent,
                                modifier = Modifier.weight(1f),
                                fontSize = dimens.fonts.historyEntry,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}