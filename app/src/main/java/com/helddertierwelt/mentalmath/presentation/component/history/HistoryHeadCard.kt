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

package com.helddertierwelt.mentalmath.presentation.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

@Composable
fun HistoryHeadCard(
) {
    val dimens = MentalMathTheme.dimens
    Card(
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                painter = painterResource(R.drawable.tune_24dp),
                contentDescription = "modeIcon",
                modifier = Modifier.size(dimens.icons.cardHeader),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Icon(
                painter = painterResource(R.drawable.addition_24dp),
                contentDescription = "modeIcon",
                modifier = Modifier.size(dimens.icons.cardHeader),
                tint = green
            )
            Icon(
                painter = painterResource(R.drawable.subtraction_24dp),
                contentDescription = "modeIcon",
                modifier = Modifier.size(dimens.icons.cardHeader),
                tint = red
            )
            Icon(
                painter = painterResource(R.drawable.multiplication_24dp),
                contentDescription = "modeIcon",
                modifier = Modifier.size(dimens.icons.cardHeader),
                tint = yellow
            )
            Icon(
                painter = painterResource(R.drawable.division_24dp),
                contentDescription = "modeIcon",
                modifier = Modifier.size(dimens.icons.cardHeader),
                tint = blue
            )
            Icon(
                painter = painterResource(R.drawable.star_24dp),
                contentDescription = "modeIcon",
                modifier = Modifier.size(dimens.icons.cardHeader),
                tint = yellow
            )
        }
    }
}