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

package com.helddertierwelt.mentalmath.presentation.component.settings.menu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.helddertierwelt.mentalmath.domain.settings.ToggleOption

@Composable
fun <T> CycleToggleRow(
    fontSize: TextUnit,
    iconSize: Dp,
    currentValue: T,
    options: List<ToggleOption<T>>,
    color: Color = MaterialTheme.colorScheme.onPrimary,
    onValueChange: (T) -> Unit
) {
    require(options.isNotEmpty())

    val currentIndex =
        options.indexOfFirst { it.value == currentValue }
            .takeIf { it >= 0 }
            ?: 0

    val currentOption = options[currentIndex]

    SettingsRow(
        fontSize = fontSize,
        iconSize = iconSize,
        iconRes = currentOption.iconRes,
        text = stringResource(currentOption.textRes),
        color = color,
        onClick = {
            val nextIndex = (currentIndex + 1) % options.size
            onValueChange(options[nextIndex].value)
        }
    )
}