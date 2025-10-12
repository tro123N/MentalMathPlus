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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.drawToBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.component.game.PopUp
import com.helddertierwelt.mentalmath.presentation.component.statistics.DoneButton
import com.helddertierwelt.mentalmath.presentation.component.statistics.SettingsAndStatsCard
import com.helddertierwelt.mentalmath.presentation.component.statistics.StatisticsTopBar
import com.helddertierwelt.mentalmath.presentation.component.statistics.TasksCard
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel
import com.helddertierwelt.mentalmath.util.SettingsLinkUtil
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun StatisticsScreen(
    navController: NavController,
) {

    val activity = LocalActivity.current
    val viewModelStoreOwner = activity as ViewModelStoreOwner
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(viewModelStoreOwner)

    // Calculate sizes based on screen size
    val containerSizePx = LocalWindowInfo.current.containerSize // IntSize in px
    val density = LocalDensity.current
    val localView = LocalView.current
    val screenWidth = with(density) { containerSizePx.width.toDp() }
    val screenHeight = with(density) { containerSizePx.height.toDp() }
    val roundButtonSize = (0.09f * screenHeight.value).dp
    val doneButtonFontSize = (0.038f * screenHeight.value).sp
    val doneIconSize = (0.057f * screenHeight.value).dp
    val shareIconSize = (0.038f * screenHeight.value).dp
    val titleFontSize = (0.032f * screenHeight.value).sp
    val resultFontSize = (0.057f * screenWidth.value).sp
    val operatorFontSize = (0.057f * screenHeight.value).sp
    val taskIconSize = (0.032f * screenHeight.value).dp
    val modeIconSize = (0.048f * screenHeight.value).dp
    val columnPadding = (0.028f * screenHeight.value).dp
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var statsCardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var hasCapturedCoordinates by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

    fun shareBitmapWithLink(context: Context, bitmap: Bitmap) {
        try {
            val imageUri = saveBitmapToCacheAndGetUri(context, bitmap)
            val record = if (statisticsViewModel.showLoadedGameRecord.value) {
                statisticsViewModel.loadedGameRecord.value
            } else {
                statisticsViewModel.lastGameRecord.value
            }
            val settingsString = SettingsLinkUtil.serialize(record)
            val appLink = "https://helddertierwelt.codeberg.page/mentalmath?settings=%s".format(settingsString)
            val shareText = "Try to beat my score of %.2f in Mental Math.\nReplay my game with seed %d:\n\n%s".format(record.totalScore, record.seed, appLink)
            Log.d("StatisticsScreen", "Sharing link: $appLink")
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
            Log.e("StatisticsScreen", "Error when sharing score and link", e)
        }
    }

    fun shareBitmapAsImage(context: Context, bitmap: Bitmap) {
        try {
            val imageUri = saveBitmapToCacheAndGetUri(context, bitmap)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.share_link_game_result))
            )
        } catch (e: Exception) {
            Log.e("StatisticsScreen", "Error when sharing result of link-game", e)
        }
    }

    Scaffold(
        topBar = {
            StatisticsTopBar(
                R.string.game_result,
                titleFontSize,
                shareIconSize,
                onShareClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0)
                        val bitmap = captureSettingsCardBitmap()
                        if (bitmap != null) {
                            shareBitmapWithLink(context, bitmap)
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
                        .padding(columnPadding),
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
                            operatorSize = operatorFontSize,
                            iconSize = modeIconSize,
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
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
                    Spacer(modifier = Modifier.height(columnPadding))
                    DoneButton(
                        buttonTextId = R.string.done,
                        size = roundButtonSize,
                        fontSize = doneButtonFontSize,
                        iconSize = doneIconSize,
                        modifier = Modifier,
                        onClick = {
                            settingsViewModel.loadBestGames()
                            navController.navigate("settings") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }
    )

    val showLinkGamePopUp by statisticsViewModel.showLinkGamePopUp.collectAsState()
    val linkScore by statisticsViewModel.scoreFromLink.collectAsState()
    val seed by statisticsViewModel.seedFromLink.collectAsState()
    val currentScore = statisticsViewModel.lastGameRecord.collectAsState().value.totalScore
    if (showLinkGamePopUp) {
        val (title, description) = if (linkScore == 0.0f) {
            when{
                currentScore > 0.0f -> "Easy Win" to "You: %.2f | Opponent: %.2f\nSeed: %d\nA win, but without a competition!".format(currentScore, linkScore, seed)
                else -> "Anyone here?" to "You: %.2f | Opponent: %.2f\nSeed: %d\nA point-less game!".format(currentScore, linkScore, seed)
            }
        } else when {
            currentScore / linkScore > 1.05f -> "Solid Win" to "You: %.2f | Opponent: %.2f\nSeed: %d\nThat was some serious mental math!".format(currentScore, linkScore, seed)
            currentScore / linkScore > 1.0f -> "Close Win" to "You: %.2f | Opponent: %.2f\nSeed: %d\nA tight win, but it counts!".format(currentScore, linkScore, seed)
            currentScore / linkScore == 1.0f -> "Draw" to "You: %.2f | Opponent: %.2f\nSeed: %d\nA perfect tie - time for a decider!".format(currentScore, linkScore, seed)
            currentScore / linkScore >= 0.95f -> "Close Loss" to "You: %.2f | Opponent: %.2f\nSeed: %d\nA tight loss, but you almost had it!".format(currentScore, linkScore, seed)
            else -> "Defeat" to "You: %.2f | Opponent: %.2f\nSeed: %d\nShake it off and give it another try!".format(currentScore, linkScore, seed)
        }
        val icon: Int
        val color: Color
        if (linkScore == 0.0f) {
            if (currentScore > 0.0f) {
                icon = R.drawable.favorite_24dp
                color = red
            } else {
                icon = R.drawable.question_mark_24dp
                color = blue
            }
        } else {
            val ratio = currentScore / linkScore
            if (ratio > 1.0f) {
                icon = R.drawable.trophy_24dp
                color = yellow
            } else if (ratio == 1.0f) {
                icon = R.drawable.partner_heart_24dp
                color = green
            } else {
                icon = R.drawable.rainy_24dp
                color = blue
            }
        }
        PopUp(
            titleSize = titleFontSize,
            descriptionSize = (0.018f * screenHeight.value).sp,
            title = title,
            description = description,
            confirmationText = "Share",
            cancelText = "Ok",
            icon = {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = "challenge result",
                    tint = color,
                    modifier = Modifier.size((0.1f * screenHeight.value).dp)
                )
            },
            confirmColor = green,
            dismissColor = green,
            onDismissRequest = { statisticsViewModel.setShowLinkGamePopUp(false) },
            onCancel = { statisticsViewModel.setShowLinkGamePopUp(false) },
            onConfirm  = {
                renderChallengeResultContentToBitmap(
                    activity = activity,
                    icon = icon,
                    iconTint = color,
                    iconSize = (0.1f * screenHeight.value).dp,
                    title = title,
                    titleSize = titleFontSize,
                    description = description,
                    descriptionSize = (0.018f * screenHeight.value).sp,
                    widthPx = 800,
                    heightPx = 600
                ) { bitmap ->
                    if (bitmap != null) {
                        shareBitmapAsImage(context, bitmap)
                    } else {
                        Log.e("StatisticsScreen", "ChallengeResultContent Bitmap could not be rendered")
                    }
                }
            }
        )
    }


    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                settingsViewModel.loadBestGames()
                navController.navigate("settings") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        backPressedDispatcher?.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }
}


