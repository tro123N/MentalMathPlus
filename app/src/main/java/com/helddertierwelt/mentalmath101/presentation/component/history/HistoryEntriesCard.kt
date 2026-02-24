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

package com.helddertierwelt.mentalmath101.presentation.component.history

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath101.data.entity.GameRecord
import java.util.Locale

@Composable
fun HistoryEntriesCard(
    gameRecords: List<GameRecord>,
    fontSize: TextUnit,
    onClick: (GameRecord) -> Unit,
    isEnabled: Boolean = true,
    currentSortMode: SortMode,
    showActiveScore: Boolean
) {
    fun sortGameRecords(records: List<GameRecord>, ascending: Boolean): List<GameRecord> {
        return records.sortedWith { record1, record2 ->
            compareValuesBy(
                record1, record2,
                GameRecord::isModeEnabled,
                GameRecord::modeLimit,
                GameRecord::isAdditionEnabled,
                GameRecord::isSubtractionEnabled,
                GameRecord::isMultiplicationEnabled,
                GameRecord::isDivisionEnabled,
                GameRecord::totalScore
            )
        }.let { if (ascending) it else it.asReversed() }
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
                Row(Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "There are no games with a score",
                        fontSize = fontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                gameRecordsForHistory.forEachIndexed { index, game ->
                    val limit = if (game.isModeEnabled) "%d0".format(Locale.ENGLISH,game.modeLimit) else "%d:00".format(Locale.ENGLISH,game.modeLimit)
                    val operatorStrings = listOf(
                        if (game.isAdditionEnabled) "+" else "",
                        if (game.isSubtractionEnabled) "−" else "",
                        if (game.isMultiplicationEnabled) "×" else "",
                        if (game.isDivisionEnabled) "÷" else ""
                    )
                    val scoreString = "%.2f".format(Locale.ENGLISH,if (showActiveScore) game.activeScore else game.totalScore)
                    val allStrings = listOf(limit) + operatorStrings + scoreString

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
                            .clickable(enabled = isEnabled) { onClick(game) }
                    ) {
                        allStrings.forEach { textContent ->
                            Text(
                                text = textContent,
                                modifier = Modifier.weight(1f),
                                fontSize = fontSize,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}