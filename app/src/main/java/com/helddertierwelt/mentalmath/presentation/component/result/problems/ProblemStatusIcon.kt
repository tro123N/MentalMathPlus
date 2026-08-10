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

package com.helddertierwelt.mentalmath.presentation.component.result.problems

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.game.ProblemStatus
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemStatusIcon(
    status: ProblemStatus,
    iconSize: Dp,
) {
    val tooltipState = rememberTooltipState()

    val (icon, contentDescription, tooltip, tint) = when (status) {
        ProblemStatus.CORRECT -> StatusIcon(
            icon = R.drawable.check_circle_24dp,
            contentDescription = "correct",
            tooltip = stringResource(R.string.status_correct),
            tint = green
        )

        ProblemStatus.INCORRECT -> StatusIcon(
            icon = R.drawable.cancel_24dp,
            contentDescription = "incorrect",
            tooltip = stringResource(R.string.status_incorrect),
            tint = red
        )

        ProblemStatus.SKIPPED -> StatusIcon(
            icon = R.drawable.skipped_24dp,
            contentDescription = "skipped",
            tooltip = stringResource(R.string.status_skipped),
            tint = yellow
        )
    }

    TooltipBox(
        state = tooltipState,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
            8.dp
        ),
        tooltip = {
            PlainTooltip(
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Text(tooltip)
            }
        }
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .then(
                    if (status == ProblemStatus.SKIPPED) {
                        Modifier.scale(
                            scaleX = if (
                                LocalLayoutDirection.current == LayoutDirection.Rtl
                            ) -1f else 1f,
                            scaleY = 1f
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}

private data class StatusIcon(
    val icon: Int,
    val contentDescription: String,
    val tooltip: String,
    val tint: Color
)
