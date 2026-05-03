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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun QuickToastHost(
    event: ToastEvent?,
    durationMs: Long = 400,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(event?.id) {
        if (event != null) {
            visible = true
            delay(durationMs)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && event != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = fadeOut(animationSpec = tween(durationMillis = 400))
    ) {
        Text(
            text = event?.message.orEmpty(),
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.onPrimary,
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}

data class ToastEvent(
    val message: String,
    val id: Long = System.currentTimeMillis()
)