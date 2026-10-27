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
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.deserialize
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.isValidQueryParameter
import com.helddertierwelt.mentalmath.domain.settings.enums.ThemeMode
import com.helddertierwelt.mentalmath.presentation.component.popup.AlreadyPlayedPopUp
import com.helddertierwelt.mentalmath.presentation.component.popup.DeepLinkPopUp
import com.helddertierwelt.mentalmath.presentation.screen.Navigation
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.MainActivityViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.ResultViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val resultViewModel: ResultViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val climbViewModel: ClimbViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Screen as long as settings are loading
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !settingsViewModel.isLoaded.value
            }
        }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                handleDeepLinkIntent(intent)
            }
        }
        enableEdgeToEdge()
        setContent {
            lifecycle.addObserver(gameViewModel)
            lifecycle.addObserver(climbViewModel)
            val navController = rememberNavController()
            val settings by settingsViewModel.settingsState.collectAsState()
            MentalMathTheme(themeMode = settings.themeMode) {

                // set system bar colors
                val darkTheme =
                    when (settingsViewModel.settingsState.collectAsState().value.themeMode) {
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    }

                // deprecated API systemUiController
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
                when (val popUp = mainActivityViewModel.popUp.collectAsState().value) {
                    // App link pop up
                    is MainActivityViewModel.PopUp.DeepLink -> {
                        val gameRecordFromAppLink = popUp.gameRecord
                        DeepLinkPopUp(
                            linkScore = gameRecordFromAppLink.activeScore,
                            onDismissRequest = { mainActivityViewModel.closePopUp() },
                            onConfirm = {
                                gameViewModel.resetGameState()
                                gameViewModel.setIsLinkGame(true)
                                gameViewModel.setSettingsFromLink(gameRecordFromAppLink, settingsViewModel.settingsState.value)
                                resultViewModel.closeAllPopUps()
                                resultViewModel.prepareGameFromLink(gameRecordFromAppLink)
                                mainActivityViewModel.playFeedback(FeedbackType.START)
                                navController.navigate("game") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                                gameViewModel.startGame(gameRecordFromAppLink.seed)
                                mainActivityViewModel.closePopUp()
                            },
                            onCancel = {
                                mainActivityViewModel.playFeedback(FeedbackType.TAP)
                                mainActivityViewModel.closePopUp()
                            }
                        )
                    }

                    // Already played pop up
                    MainActivityViewModel.PopUp.AlreadyPlayed -> {
                        AlreadyPlayedPopUp(
                            onDismissRequest = { mainActivityViewModel.closePopUp() },
                            onConfirm = {
                                mainActivityViewModel.playFeedback(FeedbackType.TAP)
                                mainActivityViewModel.closePopUp()
                            }
                        )
                    }

                    null -> {}
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        Log.d("DeepLink", "onNewIntent called. Intent: $intent")
        super.onNewIntent(intent)
        lifecycleScope.launch { handleDeepLinkIntent(intent) }
    }


    private suspend fun handleDeepLinkIntent(intent: Intent?) {
        val data = intent?.data
        val encodedSettingsString = data?.getQueryParameter("s")
        if (encodedSettingsString != null && isValidQueryParameter(encodedSettingsString)) {
            val linkGameRecord = deserialize(encodedSettingsString)

            if (settingsViewModel.wasAlreadyPlayed(linkGameRecord)) {
                mainActivityViewModel.showPopUp(MainActivityViewModel.PopUp.AlreadyPlayed)
            } else {
                mainActivityViewModel.showPopUp(MainActivityViewModel.PopUp.DeepLink(linkGameRecord))
            }

        } else {
            Log.d("DeepLink", "SettingsString is invalid or missing.")
        }
    }
}