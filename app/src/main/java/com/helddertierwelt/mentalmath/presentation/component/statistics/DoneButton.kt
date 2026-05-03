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

package com.helddertierwelt.mentalmath.presentation.component.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.green

@Composable
fun DoneButton(
    buttonTextId: Int,
    onClick: () -> Unit,
    iconSize: Dp,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier
) {

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(size)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                painter = painterResource(R.drawable.check_24dp),
                contentDescription = "Done",
                modifier = Modifier.size(iconSize),
                tint = green
            )
            Text(
                text = stringResource(buttonTextId),
                fontSize = fontSize,
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}