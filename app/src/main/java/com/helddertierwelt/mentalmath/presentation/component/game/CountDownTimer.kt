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

package com.helddertierwelt.mentalmath.presentation.component.game

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import kotlin.math.ceil

@Composable
fun CountdownTimer(
    fontSize: TextUnit,
    navController: NavController,
    time: Long,
) {

    val activity = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(activity)
    var timeLeft = time - gameViewModel.gamesState.value.activeTime
    var formattedTime by remember { mutableStateOf(formatTime(timeLeft)) }

    LaunchedEffect(timeLeft) {
        formattedTime = formatTime(timeLeft)
        if (timeLeft <= 0) {
            gameViewModel.endGame()
            navController.navigate("statistics") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Text(
        text = formattedTime,
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = fontSize,
        modifier = Modifier.padding(end = 16.dp)
    )
}

fun formatTime(milliseconds: Long): String {
    val seconds = ceil(milliseconds / 1000.0).toLong()
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return buildString {
        if (minutes > 0) {
            append("$minutes:")
            append(remainingSeconds.toString().padStart(2, '0'))
        } else {
            append("$remainingSeconds")
        }
    }
}