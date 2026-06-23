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
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.deserialize
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.isValidQueryParameter
import com.helddertierwelt.mentalmath.domain.settings.ThemeMode
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.screen.Navigation
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.MainActivityViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val statisticsViewModel: StatisticsViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val gameRecordFromAppLink = mutableStateOf(GameRecord())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            lifecycleScope.launch { handleDeepLinkIntent(intent) }
        }
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color(0xFFEEEEEE).toArgb(),
                darkScrim = Color(0xFF1E1E1E).toArgb()
            )
        )
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
                val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
                val titleFontSize = (0.031f * screenHeight.value).sp
                val popUpDescriptionFontSize = (0.018f * screenHeight.value).sp

                val showAlreadyPlayedPopUp = mainActivityViewModel.showAlreadyPlayedPopUp.collectAsState().value
                val showDeepLinkPopUp = mainActivityViewModel.showDeepLinkPopUp.collectAsState().value

                // App link pop up
                if (showDeepLinkPopUp) {
                    PopUp(
                        onDismissRequest = { mainActivityViewModel.setShowDeepLinkPopUp(false) },
                        onConfirm = {
                            gameViewModel.reset()
                            gameViewModel.setIsLinkGame(true)
                            settingsViewModel.applySettingsFromLink(gameRecordFromAppLink.value)
                            gameViewModel.setSettings(settingsViewModel.settingsState.value)
                            statisticsViewModel.prepareGameFromLink(gameRecordFromAppLink.value)
                            navController.navigate("game") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                            gameViewModel.startGame(gameRecordFromAppLink.value.seed)
                            gameRecordFromAppLink.value = GameRecord()
                            mainActivityViewModel.setShowDeepLinkPopUp(false)
                        },
                        onCancel = { mainActivityViewModel.setShowDeepLinkPopUp(false) },
                        titleSize = titleFontSize,
                        descriptionSize = popUpDescriptionFontSize,
                        title = stringResource(R.string.app_link_game_popup_title),
                        description = if (gameRecordFromAppLink.value.activeScore < 0f) {
                            stringResource(R.string.app_link_popup_description_new_game)
                        } else {
                            stringResource(R.string.app_link_popup_description_challenge)
                        },
                        confirmationText = stringResource(R.string.app_link_popup_confirmation_text),
                        cancelText = stringResource(R.string.cancel),
                        dismissColor = red,
                        confirmColor = green
                    )
                }

                // Already played pop up
                if (showAlreadyPlayedPopUp) {
                    PopUp(
                        onDismissRequest = { mainActivityViewModel.setShowAlreadyPlayedPopUp(false) },
                        onConfirm = { mainActivityViewModel.setShowAlreadyPlayedPopUp(false) },
                        onCancel = { mainActivityViewModel.setShowAlreadyPlayedPopUp(false) },
                        titleSize = titleFontSize,
                        descriptionSize = popUpDescriptionFontSize,
                        title = stringResource(R.string.already_played_popup_title),
                        description = stringResource(R.string.already_played_popup_description),
                        confirmationText = stringResource(R.string.ok),
                        confirmColor = green
                    )
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

            if (settingsViewModel.checkIfGameWasAlreadyPlayed(linkGameRecord)) {
                mainActivityViewModel.setShowDeepLinkPopUp(false)
                mainActivityViewModel.setShowAlreadyPlayedPopUp(true)
            } else {
                mainActivityViewModel.setShowAlreadyPlayedPopUp(false)
                mainActivityViewModel.setShowDeepLinkPopUp(true)
                gameRecordFromAppLink.value = linkGameRecord
            }

        } else {
            Log.d("DeepLink", "SettingsString is invalid or missing.")
        }
    }
}