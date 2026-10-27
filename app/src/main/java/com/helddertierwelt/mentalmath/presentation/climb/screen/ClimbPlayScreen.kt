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

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.Phase
import com.helddertierwelt.mentalmath.domain.climb.engine.Session
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.AnswerField
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.ChoiceButtons
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbKeypad
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.FlashFeedback
import com.helddertierwelt.mentalmath.presentation.climb.component.GuidedSteps
import com.helddertierwelt.mentalmath.presentation.climb.component.PillButton
import com.helddertierwelt.mentalmath.presentation.climb.component.ProblemDisplay
import com.helddertierwelt.mentalmath.presentation.climb.component.SolutionSteps
import com.helddertierwelt.mentalmath.presentation.component.popup.QuitGamePopUp
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel

private fun typeLabel(type: SessionType): String = when (type) {
    SessionType.GUIDED -> "Geführt"
    SessionType.PRACTICE -> "Üben"
    SessionType.TEST -> "Prüfung"
    SessionType.REFRESH -> "Auffrischen"
    SessionType.REVIEW -> "Wiederholung"
}

/** Die laufende Übungseinheit: Aufgabe, Eingabe, Rückmeldung. */
@Composable
fun ClimbPlayScreen(navController: NavController) {
    val dimens = MentalMathTheme.dimens
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val settingsViewModel: SettingsViewModel = hiltViewModel(owner)
    val settings by settingsViewModel.settingsState.collectAsState()
    val sessionOrNull by viewModel.session.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val elapsed by viewModel.elapsedMs.collectAsState()
    var showQuit by remember { mutableStateOf(false) }

    fun leave() {
        viewModel.quitSession()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }

    LaunchedEffect(sessionOrNull?.phase, summary) {
        val s = sessionOrNull
        if (s != null && s.phase == Phase.FINISHED && summary != null) {
            navController.navigate("climbresult") {
                popUpTo("climbplay") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.session.value == null) leave() else showQuit = true
            }
        }
        backDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    val session = sessionOrNull
    if (session == null) {
        // Keine laufende Einheit (z. B. nach einem Neustart der App): nur einen Ausweg anbieten.
        Scaffold(topBar = { ClimbTopBar(title = "Kletterweg", onNavigation = { leave() }) }) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                BodyText("Hier läuft gerade keine Übung.")
                Spacer(Modifier.height(16.dp))
                com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton(
                    text = "Zum Kletterweg",
                    icon = R.drawable.terrain_24dp,
                    iconTint = green,
                    onClick = {
                        navController.navigate("climb") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                ClimbTopBar(
                    title = "${typeLabel(session.type)} · ${session.title}",
                    navigationIcon = R.drawable.close_24dp,
                    navigationTint = red,
                    navigationDescription = "Übung beenden",
                    onNavigation = { showQuit = true },
                    actions = {
                        Text(
                            text = "${minOf(session.done + 1, session.total)}/${session.total}",
                            fontSize = dimens.fonts.settingsLabel,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(end = 20.dp),
                        )
                    },
                )
                val limit = session.timeLimitMs
                val progressValue = if (limit != null) {
                    (elapsed.toFloat() / limit).coerceIn(0f, 1f)
                } else {
                    session.done.toFloat() / session.total.coerceAtLeast(1)
                }
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (limit != null && progressValue > 0.75f) red else blue,
                    trackColor = MaterialTheme.colorScheme.background,
                    drawStopIndicator = {},
                    strokeCap = StrokeCap.Butt,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = dimens.components.gameScreenPadding, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    session.problem?.let { problem ->
                        ProblemDisplay(problem = problem)
                        Spacer(Modifier.height(16.dp))
                        if (session.isGuided) {
                            GuidedSteps(session = session)
                        } else if (problem.input != InputKind.CHOICE) {
                            AnswerField(session = session, unit = problem.prompt.unit)
                        }
                        Spacer(Modifier.height(12.dp))
                        FlashFeedback(session = session)
                        if (session.phase == Phase.REVEAL && !session.isGuided) {
                            RevealCard(session)
                        }
                        session.feedback?.note?.let { note ->
                            if (session.phase == Phase.ANSWERING && session.isGuided) {
                                BodyText(note, color = yellow)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            BottomControls(session = session, viewModel = viewModel, inputMode = settings.inputMode)
        }
    }

    if (showQuit) {
        QuitGamePopUp(
            onDismissRequest = { showQuit = false },
            onConfirm = {
                showQuit = false
                leave()
            },
            onCancel = {
                viewModel.playTap()
                showQuit = false
            },
        )
    }
}

@Composable
private fun RevealCard(session: Session) {
    val problem = session.problem ?: return
    val feedback = session.feedback
    ClimbCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.close_24dp),
                contentDescription = null,
                tint = red,
                modifier = Modifier.size(MentalMathTheme.dimens.icons.problemStatus),
            )
            Spacer(Modifier.size(8.dp))
            BodyText("Richtig wäre ${feedback?.correctAnswer ?: ""}")
        }
        feedback?.note?.let { BodyText(it, color = yellow) }
        Spacer(Modifier.height(8.dp))
        SolutionSteps(problem = problem, showHints = true)
    }
}

@Composable
private fun BottomControls(
    session: Session,
    viewModel: ClimbViewModel,
    inputMode: com.helddertierwelt.mentalmath.domain.settings.enums.InputMode,
) {
    val problem = session.problem ?: return
    when {
        session.phase == Phase.REVEAL -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton(
                    text = "Weiter",
                    icon = R.drawable.keyboard_tab_24dp,
                    iconTint = green,
                    onClick = { viewModel.onContinue() },
                )
            }
        }

        session.inputKind == InputKind.CHOICE -> {
            ChoiceButtons(
                choices = problem.choices,
                enabled = session.phase == Phase.ANSWERING,
                onChoose = { viewModel.onChoice(it) },
            )
        }

        else -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (session.isGuided) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(
                            text = if (session.showHint) "Tipp aus" else "Tipp",
                            onClick = { viewModel.onToggleHint() },
                            color = yellow,
                            enabled = session.phase == Phase.ANSWERING,
                        )
                        PillButton(
                            text = "Schritt zeigen",
                            onClick = { viewModel.onRevealStep() },
                            color = blue,
                            enabled = session.phase == Phase.ANSWERING,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                ClimbKeypad(
                    inputKind = session.inputKind,
                    inputMode = inputMode,
                    inputEmpty = session.input.isEmpty(),
                    enabled = session.phase == Phase.ANSWERING,
                    onKey = { viewModel.onKey(it) },
                    onBackspace = { viewModel.onBackspace() },
                    onEnter = { viewModel.onSubmit() },
                )
            }
        }
    }
}
