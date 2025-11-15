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

package com.helddertierwelt.mentalmath.presentation.component.settings.menu

import android.content.Intent
import android.util.Log
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import com.helddertierwelt.mentalmath.presentation.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch
import java.security.SecureRandom

@Composable
fun MenuSideSheet(
    isSheetOpen: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    navController: NavController
) {
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val settingsViewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner)
    val gameViewModel: GameViewModel = hiltViewModel(viewModelStoreOwner)
    val statisticsViewModel: StatisticsViewModel = hiltViewModel(viewModelStoreOwner)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val sheetWidthFactor = 0.6f
    val minOffset = -screenWidthPx * sheetWidthFactor
    val maxOffset = 0f
    val offsetX = remember { Animatable(minOffset) }
    val overlayAlpha = 0.38f * (offsetX.value - minOffset) / (maxOffset - minOffset)

    val fontSize = (0.020f * screenHeight.value).sp
    val iconSize = (0.032f * screenHeight.value).dp
    val logoSize = (0.11f * screenHeight.value).dp
    val scrollState = rememberScrollState()
    var isInteractionEnabled by remember { mutableStateOf(true) }
    var isFirstRender by remember { mutableStateOf(true) }
    val isRandomGameButtonClicked = settingsViewModel.isRandomGameButtonClicked.collectAsState().value

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
                    Spacer(modifier = Modifier.height(16.dp))
                    LogoRow(
                        size = logoSize,
                        imageId = R.drawable.mental_math,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://mental-math.codeberg.page".toUri()
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))


                    // Share Game Link
                    IntentRow(
                        iconResId = R.drawable.swords_24dp,
                        contentDescription = "Share Game",
                        text = "Share Game",
                        fontSize = fontSize,
                        iconSize = iconSize,
                        isEnabled = settingsViewModel.isAnyOperationEnabled(),
                        intentProvider = {
                            Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                var seed: Long
                                do {
                                    seed = SecureRandom().nextLong()
                                } while (seed < 0)
                                val seedLink = settingsViewModel.generateSeedLink(seed)
                                val shareText =
                                    "Let’s play a round Mental Math!\nClick the link below to start a fresh game with my settings and seed:\n%d\n\n%s".format(
                                        seed,
                                        seedLink
                                    )
                                Log.d("SettingsScreen", "Sharing seed game: $shareText")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }, context.getString(R.string.share_score))
                        }
                    )

                    // Random Game
                    ButtonRow(
                        iconResId = R.drawable.casino_24dp,
                        contentDescription = "Random Game",
                        text = "Random Game",
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

                    // Input Layout Toggle
                    InputLayoutToggleRow(
                        fontSize = fontSize,
                        iconSize = iconSize,
                        isNumpad = settingsViewModel.settingsState.collectAsState().value.isNumpadDefault,
                        onClick = { settingsViewModel.toggleLayout() }
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
                        text = "About",
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://mental-math.codeberg.page".toUri()
                            )
                        }
                    )

                    // Source Code Link
                    IntentRow(
                        iconResId = R.drawable.code_24dp,
                        contentDescription = "Source Code",
                        text = "Source Code",
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://codeberg.org/Mental-Math/MentalMath".toUri()
                            )
                        }
                    )

                    // License Link
                    IntentRow(
                        iconResId = R.drawable.balance_24dp,
                        contentDescription = "License",
                        text = "License",
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://codeberg.org/Mental-Math/MentalMath/src/branch/main/LICENSE.md".toUri()
                            )
                        }
                    )

                    // Privacy Policy Link
                    IntentRow(
                        iconResId = R.drawable.domino_mask_24dp,
                        contentDescription = "Privacy Policy",
                        text = "Privacy Policy",
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://codeberg.org/Mental-Math/MentalMath/src/branch/main/PRIVACY_POLICY.md".toUri()
                            )
                        }
                    )

                    // Feedback Link
                    IntentRow(
                        iconResId = R.drawable.feedback_24dp,
                        contentDescription = "Feedback",
                        text = "Feed\u200Bback",
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
                        }
                    )

                    // Recommendation Link
                    IntentRow(
                        iconResId = R.drawable.thumb_up_24dp,
                        contentDescription = "Recommend",
                        text = "Recom\u200Bmend",
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "market://details?id=com.helddertierwelt.mentalmath".toUri()
                            )
                        }
                    )

                    // Donation Link
                    IntentRow(
                        iconResId = R.drawable.volunteer_activism_24dp,
                        contentDescription = "Donate",
                        text = "Donate",
                        fontSize = fontSize,
                        iconSize = iconSize,
                        intentProvider = {
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://ko-fi.com/mental_math".toUri()
                            )
                        }
                    )
                }

                // Clear Data Button
                ButtonRow(
                    iconResId = R.drawable.delete_24dp,
                    contentDescription = "Reset",
                    text = "Reset App",
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