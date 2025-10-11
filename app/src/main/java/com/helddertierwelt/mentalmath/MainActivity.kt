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

/*
 * Copyright (C) 2025 HeldDerTierwelt
 *
 * This file is part of Mental Math.
 *
 * Mental Math is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mental Math is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mental Math. If not, see <https://www.gnu.org/licenses/>.
 */

package com.helddertierwelt.mentalmath

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.screen.Navigation
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.ThemeMode
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.util.SettingsLinkUtil.deserialize
import com.helddertierwelt.mentalmath.util.SettingsLinkUtil.isValidSettingsString
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val showDeepLinkPopUp = mutableStateOf(false)
    private val gameRecordFromAppLink = mutableStateOf<GameRecord>(GameRecord())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent(intent)
        enableEdgeToEdge()
        setContent {

            // Splash Screen as long as settings are loading
            installSplashScreen().apply {
                setKeepOnScreenCondition {
                    !settingsViewModel.isLoaded.value
                }
            }

            val navController = rememberNavController()
            lifecycle.addObserver(gameViewModel)
            MentalMathTheme(themeMode = settingsViewModel.settingsState.collectAsState().value.themeMode) {

                // set system bar colors
                val darkTheme = when (settingsViewModel.settingsState.collectAsState().value.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                val systemUiController = rememberSystemUiController()
                val navBarColor = MaterialTheme.colorScheme.background
                SideEffect {
                    systemUiController.setNavigationBarColor(
                        color = navBarColor,
                        darkIcons = !darkTheme
                    )
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = !darkTheme
                    )
                }
                Navigation(
                    navController = navController
                )

                // Deep Link PopUp
                if (showDeepLinkPopUp.value) {
                    val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
                    val titleFontSize = (0.031f * screenHeight.value).sp
                    val popUpDescriptionFontSize = (0.018f * screenHeight.value).sp
                    PopUp(
                        onDismissRequest = { showDeepLinkPopUp.value = false },
                        onConfirm = {
                            gameViewModel.resetGame()
                            settingsViewModel.applySettingsFromLink(gameRecordFromAppLink.value)
                            gameViewModel.setSettings(settingsViewModel)
                            navController.navigate("game") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                            gameViewModel.startGame(gameRecordFromAppLink.value.seed)
                            dismissDeepLinkPopUp()                        },
                        onCancel = { showDeepLinkPopUp.value = false },
                        titleSize = titleFontSize,
                        descriptionSize = popUpDescriptionFontSize,
                        title = "Start game from link?",
                        description = "Start a new game using the settings provided by the link. Any current game progress will be lost.",
                        confirmationText = "Start",
                        cancelText = "Cancel",
                        dismissColor = red,
                        confirmColor = green
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        Log.d("DeepLink", "onNewIntent called. Intent: $intent")
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val data = intent?.data
        val encodedSettingsString = data?.getQueryParameter("settings")
        if (encodedSettingsString != null && isValidSettingsString(encodedSettingsString)) {
            showDeepLinkPopUp.value = true
            gameRecordFromAppLink.value = deserialize(encodedSettingsString)
        } else {
            Log.d("DeepLink", "SettingsString is invalid or missing.")
        }
    }

    private fun dismissDeepLinkPopUp() {
        showDeepLinkPopUp.value = false
        gameRecordFromAppLink.value = GameRecord()
    }
}