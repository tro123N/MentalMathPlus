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

package com.helddertierwelt.mentalmath.presentation.component.result.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressColumn(
    gameMode: GameMode,
    totalAnswers: Int,
    activeTime: Long,
) {
    val dimens = MentalMathTheme.dimens
    val tooltipText = when (gameMode) {
        GameMode.TIMED -> stringResource(R.string.time_spent)
        GameMode.PROBLEMS -> stringResource(R.string.problems_completed)
        GameMode.SURVIVAL -> stringResource(R.string.problems_completed)
    }
    val tooltipState = rememberTooltipState()
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        TooltipBox(
            state = tooltipState,
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above, 8.dp),
            tooltip = {
                PlainTooltip(
                    shape = RoundedCornerShape(dimens.components.cornerRadius),
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Text(tooltipText)
                }
            }
        ) {
            Icon(
                painter = when (gameMode) {
                    GameMode.TIMED -> painterResource(R.drawable.tag_24dp)
                    GameMode.PROBLEMS -> painterResource(R.drawable.timer_24dp)
                    GameMode.SURVIVAL -> painterResource(R.drawable.tag_24dp)
                },
                contentDescription = tooltipText,
                modifier = Modifier.size(dimens.icons.summaryStat),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (gameMode) {
                GameMode.TIMED -> "$totalAnswers"
                GameMode.PROBLEMS -> formatTimeWithDecimalSeconds(activeTime, stringResource(R.string.second))
                GameMode.SURVIVAL -> "$totalAnswers"
            },
            fontSize = dimens.fonts.resultText,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
