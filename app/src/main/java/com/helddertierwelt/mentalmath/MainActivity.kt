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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.screen.Navigation
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.ThemeMode
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    // States für DeepLink-PopUp
    private val showDeepLinkPopUp = mutableStateOf(false)
    private val deepLinkSettingsString = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            installSplashScreen().apply {
                setKeepOnScreenCondition {
                    !settingsViewModel.isLoaded.value
                }
            }
            val navController = rememberNavController()
            lifecycle.addObserver(gameViewModel)
            MentalMathTheme(themeMode = settingsViewModel.settingsState.value.themeMode) {
                val darkTheme = when (settingsViewModel.settingsState.value.themeMode) {
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
                if (showDeepLinkPopUp.value) {
                    PopUp(
                        onDismissRequest = { showDeepLinkPopUp.value = false },
                        onConfirm = {
                            showDeepLinkPopUp.value = false
                            gameViewModel.endGame()
                            settingsViewModel.applySettingsString(deepLinkSettingsString.value!!)
                        },
                        onCancel = { showDeepLinkPopUp.value = false },
                        titleSize = MaterialTheme.typography.titleLarge.fontSize,
                        descriptionSize = MaterialTheme.typography.bodyLarge.fontSize,
                        title = "Test",
                        description = "",
                        confirmationText = "",
                        cancelText = ""
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val data = intent.data
        val settingsString = data?.getQueryParameter("settings")
        if (settingsString != null && settingsViewModel.isValidSettingsString(settingsString)) {
            if (gameViewModel.gamesState.value.isGameStarted) {
                showDeepLinkPopUp.value = true
                deepLinkSettingsString.value = settingsString
            } else {
                settingsViewModel.applySettingsString(settingsString)
            }
        }
    }
}