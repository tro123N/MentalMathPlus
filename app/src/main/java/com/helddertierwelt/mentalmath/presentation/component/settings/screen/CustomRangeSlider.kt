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

package com.helddertierwelt.mentalmath.presentation.component.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangeSlider(
    valueRange: ClosedFloatingPointRange<Float>,
    value: Pair<Float, Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onValueChangeFinished: () -> Unit,
    isEnabled: Boolean,
    activeTrackColor: Color,
) {
    val dimens = MentalMathTheme.dimens
    key(isEnabled) {

        val startInteractionSource = remember { MutableInteractionSource() }
        val endInteractionSource = remember { MutableInteractionSource() }
        val isDraggingStart by startInteractionSource.collectIsDraggedAsState()
        val isDraggingEnd by endInteractionSource.collectIsDraggedAsState()
        var sliderWidth by remember { mutableIntStateOf(0) }
        val value = value.first..value.second

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
                            x = thumbOffset(value.start),
                            y = (-dimens.components.sliderHeight.roundToPx() / 1.25f).toInt()
                        )
                    }
                    .alpha(if (isDraggingStart) 1f else 0f)
                    .background(
                        activeTrackColor,
                        shape = RoundedCornerShape(dimens.components.cornerRadius)
                    )
                    .padding(horizontal = 6.dp),
                text = "${value.start.toInt()}",
                color = MaterialTheme.colorScheme.surface,
                fontSize = dimens.fonts.caption
            )

            Text(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = thumbOffset(value.endInclusive),
                            y = (-dimens.components.sliderHeight.roundToPx() / 1.25f).toInt()
                        )
                    }
                    .alpha(if (isDraggingEnd) 1f else 0f)
                    .background(
                        activeTrackColor,
                        shape = RoundedCornerShape(dimens.components.cornerRadius)
                    )
                    .padding(horizontal = 6.dp),
                text = "${value.endInclusive.toInt()}",
                color = MaterialTheme.colorScheme.surface,
                fontSize = dimens.fonts.caption
            )

            val sliderColors: SliderColors
            if (isEnabled) {
                sliderColors = SliderDefaults.colors(
                    thumbColor = activeTrackColor,
                    activeTrackColor = activeTrackColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surface,
                    activeTickColor = MaterialTheme.colorScheme.background,
                    inactiveTickColor = activeTrackColor
                )
            } else {
                sliderColors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onPrimary,
                    activeTrackColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surface,
                    activeTickColor = MaterialTheme.colorScheme.background,
                    inactiveTickColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            RangeSlider(
                value = value,
                valueRange = valueRange,
                steps = valueRange.endInclusive.toInt() - valueRange.start.toInt() - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.components.sliderHeight),
                colors = sliderColors,
                enabled = isEnabled,
                startInteractionSource = startInteractionSource,
                endInteractionSource = endInteractionSource,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished
            )
        }
    }
}