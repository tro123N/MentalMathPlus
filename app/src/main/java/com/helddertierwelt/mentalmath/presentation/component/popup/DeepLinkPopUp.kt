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

package com.helddertierwelt.mentalmath.presentation.component.popup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red

@Composable
fun DeepLinkPopUp(
    linkScore: Float,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    PopUp(
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        onCancel = onCancel,
        title = stringResource(R.string.app_link_game_popup_title),
        description = if (linkScore < 0f) {
            stringResource(R.string.app_link_popup_description_new_game)
        } else {
            stringResource(R.string.app_link_popup_description_challenge)
        },
        confirmationText = stringResource(R.string.app_link_popup_confirmation_text),
        cancelText = stringResource(R.string.cancel),
        dismissColor = red,
        confirmColor = green
    )
}
