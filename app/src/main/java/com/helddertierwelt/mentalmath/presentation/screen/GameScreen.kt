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

package com.helddertierwelt.mentalmath.presentation.screen

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.component.game.BackspaceButton
import com.helddertierwelt.mentalmath.presentation.component.game.EnterButton
import com.helddertierwelt.mentalmath.presentation.component.game.GameTopBar
import com.helddertierwelt.mentalmath.presentation.component.game.NumberButton
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.component.game.ProblemInput
import com.helddertierwelt.mentalmath.presentation.component.game.QuickToastHost
import com.helddertierwelt.mentalmath.presentation.component.game.ToastEvent
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val screenHeight =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

    // font and icon sizes
    val roundButtonSize = (0.10f * screenHeight.value).dp
    val resultLineWidth = (0.32f * screenHeight.value).dp
    val columnPadding = (0.028f * screenHeight.value).dp
    val closeIconSize = (0.057f * screenHeight.value).dp
    val doubleArrowIconSize = (0.052f * screenHeight.value).dp
    val backSpaceIconSize = (0.038f * screenHeight.value).dp
    val titleFontSize = (0.031f * screenHeight.value).sp
    val popUpDescriptionFontSize = (0.018f * screenHeight.value).sp
    val numberFontSize = (0.040f * screenHeight.value).sp
    val problemFontSize = (0.057f * screenHeight.value).sp

    // viewModels
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(viewModelStoreOwner)
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(viewModelStoreOwner)

    // variables
    val settingsState = settingsViewModel.settingsState.collectAsState().value
    val isRightToLeft = settingsState.isRightToLeft
    val upperNumberRow = if (settingsState.isNumpadDefault) 7..9 else 1..3
    val lowerNumberRow = if (settingsState.isNumpadDefault) 1..3 else 7..9
    val gameState = gameViewModel.gameState.collectAsState().value
    val isGeneratingProblem by gameViewModel.isGeneratingProblem.collectAsState()
    var toastEvent by remember { mutableStateOf<ToastEvent?>(null) }
    val rtlMessage = stringResource(R.string.rtl_toast)
    val ltrMessage = stringResource(R.string.ltr_toast)


    LaunchedEffect(Unit) {
        gameViewModel.startTimerIfNeeded()
    }

    Scaffold(
        topBar = {
            Column {
                GameTopBar(
                    navController = navController,
                    iconSize = closeIconSize,
                    fontSize = numberFontSize
                )
                LinearProgressIndicator(
                    progress = {
                        if (settingsState.isModeEnabled) {
                            gameState.totalAnswers / (settingsState.limit * 10)
                        } else {
                            gameState.activeTime / (settingsState.limit * 60000L)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = blue,
                    trackColor = MaterialTheme.colorScheme.background,
                    drawStopIndicator = {},
                    strokeCap = StrokeCap.Butt
                )
            }
        },
        content = { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(columnPadding, columnPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.matchParentSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                    QuickToastHost(
                                        event = toastEvent,
                                        durationMs = 1000
                                    )
                            }
                            Box(
                                modifier = Modifier.matchParentSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ProblemInput(
                                    operand1 = gameState.operand1,
                                    operand2 = gameState.operand2,
                                    operator = gameState.operator.symbol,
                                    input = gameState.input,
                                    fontSize = problemFontSize,
                                    width = resultLineWidth,
                                    isCorrect = gameState.isCorrect,
                                    isRightToLeft = isRightToLeft,
                                    onClick = {
                                        val isRightToLeftUpdated = settingsViewModel.toggleInputMethod()
                                        val message = if (isRightToLeftUpdated) rtlMessage else ltrMessage
                                        toastEvent = ToastEvent(message)
                                    }
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            upperNumberRow.forEach { number ->
                                NumberButton(
                                    number = number,
                                    onClick = {
                                        if (isRightToLeft) {
                                            gameViewModel.addFirstDigit(number)
                                        } else {
                                            gameViewModel.addLastDigit(number)
                                        }
                                    },
                                    size = roundButtonSize,
                                    fontSize = numberFontSize
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (4..6).forEach { number ->
                                NumberButton(
                                    number = number,
                                    onClick = {
                                        if (isRightToLeft) {
                                            gameViewModel.addFirstDigit(number)
                                        } else {
                                            gameViewModel.addLastDigit(number)
                                        }
                                    },
                                    size = roundButtonSize,
                                    fontSize = numberFontSize
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (lowerNumberRow).forEach { number ->
                                NumberButton(
                                    number = number,
                                    onClick = {
                                        if (isRightToLeft) {
                                            gameViewModel.addFirstDigit(number)
                                        } else {
                                            gameViewModel.addLastDigit(number)
                                        }
                                    },
                                    size = roundButtonSize,
                                    fontSize = numberFontSize
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BackspaceButton(
                            onClick = {
                                if (isRightToLeft) {
                                    gameViewModel.removeFirstDigit()
                                } else {
                                    gameViewModel.removeLastDigit()
                                }
                            },
                            isRightToLeft = isRightToLeft,
                            size = roundButtonSize,
                            iconSize = backSpaceIconSize
                        )
                        NumberButton(
                            number = 0,
                            onClick = {
                                if (isRightToLeft) {
                                    gameViewModel.addFirstDigit(0)
                                } else {
                                    gameViewModel.addLastDigit(0)
                                }
                            },
                            size = roundButtonSize,
                            fontSize = numberFontSize
                        )
                        EnterButton(
                            onClick = {
                                gameViewModel.addProblemToList()
                                gameViewModel.checkAnswerAndCount()
                                val totalAnswers = gameViewModel.gameState.value.totalAnswers
                                val totalProblems = (settingsState.limit * 10).roundToInt()
                                val isLinkGame = gameViewModel.gameState.value.isLinkGame
                                if (settingsState.isModeEnabled && totalAnswers == totalProblems) {
                                    gameViewModel.endGame()
                                    if (isLinkGame) {
                                        statisticsViewModel.setShowLinkGamePopUp(true)
                                    }
                                    statisticsViewModel.setShowLoadedGameRecord(false)
                                    navController.navigate("statistics") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    gameViewModel.generateNewProblem()
                                }
                            },
                            enabled = !isGeneratingProblem,
                            isToggled = gameState.input.isEmpty(),
                            size = roundButtonSize,
                            iconId1 = R.drawable.keyboard_double_arrow_right_24dp,
                            iconId2 = R.drawable.keyboard_tab_24dp,
                            iconSize = doubleArrowIconSize
                        )
                    }
                }
            }
        }
    )

    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var showDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showDialog = true
            }
        }
        backPressedDispatcher?.addCallback(callback)

        onDispose {
            callback.remove()
        }
    }

    if (showDialog) {
        PopUp(
            onDismissRequest = { showDialog = false },
            onConfirm = {
                showDialog = false
                gameViewModel.cancelGame()
                navController.navigate("settings") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onCancel = { showDialog = false },
            titleSize = titleFontSize,
            descriptionSize = popUpDescriptionFontSize,
            title = stringResource(R.string.quit_game_popup_title),
            description = stringResource(R.string.quit_game_popup_description),
            confirmationText = stringResource(R.string.quit_game_popup_confirmation_text),
            cancelText = stringResource(R.string.quit_game_popup_cancel_text),
            dismissColor = green,
            confirmColor = red
        )
    }
}
