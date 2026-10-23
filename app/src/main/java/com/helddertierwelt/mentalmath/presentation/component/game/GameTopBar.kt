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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.game.GameState
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(
    gameState: GameState,
    onClose: () -> Unit,
) {
    val dimens = MentalMathTheme.dimens

    TopAppBar(
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.close_24dp),
                    contentDescription = "close",
                    modifier = Modifier.size(dimens.icons.quitGame),
                    tint = red
                )
            }
        },
        actions = {
            when (gameState.gameMode) {

                GameMode.PROBLEMS -> {
                    val totalTasks = gameState.modeLimit * 10
                    val answeredTasks = gameState.totalAnswers

                    Text(
                        text = "$answeredTasks/$totalTasks",
                        fontSize = dimens.fonts.gameProgress,
                        modifier = Modifier.padding(end = 24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                GameMode.TIMED -> {
                    CountdownDisplay(
                        time = gameState.modeLimit.toLong() * 60000L,
                        activeTime = gameState.activeTime,
                    )
                }

                GameMode.SURVIVAL -> {
                    SurvivalDisplay(
                        timeLeft = gameState.survivalTimeLeft,
                        totalAnswers = gameState.totalAnswers,
                        lives = gameState.lives,
                    )
                }
            }
        },
    )
}