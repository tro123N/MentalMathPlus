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

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.red

@Composable
fun PulsatingCountdown(
    time: String,
    paddingEnd: Dp,
) {
    val dimens = MentalMathTheme.dimens
    val normalColor = MaterialTheme.colorScheme.onPrimary
    val darkRed = red.copy(
        red = red.red * 0.8f,
        green = red.green * 0.8f,
        blue = red.blue * 0.8f
    )
    val color = remember { Animatable(normalColor) }

    LaunchedEffect(Unit) {
        color.animateTo(
            targetValue = red,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )

        while (true) {
            color.animateTo(
                targetValue = darkRed,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                )
            )
            color.animateTo(
                targetValue = red,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                )
            )
        }
    }

    Text(
        text = time,
        color = color.value,
        fontSize = dimens.fonts.gameProgress,
        modifier = Modifier.padding(end = paddingEnd)
    )
}