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
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun SettingsOperatorColumn(
    operatorId: Int,
    difficultyRange: Pair<Int, Int>,
    operatorSize: TextUnit,
    fontSize: TextUnit,
    operatorColor: Color,
) {

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var range = "-"
        if (difficultyRange.first > 0 && difficultyRange.second > 0) {
            range = if (difficultyRange.first == difficultyRange.second) {
                difficultyRange.first.toString()
            } else {
                "%s-%s".format(difficultyRange.first, difficultyRange.second)
            }
        }
        Text(
            text = stringResource(operatorId),
            fontSize = operatorSize,
            color = operatorColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = range,
            fontSize = fontSize,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}