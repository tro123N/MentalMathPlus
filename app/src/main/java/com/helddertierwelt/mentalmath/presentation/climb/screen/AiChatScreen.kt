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

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.data.climb.ReminderNotification
import com.helddertierwelt.mentalmath.data.climb.ai.EngineStatus
import com.helddertierwelt.mentalmath.data.climb.ai.ModelState
import com.helddertierwelt.mentalmath.domain.climb.ai.AiAction
import com.helddertierwelt.mentalmath.domain.climb.ai.AiActions
import com.helddertierwelt.mentalmath.domain.climb.ai.AiModel
import com.helddertierwelt.mentalmath.domain.climb.ai.AiQuiz
import com.helddertierwelt.mentalmath.domain.climb.ai.AiRole
import com.helddertierwelt.mentalmath.domain.climb.ai.AiStatus
import com.helddertierwelt.mentalmath.domain.climb.engine.SessionType
import com.helddertierwelt.mentalmath.domain.climb.model.Palette
import com.helddertierwelt.mentalmath.presentation.climb.AiViewModel
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.AiAssistantBubble
import com.helddertierwelt.mentalmath.presentation.climb.component.AiModelCard
import com.helddertierwelt.mentalmath.presentation.climb.component.AiSuggestions
import com.helddertierwelt.mentalmath.presentation.climb.component.AiUserBubble
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.SwitchRow
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import kotlinx.coroutines.flow.first

/** Kurzer Text zum Zustand der KI, z. B. „Gemma 4 E4B · Grafikeinheit“. */
private fun statusText(status: EngineStatus, model: AiModel?): String = when (status) {
    EngineStatus.Off -> (model?.title ?: "KI") + " · bereit zum Laden"
    is EngineStatus.Loading -> status.step
    is EngineStatus.Ready -> status.model.title + " · " + (if (status.backend == "GPU") "Grafikeinheit" else "Prozessor") + (if (status.fast) " · schnell" else "")
    is EngineStatus.Failed -> status.message
}

