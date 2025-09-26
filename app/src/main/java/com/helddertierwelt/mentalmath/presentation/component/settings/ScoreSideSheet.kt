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

package com.helddertierwelt.mentalmath.presentation.component.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@Composable
fun ScoreSideSheet(
    isSheetOpen: Boolean,
    screenWidth: Dp,
    onDismissRequested: () -> Unit,
    screenHeight: Dp,
) {
    val activity = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(activity)
    val coroutineScope = rememberCoroutineScope()

    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val sheetWidthFactor = 0.6f
    val offsetX = remember { Animatable(screenWidthPx) }
    val minOffset = screenWidthPx * (1f - sheetWidthFactor)
    val maxOffset = screenWidthPx
    val overlayAlpha = 0.4f * (1 - (offsetX.value - minOffset) / (maxOffset - minOffset))

    val fontSize = (0.018f * screenHeight.value).sp
    val iconSize = (0.032f * screenHeight.value).dp
    var isInteractionEnabled by remember { mutableStateOf(true) }

    // Handle side sheet open/close animation
    LaunchedEffect(isSheetOpen) {
        isInteractionEnabled = false
        val target = if (isSheetOpen) screenWidthPx * (1f - sheetWidthFactor) else screenWidthPx
        if (offsetX.value != target) {
            offsetX.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
        isInteractionEnabled = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Dark overlay
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha))
                    .then(
                        if (isInteractionEnabled) {
                            Modifier.clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { onDismissRequested() }
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // Side sheet
        Card(
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxHeight()
                .width(screenWidth * sheetWidthFactor)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (offsetX.value + dragAmount).coerceIn(
                                screenWidthPx * (1f - sheetWidthFactor),
                                screenWidthPx
                            )
                            coroutineScope.launch {
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            if (offsetX.value > screenWidthPx * (1f - sheetWidthFactor) + screenWidthPx * sheetWidthFactor * 0.2f) {
                                onDismissRequested()
                            } else {
                                coroutineScope.launch {
                                    offsetX.animateTo(screenWidthPx * (1f - sheetWidthFactor))
                                }
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {

            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {


            }
        }
    }
}