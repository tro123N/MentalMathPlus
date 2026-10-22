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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.domain.settings.enums.FormMode
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.domain.settings.enums.InputDirectionMode
import com.helddertierwelt.mentalmath.presentation.component.game.BackspaceButton
import com.helddertierwelt.mentalmath.presentation.component.game.EnterButton
import com.helddertierwelt.mentalmath.presentation.component.game.GameTopBar
import com.helddertierwelt.mentalmath.presentation.component.game.NumberButton
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.component.game.ProblemInput
import com.helddertierwelt.mentalmath.presentation.component.game.QuickToastHost
import com.helddertierwelt.mentalmath.presentation.component.game.ToastEvent
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.ResultViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel

@Composable
fun GameScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val dimens = MentalMathTheme.dimens

    // viewModels
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(viewModelStoreOwner)
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val resultViewModel: ResultViewModel = hiltViewModel(viewModelStoreOwner)

    // variables
    val settingsState = settingsViewModel.settingsState.collectAsState().value
    val upperNumberRow = settingsState.inputMode.upperNumberRow
    val lowerNumberRow = settingsState.inputMode.lowerNumberRow
    val gameState = gameViewModel.gameState.collectAsState().value
    val inputDirectionMode = gameState.inputDirectionMode
    val formMode = gameState.formMode
    val disableEnterButton by gameViewModel.disableEnterButton.collectAsState()
    var toastEvent by remember { mutableStateOf<ToastEvent?>(null) }
    val rtlMessage = stringResource(R.string.rtl_toast)
    val ltrMessage = stringResource(R.string.ltr_toast)
    val horizontalLayoutMessage = stringResource(R.string.horizontal_toast)
    val verticalLayoutMessage = stringResource(R.string.vertical_toast)


    LaunchedEffect(Unit) {
        gameViewModel.startTimerIfNeeded()
    }

    LaunchedEffect(Unit) {
        gameViewModel.gameEvents.collect { event ->
            when (event) {
                GameViewModel.GameEvent.GameFinished -> {

                    val currentGameState = gameViewModel.gameState.value

                    resultViewModel.setShowLoadedGameRecord(false)
                    resultViewModel.setShowLinkGamePopUp(currentGameState.isLinkGame)
                    resultViewModel.setShowSurvivalCompletedPopUp(
                        currentGameState.gameMode == GameMode.SURVIVAL && currentGameState.totalAnswers >= 100
                    )

                    navController.navigate("result") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                GameTopBar(
                    navController = navController,
                )
                LinearProgressIndicator(
                    progress = {
                        when (gameState.gameMode) {
                            GameMode.PROBLEMS -> gameState.totalAnswers / (gameState.modeLimit * 10f)
                            GameMode.TIMED -> gameState.activeTime / (gameState.modeLimit.toFloat() * 60000L)
                            GameMode.SURVIVAL -> gameViewModel.getSurvivalProgress()
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
                        .padding(dimens.components.gameScreenPadding, dimens.components.gameScreenPadding),
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
                                    problemStatus = gameState.problemStatus,
                                    inputDirectionMode = inputDirectionMode,
                                    formMode = formMode,
                                    onInputClick = {
                                        val inputDirectionModeUpdated = gameViewModel.toggleInputDirection()
                                        val message = when (inputDirectionModeUpdated) {
                                            InputDirectionMode.RIGHT_TO_LEFT -> rtlMessage
                                            InputDirectionMode.LEFT_TO_RIGHT -> ltrMessage
                                        }
                                        toastEvent = ToastEvent(message)
                                    },
                                    onProblemClick = {
                                        val formModeUpdated = gameViewModel.toggleFormMode()
                                        val message = when (formModeUpdated) {
                                            FormMode.VERTICAL -> verticalLayoutMessage
                                            FormMode.HORIZONTAL -> horizontalLayoutMessage
                                        }
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
                                    onPress = { gameViewModel.playFeedback(FeedbackType.NUMPAD) },
                                    onClick = {
                                        when (inputDirectionMode) {
                                            InputDirectionMode.LEFT_TO_RIGHT -> { gameViewModel.addLastDigit(number) }
                                            InputDirectionMode.RIGHT_TO_LEFT -> { gameViewModel.addFirstDigit(number) }
                                        }
                                    },
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
                                    onPress = { gameViewModel.playFeedback(FeedbackType.NUMPAD) },
                                    onClick = {
                                        when (inputDirectionMode) {
                                            InputDirectionMode.LEFT_TO_RIGHT -> { gameViewModel.addLastDigit(number) }
                                            InputDirectionMode.RIGHT_TO_LEFT -> { gameViewModel.addFirstDigit(number) }
                                        }
                                    },
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
                                    onPress = { gameViewModel.playFeedback(FeedbackType.NUMPAD) },
                                    onClick = {
                                        when (inputDirectionMode) {
                                            InputDirectionMode.LEFT_TO_RIGHT -> { gameViewModel.addLastDigit(number) }
                                            InputDirectionMode.RIGHT_TO_LEFT -> { gameViewModel.addFirstDigit(number) }
                                        }
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BackspaceButton(
                            onPress = { gameViewModel.playFeedback(FeedbackType.BACKSPACE) },
                            onClick = {
                                when (inputDirectionMode) {
                                    InputDirectionMode.LEFT_TO_RIGHT -> { gameViewModel.removeLastDigit() }
                                    InputDirectionMode.RIGHT_TO_LEFT -> { gameViewModel.removeFirstDigit() }
                                }
                            },
                            inputDirectionMode = inputDirectionMode,
                        )
                        NumberButton(
                            number = 0,
                            onPress = { gameViewModel.playFeedback(FeedbackType.NUMPAD) },
                            onClick = {
                                when (inputDirectionMode) {
                                    InputDirectionMode.LEFT_TO_RIGHT -> { gameViewModel.addLastDigit(0) }
                                    InputDirectionMode.RIGHT_TO_LEFT -> { gameViewModel.addFirstDigit(0) }
                                }
                            },
                        )
                        EnterButton(
                            onClick = { gameViewModel.submitAnswer() },
                            enabled = !disableEnterButton,
                            isToggled = gameState.input.isEmpty(),
                            iconId1 = R.drawable.keyboard_double_arrow_right_24dp,
                            iconId2 = R.drawable.keyboard_tab_24dp,
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
            onCancel = {
                gameViewModel.playFeedback(FeedbackType.TAP)
                showDialog = false
            },
            title = stringResource(R.string.quit_game_popup_title),
            description = stringResource(R.string.quit_game_popup_description),
            confirmationText = stringResource(R.string.quit_game_popup_confirmation_text),
            cancelText = stringResource(R.string.quit_game_popup_cancel_text),
            dismissColor = green,
            confirmColor = red
        )
    }
}
