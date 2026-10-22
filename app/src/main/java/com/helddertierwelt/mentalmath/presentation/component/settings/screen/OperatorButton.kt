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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme

@Composable
fun OperatorButton(
    icon: Int,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val dimens = MentalMathTheme.dimens

    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(dimens.components.button),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "Mode",
            modifier = Modifier.size(dimens.icons.operatorButton),
            tint = if (isSelected) textColor else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        )
    }
}