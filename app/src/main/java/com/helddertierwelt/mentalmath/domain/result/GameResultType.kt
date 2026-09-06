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

package com.helddertierwelt.mentalmath.domain.result

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.settings.AppLocale
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.presentation.component.result.summary.formatScore
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

enum class GameResultType(
    val icon: Int,
    val color: Color,
    @StringRes val title: Int,
    val descriptions: List<Int>,
    val sound: FeedbackType
) {
    WAITING(
        icon = R.drawable.hourglass_bottom_24dp,
        color = blue,
        title = R.string.waiting_title,
        descriptions = GameResultDescriptions.waiting,
        sound = FeedbackType.NEUTRAL
    ),
    EASY_WIN(
        icon = R.drawable.favorite_24dp,
        color = red,
        title = R.string.easy_win_title,
        descriptions = GameResultDescriptions.easyWin,
        sound = FeedbackType.GOOD
    ),
    ANYONE_HERE(
        icon = R.drawable.question_mark_24dp,
        color = blue,
        title = R.string.anyone_here_title,
        descriptions = GameResultDescriptions.anyoneHere,
        sound = FeedbackType.NEUTRAL
    ),
    WIN(
        icon = R.drawable.trophy_24dp,
        color = yellow,
        title = R.string.win_title,
        descriptions = GameResultDescriptions.win,
        sound = FeedbackType.BEST
    ),
    CLOSE_WIN(
        icon = R.drawable.trophy_24dp,
        color = yellow,
        title = R.string.close_win_title,
        descriptions = GameResultDescriptions.closeWin,
        sound = FeedbackType.BEST
    ),
    DRAW(
        icon = R.drawable.partner_heart_24dp,
        color = green,
        title = R.string.draw_title,
        descriptions = GameResultDescriptions.draw,
        sound = FeedbackType.OKAY
    ),
    CLOSE_DEFEAT(
        icon = R.drawable.rainy_24dp,
        color = blue,
        title = R.string.close_defeat_title,
        descriptions = GameResultDescriptions.closeDefeat,
        sound = FeedbackType.BAD
    ),
    DEFEAT(
        icon = R.drawable.rainy_24dp,
        color = blue,
        title = R.string.defeat_title,
        descriptions = GameResultDescriptions.defeat,
        sound = FeedbackType.BAD
    );

    @SuppressLint("ResourceType")
    @Composable
    fun getRandomDescription(
        currentScore: Float,
        linkScore: Float,
        seed: Long
    ): String {

        val template = stringResource(descriptions.random())
        return if (linkScore < 0f) {
            AppLocale.format(template, formatScore(currentScore), seed)
        } else {
            AppLocale.format(template, formatScore(currentScore), formatScore(linkScore), seed)
        }
    }
}