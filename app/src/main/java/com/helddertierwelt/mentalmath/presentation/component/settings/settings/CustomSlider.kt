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

package com.helddertierwelt.mentalmath.presentation.component.settings.settings

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun CustomSlider(
    valueRange: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    isToggled: Boolean,
    activeTrackColor: Color,
    size: Dp,
    thumbnailFontSize: TextUnit,
) {
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

        val positionFraction =
            (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        val shorterSliderWidth = sliderWidth / 1.1
        val offsetX = (shorterSliderWidth * positionFraction - shorterSliderWidth / 2).toInt()
        Text(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = offsetX, y = (-size.roundToPx() / 1.25f).toInt()
                    )
                }
                .alpha(if (isDragging) 1f else 0f)
                .background(
                    activeTrackColor, shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 6.dp),
            text = if (isToggled) "${(value * 10).roundToInt()}".format(value) else "${value.toInt()}min".format(value),
            color = MaterialTheme.colorScheme.onSecondary,
            fontSize = thumbnailFontSize
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
                .height(size),
            colors = sliderColors,
            interactionSource = interactionSource,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}
