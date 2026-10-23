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

package com.helddertierwelt.mentalmath.presentation.component.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SurvivalDisplay(
    timeLeft: Long,
    totalAnswers: Int,
    lives: Int,
) {
    val dimens = MentalMathTheme.dimens
    var highlightTimer by remember { mutableStateOf(false) }

    LaunchedEffect(totalAnswers) {
        if (totalAnswers in 1..40 && totalAnswers % 5 == 0 ) {
            highlightTimer = true
            delay(1000.milliseconds)
            highlightTimer = false
        }
    }

    val isTimeRunningOut = timeLeft <= 3_000
    val timerColor by animateColorAsState(
        targetValue = when {
            isTimeRunningOut -> red
            highlightTimer -> yellow
            else -> MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(500),
        label = "timerColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(end = 24.dp)
    ) {

        Text(
            text = ceil(timeLeft / 1000.0).toLong().toString(),
            color = timerColor,
            fontSize = dimens.fonts.gameProgress,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(3) { index ->
                Icon(
                    painter = painterResource(
                        if (index >= 3 - lives)
                            R.drawable.heart_24dp
                        else
                            R.drawable.heart_outlined_24dp
                    ),
                    contentDescription = "Lives",
                    modifier = Modifier.size(dimens.icons.survivalHeart),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
