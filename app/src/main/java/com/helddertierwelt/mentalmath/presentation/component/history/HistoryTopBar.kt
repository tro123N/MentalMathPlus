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

package com.helddertierwelt.mentalmath.presentation.component.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTopBar(
    titleId: Int,
    titleFontSize: TextUnit,
    iconSize: Dp,
    onClick: (SortMode) -> Unit,
    currentSortMode: SortMode
) {
    val modeIconPairs = listOf(
        R.drawable.calendar_clock_24dp to R.drawable.arrow_downward_24dp,
        R.drawable.calendar_clock_24dp to R.drawable.arrow_upward_24dp,
        R.drawable.sort_by_alpha_24dp to R.drawable.arrow_downward_24dp,
        R.drawable.sort_by_alpha_24dp to R.drawable.arrow_upward_24dp,
    )
    val modes = listOf(SortMode.TIME_DESCENDING, SortMode.TIME_ASCENDING, SortMode.ALPHA_DESCENDING, SortMode.ALPHA_ASCENDING)
    val currentIndex = modes.indexOf(currentSortMode).coerceAtLeast(0)

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = stringResource(id = titleId),
                    fontSize = titleFontSize
                )
            }
        },
        actions = {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val nextIndex = (currentIndex + 1) % modes.size
                        onClick(modes[nextIndex]) },
            ){
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(modeIconPairs[currentIndex].first),
                    contentDescription = "Share",
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Icon(
                    painter = painterResource(modeIconPairs[currentIndex].second),
                    contentDescription = "Share",
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    )
}