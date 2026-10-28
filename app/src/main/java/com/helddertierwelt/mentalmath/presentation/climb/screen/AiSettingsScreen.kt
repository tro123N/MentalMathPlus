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

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.data.climb.ai.EngineStatus
import com.helddertierwelt.mentalmath.data.climb.ai.ModelState
import com.helddertierwelt.mentalmath.domain.climb.ai.AiModel
import com.helddertierwelt.mentalmath.presentation.climb.AiViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.AiModelCard
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.SwitchRow
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

/** Einstellungen des KI-Coachs: Modell, Denkmodus, Download und Speicher. */
@Composable
fun AiSettingsScreen(navController: NavController) {
    val owner = LocalActivity.current as ViewModelStoreOwner
    val ai: AiViewModel = hiltViewModel(owner)
    val models by ai.models.collectAsState()
    val settings by ai.settings.collectAsState()
    val status by ai.engineStatus.collectAsState()
    val notice by ai.notice.collectAsState()
    val context = LocalContext.current
    val ram = remember { ai.deviceRamGb() }
    var deleting by remember { mutableStateOf<AiModel?>(null) }
    var clearing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        ai.onVisible(preload = false)
        onDispose { ai.onHidden() }
    }
    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            ai.clearNotice()
        }
    }

    fun back() {
        ai.playTap()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }
    BackHandler { back() }

    val selected = AiModel.chosen(settings.aiModel)
    val crashed = remember(models, status) { AiModel.entries.filter { ai.crashedEverywhere(it) }.toSet() }

    Scaffold(topBar = { ClimbTopBar(title = "KI-Einstellungen", onNavigation = { back() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("aiSettings"),
        ) {
            ClimbCard {
                CardTitle("So arbeitet der KI-Coach")
                BodyText("Die KI ist ein Sprachmodell von Google (Gemma 4) und rechnet direkt auf deinem Handy – auf der Grafikeinheit, wenn möglich. Sie braucht kein Internet, kostet nichts, und deine Fragen und dein Lernstand verlassen das Handy nie.")
                Spacer(Modifier.height(6.dp))
                val line = when (val s = status) {
                    EngineStatus.Off -> "Gerade nicht geladen (spart Arbeitsspeicher). Beim Öffnen des KI-Coachs lädt sie in wenigen Sekunden."
                    is EngineStatus.Loading -> s.step
                    is EngineStatus.Ready -> "Geladen: ${s.model.title} auf dem ${if (s.backend == "GPU") "Grafikprozessor" else "Prozessor"}" +
                        (if (s.fast) ", schnelles Schreiben an" else "") + " (geladen in ${(s.loadMs + 500) / 1000} s)."
                    is EngineStatus.Failed -> s.message
                }
                CaptionText(line)
            }
            CardGap()

            ClimbCard(modifier = Modifier.testTag("aiModels")) {
                CardTitle("Modell")
                BodyText("Gemma 4 E4B ist die beste Wahl für die meisten Handys. Gemma 4 12B ist noch klüger, aber ein Experiment: Es braucht sehr viel Arbeitsspeicher und antwortet langsamer. Klappt es auf der Grafikeinheit nicht, nimmt die App beim nächsten Mal den Prozessor.")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AiModel.entries.filter { it.phone || models[it] !is ModelState.Missing }.forEach { m ->
                        AiModelCard(
                            model = m,
                            state = models[m] ?: ModelState.Missing,
                            selected = m == selected,
                            deviceRamGb = ram,
                            crashed = m in crashed,
                            onDownload = { ai.download(m) },
                            onCancel = { ai.cancelDownload(m) },
                            onSelect = { ai.select(m) },
                            onDelete = { deleting = m },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                CaptionText("Dein Handy: etwa $ram GB Arbeitsspeicher, ${AiModel.gigabytes(ai.freeBytes())} Speicher frei.")
            }
            CardGap()

            ClimbCard(modifier = Modifier.testTag("aiOptions")) {
                CardTitle("Antworten")
                SwitchRow(
                    title = "Gründlich nachdenken",
                    subtitle = "Die KI denkt vor der Antwort nach (Denkmodus). Klüger, besonders bei Rechenwegen – dafür dauert es etwas länger.",
                    checked = settings.aiThinking,
                    onCheckedChange = { ai.setThinking(it) },
                    modifier = Modifier.testTag("aiThinkingSwitch"),
                )
                SwitchRow(
                    title = "Schneller schreiben",
                    subtitle = "Die KI sagt mehrere Wörter auf einmal voraus (wenn das Modell es kann). Schalte es aus, falls Antworten seltsam aussehen.",
                    checked = settings.aiFast,
                    onCheckedChange = { ai.setFast(it) },
                )
                SwitchRow(
                    title = "Nur über WLAN laden",
                    subtitle = "Das Modell ist mehrere Gigabyte groß.",
                    checked = settings.aiWifiOnly,
                    onCheckedChange = { ai.setWifiOnly(it) },
                )
            }
            CardGap()

            ClimbCard {
                CardTitle("Gespräch und Probleme")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "Gespräch löschen",
                        subtitle = "Der KI-Coach vergisst den bisherigen Chat",
                        icon = R.drawable.delete_24dp,
                        iconTint = red,
                        onClick = { clearing = true },
                        modifier = Modifier.testTag("aiClearChat"),
                    )
                    ActionButton(
                        text = "KI neu starten",
                        subtitle = "Vergisst Ladeprobleme und lädt die KI beim nächsten Mal neu",
                        icon = R.drawable.refresh_24dp,
                        iconTint = yellow,
                        onClick = {
                            ai.resetEngine()
                            Toast.makeText(context, "Die KI wird beim nächsten Mal neu geladen.", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
            CardGap()

            ClimbCard {
                CardTitle("Lizenz und Datenschutz")
                BodyText("Modelle: Gemma 4 von Google, Lizenz Apache 2.0, geladen von Hugging Face (Konto „litert-community“). Die KI-Laufzeit LiteRT-LM stammt ebenfalls von Google (Apache 2.0).")
                Spacer(Modifier.height(6.dp))
                BodyText("Das Internet braucht die App nur für den Download des Modells. Gespräche und Lernstand bleiben auf dem Handy.")
                Spacer(Modifier.height(6.dp))
                CaptionText("Die KI kann sich irren. Rechnungen prüft sie mit dem eingebauten Rechner, Aufgaben der App mit deren geprüftem Lösungsweg.")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    deleting?.let { m ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            textContentColor = MaterialTheme.colorScheme.onPrimary,
            title = { Text("${m.title} löschen?") },
            text = { BodyText("Das gibt ${m.sizeLabel} Speicher frei. Zum erneuten Benutzen muss das Modell wieder heruntergeladen werden.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ai.delete(m)
                        deleting = null
                    },
                    modifier = Modifier.testTag("aiDeleteConfirm"),
                ) { Text("Löschen", color = red) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Abbrechen", color = MaterialTheme.colorScheme.onPrimary) } },
        )
    }
    if (clearing) {
        AlertDialog(
            onDismissRequest = { clearing = false },
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            textContentColor = MaterialTheme.colorScheme.onPrimary,
            title = { Text("Gespräch löschen?") },
            text = { BodyText("Der bisherige Chat mit dem KI-Coach wird gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    ai.newChat()
                    clearing = false
                }) { Text("Löschen", color = accent) }
            },
            dismissButton = { TextButton(onClick = { clearing = false }) { Text("Abbrechen", color = MaterialTheme.colorScheme.onPrimary) } },
        )
    }
}
