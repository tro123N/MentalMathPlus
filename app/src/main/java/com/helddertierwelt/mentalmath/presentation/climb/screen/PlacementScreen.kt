/*
Mental Math Plus - Kletterweg zum Kopfrechnen
Copyright (C) 2026 tro123N
Based on Mental Math, Copyright (C) 2025-2026 HeldDerTierwelt

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

package com.helddertierwelt.mentalmath.presentation.climb.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.Placement
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import kotlin.math.ceil

/** Einstufungstest: eine Welt wählen, die man schon kann, und sie mit einem Test überspringen. */
@Composable
fun PlacementScreen(navController: NavController) {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val progress by viewModel.progress.collectAsState()
    val candidates = Placement.candidates(viewModel.worlds, progress)
    val recommended = Placement.recommended(viewModel.worlds, progress)

    fun back() {
        viewModel.playTap()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }
    BackHandler { back() }

    Scaffold(topBar = { ClimbTopBar(title = "Einstufungstest", onNavigation = { back() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("placementColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            ClimbCard {
                CardTitle("Welten überspringen")
                BodyText(
                    "Du kannst schon einiges? Dann musst du nicht jedes Level einzeln klettern. " +
                        "Der Einstufungstest prüft eine ganze Welt auf einmal.",
                )
                Spacer(Modifier.height(10.dp))
                CaptionText("• Aus jedem Level der Welt kommen ${Placement.PER_SKILL} Aufgaben, jede mit dem Zeitlimit ihres Levels.")
                CaptionText("• Bestanden mit höchstens einem Fehler pro zehn Aufgaben und im Schnitt dem Tempo für einen Stern.")
                CaptionText("• Dann gelten alle Levels der Welt als geschafft (ein Stern) und die nächste Welt ist offen.")
                CaptionText("• Die übersprungenen Levels kommen nach ein paar Tagen in die Wiederholung.")
            }
            CardGap()
            if (candidates.isEmpty()) {
                ClimbCard { BodyText("Du hast schon alle Welten geschafft – hier gibt es nichts mehr zu überspringen.") }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    candidates.forEach { world ->
                        val count = Placement.problemCount(world)
                        val minutes = ceil(Placement.estimatedSeconds(world) / 60.0).toInt().coerceAtLeast(1)
                        val done = world.skills.count { progress.of(it.id).mastered }
                        val isRecommended = world.id == recommended?.id
                        val doneText = if (done > 0) " · $done/${world.skills.size} schon geschafft" else ""
                        ActionButton(
                            text = "Welt ${world.number} · ${world.title}",
                            subtitle = (if (isRecommended) "Empfohlen · " else "") + "$count Aufgaben · etwa $minutes min$doneText",
                            icon = R.drawable.keyboard_double_arrow_right_24dp,
                            iconTint = if (isRecommended) green else accent,
                            onClick = {
                                if (viewModel.startPlacement(world.id)) navController.navigate("climbplay")
                            },
                            modifier = Modifier.testTag("place_${world.id}"),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
