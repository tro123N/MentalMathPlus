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
import java.util.Locale

@Composable
fun ScoreList(
    gameRecords: List<GameRecord>,
    fontSizeScore: TextUnit,
    lastGameId: Int,
    scoreSelector: (GameRecord) -> Float,
    onClick: (GameRecord) -> Unit,
    ) {

    Column(
        modifier = Modifier.padding(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        gameRecords.forEachIndexed { index, game ->
            var color = MaterialTheme.colorScheme.onPrimary
            if (game.id == lastGameId) {
                color = green
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
                    text = "${index + 1}",
                    color = color,
                    fontSize = fontSizeScore,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.2f),
                    fontWeight = Bold
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.2f", scoreSelector(game)),
                    color = color,
                    fontSize = fontSizeScore,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.8f)
                )
            }
        }
    }
}