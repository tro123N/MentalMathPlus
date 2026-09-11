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

package com.helddertierwelt.mentalmath.presentation.component.settings.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R

@Composable
fun ExpandableMenuGroup(
    title: String,
    fontSize: TextUnit,
    iconResId: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
    iconSize: Dp,
    color: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onToggle)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = color
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                fontSize = fontSize,
                lineHeight = fontSize,
                color = color,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(if (isExpanded) R.drawable.keyboard_arrow_up_24dp else R.drawable.keyboard_arrow_down_24dp),
                contentDescription = "isExpanded",
                modifier = Modifier.size(iconSize),
                tint = color
            )
        }
    }

    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}