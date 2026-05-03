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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.component.settings.menu.MenuSideSheet
import com.helddertierwelt.mentalmath.presentation.component.settings.score.ScoreSideSheet
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.CustomRangeSlider
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.CustomSlider
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.ModeButton
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.OperatorButton
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.SettingsTopBar
import com.helddertierwelt.mentalmath.presentation.component.settings.screen.StartButton
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.game.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.history.HistoryViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.mainactivity.MainActivityViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.settings.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.statistics.StatisticsViewModel
import com.helddertierwelt.mentalmath.util.NotificationScheduler
import java.security.SecureRandom

@Composable
fun SettingsScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val containerSizePx = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val screenHeight = with(density) { containerSizePx.height.toDp() }
    val screenWidth = with(density) { containerSizePx.width.toDp() }

    // font and icon sizes
    val roundButtonSize = (0.09f * screenHeight.value).dp
    val sliderSize = (0.05f * screenHeight.value).dp
    val startIconSize = (0.057f * screenHeight.value).dp
    val modeIconSize = (0.048f * screenHeight.value).dp
    val operationIconSize = (0.043f * screenHeight.value).dp
    val topBarIconSize = (0.038f * screenHeight.value).dp
    val popUpDescriptionFontSize = (0.018f * screenHeight.value).sp
    val textLabelFontSize = (0.024f * screenHeight.value).sp
    val titleFontSize = (0.031f * screenHeight.value).sp
    val startButtonFontSize = (0.038f * screenHeight.value).sp
    val thumbnailFontSize = (0.015f * screenHeight.value).sp

    // viewModels
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(viewModelStoreOwner)
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(viewModelStoreOwner)
    val historyViewModel: HistoryViewModel = hiltViewModel(viewModelStoreOwner)
    val mainActivityViewModel: MainActivityViewModel = hiltViewModel(viewModelStoreOwner)

    // variables
    val settingsState = settingsViewModel.settingsState.collectAsState().value
    val isLoaded = settingsViewModel.isLoaded.collectAsState().value
    val bestGames = settingsViewModel.bestGames.collectAsState().value
    val bestGamesEmpty = bestGames.isEmpty()
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
            settingsViewModel.loadBestGames()
            settingsViewModel.setIsStartButtonClicked(false)
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                titleFontSize = titleFontSize,
                onMenuClick = { settingsViewModel.toggleMenuOpen() },
                onScoreClick = { settingsViewModel.toggleScoreOpen() },
                iconSize = topBarIconSize,
                scoreEnabled = !bestGamesEmpty && !isSliderActive,
                isGreyedOut = bestGamesEmpty
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
                        fontSize = textLabelFontSize,
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeight = textLabelFontSize
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ModeButton(
                            iconId1 = R.drawable.tag_24dp,
                            iconId2 = R.drawable.hourglass_bottom_24dp,
                            isToggled = settingsState.isModeEnabled,
                            onClick = {
                                settingsViewModel.toggleMode()
                                settingsViewModel.loadBestGames()
                            },
                            iconSize = modeIconSize,
                            size = roundButtonSize
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
                                settingsViewModel.loadBestGames()
                            },
                            isToggled = settingsState.isModeEnabled,
                            activeTrackColor = MaterialTheme.colorScheme.onPrimary,
                            size = sliderSize,
                            thumbnailFontSize = thumbnailFontSize
                        )
                    }
                    Text(
                        text = stringResource(R.string.operator_instruction),
                        fontSize = textLabelFontSize,
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeight = textLabelFontSize
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
                                settingsViewModel.loadBestGames()
                            },
                            size = roundButtonSize,
                            iconSize = operationIconSize,
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
                                settingsViewModel.loadBestGames()
                            },
                            isEnabled = settingsState.isAdditionEnabled,
                            activeTrackColor = green,
                            size = sliderSize,
                            thumbnailFontSize = thumbnailFontSize
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
                                settingsViewModel.loadBestGames()
                            },
                            size = roundButtonSize,
                            iconSize = operationIconSize
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
                                settingsViewModel.loadBestGames()
                            },
                            isEnabled = settingsState.isSubtractionEnabled,
                            activeTrackColor = red,
                            size = sliderSize,
                            thumbnailFontSize = thumbnailFontSize
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
                                settingsViewModel.loadBestGames()
                            },
                            size = roundButtonSize,
                            iconSize = operationIconSize
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
                                settingsViewModel.loadBestGames()
                            },
                            isEnabled = settingsState.isMultiplicationEnabled,
                            activeTrackColor = yellow,
                            size = sliderSize,
                            thumbnailFontSize = thumbnailFontSize
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
                                settingsViewModel.loadBestGames()
                            },
                            size = roundButtonSize,
                            iconSize = operationIconSize
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
                                settingsViewModel.loadBestGames()
                            },
                            isEnabled = settingsState.isDivisionEnabled,
                            activeTrackColor = blue,
                            size = sliderSize,
                            thumbnailFontSize = thumbnailFontSize
                        )
                    }
                    StartButton(
                        onClick = {
                            settingsViewModel.setIsStartButtonClicked(true)
                            statisticsViewModel.setShowLinkGamePopUp(false)
                            gameViewModel.reset()
                            gameViewModel.setSettings(settingsViewModel)
                            navController.navigate("game") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }

                            var seed: Long
                            do {
                                seed = SecureRandom().nextLong()
                            } while (seed < 0)
                            gameViewModel.startGame(seed)
                        },
                        size = roundButtonSize,
                        fontSize = startButtonFontSize,
                        iconSize = startIconSize,
                        isEnabled = settingsState.isAdditionEnabled ||
                                settingsState.isSubtractionEnabled ||
                                settingsState.isMultiplicationEnabled ||
                                settingsState.isDivisionEnabled,
                        isClicked = isStartButtonClicked
                    )
                }
                ScoreSideSheet(
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    navController = navController,
                )
                MenuSideSheet(
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    navController = navController
                )
            }
        }
    )

    if (settingsViewModel.showNotificationPermissionPopUp.collectAsState().value) {
        PopUp(
            onDismissRequest = { settingsViewModel.setShowNotificationPermissionPopUp(false) },
            onConfirm = {
                openNotificationSettings(context)
                settingsViewModel.setShowNotificationPermissionPopUp(false)
            },
            onCancel = { settingsViewModel.setShowNotificationPermissionPopUp(false) },
            titleSize = titleFontSize,
            descriptionSize = popUpDescriptionFontSize,
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
                gameViewModel.resetGameData()
                settingsViewModel.resetSettings()
                NotificationScheduler.cancelNotificationWork(context)
                statisticsViewModel.resetStatistics()
                historyViewModel.resetHistory()
                mainActivityViewModel.resetMainActivity()
            },
            onCancel = { settingsViewModel.setShowResetPopUp(false) },
            titleSize = titleFontSize,
            descriptionSize = popUpDescriptionFontSize,
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
