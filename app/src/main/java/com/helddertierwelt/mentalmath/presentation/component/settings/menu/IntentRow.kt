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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme

@Composable
fun IntentRow(
    @DrawableRes iconResId: Int,
    contentDescription: String,
    text: String,
    intentProvider: () -> Intent,
    onClickSound: () -> Unit,
    errorMessage: String,
    color: Color = MaterialTheme.colorScheme.onPrimary,
    isEnabled: Boolean = true
) {
    val dimens = MentalMathTheme.dimens
    val context = LocalContext.current

    ActionRow(
        iconResId = iconResId,
        contentDescription = contentDescription,
        text = text,
        color = color,
        isEnabled = isEnabled
    ) {
        onClickSound()
        try {
            context.startActivity(intentProvider())
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                errorMessage,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
