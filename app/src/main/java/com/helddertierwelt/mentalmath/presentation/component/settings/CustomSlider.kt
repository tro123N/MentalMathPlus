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

package com.helddertierwelt.mentalmath.presentation.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme

@Composable
fun CustomSlider(
    valueRange: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    gameMode: GameMode,
    activeTrackColor: Color,
) {
    val dimens = MentalMathTheme.dimens
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()
    var sliderWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                sliderWidth = it.size.width
            },
        contentAlignment = Alignment.Center
    ) {

        fun thumbOffset(thumbValue: Float): Int {
            val positionFraction =
                (thumbValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val shorterSliderWidth = sliderWidth / 1.05
            return (shorterSliderWidth * positionFraction - shorterSliderWidth / 2).toInt()
        }

        Text(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = thumbOffset(value),
                        y = (-dimens.components.sliderHeight.roundToPx() / 1.25f).toInt()
                    )
                }
                .alpha(if (isDragging) 1f else 0f)
                .background(
                    activeTrackColor, shape = RoundedCornerShape(dimens.components.cornerRadius)
                )
                .padding(horizontal = 6.dp),
            text = when (gameMode) {
                GameMode.TIMED -> "${value.toInt()}${stringResource(R.string.minute)}"
                GameMode.PROBLEMS -> "${value.toInt() * 10}"
                GameMode.SURVIVAL -> "${value.toInt() * 20}${stringResource(R.string.second)} / ${stringResource(R.string.problem)}"
            },
            color = MaterialTheme.colorScheme.surface,
            fontSize = dimens.fonts.caption
        )

        val sliderColors = SliderDefaults.colors(
            thumbColor = activeTrackColor,
            activeTrackColor = activeTrackColor,
            inactiveTrackColor = MaterialTheme.colorScheme.surface,
            activeTickColor = MaterialTheme.colorScheme.background,
            inactiveTickColor = activeTrackColor
        )

        Slider(
            onValueChange = onValueChange,
            value = value,
            valueRange = valueRange,
            steps = valueRange.endInclusive.toInt() - valueRange.start.toInt() - 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.components.sliderHeight),
            colors = sliderColors,
            interactionSource = interactionSource,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}
