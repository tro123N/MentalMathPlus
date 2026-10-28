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

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.helddertierwelt.mentalmath.presentation.climb.screen.ClimbHomeScreen
import com.helddertierwelt.mentalmath.presentation.climb.screen.ClimbPlayScreen
import com.helddertierwelt.mentalmath.presentation.climb.screen.ClimbResultScreen
import com.helddertierwelt.mentalmath.presentation.climb.screen.LearnScreen
import com.helddertierwelt.mentalmath.presentation.climb.screen.LevelScreen
import com.helddertierwelt.mentalmath.presentation.viewmodel.ResultViewModel

@Composable
fun Navigation(
    navController: NavHostController
) {
    val activity = LocalActivity.current as ViewModelStoreOwner
    val resultViewModel: ResultViewModel = hiltViewModel(activity)
    val showLoadedGameRecord = resultViewModel.showLoadedGameRecord.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = "climb",
        ) {
            // Mental Math Plus: Kletterweg
            composable(route = "climb") {
                ClimbHomeScreen(navController = navController)
            }

            composable(
                route = "level/{skillId}",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) { entry ->
                LevelScreen(
                    navController = navController,
                    skillId = entry.arguments?.getString("skillId") ?: "",
                )
            }

            composable(
                route = "learn/{skillId}",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) { entry ->
                LearnScreen(
                    navController = navController,
                    skillId = entry.arguments?.getString("skillId") ?: "",
                )
            }

            composable(
                route = "handbook",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) {
                com.helddertierwelt.mentalmath.presentation.climb.screen.HandbookScreen(navController = navController)
            }

            composable(
                route = "aptraining",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) {
                com.helddertierwelt.mentalmath.presentation.climb.screen.ApTrainingScreen(navController = navController)
            }

            composable(
                route = "ai",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) {
                com.helddertierwelt.mentalmath.presentation.climb.screen.AiChatScreen(navController = navController)
            }

            composable(
                route = "aisettings",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) {
                com.helddertierwelt.mentalmath.presentation.climb.screen.AiSettingsScreen(navController = navController)
            }

            composable(
                route = "stats",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) {
                com.helddertierwelt.mentalmath.presentation.climb.screen.StatsScreen(navController = navController)
            }

            composable(
                route = "placement",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) {
                com.helddertierwelt.mentalmath.presentation.climb.screen.PlacementScreen(navController = navController)
            }

            composable(
                route = "plussettings",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) {
                com.helddertierwelt.mentalmath.presentation.climb.screen.PlusSettingsScreen(navController = navController)
            }

            composable(
                route = "free",
                enterTransition = { fadeIn() },
            ) {
                com.helddertierwelt.mentalmath.presentation.climb.screen.FreeSetupScreen(navController = navController)
            }

            composable(
                route = "climbplay",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            ) {
                ClimbPlayScreen(navController = navController)
            }

            composable(
                route = "climbresult",
                enterTransition = { fadeIn() },
            ) {
                ClimbResultScreen(navController = navController)
            }

            composable(
                route = "settings",
                enterTransition = { if (!showLoadedGameRecord) fadeIn() else null },
            ) {
                SettingsScreen(
                    navController = navController
                )
            }

            composable(
                route = "game",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) }
            ) {
                GameScreen(
                    navController = navController
                )
            }

            composable(
                route = "result",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                exitTransition = {
                    if (!showLoadedGameRecord) null else slideOutHorizontally(
                        targetOffsetX = { it })
                }
            ) {
                ResultScreen(
                    navController = navController
                )
            }

            composable(
                route = "history",
            ) {
                HistoryScreen(
                    navController = navController
                )
            }
        }
    }
}
