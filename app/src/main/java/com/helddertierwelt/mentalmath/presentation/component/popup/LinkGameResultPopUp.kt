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

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.result.GameResultInfo
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.green

@Composable
fun LinkGameResultPopUp(
    gameResultInfo: GameResultInfo,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val dimens = MentalMathTheme.dimens
    PopUp(
        title = gameResultInfo.title,
        description = gameResultInfo.description,
        confirmationText = stringResource(R.string.result_popup_confirmation_text),
        cancelText = stringResource(R.string.ok),
        icon = {
            Icon(
                painter = painterResource(gameResultInfo.icon),
                contentDescription = "challenge result",
                tint = gameResultInfo.color,
                modifier = Modifier.size(dimens.icons.popUp)
            )
        },
        confirmColor = green,
        dismissColor = green,
        onDismissRequest = onDismissRequest,
        onCancel = onCancel,
        onConfirm = onConfirm
    )
}
