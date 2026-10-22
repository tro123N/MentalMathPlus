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

package com.helddertierwelt.mentalmath.presentation.component.settings.score

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.data.entity.GameScore
import com.helddertierwelt.mentalmath.presentation.component.result.summary.formatScore
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.green

@Composable
fun ScoreList(
    gameScores: List<GameScore>,
    lastGameId: Int,
    onClick: (GameScore) -> Unit,
    isEnabled: Boolean = true
) {
    val dimens = MentalMathTheme.dimens

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.End
    ) {
        gameScores.forEachIndexed { index, gameScore ->
            var color = MaterialTheme.colorScheme.onPrimary
            if (gameScore.id == lastGameId) {
                color = green
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, top= 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = isEnabled) { onClick(gameScore) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "${index + 1}",
                    color = color,
                    fontSize = dimens.fonts.sideSheetEntry,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.3f),
                    fontWeight = Bold
                )
                Text(
                    text = formatScore(gameScore.activeScore),
                    color = color,
                    fontSize = dimens.fonts.sideSheetEntry,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .weight(0.7f)
                )
            }
        }
    }
}