// Composable for sharing challenge result content
@Composable
fun ChallengeResultContent(
    icon: Int,
    iconTint: Color,
    iconSize: Dp,
    title: String,
    titleSize: TextUnit,
    description: String,
    descriptionSize: TextUnit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = titleSize,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = descriptionSize,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun renderChallengeResultContentToBitmap(
    activity: Activity,
    icon: Int,
    iconTint: Color,
    iconSize: Dp,
    title: String,
    titleSize: TextUnit,
    description: String,
    descriptionSize: TextUnit,
    widthPx: Int = 600,
    heightPx: Int = 400,
    onBitmapReady: (Bitmap?) -> Unit
) {
    val composeView = ComposeView(activity)
    composeView.setContent {
        MaterialTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                ChallengeResultContent(
                    icon = icon,
                    iconTint = iconTint,
                    iconSize = iconSize,
                    title = title,
                    titleSize = titleSize,
                    description = description,
                    descriptionSize = descriptionSize
                )
            }
        }
    }
    val container = FrameLayout(activity)
    container.alpha = 0f
    val params = FrameLayout.LayoutParams(widthPx, heightPx)
    params.gravity = Gravity.CENTER
    container.addView(composeView, params)
    activity.addContentView(container, params)
    Handler(Looper.getMainLooper()).post {
        val widthSpec = MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY)
        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, widthPx, heightPx)
        val bitmap = createBitmap(widthPx, heightPx)
        val canvas = android.graphics.Canvas(bitmap)
        composeView.draw(canvas)
        (container.parent as? ViewGroup)?.removeView(container)
        onBitmapReady(bitmap)
    }
}
