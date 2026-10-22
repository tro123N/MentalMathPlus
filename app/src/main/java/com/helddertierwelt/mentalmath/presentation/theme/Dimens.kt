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

package com.helddertierwelt.mentalmath.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class Dimens(
    val fonts: FontSizes,
    val icons: IconSizes,
    val components: ComponentSizes,
) {
    companion object {
        fun from(screenHeight: Dp): Dimens {
            fun dp(factor: Float) = (factor * screenHeight.value).dp
            fun sp(factor: Float) = (factor * screenHeight.value).sp

            return Dimens(
                fonts = FontSizes(
                    title = sp(0.031f),
                    actionButton = sp(0.038f),
                    popUpDescription = sp(0.018f),
                    caption = sp(0.015f),
                    settingsLabel = sp(0.024f),
                    sideSheetEntry = sp(0.020f),
                    gameProgress = sp(0.040f),
                    numPadKey = sp(0.040f),
                    problem = sp(0.057f),
                    summaryScore = sp(0.031f),
                    resultText = sp(0.024f),
                    historyEntry = sp(0.015f),
                ),
                icons = IconSizes(
                    topBarAction = dp(0.038f),
                    actionButton = dp(0.057f),
                    popUp = dp(0.1f),
                    modeButton = dp(0.048f),
                    operatorButton = dp(0.043f),
                    menuItem = dp(0.032f),
                    quitGame = dp(0.057f),
                    survivalHeart = dp(0.042f),
                    enterKey = dp(0.052f),
                    backspaceKey = dp(0.038f),
                    summaryScore = dp(0.057f),
                    summaryStat = dp(0.046f),
                    problemStatus = dp(0.032f),
                    cardHeader = dp(0.038f),
                ),
                components = ComponentSizes(
                    button = dp(0.09f),
                    sliderHeight = dp(0.05f),
                    menuLogo = dp(0.11f),
                    gameScreenPadding = dp(0.028f),
                    numPadKey = dp(0.10f),
                    answerLineWidth = dp(0.32f),
                ),
            )
        }
    }
}

@Immutable
data class FontSizes(
    val title: TextUnit,
    val actionButton: TextUnit,
    val popUpDescription: TextUnit,
    val caption: TextUnit,
    val settingsLabel: TextUnit,
    val sideSheetEntry: TextUnit,
    val gameProgress: TextUnit,
    val numPadKey: TextUnit,
    val problem: TextUnit,
    val summaryScore: TextUnit,
    val resultText: TextUnit,
    val historyEntry: TextUnit,
)

@Immutable
data class IconSizes(
    val topBarAction: Dp,
    val actionButton: Dp,
    val popUp: Dp,
    val modeButton: Dp,
    val operatorButton: Dp,
    val menuItem: Dp,
    val quitGame: Dp,
    val survivalHeart: Dp,
    val enterKey: Dp,
    val backspaceKey: Dp,
    val summaryScore: Dp,
    val summaryStat: Dp,
    val problemStatus: Dp,
    val cardHeader: Dp,
)

@Immutable
data class ComponentSizes(
    val button: Dp,
    val sliderHeight: Dp,
    val menuLogo: Dp,
    val gameScreenPadding: Dp,
    val numPadKey: Dp,
    val answerLineWidth: Dp,
)

val LocalDimens = staticCompositionLocalOf<Dimens> { error("No Dimens provided") }
