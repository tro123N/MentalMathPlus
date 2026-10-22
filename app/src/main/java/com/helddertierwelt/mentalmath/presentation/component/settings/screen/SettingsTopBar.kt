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

package com.helddertierwelt.mentalmath.presentation.component.settings.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    onMenuClick: () -> Unit,
    onScoreClick: () -> Unit,
    scoreEnabled: Boolean,
    isGreyedOut: Boolean,
) {
    val dimens = MentalMathTheme.dimens
    TopAppBar(
        title = {
            Text(text = stringResource(R.string.app_name), fontSize = dimens.fonts.title)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Icon(
                    painterResource(R.drawable.menu_24dp),
                    contentDescription = "menu",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(dimens.icons.topBarAction),
                )
            }
        },
        actions = {
            IconButton(
                enabled = scoreEnabled,
                onClick = onScoreClick,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(
                    painterResource(R.drawable.leaderboard_24dp),
                    contentDescription = "share",
                    tint = if (!isGreyedOut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                    modifier = Modifier.size(dimens.icons.topBarAction),
                )
            }
        }
    )
}