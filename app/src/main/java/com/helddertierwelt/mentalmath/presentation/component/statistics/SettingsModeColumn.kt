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

package com.helddertierwelt.mentalmath.presentation.component.statistics

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
import com.helddertierwelt.mentalmath.R
import java.util.Locale

@Composable
fun SettingsModeColumn(
    modeEnabled: Boolean,
    fontSize: TextUnit,
    iconSize: Dp,
    limit: Int
) {

    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var iconId = R.drawable.tag_24dp
        var limitText = "%d".format(Locale.ENGLISH, limit * 10)
        if (!modeEnabled) {
            iconId = R.drawable.hourglass_bottom_24dp
            limitText = "%s:00".format(Locale.ENGLISH, limit)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            painter = painterResource(iconId),
            contentDescription = "modeIcon",
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = limitText,
            fontSize = fontSize,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}