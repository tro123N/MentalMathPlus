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

package com.helddertierwelt.mentalmath.presentation.component.settings.score

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch

@Composable
fun ScoreSideSheet(
    isSheetOpen: Boolean,
    onDismissRequested: () -> Unit,
    screenWidth: Dp,
    screenHeight: Dp,
    navController: NavController,
) {
    val activity = LocalActivity.current as ViewModelStoreOwner
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(activity)
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)

    val coroutineScope = rememberCoroutineScope()
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val sheetWidthFactor = 0.5f
    var isFirstRender by remember { mutableStateOf(true) }
    val offsetX = remember { Animatable(screenWidthPx) }
    val minOffset = screenWidthPx * (1f - sheetWidthFactor)
    val maxOffset = screenWidthPx
    val overlayAlpha = 0.4f * (1 - (offsetX.value - minOffset) / (maxOffset - minOffset))

    val fontSizeTitle = (0.031f * screenHeight.value).sp
    val fontSizeScore = (0.020f * screenHeight.value).sp
    var isInteractionEnabled by remember { mutableStateOf(true) }

    // Handle side sheet open/close animation
    LaunchedEffect(isSheetOpen) {
        val target = if (isSheetOpen) screenWidthPx * (1f - sheetWidthFactor) else screenWidthPx
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
                            ) { onDismissRequested() }
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

            // load game records based on current settings
            val settingsState = settingsViewModel.settingsState.value
            LaunchedEffect(settingsState) {
                val settingsState = settingsViewModel.settingsState.value
                val additionRangeStart =
                    if (settingsState.isAdditionEnabled) settingsState.additionRange.first.toInt() else 0
                val additionRangeEnd =
                    if (settingsState.isAdditionEnabled) settingsState.additionRange.second.toInt() else 0
                val subtractionRangeStart =
                    if (settingsState.isSubtractionEnabled) settingsState.subtractionRange.first.toInt() else 0
                val subtractionRangeEnd =
                    if (settingsState.isSubtractionEnabled) settingsState.subtractionRange.second.toInt() else 0
                val multiplicationRangeStart =
                    if (settingsState.isMultiplicationEnabled) settingsState.multiplicationRange.first.toInt() else 0
                val multiplicationRangeEnd =
                    if (settingsState.isMultiplicationEnabled) settingsState.multiplicationRange.second.toInt() else 0
                val divisionRangeStart =
                    if (settingsState.isDivisionEnabled) settingsState.divisionRange.first.toInt() else 0
                val divisionRangeEnd =
                    if (settingsState.isDivisionEnabled) settingsState.divisionRange.second.toInt() else 0
                statisticsViewModel.loadBestActiveGames(
                    settingsState.isModeEnabled,
                    settingsState.limit.toInt(),
                    additionRangeStart,
                    additionRangeEnd,
                    subtractionRangeStart,
                    subtractionRangeEnd,
                    multiplicationRangeStart,
                    multiplicationRangeEnd,
                    divisionRangeStart,
                    divisionRangeEnd,
                )
                statisticsViewModel.loadBestTotalGames(
                    settingsState.isModeEnabled,
                    settingsState.limit.toInt(),
                    additionRangeStart,
                    additionRangeEnd,
                    subtractionRangeStart,
                    subtractionRangeEnd,
                    multiplicationRangeStart,
                    multiplicationRangeEnd,
                    divisionRangeStart,
                    divisionRangeEnd,
                )
            }
            val bestActiveGames by statisticsViewModel.bestActiveGames.collectAsState()
            val bestTotalGames by statisticsViewModel.bestTotalGames.collectAsState()
            val lastGameId = statisticsViewModel.lastGameRecord.collectAsState().value.id


            if (bestActiveGames.isEmpty() && bestTotalGames.isEmpty()) {
                settingsViewModel.disableScoreSheet()
            } else {
                settingsViewModel.enableScoreSheet()
                @Composable
                fun onScoreClick(): (GameRecord) -> Unit = { gameRecord ->
                    statisticsViewModel.setLoadedGameRecord(gameRecord)
                    statisticsViewModel.setShowLoadedGameRecord(true)
                    navController.navigate("statistics") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp, 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { settingsViewModel.toggleDefaultScoreOption() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (settingsState.isActiveScoreDefault) "Active\nScore" else "Total  \nScore",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = fontSizeTitle,
                        textAlign = TextAlign.Start,
                    )
                    Icon(
                        painter = painterResource(R.drawable.star_24dp),
                        contentDescription = "activeScore",
                        modifier = Modifier.size(128.dp),
                        tint = if (settingsState.isActiveScoreDefault) yellow else green
                    )
                }
                if (settingsState.isActiveScoreDefault) {
                    ScoreList(
                        gameRecords = bestActiveGames,
                        fontSizeScore = fontSizeScore,
                        lastGameId = lastGameId,
                        scoreSelector = { gameRecord -> gameRecord.activeScore },
                        onClick = onScoreClick()
                    )
                } else {
                    ScoreList(
                        gameRecords = bestTotalGames,
                        fontSizeScore = fontSizeScore,
                        lastGameId = lastGameId,
                        scoreSelector = { gameRecord -> gameRecord.totalScore },
                        onClick = onScoreClick()
                    )
                }
            }
        }
    }
}