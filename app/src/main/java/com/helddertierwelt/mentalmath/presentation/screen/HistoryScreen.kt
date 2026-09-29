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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.presentation.component.history.HistoryDoneButton
import com.helddertierwelt.mentalmath.presentation.component.history.HistoryEntriesCard
import com.helddertierwelt.mentalmath.presentation.component.history.HistoryHeadCard
import com.helddertierwelt.mentalmath.presentation.component.history.HistoryTopBar
import com.helddertierwelt.mentalmath.presentation.viewmodel.HistoryViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.ResultViewModel

@Composable
fun HistoryScreen(
    navController: NavController,
) {
    // viewModels
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val historyViewModel: HistoryViewModel = hiltViewModel(viewModelStoreOwner)
    val resultViewModel: ResultViewModel = hiltViewModel(viewModelStoreOwner)

    // font and icon sizes
    val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val iconSize = (0.038f * screenHeight.value).dp
    val roundButtonSize = (0.09f * screenHeight.value).dp
    val doneIconSize = (0.057f * screenHeight.value).dp
    val fontSize = (0.038f * screenHeight.value).sp
    val titleFontSize = (0.032f * screenHeight.value).sp
    val scrollState = rememberScrollState()

    // variables
    val allGames = historyViewModel.allGames.collectAsState().value
    val currentSortMode = historyViewModel.sortMode.collectAsState().value
    val isEntryClicked = historyViewModel.isEntryClicked.collectAsState().value

    fun navigateBack() {
        navController.navigate("settings") {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        historyViewModel.setIsEntryClicked(false)
    }

    Scaffold(
        topBar = {
            HistoryTopBar(
                R.string.history,
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    historyViewModel.playFeedback(FeedbackType.TAP)
                    historyViewModel.setSortMode(it)
                },
                currentSortMode = currentSortMode
            )
        },
        content = { padding ->
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    HistoryHeadCard(
                        iconSize = iconSize
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        HistoryEntriesCard(
                            gameRecords = allGames,
                            currentSortMode = currentSortMode,
                            fontSize = 12.sp,
                            isEnabled = !isEntryClicked,
                            onClick = { gameRecordId ->
                                historyViewModel.playFeedback(FeedbackType.TAP)
                                historyViewModel.setIsEntryClicked(true)
                                resultViewModel.setNavigatedFromHistory(true)
                                resultViewModel.setLoadedGameRecord(gameRecordId)
                                resultViewModel.setShowLoadedGameRecord(true)
                                resultViewModel.setShowLinkGamePopUp(false)
                                navController.navigate("result")
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    HistoryDoneButton(
                        buttonTextId = R.string.done,
                        size = roundButtonSize,
                        fontSize = fontSize,
                        iconSize = doneIconSize,
                        modifier = Modifier,
                        onClick = {
                            historyViewModel.playFeedback(FeedbackType.TAP)
                            navigateBack()
                        }
                    )
                }
            }
        }
    )

    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { navigateBack() }
        }
        backPressedDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }
}