/** Der KI-Coach: Gespräch mit der KI auf dem Handy. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(navController: NavController) {
    val dimens = MentalMathTheme.dimens
    val owner = LocalActivity.current as ViewModelStoreOwner
    val ai: AiViewModel = hiltViewModel(owner)
    val climb: ClimbViewModel = hiltViewModel(owner)
    val chat by ai.chat.collectAsState()
    val busy by ai.busy.collectAsState()
    val activity by ai.activity.collectAsState()
    val status by ai.engineStatus.collectAsState()
    val models by ai.models.collectAsState()
    val settings by ai.settings.collectAsState()
    val focus by ai.focus.collectAsState()
    val topic by ai.topic.collectAsState()
    val notice by ai.notice.collectAsState()
    val crashNote by ai.crashNote.collectAsState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var input by rememberSaveable { mutableStateOf("") }

    // Modell im Speicher halten, solange der Bildschirm sichtbar ist; danach nach einer Weile freigeben.
    DisposableEffect(Unit) {
        ai.onVisible()
        onDispose { ai.onHidden() }
    }
    val lifecycleOwner = LocalActivity.current as LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> ai.onVisible()
                Lifecycle.Event.ON_STOP -> ai.onHidden(30_000)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

    // Welches Modell spricht? Das gewählte, sonst ein anderes geladenes.
    val ready = models.filterValues { it is ModelState.Ready }.keys
    val crashed = remember(ready, status) { AiModel.entries.filter { ai.crashedEverywhere(it) }.toSet() }
    val selected = AiModel.of(settings.aiModel) ?: AiModel.DEFAULT
    val active = if (selected in ready && selected !in crashed) selected else AiModel.fallbackOrder.firstOrNull { it in ready && it !in crashed }

    var pendingReminder by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingReminder?.let { (h, m) -> climb.setReminder(true, h, m) }
        pendingReminder = null
        if (!granted) Toast.makeText(context, "Ohne Erlaubnis für Benachrichtigungen kann die Erinnerung nicht erscheinen.", Toast.LENGTH_LONG).show()
    }

    fun applySetting(a: AiAction.Setting) {
        when (a.key) {
            AiAction.SETTING_REMINDER -> if (a.value == "off") {
                climb.setReminder(false)
            } else {
                val h = a.value.substringBefore(':').toIntOrNull() ?: 18
                val m = a.value.substringAfter(':', "0").toIntOrNull() ?: 0
                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    pendingReminder = h to m
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    climb.setReminder(true, h, m)
                    if (!ReminderNotification.allowed(context)) {
                        Toast.makeText(context, "Benachrichtigungen sind für die App ausgeschaltet – bitte in den Android-Einstellungen erlauben.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            AiAction.SETTING_TRICK_TIPS -> climb.setTrickTips(a.value == "on")
            AiAction.SETTING_ADAPTIVE -> climb.setAdaptive(a.value == "on")
            AiAction.SETTING_THEME -> climb.setPalette(Palette.of(a.value))
        }
    }

    fun runAction(messageId: Long, index: Int, a: AiAction) {
        ai.playTap()
        fun play(started: Boolean): Boolean {
            if (started) navController.navigate("climbplay")
            return started
        }
        val ok = when (a) {
            is AiAction.ApPractice -> play(climb.startApPractice(AiActions.categories(a.categories).toSet()))
            is AiAction.ApSimulation -> play(climb.startApSimulation(a.seconds, a.count, 5))
            AiAction.ApReview -> play(climb.startApReview()).also {
                if (!it) Toast.makeText(context, "Im Fehlertraining ist gerade nichts fällig.", Toast.LENGTH_SHORT).show()
            }
            is AiAction.ApTypes -> play(climb.startApTypes(a.type, a.count))
            is AiAction.Level -> when (a.mode) {
                AiAction.MODE_LEARN -> {
                    navController.navigate("learn/${a.skillId}")
                    true
                }
                AiAction.MODE_PRACTICE -> play(climb.startSession(SessionType.PRACTICE, a.skillId))
                AiAction.MODE_TEST -> play(climb.startSession(SessionType.TEST, a.skillId))
                else -> {
                    navController.navigate("level/${a.skillId}")
                    true
                }
            }
            is AiAction.Setting -> {
                applySetting(a)
                true
            }
        }
        if (ok) ai.markDone(messageId, index)
    }

    fun copy(text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("KI-Coach", text))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, "Kopiert", Toast.LENGTH_SHORT).show()
    }

    fun send(text: String) {
        if (text.isBlank() || busy) return
        ai.send(text)
        input = ""
        // Tastatur einklappen, damit die Antwort Platz hat.
        keyboard?.hide()
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            ClimbTopBar(
                title = "KI-Coach",
                onNavigation = { back() },
                actions = {
                    if (active != null) {
                        IconButton(onClick = { ai.newChat() }, enabled = chat.messages.isNotEmpty() || focus != null || topic != null, modifier = Modifier.testTag("aiNewChat")) {
                            Icon(
                                painter = painterResource(R.drawable.add_24dp),
                                contentDescription = "Neues Gespräch",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(dimens.icons.topBarAction),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            ai.playTap()
                            navController.navigate("aisettings")
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("aiSettingsButton"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.tune_24dp),
                            contentDescription = "KI-Einstellungen",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(dimens.icons.topBarAction),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            when {
                !ai.supported -> Column(Modifier.padding(20.dp)) {
                    ClimbCard {
                        CardTitle("KI-Coach")
                        BodyText("Die KI läuft direkt auf dem Handy und braucht dafür Android 12 oder neuer. Auf diesem Gerät ist sie leider nicht verfügbar.")
                    }
                }
                active == null -> AiSetup(ai, models, selected, crashed, settings.aiWifiOnly)
                else -> {
                    StatusStrip(
                        text = statusText(status, active),
                        loading = status is EngineStatus.Loading,
                        failed = status is EngineStatus.Failed,
                        thinking = settings.aiThinking,
                        onThinking = { ai.setThinking(!settings.aiThinking) },
                        onOpen = {
                            ai.playTap()
                            navController.navigate("aisettings")
                        },
                    )
                    val listState = rememberLazyListState()
                    val messages = chat.messages
                    val last = messages.lastOrNull()
                    // Beim Öffnen (und bei einer neuen Aufgabe) ans Ende springen.
                    LaunchedEffect(focus, topic) {
                        val total = snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
                        listState.scrollToItem(total - 1)
                    }
                    // Fährt die Tastatur aus, bleibt das Ende des Gesprächs sichtbar.
                    val imeVisible = WindowInsets.isImeVisible
                    LaunchedEffect(imeVisible) {
                        val total = listState.layoutInfo.totalItemsCount
                        if (imeVisible && total > 0) listState.animateScrollToItem(total - 1)
                    }
                    // Beim Schreiben unten bleiben (außer die Person hat nach oben gescrollt).
                    LaunchedEffect(messages.size, last?.text?.length, last?.thought?.length, last?.actions?.size, last?.status) {
                        val total = listState.layoutInfo.totalItemsCount
                        if (total == 0) return@LaunchedEffect
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        if (lastVisible >= total - 3 || last?.status == AiStatus.STREAMING && last.text.isEmpty()) listState.animateScrollToItem(total - 1)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("aiList"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        crashNote?.let { note ->
                            item(key = "crash") {
                                NoteCard(note, onClose = { ai.clearCrashNote() })
                            }
                        }
                        if (messages.isEmpty() && focus == null && topic == null) {
                            item(key = "welcome") {
                                ClimbCard(modifier = Modifier.testTag("aiWelcome")) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(painter = painterResource(R.drawable.auto_awesome_24dp), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(8.dp))
                                        CardTitle("Hallo! Ich bin dein KI-Coach.", modifier = Modifier.padding(bottom = 0.dp))
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    BodyText("Ich kenne deinen Lernstand, erkläre Rechenwege und Fehler, stelle dir Aufgaben und bereite passende Übungen vor. Ich laufe nur auf deinem Handy – ohne Internet, kostenlos, deine Daten bleiben bei dir.")
                                    Spacer(Modifier.height(6.dp))
                                    CaptionText("Ich kann mich irren – meine Rechnungen prüfe ich mit dem eingebauten Rechner.")
                                    Spacer(Modifier.height(12.dp))
                                    AiSuggestions(ai.suggestions(), enabled = !busy, onPick = { send(it) })
                                }
                            }
                        }
                        items(messages, key = { it.id }) { m ->
                            if (m.role == AiRole.USER) {
                                AiUserBubble(m)
                            } else {
                                val cards = remember(m.quiz, chat.quiz) {
                                    m.quiz.mapNotNull { id -> chat.quiz.firstOrNull { it.id == id }?.let { AiQuiz.card(it) } }
                                }
                                AiAssistantBubble(
                                    m = m,
                                    activity = if (m === last) activity else null,
                                    isLast = m === last,
                                    busy = busy,
                                    onAction = { i, a -> runAction(m.id, i, a) },
                                    onRetry = { ai.retry() },
                                    onCopy = { copy(it) },
                                    quiz = cards,
                                    onAnswer = { answer -> send(answer) },
                                )
                            }
                        }
                        val t = topic
                        if (t != null && focus == null) {
                            item(key = "topic") {
                                ClimbCard(modifier = Modifier.testTag("aiTopic")) {
                                    CaptionText("Frage zu diesem Level")
                                    Spacer(Modifier.height(4.dp))
                                    BodyText(ai.topicName(t) ?: t)
                                    Spacer(Modifier.height(10.dp))
                                    AiSuggestions(ai.suggestions(), enabled = !busy, onPick = { send(it) })
                                }
                            }
                        }
                        val f = focus
                        if (f != null && messages.none { it.task == f.card }) {
                            item(key = "focus") {
                                ClimbCard(modifier = Modifier.testTag("aiFocus")) {
                                    CaptionText("Frage zu dieser Aufgabe (${f.source})")
                                    Spacer(Modifier.height(4.dp))
                                    BodyText(f.card)
                                    Spacer(Modifier.height(4.dp))
                                    if (f.answer.isNotEmpty()) CaptionText("Deine Antwort: ${f.answer}" + if (f.correct) " – richtig" else " – falsch")
                                    Spacer(Modifier.height(10.dp))
                                    AiSuggestions(ai.suggestions(), enabled = !busy, onPick = { send(it) })
                                }
                            }
                        }
                    }
                    InputRow(
                        value = input,
                        onValueChange = { input = it },
                        busy = busy,
                        onSend = { send(input) },
                        onStop = { ai.stop() },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusStrip(text: String, loading: Boolean, failed: Boolean, thinking: Boolean, onThinking: () -> Unit, onOpen: () -> Unit) {
    val dimens = MentalMathTheme.dimens
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Icon(painter = painterResource(R.drawable.memory_24dp), contentDescription = null, tint = if (failed) yellow else accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                fontSize = dimens.fonts.caption,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpen() }
                    .testTag("aiStatus"),
            )
            Spacer(Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (thinking) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background)
                    .clickable { onThinking() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("aiThinkingToggle"),
            ) {
                Icon(painter = painterResource(R.drawable.lightbulb_24dp), contentDescription = null, tint = if (thinking) yellow else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (thinking) "Gründlich" else "Schnell", fontSize = dimens.fonts.caption, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        if (loading) LinearProgressIndicator(color = accent, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NoteCard(text: String, onClose: () -> Unit) {
    val dimens = MentalMathTheme.dimens
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(yellow.copy(alpha = 0.15f))
            .padding(12.dp),
    ) {
        Text(text, fontSize = dimens.fonts.caption, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.close_24dp),
            contentDescription = "Schließen",
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onClose() }
                .padding(4.dp),
        )
    }
}

@Composable
private fun InputRow(value: String, onValueChange: (String) -> Unit, busy: Boolean, onSend: () -> Unit, onStop: () -> Unit) {
    val dimens = MentalMathTheme.dimens
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Frag mich etwas …") },
            maxLines = 5,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                focusedBorderColor = accent,
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("aiInput"),
        )
        Spacer(Modifier.width(6.dp))
        val canSend = value.isNotBlank() && !busy
        IconButton(
            onClick = { if (busy) onStop() else onSend() },
            enabled = busy || canSend,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(if (busy || canSend) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                .testTag(if (busy) "aiStop" else "aiSend"),
        ) {
            Icon(
                painter = painterResource(if (busy) R.drawable.stop_24dp else R.drawable.send_24dp),
                contentDescription = if (busy) "Antwort abbrechen" else "Senden",
                tint = Color.White,
                modifier = Modifier.size(dimens.icons.menuItem * 0.85f),
            )
        }
    }
}

/** Einrichtung: Modell wählen und herunterladen. */
@Composable
private fun AiSetup(ai: AiViewModel, models: Map<AiModel, ModelState>, selected: AiModel, crashed: Set<AiModel>, wifiOnly: Boolean) {
    val ram = remember { ai.deviceRamGb() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("aiSetup"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ClimbCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.auto_awesome_24dp), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                CardTitle("KI-Coach einrichten", modifier = Modifier.padding(bottom = 0.dp))
            }
            Spacer(Modifier.height(6.dp))
            BodyText("Der KI-Coach ist ein Sprachmodell von Google (Gemma 4), das direkt auf deinem Handy rechnet: ohne Internet, ohne Kosten, und nichts verlässt dein Handy.")
            Spacer(Modifier.height(6.dp))
            BodyText("Dafür wird das Modell einmal heruntergeladen. Danach kennt die KI deinen Lernstand, erklärt Aufgaben und Fehler, stellt dir Aufgaben und bereitet Übungen vor.")
            Spacer(Modifier.height(6.dp))
            CaptionText("Dein Handy: etwa $ram GB Arbeitsspeicher, ${AiModel.gigabytes(ai.freeBytes())} frei.")
        }
        AiModel.entries.forEach { m ->
            AiModelCard(
                model = m,
                state = models[m] ?: ModelState.Missing,
                selected = m == selected,
                deviceRamGb = ram,
                crashed = m in crashed,
                onDownload = { ai.download(m) },
                onCancel = { ai.cancelDownload(m) },
                onSelect = { ai.select(m) },
                onDelete = null,
            )
        }
        ClimbCard {
            SwitchRow(
                title = "Nur über WLAN laden",
                subtitle = "Empfohlen: Das Modell ist mehrere Gigabyte groß.",
                checked = wifiOnly,
                onCheckedChange = { ai.setWifiOnly(it) },
            )
            Spacer(Modifier.height(4.dp))
            CaptionText("Der Download läuft auch weiter, wenn du die App verlässt. Lizenz der Modelle: Apache 2.0 (frei nutzbar).")
        }
    }
}
