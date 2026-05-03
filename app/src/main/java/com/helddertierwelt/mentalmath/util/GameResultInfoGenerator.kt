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

package com.helddertierwelt.mentalmath.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

object GameResultInfoGenerator {

    @Composable
    fun getGamePopUpInfo(
        currentScore: Float,
        linkScore: Float,
        seed: Long
    ): GameResultInfo {
        val type = when {
            linkScore < 0f -> GameResultType.WAITING
            linkScore == 0f && currentScore > 0f -> GameResultType.EASY_WIN
            linkScore == 0f && currentScore <= 0f -> GameResultType.ANYONE_HERE
            currentScore / linkScore > 1.1f -> GameResultType.WIN
            currentScore / linkScore > 1.0f -> GameResultType.CLOSE_WIN
            currentScore / linkScore == 1.0f -> GameResultType.DRAW
            currentScore / linkScore >= 0.9f -> GameResultType.CLOSE_DEFEAT
            else -> GameResultType.DEFEAT
        }

        return GameResultInfo(
            icon = type.icon,
            color = type.color,
            title = stringResource(type.title),
            description = type.getRandomDescription(currentScore, linkScore, seed)
        )
    }
}