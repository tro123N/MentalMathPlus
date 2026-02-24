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

package com.helddertierwelt.mentalmath101.presentation.component.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath101.R
import java.util.Locale

@Composable
fun StatsProgressColumn(
    modeEnabled: Boolean,
    fontSize: TextUnit,
    iconSize: Dp,
    totalAnswers: Int,
    activeTime: Long,
) {

    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var iconId = R.drawable.timer_24dp
        var progressText = formatTime(activeTime)
        if (!modeEnabled) {
            iconId = R.drawable.tag_24dp
            progressText = "%d".format(Locale.ENGLISH,totalAnswers)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            painter = painterResource(iconId),
            contentDescription = "progressIcon",
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = progressText,
            fontSize = fontSize,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return buildString {
        if (hours > 0) {
            append("$hours:")
            append(minutes.toString().padStart(2, '0'))
            append(":")
            append(remainingSeconds.toString().padStart(2, '0'))
        } else if (minutes > 0) {
            append("$minutes:")
            append(remainingSeconds.toString().padStart(2, '0'))
        } else {
            append("${remainingSeconds}s")
        }
    }
}