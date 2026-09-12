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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.settings.enums.InputDirectionMode
import com.helddertierwelt.mentalmath.presentation.theme.red

@Composable
fun BackspaceButton(
    onClick: () -> Unit,
    onPress: () -> Unit,
    iconSize: Dp,
    size: Dp,
    inputDirectionMode: InputDirectionMode,
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                onPress()
            }
        }
    }

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        modifier = Modifier.size(size),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = red,
            contentColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Icon(
            painter = painterResource(
                when (inputDirectionMode) {
                    InputDirectionMode.LEFT_TO_RIGHT -> R.drawable.backspace_24dp
                    InputDirectionMode.RIGHT_TO_LEFT -> R.drawable.backspace_reverse_24dp
                }
            ),
            contentDescription = "Backspace",
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSecondary
        )
    }
}