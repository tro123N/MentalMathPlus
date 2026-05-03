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

package com.helddertierwelt.mentalmath.presentation.component.settings.score

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch

@Composable
fun ScoreSideSheet(
    screenWidth: Dp,
    screenHeight: Dp,
    navController: NavController,
) {
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(viewModelStoreOwner)
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)

    val coroutineScope = rememberCoroutineScope()
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val sheetWidthFactor = 0.5f
    var isFirstRender by remember { mutableStateOf(true) }
    val minOffset = screenWidthPx * (1f - sheetWidthFactor)
    val maxOffset = screenWidthPx
    val offsetX = remember { Animatable(maxOffset) }
    val overlayAlpha = 0.38f * (1 - (offsetX.value - minOffset) / (maxOffset - minOffset))

    val fontSizeTitle = (0.031f * screenHeight.value).sp
    val fontSizeScore = (0.020f * screenHeight.value).sp

    val isSheetOpen = settingsViewModel.isScoreOpen.collectAsState().value
    var isInteractionEnabled by remember { mutableStateOf(true) }

    // Handle side sheet open/close animation
    LaunchedEffect(isSheetOpen) {
        val target = if (isSheetOpen) minOffset else maxOffset
        if (isFirstRender) {
            offsetX.snapTo(target)
            isFirstRender = false
        } else {
            isInteractionEnabled = false
            offsetX.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
        isInteractionEnabled = true
        settingsViewModel.setIsScoreClicked(false)
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
                                interactionSource = remember { MutableInteractionSource() }
                            ) { settingsViewModel.toggleScoreOpen() }
                        } else {
                            Modifier
                        }
                    )
            )
        }

        Card(
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxHeight()
                .width(screenWidth * sheetWidthFactor)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = (offsetX.value + delta).coerceIn(minOffset, maxOffset)
                        coroutineScope.launch {
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragStopped = {
                        coroutineScope.launch {
                            if (offsetX.value > minOffset + maxOffset * sheetWidthFactor * 0.2f) {
                                settingsViewModel.toggleScoreOpen()
                            } else {
                                offsetX.animateTo(minOffset)
                            }
                        }
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val bestGames by settingsViewModel.bestGames.collectAsState()
                val lastGameId = statisticsViewModel.lastGameRecord.collectAsState().value.id
                Spacer(modifier = Modifier.height(16.dp))
                if (!bestGames.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.top_ten_scores),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = fontSizeTitle,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Icon(
                            painter = painterResource(R.drawable.star_24dp),
                            contentDescription = "score",
                            modifier = Modifier.size(100.dp),
                            tint = yellow
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val isScoreClicked = settingsViewModel.isScoreClicked.collectAsState().value
                    ScoreList(
                        gameRecords = bestGames,
                        fontSizeScore = fontSizeScore,
                        lastGameId = lastGameId,
                        isEnabled = !isScoreClicked,
                        onClick = { gameRecord ->
                            settingsViewModel.setIsScoreClicked(true)
                            statisticsViewModel.setLoadedGameRecord(gameRecord)
                            statisticsViewModel.setShowLoadedGameRecord(true)
                            statisticsViewModel.setShowLinkGamePopUp(false)
                            navController.navigate("statistics") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}