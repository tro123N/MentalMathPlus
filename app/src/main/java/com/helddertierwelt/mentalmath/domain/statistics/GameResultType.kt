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

package com.helddertierwelt.mentalmath.domain.statistics

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.domain.settings.AppLocale

enum class GameResultType(
    val icon: Int,
    val color: Color,
    @param:StringRes val title: Int,
    @param:StringRes val descriptions: Int
) {
    WAITING(
        icon = R.drawable.hourglass_bottom_24dp,
        color = blue,
        title = R.string.waiting_title,
        descriptions = R.array.waiting_descriptions
    ),
    EASY_WIN(
        icon = R.drawable.favorite_24dp,
        color = red,
        title = R.string.easy_win_title,
        descriptions = R.array.easy_win_descriptions
    ),
    ANYONE_HERE(
        icon = R.drawable.question_mark_24dp,
        color = blue,
        title = R.string.anyone_here_title,
        descriptions = R.array.anyone_here_descriptions
    ),
    WIN(
        icon = R.drawable.trophy_24dp,
        color = yellow,
        title = R.string.win_title,
        descriptions = R.array.win_descriptions
    ),
    CLOSE_WIN(
        icon = R.drawable.trophy_24dp,
        color = yellow,
        title = R.string.close_win_title,
        descriptions = R.array.close_win_descriptions
    ),
    DRAW(
        icon = R.drawable.partner_heart_24dp,
        color = green,
        title = R.string.draw_title,
        descriptions = R.array.draw_descriptions
    ),
    CLOSE_DEFEAT(
        icon = R.drawable.rainy_24dp,
        color = blue,
        title = R.string.close_defeat_title,
        descriptions = R.array.close_defeat_descriptions
    ),
    DEFEAT(
        icon = R.drawable.rainy_24dp,
        color = blue,
        title = R.string.defeat_title,
        descriptions = R.array.defeat_descriptions
    );

    @SuppressLint("ResourceType")
    @Composable
    fun getRandomDescription(
        currentScore: Float,
        linkScore: Float,
        seed: Long
    ): String {

        val templates = stringArrayResource(id = descriptions)
        val template = templates.random()

        return if (linkScore < 0f) {
            AppLocale.format(template, currentScore, seed)
        } else {
            AppLocale.format(template, currentScore, linkScore, seed)
        }
    }
}