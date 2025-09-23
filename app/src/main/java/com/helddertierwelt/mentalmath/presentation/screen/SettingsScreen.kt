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

import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.Composable
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
import com.helddertierwelt.mentalmath.presentation.component.settings.CustomRangeSlider
import com.helddertierwelt.mentalmath.presentation.component.settings.CustomSlider
import com.helddertierwelt.mentalmath.presentation.component.settings.ModeButton
import com.helddertierwelt.mentalmath.presentation.component.settings.OperatorButton
import com.helddertierwelt.mentalmath.presentation.component.settings.SettingsTopBar
import com.helddertierwelt.mentalmath.presentation.component.settings.SideSheetLeft
import com.helddertierwelt.mentalmath.presentation.component.settings.StartButton
import com.helddertierwelt.mentalmath.presentation.component.settings.TextLabel
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {

    val activity = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(activity)

    val context = LocalContext.current
    val settingsState = settingsViewModel.settingsState.value
    val valueRangeOperators = 1f..9f

    // Calculate sizes based on screen height
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
    val startIconSize = (0.057f * screenHeight.value).dp
    val modeIconSize = (0.048f * screenHeight.value).dp
    val columnPadding = (0.028f * screenHeight.value).dp
    val infoIconSize = (0.038f * screenHeight.value).dp
    val appSymbolSizeTopBar = (0.024f * screenHeight.value).dp

    Scaffold(
        topBar = {
            SettingsTopBar(
                R.string.app_name,
                titleFontSize,
                onInfoClick = { settingsViewModel.toggleSheetOpen() },
                infoIconSize,
                appSymbolSizeTopBar
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
                    // TextLabel(text= screenHeight.toString(), fontSize = 25.sp) // Debug: show screen height
                    TextLabel(
                        text = stringResource(R.string.mode_instruction),
                        fontSize = textLabelFontSize,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ModeButton(
                            iconId1 = R.drawable.tag_24dp,
                            iconId2 = R.drawable.hourglass_bottom_24dp,
                            isToggled = settingsState.isModeEnabled,
                            onClick = { settingsViewModel.toggleMode() },
                            iconSize = modeIconSize,
                            size = roundButtonSize
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomSlider(
                            value = settingsState.limit,
                            onValueChange = { settingsViewModel.updateLimit(it) },
                            valueRange = 1f..5f,
                            isToggled = settingsState.isModeEnabled,
                            size = sliderSize,
                            activeTrackColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextLabel(
                        text = stringResource(R.string.operator_instruction),
                        fontSize = textLabelFontSize
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OperatorButton(
                            buttonTextId = R.string.add,
                            textColor = green,
                            isSelected = settingsState.isAdditionEnabled,
                            onClick = { settingsViewModel.togglePlus() },
                            size = roundButtonSize,
                            fontSize = operatorButtonFontSize,
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = valueRangeOperators,
                            value = settingsState.additionRange,
                            activeTrackColor = green,
                            isEnabled = settingsState.isAdditionEnabled,
                            onValueChange = { settingsViewModel.updatePlusRange(it) },
                            size = sliderSize
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
                            onClick = { settingsViewModel.toggleMinus() },
                            size = roundButtonSize,
                            fontSize = operatorButtonFontSize
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = valueRangeOperators,
                            value = settingsState.subtractionRange,
                            activeTrackColor = red,
                            isEnabled = settingsState.isSubtractionEnabled,
                            onValueChange = { settingsViewModel.updateMinusRange(it) },
                            size = sliderSize
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
                            onClick = { settingsViewModel.toggleMultiply() },
                            size = roundButtonSize,
                            fontSize = operatorButtonFontSize
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = valueRangeOperators,
                            value = settingsState.multiplicationRange,
                            activeTrackColor = yellow,
                            isEnabled = settingsState.isMultiplicationEnabled,
                            onValueChange = { settingsViewModel.updateMultiplyRange(it) },
                            size = sliderSize
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
                            onClick = { settingsViewModel.toggleDivide() },
                            size = roundButtonSize,
                            fontSize = operatorButtonFontSize
                        )
                        Spacer(Modifier.padding(8.dp))
                        CustomRangeSlider(
                            valueRange = valueRangeOperators,
                            value = settingsState.divisionRange,
                            activeTrackColor = blue,
                            isEnabled = settingsState.isDivisionEnabled,
                            onValueChange = { settingsViewModel.updateDivideRange(it) },
                            size = sliderSize
                        )
                    }
                    StartButton(
                        settingsViewModel = settingsViewModel,
                        onClick = {
                            gameViewModel.resetGame()
                            gameViewModel.setEnabledOperators(settingsViewModel)
                            navController.navigate("game") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                            gameViewModel.startGame()
                        },
                        size = roundButtonSize,
                        fontSize = startButtonFontSize,
                        iconSize = startIconSize
                    )
                }
                /*SideSheet(
                    isSheetOpen = settingsViewModel.isSheetOpen.value,
                    screenWidth = screenWidth,
                    onDismissRequested = { settingsViewModel.toggleSheetOpen() },
                    screenHeight = screenHeight,
                    settingsViewModel = settingsViewModel
                )*/
            }
        }
    )
    SideSheetLeft(
        isSheetOpen = settingsViewModel.isSheetOpen.value,
        screenWidth = screenWidth,
        onDismissRequested = { settingsViewModel.toggleSheetOpen() },
        screenHeight = screenHeight,
        settingsViewModel = settingsViewModel
    )

    BackHandler {
        (context as? android.app.Activity)?.moveTaskToBack(true)
    }
}
