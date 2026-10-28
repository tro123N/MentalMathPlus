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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.BuildConfig
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.data.climb.ReminderNotification
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.model.Backup
import com.helddertierwelt.mentalmath.domain.climb.model.Palette
import com.helddertierwelt.mentalmath.domain.settings.enums.SoundMode
import com.helddertierwelt.mentalmath.domain.settings.enums.ThemeMode
import com.helddertierwelt.mentalmath.domain.settings.enums.VibrationMode
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardGap
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.PillButton
import com.helddertierwelt.mentalmath.presentation.climb.component.SwitchRow
import com.helddertierwelt.mentalmath.presentation.theme.LocalDarkTheme
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.palettePreview
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel
import java.io.File

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: ActivityNotFoundException) {
        toast(context, "Kein Browser gefunden.")
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast(context, "Die Einstellungen konnten nicht geöffnet werden.")
    }
}

/** Datum einer Sicherung als „27.10.2026“. */
private fun dateText(ms: Long): String {
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = ms
    return "${c.get(java.util.Calendar.DAY_OF_MONTH)}.${c.get(java.util.Calendar.MONTH) + 1}.${c.get(java.util.Calendar.YEAR)}"
}

/** Einstellungen von Mental Math Plus: Darstellung, Erinnerung, Ton, Sicherung und Infos zur App. */
@Composable
fun PlusSettingsScreen(navController: NavController) {
    val dimens = MentalMathTheme.dimens
    val context = LocalContext.current
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    val settingsViewModel: SettingsViewModel = hiltViewModel(owner)
    val plus by viewModel.plusSettings.collectAsState()
    val appSettings by settingsViewModel.settingsState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val dark = LocalDarkTheme.current
    var pendingBackup by remember { mutableStateOf<Backup?>(null) }
    var notificationsBlocked by remember { mutableStateOf(false) }

    fun back() {
        viewModel.playTap()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }
    BackHandler { back() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            notificationsBlocked = false
            viewModel.setReminder(true)
        } else {
            notificationsBlocked = true
        }
    }

    fun switchReminder(on: Boolean) {
        viewModel.playTap()
        if (!on) {
            viewModel.setReminder(false)
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsBlocked = !ReminderNotification.allowed(context)
            viewModel.setReminder(true)
        }
    }

    fun shiftTime(minutes: Int) {
        viewModel.playTap()
        val total = (plus.reminderHour * 60 + plus.reminderMinute + minutes).mod(24 * 60)
        viewModel.setReminder(plus.reminderOn, total / 60, total % 60)
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(viewModel.backupText().toByteArray(Charsets.UTF_8)) } != null
            } catch (e: Exception) {
                false
            }
            toast(context, if (ok) "Sicherung gespeichert." else "Die Sicherung konnte nicht gespeichert werden.")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    if (bytes.size > 8_000_000) null else bytes.toString(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                null
            }
            if (text == null) {
                toast(context, "Die Datei konnte nicht gelesen werden.")
            } else {
                viewModel.readBackup(text)
                    .onSuccess { pendingBackup = it }
                    .onFailure { toast(context, it.message ?: "Die Datei konnte nicht gelesen werden.") }
            }
        }
    }

    fun shareBackup() {
        viewModel.playTap()
        try {
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, viewModel.backupFileName())
            file.writeText(viewModel.backupText())
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Mental Math Plus – Sicherung")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Sicherung teilen"))
        } catch (e: Exception) {
            toast(context, "Die Sicherung konnte nicht geteilt werden.")
        }
    }

    Scaffold(topBar = { ClimbTopBar(title = "Einstellungen", onNavigation = { back() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("settingsColumn")
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Darstellung
            ClimbCard(modifier = Modifier.testTag("settingsLook")) {
                CardTitle("Darstellung")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.LIGHT to "Hell", ThemeMode.DARK to "Dunkel", ThemeMode.SYSTEM to "System").forEach { (mode, label) ->
                        PillButton(
                            text = label,
                            onClick = { settingsViewModel.setThemeMode(mode) },
                            color = if (appSettings.themeMode == mode) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_${mode.name}"),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                CaptionText("Farbthema")
                Spacer(Modifier.height(4.dp))
                Palette.entries.forEach { palette ->
                    val selected = plus.paletteValue == palette
                    val (bg, card, acc) = palettePreview(palette, dark)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.components.cornerRadius))
                            .background(if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary)
                            .clickable { viewModel.setPalette(palette) }
                            .testTag("palette_${palette.name}")
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(card),
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(acc),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(palette.title, fontSize = dimens.fonts.sideSheetEntry, color = MaterialTheme.colorScheme.onPrimary)
                            CaptionText(palette.subtitle)
                        }
                        if (selected) {
                            Icon(
                                painter = painterResource(R.drawable.check_circle_24dp),
                                contentDescription = "ausgewählt",
                                tint = green,
                                modifier = Modifier.size(dimens.icons.menuItem),
                            )
                        }
                    }
                }
            }
            CardGap()

            // Erinnerung
            ClimbCard(modifier = Modifier.testTag("settingsReminder")) {
                CardTitle("Tägliche Erinnerung")
                SwitchRow(
                    title = "Erinnerung",
                    subtitle = if (plus.reminderOn) "Jeden Tag um ${plus.reminderTime} Uhr" else "Aus",
                    checked = plus.reminderOn,
                    onCheckedChange = { switchReminder(it) },
                    modifier = Modifier.testTag("reminderSwitch"),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    BodyText("Uhrzeit", modifier = Modifier.weight(1f))
                    IconButton(onClick = { shiftTime(-15) }, modifier = Modifier.testTag("timeMinus")) {
                        Icon(
                            painter = painterResource(R.drawable.remove_24dp),
                            contentDescription = "15 Minuten früher",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text(
                        text = plus.reminderTime,
                        fontSize = dimens.fonts.settingsLabel,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("reminderTime"),
                    )
                    IconButton(onClick = { shiftTime(15) }, modifier = Modifier.testTag("timePlus")) {
                        Icon(
                            painter = painterResource(R.drawable.add_24dp),
                            contentDescription = "15 Minuten später",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 12, 18, 20).forEach { h ->
                        TimeChip(
                            text = "$h:00",
                            selected = plus.reminderHour == h && plus.reminderMinute == 0,
                            onClick = {
                                viewModel.playTap()
                                viewModel.setReminder(plus.reminderOn, h, 0)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                SwitchRow(
                    title = "Nur, wenn ich noch nicht geübt habe",
                    subtitle = "Hast du an dem Tag schon gerechnet, bleibt die Erinnerung still.",
                    checked = plus.reminderOnlyIfIdle,
                    onCheckedChange = { viewModel.setReminderOnlyIfIdle(it) },
                )
                if (notificationsBlocked) {
                    Spacer(Modifier.height(6.dp))
                    BodyText("Mitteilungen sind für Mental Math Plus ausgeschaltet. Erlaube sie in den Android-Einstellungen, sonst kommt keine Erinnerung.", color = yellow)
                    Spacer(Modifier.height(6.dp))
                    ActionButton(
                        text = "Mitteilungen erlauben",
                        subtitle = "Öffnet die Android-Einstellungen",
                        icon = R.drawable.notifications_active_24dp,
                        iconTint = yellow,
                        onClick = {
                            viewModel.playTap()
                            openNotificationSettings(context)
                        },
                    )
                }
                if (plus.reminderOn) {
                    Spacer(Modifier.height(6.dp))
                    ActionButton(
                        text = "Jetzt ausprobieren",
                        subtitle = "Zeigt die Erinnerung sofort an",
                        icon = R.drawable.alarm_24dp,
                        iconTint = accent,
                        onClick = {
                            viewModel.playTap()
                            if (!ReminderNotification.show(context)) {
                                notificationsBlocked = true
                            }
                        },
                        modifier = Modifier.testTag("reminderTest"),
                    )
                }
            }
            CardGap()

            // Trainingshilfen
            ClimbCard(modifier = Modifier.testTag("settingsHelp")) {
                CardTitle("Trainingshilfen")
                SwitchRow(
                    title = "Trick-Tipp-Knopf",
                    subtitle = "Beim Üben zeigt ein Knopf, welcher Trick zur Aufgabe passt. In Prüfungen ist er nie zu sehen.",
                    checked = plus.trickTips,
                    onCheckedChange = { viewModel.setTrickTips(it) },
                )
                SwitchRow(
                    title = "Anpassende Schwierigkeit",
                    subtitle = "Freies Training: Die Aufgaben werden schwerer, wenn es gut läuft, und leichter nach Fehlern.",
                    checked = plus.adaptive,
                    onCheckedChange = { viewModel.setAdaptive(it) },
                )
            }
            CardGap()

            // KI-Coach
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ClimbCard(modifier = Modifier.testTag("settingsAi")) {
                    CardTitle("KI-Coach")
                    BodyText("Eine KI direkt auf dem Handy: erklärt Aufgaben und Fehler, kennt deinen Lernstand und bereitet Übungen vor – ohne Internet und kostenlos.")
                    Spacer(Modifier.height(10.dp))
                    ActionButton(
                        text = "KI-Coach einstellen",
                        subtitle = "Modell herunterladen, Denkmodus, Speicher",
                        icon = R.drawable.auto_awesome_24dp,
                        iconTint = accent,
                        onClick = {
                            viewModel.playTap()
                            navController.navigate("aisettings")
                        },
                        modifier = Modifier.testTag("settingsAiButton"),
                    )
                }
                CardGap()
            }

            // Ton und Vibration
            ClimbCard {
                CardTitle("Ton und Vibration")
                SwitchRow(
                    title = "Töne",
                    subtitle = null,
                    checked = appSettings.soundMode == SoundMode.SOUND_ON,
                    onCheckedChange = { settingsViewModel.setSoundMode(if (it) SoundMode.SOUND_ON else SoundMode.SOUND_OFF) },
                )
                SwitchRow(
                    title = "Vibration",
                    subtitle = null,
                    checked = appSettings.vibrationMode == VibrationMode.VIBRATION_ON,
                    onCheckedChange = { settingsViewModel.setVibrationMode(if (it) VibrationMode.VIBRATION_ON else VibrationMode.VIBRATION_OFF) },
                )
            }
            CardGap()

            // Sicherung
            ClimbCard(modifier = Modifier.testTag("settingsBackup")) {
                CardTitle("Fortschritt sichern und übertragen")
                BodyText(
                    "Speichere deinen Fortschritt als Datei – zum Beispiel vor einem Handywechsel. " +
                        "Auf dem neuen Handy lädst du die Datei hier wieder.",
                )
                Spacer(Modifier.height(4.dp))
                CaptionText("Enthalten: Kletterweg, Statistik, freies Training und diese Einstellungen. Nicht enthalten: Bestenliste und Verlauf des klassischen Modus.")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "Sicherung speichern",
                        subtitle = "Als Datei, z. B. in „Downloads“ oder Google Drive",
                        icon = R.drawable.download_24dp,
                        iconTint = accent,
                        onClick = {
                            viewModel.playTap()
                            try {
                                exportLauncher.launch(viewModel.backupFileName())
                            } catch (e: ActivityNotFoundException) {
                                toast(context, "Auf diesem Handy fehlt die Dateiauswahl.")
                            }
                        },
                        modifier = Modifier.testTag("backupExport"),
                    )
                    ActionButton(
                        text = "Sicherung teilen",
                        subtitle = "Per Mail oder Messenger an dich selbst schicken",
                        icon = R.drawable.share_24dp,
                        iconTint = accent,
                        onClick = { shareBackup() },
                        modifier = Modifier.testTag("backupShare"),
                    )
                    ActionButton(
                        text = "Sicherung laden",
                        subtitle = "Ersetzt den aktuellen Fortschritt",
                        icon = R.drawable.upload_24dp,
                        iconTint = yellow,
                        onClick = {
                            viewModel.playTap()
                            try {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                            } catch (e: ActivityNotFoundException) {
                                toast(context, "Auf diesem Handy fehlt die Dateiauswahl.")
                            }
                        },
                        modifier = Modifier.testTag("backupImport"),
                    )
                }
            }
            CardGap()

            // Über die App
            ClimbCard(modifier = Modifier.testTag("settingsAbout")) {
                CardTitle("Über Mental Math Plus")
                CaptionText("Version ${BuildConfig.VERSION_NAME}")
                Spacer(Modifier.height(6.dp))
                BodyText("Ein Hobbyprojekt von tro123N. Code und Texte wurden mit der KI Claude (Anthropic) erstellt – Fehler sind möglich.")
                Spacer(Modifier.height(6.dp))
                BodyText("Grundlage ist die App „Mental Math“ von HeldDerTierwelt (Lizenz GPLv3). Die Methoden folgen dem Buch „Mathe-Magie“ von Arthur Benjamin und Michael Shermer; Texte aus dem Buch sind nicht enthalten.")
                Spacer(Modifier.height(10.dp))
                LinkRow("Projektseite und Downloads") { openLink(context, "https://github.com/tro123N/MentalMathPlus") }
                LinkRow("Fehler melden oder Ideen schicken") { openLink(context, "https://github.com/tro123N/MentalMathPlus/issues") }
                LinkRow("Original-App „Mental Math“") { openLink(context, "https://codeberg.org/Mental-Math/MentalMath") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingBackup?.let { backup ->
        val all = viewModel.worlds.sumOf { it.skills.size }
        val fileLevels = viewModel.worlds.flatMap { it.skills }.count { backup.progress.of(it.id).mastered }
        val nowLevels = viewModel.worlds.flatMap { it.skills }.count { progress.of(it.id).mastered }
        val date = if (backup.createdAt > 0) " vom ${dateText(backup.createdAt)}" else ""
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            textContentColor = MaterialTheme.colorScheme.onPrimary,
            title = { Text("Sicherung laden?") },
            text = {
                Column {
                    BodyText("Sicherung$date: $fileLevels von $all Levels geschafft, ${Fmt.int(backup.progress.totalSolved)} Aufgaben gelöst.")
                    Spacer(Modifier.height(8.dp))
                    BodyText("Dein jetziger Fortschritt ($nowLevels von $all Levels) wird dabei ersetzt.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreBackup(backup)
                        pendingBackup = null
                        toast(context, "Sicherung geladen.")
                    },
                    modifier = Modifier.testTag("backupConfirm"),
                ) { Text("Ersetzen", color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackup = null }) { Text("Abbrechen", color = MaterialTheme.colorScheme.onPrimary) }
            },
        )
    }
}

@Composable
private fun TimeChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(dimens.components.cornerRadius * 2))
            .background(if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = dimens.fonts.sideSheetEntry,
            color = if (selected) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            maxLines = 1,
        )
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    val dimens = MentalMathTheme.dimens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.components.cornerRadius))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.info_24dp),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = dimens.fonts.sideSheetEntry, color = accent)
    }
}
