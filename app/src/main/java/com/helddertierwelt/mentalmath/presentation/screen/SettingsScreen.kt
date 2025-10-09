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

package com.helddertierwelt.mentalmath.presentation.screen

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.helddertierwelt.mentalmath.presentation.component.settings.settings.CustomRangeSlider
import com.helddertierwelt.mentalmath.presentation.component.settings.settings.CustomSlider
import com.helddertierwelt.mentalmath.presentation.component.settings.settings.ModeButton
import com.helddertierwelt.mentalmath.presentation.component.settings.settings.OperatorButton
import com.helddertierwelt.mentalmath.presentation.component.settings.settings.SettingsTopBar
import com.helddertierwelt.mentalmath.presentation.component.settings.settings.StartButton
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import java.security.SecureRandom

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {

    val activity = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(activity)
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
    val isLoaded by settingsViewModel.isLoaded

    val context = LocalContext.current
    val settingsState = settingsViewModel.settingsState.value
    val valueRangeOperators = 1f..9f

    val containerSizePx = LocalWindowInfo.current.containerSize // IntSize in px
    val density = LocalDensity.current
    val screenHeight = with(density) { containerSizePx.height.toDp() }
    val screenWidth = with(density) { containerSizePx.width.toDp() }

    val roundButtonSize = (0.09f * screenHeight.value).dp
    val sliderSize = (0.05f * screenHeight.value).dp
    val operatorButtonFontSize = (0.057f * screenHeight.value).sp
    val startButtonFontSize = (0.038f * screenHeight.value).sp
    val titleFontSize = (0.031f * screenHeight.value).sp
    val textLabelFontSize = (0.024f * screenHeight.value).sp
    val popUpDescriptionFontSize = (0.018f * screenHeight.value).sp
    val startIconSize = (0.057f * screenHeight.value).dp
    val modeIconSize = (0.048f * screenHeight.value).dp
    val columnPadding = (0.028f * screenHeight.value).dp
    val topBarIconSize = (0.038f * screenHeight.value).dp

    var showDialog by remember { mutableStateOf(false) }
    var isSliderActive by remember { mutableStateOf(false) }
    val bestTotalGames by settingsViewModel.bestTotalGames.collectAsState()
    val bestActiveGames by settingsViewModel.bestTotalGames.collectAsState()
    val bestGamesEmpty = bestTotalGames.isEmpty() && bestActiveGames.isEmpty()


    LaunchedEffect(isLoaded) {
        if (isLoaded) {
            settingsViewModel.loadBestGames()
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                titleId = R.string.app_name,
                titleFontSize = titleFontSize,
                onMenuClick = { settingsViewModel.toggleMenuOpen() },
                onScoreClick = { settingsViewModel.toggleScoreOpen() },
                iconSize = topBarIconSize,
                scoreEnabled = !bestGamesEmpty && !isSliderActive,
                isGreydOut = bestGamesEmpty
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
                    modifier = modifier
                        .fillMaxSize()
                        .padding(columnPadding, columnPadding),
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
                            value = settingsState.limit,
                            onValueChange = {
                                isSliderActive = true
                                settingsViewModel.updateLimit(it)
                            },
                            valueRange = 1f..5f,
                            isToggled = settingsState.isModeEnabled,
                            size = sliderSize,
                            activeTrackColor = MaterialTheme.colorScheme.onSurface,
                            onValueChangeFinished = {
                                isSliderActive = false
                                settingsViewModel.loadBestGames()
                            }
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
                            buttonTextId = R.string.add,
                            textColor = green,
                            isSelected = settingsState.isAdditionEnabled,
                            onClick = {
                                settingsViewModel.toggleAddition()
                                settingsViewModel.loadBestGames()
                            },
                            size = roundButtonSize,
                            fontSize = operatorButtonFontSize,
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = valueRangeOperators,
                            value = settingsState.additionRange,
                            activeTrackColor = green,
                            isEnabled = settingsState.isAdditionEnabled,
                            onValueChange = {
                                isSliderActive = true
                                settingsViewModel.updatePlusRange(it)
                            },
                            size = sliderSize,
                            onValueChangeFinished = {
                                isSliderActive = false
                                settingsViewModel.loadBestGames()
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OperatorButton(
                            buttonTextId = R.string.subtract,
                            textColor = red,
                            isSelected = settingsState.isSubtractionEnabled,
                            onClick = {
                                settingsViewModel.toggleSubtraction()
                                settingsViewModel.loadBestGames()
                            },
                            size = roundButtonSize,
                            fontSize = operatorButtonFontSize
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = valueRangeOperators,
                            value = settingsState.subtractionRange,
                            activeTrackColor = red,
                            isEnabled = settingsState.isSubtractionEnabled,
                            onValueChange = {
                                isSliderActive = true
                                settingsViewModel.updateMinusRange(it)
                            },
                            size = sliderSize,
                            onValueChangeFinished = {
                                isSliderActive = false
                                settingsViewModel.loadBestGames()
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OperatorButton(
                            buttonTextId = R.string.multiply,
                            textColor = yellow,
                            isSelected = settingsState.isMultiplicationEnabled,
                            onClick = {
                                settingsViewModel.toggleMultiplication()
                                settingsViewModel.loadBestGames()
                            },
                            size = roundButtonSize,
                            fontSize = operatorButtonFontSize
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = valueRangeOperators,
                            value = settingsState.multiplicationRange,
                            activeTrackColor = yellow,
                            isEnabled = settingsState.isMultiplicationEnabled,
                            onValueChange = {
                                isSliderActive = true
                                settingsViewModel.updateMultiplyRange(it)
                            },
                            size = sliderSize,
                            onValueChangeFinished = {
                                isSliderActive = false
                                settingsViewModel.loadBestGames()
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OperatorButton(
                            buttonTextId = R.string.divide,
                            textColor = blue,
                            isSelected = settingsState.isDivisionEnabled,
                            onClick = {
                                settingsViewModel.toggleDivide()
                                settingsViewModel.loadBestGames()
                            },
                            size = roundButtonSize,
                            fontSize = operatorButtonFontSize
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = valueRangeOperators,
                            value = settingsState.divisionRange,
                            activeTrackColor = blue,
                            isEnabled = settingsState.isDivisionEnabled,
                            onValueChange = {
                                isSliderActive = true
                                settingsViewModel.updateDivideRange(it)
                            },
                            size = sliderSize,
                            onValueChangeFinished = {
                                isSliderActive = false
                                settingsViewModel.loadBestGames()
                            }
                        )
                    }
                    StartButton(
                        onClick = {
                            gameViewModel.resetGame()
                            gameViewModel.setSettings(settingsViewModel)
                            navController.navigate("game") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                            val random = SecureRandom()
                            var seed: Long
                            do {
                                seed = random.nextLong()
                            } while (seed < 0)
                            gameViewModel.startGame(seed)
                        },
                        size = roundButtonSize,
                        fontSize = startButtonFontSize,
                        iconSize = startIconSize
                    )
                }
                ScoreSideSheet(
                    isSheetOpen = settingsViewModel.isScoreOpen.value,
                    onDismissRequested = { settingsViewModel.toggleScoreOpen() },
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    navController = navController,
                )
                MenuSideSheet(
                    isSheetOpen = settingsViewModel.isMenuOpen.value,
                    screenWidth = screenWidth,
                    onDismissRequested = { settingsViewModel.toggleMenuOpen() },
                    screenHeight = screenHeight,
                    onResetClick = { showDialog = true }
                )
            }
        }
    )

    if (showDialog) {
        PopUp(
            onDismissRequest = { showDialog = false },
            onConfirm = {
                showDialog = false
                gameViewModel.resetGameData()
                settingsViewModel.resetSettings()
                settingsViewModel.loadBestGames()
                settingsViewModel.toggleMenuOpen()
            },
            onCancel = { showDialog = false },
            titleSize = titleFontSize,
            descriptionSize = popUpDescriptionFontSize,
            title = "Reset app?",
            description = "Deletes all scores and settings.",
            confirmationText = "Reset",
            cancelText = "Cancel",
            dismissColor = green,
            confirmColor = red
        )
    }

    BackHandler {
        (context as? android.app.Activity)?.moveTaskToBack(true)
    }
}
