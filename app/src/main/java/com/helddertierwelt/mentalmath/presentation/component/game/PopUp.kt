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

package com.helddertierwelt.mentalmath.presentation.component.game


import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

@Composable
fun PopUp(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null,
    titleSize: TextUnit,
    descriptionSize: TextUnit,
    title: String,
    description: String,
    confirmationText: String,
    cancelText: String? = null,
    dismissColor: Color? = null,
    confirmColor: Color,
    icon: @Composable (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                fontSize = titleSize,
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        text = {
            Text(
                text = description,
                fontSize = descriptionSize,
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                onClick = { onConfirm() }
            ) {
                Text(
                    text = confirmationText,
                    fontSize = descriptionSize
                )
            }
        },
        dismissButton = if (onCancel != null && cancelText != null && dismissColor != null) {
            {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dismissColor,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    onClick = { onCancel() }
                ) {
                    Text(
                        text = cancelText,
                        fontSize = descriptionSize
                    )
                }
            }
        } else null,
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onPrimary,
        icon = icon
    )
}