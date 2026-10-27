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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.engine.FreeMode
import com.helddertierwelt.mentalmath.domain.climb.free.FreeTopic
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.FreeConfig
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.AreaIntro
import com.helddertierwelt.mentalmath.presentation.climb.component.PlusBottomBar
import com.helddertierwelt.mentalmath.presentation.climb.component.PlusTab
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.PillButton
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.yellow

private val levelNames = listOf("Leicht", "Mittel", "Schwer")

private fun modeIcon(mode: FreeMode): Int = when (mode) {
    FreeMode.ZEIT -> R.drawable.timer_24dp
    FreeMode.ENDLOS -> R.drawable.play_arrow_24dp
    FreeMode.UEBERLEBEN -> R.drawable.heart_24dp
    FreeMode.PRUEFUNG -> R.drawable.trophy_24dp
}

fun modeDescription(mode: FreeMode, param: Int): String = when (mode) {
    FreeMode.ZEIT -> if (param >= 60 && param % 60 == 0) "${param / 60} min Rechenzeit – so viele richtige wie möglich" else "$param s Rechenzeit"
    FreeMode.ENDLOS -> "Ohne Ende – du hörst auf, wann du willst"
    FreeMode.UEBERLEBEN -> if (param == 1) "Ein Fehler und es ist vorbei" else "$param Leben – jeder Fehler kostet eines"
    FreeMode.PRUEFUNG -> "$param Aufgaben mit Zeitlimit, am Ende eine Note"
}

fun paramLabel(mode: FreeMode, param: Int): String = when (mode) {
    FreeMode.ZEIT -> if (param % 60 == 0) "${param / 60} min" else "$param s"
    FreeMode.UEBERLEBEN -> if (param == 1) "1 Leben" else "$param Leben"
    FreeMode.PRUEFUNG -> "$param Aufgaben"
    FreeMode.ENDLOS -> ""
}

/** Kurze Beschriftung für die Auswahlknöpfe. */
private fun shortParamLabel(mode: FreeMode, param: Int): String = when (mode) {
    FreeMode.ZEIT -> "${param / 60} min"
    FreeMode.UEBERLEBEN -> "♥ $param"
    FreeMode.PRUEFUNG -> "$param"
    FreeMode.ENDLOS -> ""
}

