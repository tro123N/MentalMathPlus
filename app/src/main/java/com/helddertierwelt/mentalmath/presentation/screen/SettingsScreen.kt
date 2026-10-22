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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.data.notification.NotificationScheduler
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.component.settings.menu.MenuSideSheet
import com.helddertierwelt.mentalmath.presentation.component.settings.score.ScoreSideSheet
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.CustomRangeSlider
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.CustomSlider
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.ModeButton
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.OperatorButton
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.SettingsTopBar
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.StartButton
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.HistoryViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.MainActivityViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.ResultViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import java.security.SecureRandom

@Composable
fun SettingsScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val dimens = MentalMathTheme.dimens

    // viewModels
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(viewModelStoreOwner)
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val resultViewModel: ResultViewModel = hiltViewModel(viewModelStoreOwner)
    val historyViewModel: HistoryViewModel = hiltViewModel(viewModelStoreOwner)
    val mainActivityViewModel: MainActivityViewModel = hiltViewModel(viewModelStoreOwner)

    // variables
    val settingsState = settingsViewModel.settingsState.collectAsState().value
    val isLoaded = settingsViewModel.isLoaded.collectAsState().value
    val hasGamesWithScore = settingsViewModel.hasGamesWithScore.collectAsState().value
    val isSliderActive = settingsViewModel.isSliderActive.collectAsState().value
    val isStartButtonClicked = settingsViewModel.isStartButtonClicked.collectAsState().value

    fun openNotificationSettings(context: Context) {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }

        context.startActivity(intent)
    }

    LaunchedEffect(isLoaded) {
        if (isLoaded) {
            settingsViewModel.checkForGamesWithScore()
            settingsViewModel.setIsStartButtonClicked(false)
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                onMenuClick = {
                    settingsViewModel.playFeedback(FeedbackType.TAP)
                    settingsViewModel.toggleMenuOpen()
                },
                onScoreClick = {
                    settingsViewModel.playFeedback(FeedbackType.TAP)
                    settingsViewModel.loadBestGameScores()
                    settingsViewModel.toggleScoreOpen()
                },
                scoreEnabled = hasGamesWithScore && !isSliderActive,
                isGreyedOut = !hasGamesWithScore
            )
        },
        content = { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.mode_instruction),
                        fontSize = dimens.fonts.settingsLabel,
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeight = dimens.fonts.settingsLabel
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ModeButton(
                            gameMode = settingsState.gameMode,
                            onClick = { nextGameMode ->
                                settingsViewModel.setGameMode(nextGameMode)
                                settingsViewModel.checkForGamesWithScore()
                            },
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomSlider(
                            valueRange = 1f..5f,
                            value = settingsState.limit,
                            onValueChange = {
                                settingsViewModel.setIsSliderActive(true)
                                settingsViewModel.updateLimit(it)
                            },
                            onValueChangeFinished = {
                                settingsViewModel.setIsSliderActive(false)
                                settingsViewModel.checkForGamesWithScore()
                            },
                            gameMode = settingsState.gameMode,
                            activeTrackColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.operator_instruction),
                        fontSize = dimens.fonts.settingsLabel,
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeight = dimens.fonts.settingsLabel
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OperatorButton(
                            icon = R.drawable.addition_24dp,
                            textColor = green,
                            isSelected = settingsState.isAdditionEnabled,
                            onClick = {
                                settingsViewModel.toggleAddition()
                                settingsViewModel.checkForGamesWithScore()
                            },
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = 1f..9f,
                            value = settingsState.additionRange,
                            onValueChange = {
                                settingsViewModel.setIsSliderActive(true)
                                settingsViewModel.updatePlusRange(it)
                            },
                            onValueChangeFinished = {
                                settingsViewModel.setIsSliderActive(false)
                                settingsViewModel.checkForGamesWithScore()
                            },
                            isEnabled = settingsState.isAdditionEnabled,
                            activeTrackColor = green,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OperatorButton(
                            icon = R.drawable.subtraction_24dp,
                            textColor = red,
                            isSelected = settingsState.isSubtractionEnabled,
                            onClick = {
                                settingsViewModel.toggleSubtraction()
                                settingsViewModel.checkForGamesWithScore()
                            },
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = 1f..9f,
                            value = settingsState.subtractionRange,
                            onValueChange = {
                                settingsViewModel.setIsSliderActive(true)
                                settingsViewModel.updateMinusRange(it)
                            },
                            onValueChangeFinished = {
                                settingsViewModel.setIsSliderActive(false)
                                settingsViewModel.checkForGamesWithScore()
                            },
                            isEnabled = settingsState.isSubtractionEnabled,
                            activeTrackColor = red,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OperatorButton(
                            icon = R.drawable.multiplication_24dp,
                            textColor = yellow,
                            isSelected = settingsState.isMultiplicationEnabled,
                            onClick = {
                                settingsViewModel.toggleMultiplication()
                                settingsViewModel.checkForGamesWithScore()
                            },
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = 1f..9f,
                            value = settingsState.multiplicationRange,
                            onValueChange = {
                                settingsViewModel.setIsSliderActive(true)
                                settingsViewModel.updateMultiplyRange(it)
                            },
                            onValueChangeFinished = {
                                settingsViewModel.setIsSliderActive(false)
                                settingsViewModel.checkForGamesWithScore()
                            },
                            isEnabled = settingsState.isMultiplicationEnabled,
                            activeTrackColor = yellow,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OperatorButton(
                            icon = R.drawable.division_24dp,
                            textColor = blue,
                            isSelected = settingsState.isDivisionEnabled,
                            onClick = {
                                settingsViewModel.toggleDivide()
                                settingsViewModel.checkForGamesWithScore()
                            },
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = 1f..9f,
                            value = settingsState.divisionRange,
                            onValueChange = {
                                settingsViewModel.setIsSliderActive(true)
                                settingsViewModel.updateDivideRange(it)
                            },
                            onValueChangeFinished = {
                                settingsViewModel.setIsSliderActive(false)
                                settingsViewModel.checkForGamesWithScore()
                            },
                            isEnabled = settingsState.isDivisionEnabled,
                            activeTrackColor = blue,
                        )
                    }
                    StartButton(
                        onClick = {
                            settingsViewModel.playFeedback(FeedbackType.START)
                            settingsViewModel.setIsStartButtonClicked(true)
                            resultViewModel.closeAllPopUps()
                            gameViewModel.resetGameState()
                            val validSettings = gameViewModel.setSettings(settingsViewModel.settingsState.value)
                            if (validSettings) {
                                navController.navigate("game") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }

                                var seed: Long
                                do {
                                    seed = SecureRandom().nextLong()
                                } while (seed < 0)
                                gameViewModel.startGame(seed)
                            }
                        },
                        isEnabled = settingsState.isAdditionEnabled ||
                                settingsState.isSubtractionEnabled ||
                                settingsState.isMultiplicationEnabled ||
                                settingsState.isDivisionEnabled,
                        isClicked = isStartButtonClicked
                    )
                }
                ScoreSideSheet(
                    navController = navController,
                )
                MenuSideSheet(
                    navController = navController
                )
            }
        }
    )

    if (settingsViewModel.showNotificationPermissionPopUp.collectAsState().value) {
        PopUp(
            onDismissRequest = { settingsViewModel.setShowNotificationPermissionPopUp(false) },
            onConfirm = {
                settingsViewModel.playFeedback(FeedbackType.TAP)
                openNotificationSettings(context)
                settingsViewModel.setShowNotificationPermissionPopUp(false)
            },
            onCancel = {
                settingsViewModel.playFeedback(FeedbackType.TAP)
                settingsViewModel.setShowNotificationPermissionPopUp(false)
            },
            title = stringResource(R.string.notification_popup_title),
            description = stringResource(R.string.notification_popup_description),
            confirmationText = stringResource(R.string.notification_popup_confirmation_text),
            cancelText = stringResource(R.string.cancel),
            dismissColor = red,
            confirmColor = green
        )
    }

    if (settingsViewModel.showResetPopUp.collectAsState().value) {
        PopUp(
            onDismissRequest = { settingsViewModel.setShowResetPopUp(false) },
            onConfirm = {
                settingsViewModel.playFeedback(FeedbackType.STOP)
                gameViewModel.resetGameData()
                settingsViewModel.resetSettings()
                NotificationScheduler.cancelNotificationWork(context)
                resultViewModel.resetStatistics()
                historyViewModel.resetHistory()
                mainActivityViewModel.resetMainActivity()
            },
            onCancel = {
                settingsViewModel.playFeedback(FeedbackType.TAP)
                settingsViewModel.setShowResetPopUp(false)
            },
            title = stringResource(R.string.reset_app_popup_title),
            description = stringResource(R.string.reset_app_popup_description),
            confirmationText = stringResource(R.string.reset_app_popup_confirmation_text),
            cancelText = stringResource(R.string.cancel),
            dismissColor = green,
            confirmColor = red
        )
    }

    BackHandler {
        (context as? android.app.Activity)?.moveTaskToBack(true)
    }
}
