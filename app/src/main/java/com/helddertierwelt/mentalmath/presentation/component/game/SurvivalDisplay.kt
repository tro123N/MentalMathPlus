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

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SurvivalDisplay(
    fontSize: TextUnit,
    heartIconSize: Dp,
) {
    val viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    val gameViewModel: GameViewModel = hiltViewModel(viewModelStoreOwner)
    val gameState by gameViewModel.gameState.collectAsState()
    val timeLeft = gameViewModel.getSurvivalTimeLeft()
    var highlightTimer by remember { mutableStateOf(false) }

    LaunchedEffect(gameState.totalAnswers) {
        if (gameState.totalAnswers > 0 && gameState.totalAnswers % 5 == 0) {
            highlightTimer = true
            delay(800.milliseconds)
            highlightTimer = false
        }
    }

    val timerColor by animateColorAsState(
        targetValue = if (highlightTimer) { yellow } else { MaterialTheme.colorScheme.onPrimary },
        animationSpec = tween(150),
        label = "timerColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(end = 24.dp)
    ) {
        Text(
            text = formatTime(timeLeft),
            fontSize = fontSize,
            color = timerColor
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(3) { index ->
                Icon(
                    painter = painterResource(
                        if (index >= 3 - gameState.lives)
                            R.drawable.heart_24dp
                        else
                            R.drawable.heart_outlined_24dp
                    ),
                    contentDescription = "Lives",
                    modifier = Modifier.size(heartIconSize),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}