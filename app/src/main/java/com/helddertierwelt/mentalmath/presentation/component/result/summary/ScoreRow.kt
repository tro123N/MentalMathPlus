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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreRow(
    score: String,
    rank: Int?,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.Unspecified,
    tooltipText: String = "Score"
) {
    val dimens = MentalMathTheme.dimens
    val tooltipState = rememberTooltipState()
    val isTop10 = rank != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding( top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                painter = painterResource(R.drawable.star_24dp),
                contentDescription = tooltipText,
                modifier = Modifier.size(dimens.icons.summaryScore),
                tint = iconColor
            )
        }
        Text(
            text = score,
            fontSize = dimens.fonts.summaryScore,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            color = MaterialTheme.colorScheme.onPrimary
        )
        if (isTop10) {
            Text(
                text = "#$rank",
                fontSize = dimens.fonts.summaryScore * 0.7f,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}