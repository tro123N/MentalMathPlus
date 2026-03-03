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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R

@Composable
fun NotificationToggleRow(
    fontSize: TextUnit,
    iconSize: Dp,
    currentMode: NotificationMode,
    onModeChange: (NotificationMode) -> Unit
) {

    val icons = listOf(
        R.drawable.notifications_off_24dp,
        R.drawable.notifications_active_24dp,
        R.drawable.notifications_active_24dp,
        R.drawable.notifications_active_24dp,
    )
    val texts = listOf(
        R.string.reminder_off,
        R.string.daily_reminder,
        R.string.every_three_days,
        R.string.weekly_reminder,
    )
    val modes = listOf(NotificationMode.OFF, NotificationMode.DAILY, NotificationMode.THREEDAILY, NotificationMode.WEEKLY)
    val currentIndex = modes.indexOf(currentMode).coerceAtLeast(0)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                val nextIndex = (currentIndex + 1) % modes.size
                onModeChange(modes[nextIndex])
            }
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            painter = painterResource(icons[currentIndex]),
            contentDescription = stringResource(texts[currentIndex]),
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(texts[currentIndex]),
            fontSize = fontSize,
            lineHeight = fontSize
        )
    }
}