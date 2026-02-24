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

package com.helddertierwelt.mentalmath101.presentation.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath101.R
import com.helddertierwelt.mentalmath101.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath101.presentation.component.statistics.DoneButton
import com.helddertierwelt.mentalmath101.presentation.component.statistics.SettingsAndStatsCard
import com.helddertierwelt.mentalmath101.presentation.component.statistics.StatisticsTopBar
import com.helddertierwelt.mentalmath101.presentation.component.statistics.TasksCard
import com.helddertierwelt.mentalmath101.presentation.theme.green
import com.helddertierwelt.mentalmath101.presentation.viewmodel.settings.SettingsViewModel
import com.helddertierwelt.mentalmath101.presentation.viewmodel.statistics.StatisticsViewModel
import com.helddertierwelt.mentalmath101.util.GameResultInfoGenerator.getGamePopUpInfo
import com.helddertierwelt.mentalmath101.util.SettingsLinkUtil
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun StatisticsScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val coroutineScope = rememberCoroutineScope()

    // font and icon sizes
    val roundButtonSize = (0.09f * screenHeight.value).dp
    val doneIconSize = (0.057f * screenHeight.value).dp
    val modeIconSize = (0.046f * screenHeight.value).dp
    val shareIconSize = (0.038f * screenHeight.value).dp
    val taskIconSize = (0.032f * screenHeight.value).dp
    val fontSize = (0.038f * screenHeight.value).sp
    val titleFontSize = (0.032f * screenHeight.value).sp
    val resultFontSize = (0.024f * screenHeight.value).sp

    // viewModels
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(viewModelStoreOwner)

    // variables
    var statsCardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var hasCapturedCoordinates by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val navigatedFromHistory = statisticsViewModel.navigatedFromHistory.collectAsState().value
    val statisticsGameRecord =
        if (statisticsViewModel.showLoadedGameRecord.collectAsState().value) {
            statisticsViewModel.loadedGameRecord.collectAsState().value
        } else {
            statisticsViewModel.lastGameRecord.collectAsState().value
        }

    fun captureSettingsCardBitmap(): Bitmap? {
        val coordinates = statsCardCoordinates ?: return null
        val fullBitmap = localView.drawToBitmap()
        val bounds = coordinates.boundsInWindow()
        val left = bounds.left.toInt().coerceIn(0, fullBitmap.width)
        val top = bounds.top.toInt().coerceIn(0, fullBitmap.height)
        val width = bounds.width.toInt().coerceAtMost(fullBitmap.width - left)
        val height = bounds.height.toInt().coerceAtMost(fullBitmap.height - top)
        return Bitmap.createBitmap(fullBitmap, left, top, width, height)
    }

    fun saveBitmapToCacheAndGetUri(context: Context, bitmap: Bitmap): Uri {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "shared_image.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                stream
            )
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun shareResult(context: Context, bitmap: Bitmap, shareText: String) {
        try {
            val imageUri = saveBitmapToCacheAndGetUri(context, bitmap)
            Log.d("StatisticsScreen", "Sharing result: $shareText")
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.share_score))
            )
        } catch (e: Exception) {
            Log.e("StatisticsScreen", "Error when sharing result of link-game", e)
        }
    }

    fun createLink(): String {
        val settingsString = SettingsLinkUtil.serialize(statisticsGameRecord)
        return "https://mental-math.codeberg.page/app?s=%s".format(settingsString)
    }

    Scaffold(
        topBar = {
            StatisticsTopBar(
                R.string.game_result,
                titleFontSize = titleFontSize,
                iconSize = shareIconSize,
                onShareClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0)
                        val bitmap = captureSettingsCardBitmap()
                        if (bitmap != null) {
                            val shareText =
                                "Try to beat my score of %.2f in Mental Math.\nReplay my game with seed %d:\n\n%s".format(
                                    statisticsGameRecord.totalScore,
                                    statisticsGameRecord.seed,
                                    createLink()
                                )
                            shareResult(context, bitmap, shareText)
                        } else {
                            Log.e("StatisticsScreen", "Bitmap could not be captured")
                        }
                    }
                }
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
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        SettingsAndStatsCard(
                            fontSize = resultFontSize,
                            iconSize = modeIconSize,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                if (!hasCapturedCoordinates) {
                                    val size = coordinates.size
                                    if (size.width > 50 && size.height > 50) {
                                        statsCardCoordinates = coordinates
                                        hasCapturedCoordinates = true
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        TasksCard(
                            resultFontSize = resultFontSize,
                            iconSize = taskIconSize
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    DoneButton(
                        buttonTextId = R.string.done,
                        size = roundButtonSize,
                        fontSize = fontSize,
                        iconSize = doneIconSize,
                        modifier = Modifier,
                        onClick = {
                            if (!navigatedFromHistory) {
                                settingsViewModel.loadBestGames()
                                navController.navigate("settings") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                statisticsViewModel.setNavigatedFromHistory(false)
                                navController.navigate("history") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    )

    val showLinkGamePopUp by statisticsViewModel.showLinkGamePopUp.collectAsState()
    val seed by statisticsViewModel.seedFromLink.collectAsState()
    val linkScore by statisticsViewModel.scoreFromLink.collectAsState()
    val currentScore = statisticsViewModel.lastGameRecord.collectAsState().value.totalScore
    if (showLinkGamePopUp) {
        val gamePopUpInfo = getGamePopUpInfo(currentScore, linkScore, seed)
        val shareMessage = "Me: %.2f | You: %.2f\nSeed: %d".format(currentScore, seed)
        PopUp(
            titleSize = titleFontSize,
            descriptionSize = (0.018f * screenHeight.value).sp,
            title = gamePopUpInfo.title,
            description = gamePopUpInfo.description,
            confirmationText = "Share",
            cancelText = "Ok",
            icon = {
                Icon(
                    painter = painterResource(gamePopUpInfo.icon),
                    contentDescription = "challenge result",
                    tint = gamePopUpInfo.color,
                    modifier = Modifier.size((0.1f * screenHeight.value).dp)
                )
            },
            confirmColor = green,
            dismissColor = green,
            onDismissRequest = { statisticsViewModel.setShowLinkGamePopUp(false) },
            onCancel = { statisticsViewModel.setShowLinkGamePopUp(false) },
            onConfirm = {
                coroutineScope.launch {
                    scrollState.animateScrollTo(0)
                    val bitmap = captureSettingsCardBitmap()
                    if (bitmap != null) {
                        shareResult(context, bitmap, shareMessage)
                    } else {
                        Log.e("StatisticsScreen", "Bitmap could not be captured")
                    }
                }
            }
        )
    }

    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navigatedFromHistory) {
                    settingsViewModel.loadBestGames()
                    navController.navigate("settings") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    statisticsViewModel.setNavigatedFromHistory(false)
                    navController.navigate("history") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
        backPressedDispatcher?.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }
}