/** Freies Training: Aufgabenarten, Schwierigkeit und Modus wählen, Statistik ansehen. */
@Composable
fun FreeSetupScreen(navController: NavController) {
    val dimens = MentalMathTheme.dimens
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val config by viewModel.freeConfig.collectAsState()
    val progress by viewModel.progress.collectAsState()

    fun back() {
        viewModel.playTap()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }
    BackHandler { back() }

    fun update(c: FreeConfig) {
        viewModel.playTap()
        viewModel.setFreeConfig(c)
    }

    Scaffold(
        topBar = { ClimbTopBar(title = "Freies Training", navigationIcon = null) },
        bottomBar = { PlusBottomBar(navController, PlusTab.FREE, onTap = { viewModel.playTap() }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("freeColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            AreaIntro(
                icon = R.drawable.tune_24dp,
                title = "Üben ohne Plan",
                text = "Aufgabenarten und Modus frei wählen. Zählt nicht für den Kletterweg – hier gibt es eigene Bestwerte.",
            )
            CardGap()
            ClimbCard {
                CardTitle("Aufgabenarten")
                CaptionText("Mehrere auswählen, dann wird gemischt.")
                Spacer(Modifier.height(6.dp))
                FreeTopic.entries.forEach { topic ->
                    val selected = topic in config.topics
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.components.cornerRadius))
                            .clickable {
                                val topics = if (selected) config.topics - topic else config.topics + topic
                                update(config.copy(topics = topics))
                            }
                            .testTag("topic_${topic.name}")
                            .padding(vertical = 6.dp),
                    ) {
                        Icon(
                            painter = painterResource(if (selected) R.drawable.check_circle_24dp else R.drawable.check_24dp),
                            contentDescription = null,
                            tint = if (selected) green else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                            modifier = Modifier.size(dimens.icons.menuItem),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(topic.title, fontSize = dimens.fonts.sideSheetEntry, color = MaterialTheme.colorScheme.onPrimary)
                            CaptionText(topic.subtitle)
                        }
                    }
                }
            }
            CardGap()
            ClimbCard {
                CardTitle("Schwierigkeit")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    levelNames.forEachIndexed { i, name ->
                        val level = i + 1
                        PillButton(
                            text = name,
                            onClick = { update(config.copy(level = level)) },
                            color = if (config.level == level) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            CardGap()
            ClimbCard {
                CardTitle("Modus")
                FreeMode.entries.forEach { mode ->
                    val selected = config.mode == mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.components.cornerRadius))
                            .background(if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary)
                            .clickable {
                                val param = if (selected) config.param else FreeConfig.options(mode)[if (mode == FreeMode.ENDLOS) 0 else 1.coerceAtMost(FreeConfig.options(mode).lastIndex)]
                                update(config.copy(mode = mode, param = param))
                            }
                            .testTag("mode_${mode.name}")
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(modeIcon(mode)),
                                contentDescription = null,
                                tint = if (selected) yellow else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(dimens.icons.menuItem),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mode.title, fontSize = dimens.fonts.sideSheetEntry, color = MaterialTheme.colorScheme.onPrimary)
                                CaptionText(modeDescription(mode, if (selected) config.param else FreeConfig.options(mode)[0]))
                            }
                        }
                        if (selected && mode != FreeMode.ENDLOS) {
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FreeConfig.options(mode).forEach { option ->
                                    PillButton(
                                        text = shortParamLabel(mode, option),
                                        onClick = { update(config.copy(param = option)) },
                                        color = if (config.param == option) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            CardGap()
            ActionButton(
                text = "Los geht's",
                subtitle = if (config.topics.isEmpty()) "Wähle mindestens eine Aufgabenart" else "${config.mode.title} · ${levelNames[config.level - 1]}",
                icon = R.drawable.play_arrow_24dp,
                iconTint = green,
                enabled = config.topics.isNotEmpty(),
                onClick = { if (viewModel.startFree()) navController.navigate("climbplay") },
            )
            CardGap()
            StatsCard(progress)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun bestLabel(key: String): String? {
    val parts = key.split('_')
    if (parts.size != 3) return null
    val mode = FreeMode.entries.firstOrNull { it.name == parts[0] } ?: return null
    val param = parts[1].toIntOrNull() ?: return null
    val level = parts[2].toIntOrNull()?.takeIf { it in 1..3 } ?: return null
    val p = paramLabel(mode, param)
    return mode.title + (if (p.isNotEmpty()) " $p" else "") + " · " + levelNames[level - 1]
}

@Composable
private fun StatsCard(progress: ClimbProgress) {
    ClimbCard {
        CardTitle("Statistik")
        if (progress.free.isEmpty()) {
            BodyText("Noch keine Aufgaben im freien Training gelöst.")
        }
        val rows = FreeTopic.entries.mapNotNull { topic ->
            progress.free["free." + topic.name.lowercase()]?.takeIf { it.solved > 0 }?.let { topic to it }
        }
        rows.forEach { (topic, stat) ->
            val pct = stat.correct * 100 / stat.solved
            val avg = stat.totalMs / stat.solved / 1000.0
            BodyText(topic.title)
            CaptionText("${Fmt.int(stat.solved.toLong())} gelöst · $pct % richtig · Ø ${secondsText(avg)}")
            Spacer(Modifier.height(6.dp))
        }
        val bests = progress.freeBest.mapNotNull { (k, v) -> bestLabel(k)?.let { it to v } }.sortedBy { it.first }
        if (bests.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            CardTitle("Bestwerte")
            bests.forEach { (label, value) -> CaptionText("$label: $value richtig") }
        }
    }
}
