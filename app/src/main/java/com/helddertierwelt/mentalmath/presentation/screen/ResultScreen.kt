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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.domain.result.GameResultInfoGenerator.getGamePopUpInfo
import com.helddertierwelt.mentalmath.domain.settings.AppLocale
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.component.result.DoneButton
import com.helddertierwelt.mentalmath.presentation.component.result.ResultTopBar
import com.helddertierwelt.mentalmath.presentation.component.result.problems.ProblemsCard
import com.helddertierwelt.mentalmath.presentation.component.result.summary.SummaryCard
import com.helddertierwelt.mentalmath.presentation.component.result.summary.formatScore
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.ResultViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ResultScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val dimens = MentalMathTheme.dimens

    // viewModels
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val resultViewModel: ResultViewModel = hiltViewModel(viewModelStoreOwner)

    // variables
    val shareChallengeText = stringResource(R.string.share_challenge)
    var statsCardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var hasCapturedCoordinates by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val navigatedFromHistory = resultViewModel.navigatedFromHistory.collectAsState().value
    val resultGameRecord =
        if (resultViewModel.showLoadedGameRecord.collectAsState().value) {
            resultViewModel.loadedGameRecord.collectAsState().value
        } else {
            resultViewModel.lastGameRecord.collectAsState().value
        }

    // variables for PopUps
    val showLinkGamePopUp by resultViewModel.showLinkGamePopUp.collectAsState()
    val showSurvivalCompletedPopUp by resultViewModel.showSurvivalCompletedPopUp.collectAsState()
    val seed by resultViewModel.seedFromLink.collectAsState()
    val linkScore by resultViewModel.scoreFromLink.collectAsState()
    val currentScore = resultViewModel.lastGameRecord.collectAsState().value.activeScore

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
            Log.d("ResultScreen", "Sharing result: $shareText")
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share Score")
            )
        } catch (e: Exception) {
            Log.e("ResultScreen", "Error when sharing result of link-game", e)
        }
    }

    fun createLink(): String {
        val settingsString = SettingsLinkUtil.serialize(resultGameRecord)
        return "https://mental-math.codeberg.page/app?s=${settingsString}"
    }

    fun navigateBack() {
        if (navigatedFromHistory) {
            resultViewModel.setNavigatedFromHistory(false)
            navController.navigate("history") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate("settings") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            ResultTopBar(
                R.string.result,
                onShareClick = {
                    resultViewModel.playFeedback(FeedbackType.TAP)
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0)
                        val bitmap = captureSettingsCardBitmap()
                        if (bitmap != null) {
                            shareResult(
                                context,
                                bitmap,
                                AppLocale.format(
                                    shareChallengeText,
                                    formatScore(resultGameRecord.activeScore),
                                    resultGameRecord.seed,
                                    createLink()
                                )
                            )
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
                        SummaryCard(
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
                        ProblemsCard()
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    DoneButton(
                        buttonTextId = R.string.done,
                        modifier = Modifier,
                        onClick = {
                            resultViewModel.playFeedback(FeedbackType.TAP)
                            navigateBack()
                        }
                    )
                }
            }
        }
    )

    if (showSurvivalCompletedPopUp) {
        resultViewModel.playDelayedFeedback(FeedbackType.RESULT_PERFECT)
        PopUp(
            onDismissRequest = { resultViewModel.setShowSurvivalCompletedPopUp(false) },
            onConfirm = {
                resultViewModel.playFeedback(FeedbackType.TAP)
                resultViewModel.setShowSurvivalCompletedPopUp(false)
            },
            title = stringResource(R.string.survival_completed_popup_title),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.crown_24dp),
                    contentDescription = "Survival Completed",
                    tint = yellow,
                    modifier = Modifier.size(dimens.icons.popUp)
                )
            },
            description = stringResource(R.string.survival_completed_popup_description),
            confirmationText = stringResource(R.string.ok),
            confirmColor = green
        )
    } else if (showLinkGamePopUp) {
        val gamePopUpInfo = getGamePopUpInfo(currentScore, linkScore, seed)
        val shareMessage = if (linkScore < 0f) {
            AppLocale.format(stringResource(R.string.result_share_message_waiting), formatScore(currentScore), seed)
        } else {
            AppLocale.format(
                stringResource(R.string.result_share_message),
                formatScore(currentScore),
                formatScore(linkScore),
                seed
            )
        }
        resultViewModel.playDelayedFeedback(gamePopUpInfo.feedbackType)
        PopUp(
            title = gamePopUpInfo.title,
            description = gamePopUpInfo.description,
            confirmationText = stringResource(R.string.result_popup_confirmation_text),
            cancelText = stringResource(R.string.ok),
            icon = {
                Icon(
                    painter = painterResource(gamePopUpInfo.icon),
                    contentDescription = "challenge result",
                    tint = gamePopUpInfo.color,
                    modifier = Modifier.size(dimens.icons.popUp)
                )
            },
            confirmColor = green,
            dismissColor = green,
            onDismissRequest = { resultViewModel.setShowLinkGamePopUp(false) },
            onCancel = {
                resultViewModel.playFeedback(FeedbackType.TAP)
                resultViewModel.setShowLinkGamePopUp(false)
            },
            onConfirm = {
                resultViewModel.playFeedback(FeedbackType.TAP)
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
                navigateBack()
            }
        }
        backPressedDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }
}
