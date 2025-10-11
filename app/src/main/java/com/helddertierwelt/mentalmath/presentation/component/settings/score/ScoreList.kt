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

package com.helddertierwelt.mentalmath.presentation.component.settings.score

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.presentation.theme.green

@Composable
fun ScoreList(
    gameRecords: List<GameRecord>,
    fontSizeScore: TextUnit,
    lastGameId: Int,
    scoreSelector: (GameRecord) -> Float,
    onClick: (GameRecord) -> Unit,
) {

    Column(
        modifier = Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        gameRecords.forEachIndexed { index, game ->
            var color = MaterialTheme.colorScheme.onPrimary
            var indexValue = "%d".format(index + 1)
            if (game.id == lastGameId) {
                color = green
                indexValue = "new"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClick(game) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = indexValue,
                    color = color,
                    fontSize = fontSizeScore,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.33f),
                    fontWeight = Bold
                )
                Text(
                    text = "%.2f".format(scoreSelector(game)),
                    color = color,
                    fontSize = fontSizeScore,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.67f)
                )
            }
        }
    }
}