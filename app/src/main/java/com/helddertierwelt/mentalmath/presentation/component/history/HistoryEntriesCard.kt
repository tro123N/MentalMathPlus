package com.helddertierwelt.mentalmath.presentation.component.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.data.entity.GameRecord

@Composable
fun HistoryEntriesCard(
    gameRecords: List<GameRecord>,
    fontSize: TextUnit,
    onClick: (GameRecord) -> Unit,
    isEnabled: Boolean = true,
    ascending: Boolean,
    showActiveScore: Boolean
) {
    val gameRecordsForHistory = if (ascending) gameRecords.asReversed() else gameRecords
    Card(
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (gameRecords.isEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "There are no games with a score",
                        Modifier.weight(1f),
                        fontSize = fontSize,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                gameRecordsForHistory.forEachIndexed { _, game ->
                    val limit = if (game.isModeEnabled) "%d0".format(game.modeLimit) else "%d:00".format(game.modeLimit)
                    val additionRange = game.additionRangeStart to game.additionRangeEnd
                    val additionRangeString = when {
                        additionRange.first <= 0 || additionRange.second <= 0 -> "-"
                        additionRange.first == additionRange.second -> additionRange.first.toString()
                        else -> "${additionRange.first}-${additionRange.second}"
                    }
                    val subtractionRange = game.subtractionRangeStart to game.subtractionRangeEnd
                    val subtractionRangeString = when {
                        subtractionRange.first <= 0 || subtractionRange.second <= 0 -> "-"
                        subtractionRange.first == subtractionRange.second -> subtractionRange.first.toString()
                        else -> "${subtractionRange.first}-${subtractionRange.second}"
                    }
                    val multiplicationRange = game.multiplicationRangeStart to game.multiplicationRangeEnd
                    val multiplicationRangeString = when {
                        multiplicationRange.first <= 0 || multiplicationRange.second <= 0 -> "-"
                        multiplicationRange.first == multiplicationRange.second -> multiplicationRange.first.toString()
                        else -> "${multiplicationRange.first}-${multiplicationRange.second}"
                    }
                    val divisionRange = game.divisionRangeStart to game.divisionRangeEnd
                    val divisionRangeString = when {
                        divisionRange.first <= 0 || divisionRange.second <= 0 -> "-"
                        divisionRange.first == divisionRange.second -> divisionRange.first.toString()
                        else -> "${divisionRange.first}-${divisionRange.second}"
                    }
                    val score = if (showActiveScore) game.activeScore else game.totalScore
                    val scoreString = "%.2f".format(score)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = isEnabled) { onClick(game) },
                    ) {
                        Text(limit, Modifier.weight(1f), fontSize = fontSize, textAlign = TextAlign.Center)
                        Text(
                            additionRangeString,
                            Modifier.weight(1f),
                            fontSize = fontSize,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            subtractionRangeString,
                            Modifier.weight(1f),
                            fontSize = fontSize,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            multiplicationRangeString,
                            Modifier.weight(1f),
                            fontSize = fontSize,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            divisionRangeString,
                            Modifier.weight(1f),
                            fontSize = fontSize,
                            textAlign = TextAlign.Center
                        )
                        Text(scoreString, Modifier.weight(1f), fontSize = fontSize, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}