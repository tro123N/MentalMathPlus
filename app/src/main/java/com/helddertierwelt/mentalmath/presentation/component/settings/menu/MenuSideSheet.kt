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

package com.helddertierwelt.mentalmath.presentation.component.settings.menu

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.BuildConfig
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.HistoryViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel
import com.helddertierwelt.mentalmath.domain.settings.AppLocale
import com.helddertierwelt.mentalmath.data.notification.NotificationScheduler
import com.helddertierwelt.mentalmath.data.notification.NotificationScheduler.calculateAnchorTime
import kotlinx.coroutines.launch
import java.security.SecureRandom

@Composable
fun MenuSideSheet(
    screenWidth: Dp,
    screenHeight: Dp,
    navController: NavController
) {
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val gameViewModel: GameViewModel = hiltViewModel(viewModelStoreOwner)
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(viewModelStoreOwner)
    val historyViewModel: HistoryViewModel = hiltViewModel(viewModelStoreOwner)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val sheetWidthFactor = 0.6f
    val minOffset = -screenWidthPx * sheetWidthFactor
    val maxOffset = 0f
    val offsetX = remember { Animatable(minOffset) }
    val overlayAlpha = 0.38f * (offsetX.value - minOffset) / (maxOffset - minOffset)

    val fontSize = (0.020f * screenHeight.value).sp
    val iconSize = (0.032f * screenHeight.value).dp
    val logoSize = (0.11f * screenHeight.value).dp

    val isSheetOpen = settingsViewModel.isMenuOpen.collectAsState().value
    val scrollState = rememberScrollState()
    var isInteractionEnabled by remember { mutableStateOf(true) }
    var isFirstRender by remember { mutableStateOf(true) }
    val isRandomGameButtonClicked = settingsViewModel.isRandomGameButtonClicked.collectAsState().value
    val notificationToastText = stringResource(R.string.notification_toast_text)

    LaunchedEffect(Unit) {
        settingsViewModel.settingsState.collect { settingsState ->
            if (settingsState.notificationMode != NotificationMode.OFF) {
                if (!settingsViewModel.isPermissionGranted(context)) {
                    settingsViewModel.setNotificationMode(NotificationMode.OFF)
                    NotificationScheduler.cancelNotificationWork(context)
                }
            }
        }
    }

    LaunchedEffect(isSheetOpen) {
        val target = if (isSheetOpen) maxOffset else minOffset
        if (isFirstRender) {
            offsetX.snapTo(target)
            isFirstRender = false
        } else {
            isInteractionEnabled = false
            offsetX.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
        isInteractionEnabled = true
        settingsViewModel.setIsRandomGameButtonClicked(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha))
                    .then(
                        if (isInteractionEnabled) {
                            Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { settingsViewModel.toggleMenuOpen() }
                        } else {
                            Modifier
                        }
                    )
            )
        }

        val shareNewGameMessage = stringResource(R.string.share_new_game)
        Card(
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxHeight()
                .width(screenWidth * sheetWidthFactor)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = (offsetX.value + delta).coerceIn(minOffset, maxOffset)
                        coroutineScope.launch {
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragStopped = {
                        coroutineScope.launch {
                            if (offsetX.value < minOffset * 0.2f) {
                                settingsViewModel.toggleMenuOpen()
                            } else {
                                offsetX.animateTo(maxOffset)
                            }
                        }
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .weight(1f)
                ) {

                    // App Logo
                    Spacer(modifier = Modifier.height(8.dp))
                    LogoRow(
                        size = logoSize,
                        imageId = R.drawable.mental_math,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://codeberg.org/Mental-Math/MentalMath/releases/tag/v${BuildConfig.VERSION_NAME}".toUri()
                            )
                        },
                        text = " v${BuildConfig.VERSION_NAME}",
                        errorMessage = stringResource(R.string.no_browser)
                    )

                    // Share Game Link
                    IntentRow(
                        iconResId = R.drawable.swords_24dp,
                        contentDescription = "Share Game",
                        text = stringResource(R.string.share_game),
                        fontSize = fontSize,
                        iconSize = iconSize,
                        isEnabled = settingsViewModel.isAnyOperationEnabled(),
                        intentProvider = {
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    var seed: Long
                                    do {
                                        seed = SecureRandom().nextLong()
                                    } while (seed < 0)
                                    val seedLink = settingsViewModel.generateSeedLink(seed)
                                    val shareText = AppLocale.format(shareNewGameMessage, seed, seedLink)
                                    Log.d("SettingsScreen", "Sharing seed game: $shareText")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                },
                                "Share Game"
                            )
                        },
                        errorMessage = stringResource(R.string.no_share)
                    )

                    // Random Game
                    ButtonRow(
                        iconResId = R.drawable.casino_24dp,
                        contentDescription = "Random Game",
                        text = stringResource(R.string.random_game),
                        fontSize = fontSize,
                        iconSize = iconSize,
                        onClick = {
                            settingsViewModel.setIsRandomGameButtonClicked(true)
                            statisticsViewModel.setShowLinkGamePopUp(false)
                            gameViewModel.reset()
                            settingsViewModel.applyRandomSettings()
                            gameViewModel.setSettings(settingsViewModel)
                            navController.navigate("game") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                            var seed: Long
                            do {
                                seed = SecureRandom().nextLong()
                            } while (seed < 0)
                            gameViewModel.startGame(seed)
                        },
                        isEnabled = !isRandomGameButtonClicked
                    )

                    // History
                    ButtonRow(
                        iconResId = R.drawable.history_24dp,
                        contentDescription = "History",
                        text = stringResource(R.string.history),
                        fontSize = fontSize,
                        iconSize = iconSize,
                        onClick = {
                            historyViewModel.loadAllGames()
                            navController.navigate("history") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )

                    // Input Layout Toggle
                    InputLayoutToggleRow(
                        fontSize = fontSize,
                        iconSize = iconSize,
                        isNumpad = settingsViewModel.settingsState.collectAsState().value.isNumpadDefault,
                        onClick = { settingsViewModel.toggleInputLayout() }
                    )

                    // Input Method Toggle
                    InputMethodToggleRow(
                        fontSize = fontSize,
                        iconSize = iconSize,
                        rightToLeft = settingsViewModel.settingsState.collectAsState().value.isRightToLeft,
                        onClick = { settingsViewModel.toggleInputMethod() }
                    )

                    NotificationToggleRow(
                        fontSize = fontSize,
                        iconSize = iconSize,
                        currentMode = settingsViewModel.settingsState.collectAsState().value.notificationMode,
                        onModeChange = {
                            if (it == NotificationMode.OFF || settingsViewModel.isPermissionGranted(context)) {
                                settingsViewModel.setNotificationMode(it)
                                NotificationScheduler.scheduleNotificationWork(context, it, calculateAnchorTime(it))
                                if (it == NotificationMode.DAILY) {
                                    Toast.makeText(
                                        context,
                                        notificationToastText,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                settingsViewModel.setShowNotificationPermissionPopUp(true)
                            }
                        }
                    )

                    // Dark Mode Toggle
                    DarkModeToggleRow(
                        fontSize = fontSize,
                        iconSize = iconSize,
                        currentMode = settingsViewModel.settingsState.collectAsState().value.themeMode,
                        onModeChange = { settingsViewModel.setThemeMode(it) }
                    )

                    // Website Link
                    IntentRow(
                        iconResId = R.drawable.info_24dp,
                        contentDescription = "About",
                        text = stringResource(R.string.about),
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://mental-math.codeberg.page".toUri()
                            )
                        },
                        errorMessage = stringResource(R.string.no_browser)
                    )

                    // Feedback Link
                    IntentRow(
                        iconResId = R.drawable.feedback_24dp,
                        contentDescription = "Feedback",
                        text = stringResource(R.string.feedback),
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(Intent.ACTION_SEND)
                                .apply {
                                    selector = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                                    putExtra(
                                        Intent.EXTRA_EMAIL,
                                        arrayOf("mental_math_feedback@pm.me")
                                    )
                                    putExtra(Intent.EXTRA_SUBJECT, "Mental Math: Feedback")
                                }
                        },
                        errorMessage = stringResource(R.string.no_mail)
                    )

                    // Recommendation Link
                    IntentRow(
                        iconResId = R.drawable.thumb_up_24dp,
                        contentDescription = "Recommend",
                        text = stringResource(R.string.recommend),
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "market://details?id=com.helddertierwelt.mentalmath".toUri()
                            )
                        },
                        errorMessage = stringResource(R.string.no_app_store)
                    )

                    // Donation Link
                    IntentRow(
                        iconResId = R.drawable.volunteer_activism_24dp,
                        contentDescription = "Donate",
                        text = stringResource(R.string.donate),
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://ko-fi.com/mental_math".toUri()
                            )
                        },
                        errorMessage = stringResource(R.string.no_browser)
                    )
                }

                // Clear Data Button
                ButtonRow(
                    iconResId = R.drawable.delete_24dp,
                    contentDescription = "Reset",
                    text = stringResource(R.string.reset_app),
                    fontSize = fontSize,
                    iconSize = iconSize,
                    onClick = { settingsViewModel.setShowResetPopUp(true) },
                    color = red
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}