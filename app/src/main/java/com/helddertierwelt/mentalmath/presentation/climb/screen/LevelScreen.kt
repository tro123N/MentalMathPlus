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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.Evaluation
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.StarRow
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.yellow

/** Übersicht eines Levels: Methode, Ziele, Stand und die vier Stufen Lernen → Geführt → Üben → Prüfung. */
@Composable
fun LevelScreen(navController: NavController, skillId: String) {
    val dimens = MentalMathTheme.dimens
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val progress by viewModel.progress.collectAsState()
    val skill = viewModel.skill(skillId)

    fun back() {
        viewModel.playTap()
        if (!navController.popBackStack()) {
            navController.navigate("climb") { launchSingleTop = true }
        }
    }

    BackHandler { back() }

    if (skill == null) {
        Scaffold(topBar = { ClimbTopBar(title = "Level", onNavigation = { back() }) }) { padding ->
            Column(Modifier.padding(padding).padding(24.dp)) { BodyText("Dieses Level gibt es nicht.") }
        }
        return
    }

    val sp = progress.of(skill.id)
    val unlocked = viewModel.isUnlocked(progress, skill.id)
    val blockers = viewModel.blockers(progress, skill.id)
    val goals = skill.goals
    val mistakes = Evaluation.allowedMistakes(goals)

    fun start(type: SessionType) {
        if (viewModel.startSession(type, skill.id)) navController.navigate("climbplay")
    }

    Scaffold(
        topBar = { ClimbTopBar(title = skill.title, onNavigation = { back() }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("levelColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            ClimbCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        CardTitle(skill.method)
                        CaptionText("Im Buch: ${skill.book.label}")
                    }
                    StarRow(stars = sp.stars, size = dimens.icons.cardHeader)
                }
                Spacer(Modifier.height(10.dp))
                BodyText(skill.summary)
            }
            CardGap()

            if (sp.mastered && sp.needsAttention) {
                ClimbCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.refresh_24dp),
                            contentDescription = null,
                            tint = yellow,
                            modifier = Modifier.size(dimens.icons.menuItem),
                        )
                        Spacer(Modifier.width(12.dp))
                        BodyText(
                            if (sp.refreshPending) {
                                "Ein Level, das hierauf aufbaut, hat nicht geklappt. Frische dieses Level zuerst auf – danach geht es dort weiter."
                            } else {
                                "Dieses Level ist etwas eingerostet. Eine kurze Auffrischung bringt es zurück."
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ActionButton(
                        text = "Auffrischen",
                        subtitle = "${com.helddertierwelt.mentalmath.domain.climb.engine.Progression.REFRESH_SIZE} Aufgaben, Ziel wie bei einem Stern",
                        icon = R.drawable.refresh_24dp,
                        iconTint = yellow,
                        onClick = { start(SessionType.REFRESH) },
                    )
                }
                CardGap()
            }

            ClimbCard {
                CardTitle("Ziele der Prüfung")
                BodyText(
                    "${goals.testSize} Aufgaben, " +
                        (if (mistakes == 0) "ohne Fehler" else if (mistakes == 1) "höchstens 1 Fehler" else "höchstens $mistakes Fehler") +
                        ", höchstens ${secondsText(goals.timeLimit)} pro Aufgabe.",
                )
                Spacer(Modifier.height(8.dp))
                listOf(1 to goals.star1, 2 to goals.star2, 3 to goals.star3).forEach { (stars, sec) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp),
                    ) {
                        StarRow(stars = stars, size = dimens.icons.problemStatus * 0.75f)
                        Spacer(Modifier.width(12.dp))
                        BodyText("im Schnitt höchstens ${secondsText(sec)}")
                    }
                }
                if (sp.testsTaken > 0) {
                    Spacer(Modifier.height(8.dp))
                    val best = if (sp.bestAvgMs > 0) " · Bestzeit ${secondsText(sp.bestAvgMs / 1000.0)}" else ""
                    CaptionText("Prüfungen: ${sp.testsPassed} von ${sp.testsTaken} bestanden$best")
                }
            }
            CardGap()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(
                    text = "1. Methode lernen",
                    subtitle = "Erklärung mit Beispiel",
                    icon = R.drawable.book_24dp,
                    iconTint = blue,
                    enabled = unlocked,
                    onClick = {
                        viewModel.playTap()
                        navController.navigate("learn/${skill.id}")
                    },
                )
                ActionButton(
                    text = "2. Geführt üben",
                    subtitle = "${skill.guidedCount} Aufgaben, Zwischenschritte eintippen",
                    icon = R.drawable.list_numbered_24dp,
                    iconTint = blue,
                    enabled = unlocked,
                    onClick = { start(SessionType.GUIDED) },
                )
                ActionButton(
                    text = "3. Frei üben",
                    subtitle = "${skill.practiceCount} Aufgaben, Lösungsweg bei Fehlern",
                    icon = R.drawable.play_arrow_24dp,
                    iconTint = green,
                    enabled = unlocked,
                    onClick = { start(SessionType.PRACTICE) },
                )
                val blockerText = blockers.firstOrNull()?.let { "Erst „${it.title}“ auffrischen" }
                ActionButton(
                    text = "4. Prüfung",
                    subtitle = blockerText ?: "Auf Zeit – entscheidet über die Sterne",
                    icon = if (blockerText != null) R.drawable.lock_24dp else R.drawable.trophy_24dp,
                    iconTint = yellow,
                    enabled = unlocked && blockers.isEmpty(),
                    onClick = { start(SessionType.TEST) },
                )
                if (blockers.isNotEmpty()) {
                    val first = blockers.first()
                    ActionButton(
                        text = "Zur Grundlage: ${first.title}",
                        subtitle = "Dort auffrischen, dann zurückkommen",
                        icon = R.drawable.refresh_24dp,
                        iconTint = yellow,
                        onClick = {
                            viewModel.playTap()
                            navController.navigate("level/${first.id}")
                        },
                    )
                }
            }
            if (!unlocked) {
                Spacer(Modifier.height(12.dp))
                CaptionText("Dieses Level wird frei, sobald du das Level davor mit mindestens einem Stern geschafft hast.")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Sekunden lesbar, z. B. „9 s“ oder „1:30 min“. */
fun secondsText(seconds: Double): String {
    val total = kotlin.math.round(seconds * 10) / 10.0
    return if (total >= 60) {
        val s = total.toInt()
        "${s / 60}:${(s % 60).toString().padStart(2, '0')} min"
    } else if (total == kotlin.math.floor(total)) {
        "${total.toInt()} s"
    } else {
        String.format(java.util.Locale.GERMANY, "%.1f s", total)
    }
}
