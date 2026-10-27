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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.domain.climb.model.ClimbProgress
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.LevelBadge
import com.helddertierwelt.mentalmath.presentation.climb.component.StarRow
import com.helddertierwelt.mentalmath.presentation.component.settings.menu.MenuSideSheet
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow
import com.helddertierwelt.mentalmath.presentation.viewmodel.SettingsViewModel

/** Speichert, welche Welten auf- oder zugeklappt sind (als „id=1“/„id=0“). */
private val expandedSaver = listSaver<SnapshotStateMap<String, Boolean>, String>(
    save = { map -> map.entries.map { "${it.key}=${if (it.value) 1 else 0}" } },
    restore = { saved ->
        mutableStateMapOf<String, Boolean>().apply {
            saved.forEach { entry ->
                val parts = entry.split('=')
                if (parts.size == 2) put(parts[0], parts[1] == "1")
            }
        }
    },
)

/** Startseite: der Kletterweg mit allen Welten und Levels. */
@Composable
fun ClimbHomeScreen(navController: NavController) {
    val dimens = MentalMathTheme.dimens
    val context = LocalContext.current
    val owner = LocalActivity.current as ViewModelStoreOwner
    val climbViewModel: ClimbViewModel = hiltViewModel(owner)
    val settingsViewModel: SettingsViewModel = hiltViewModel(owner)
    val progress by climbViewModel.progress.collectAsState()
    val worlds = climbViewModel.worlds
    val next = climbViewModel.nextSkill(progress)
    val currentWorldId = next?.let { n -> worlds.firstOrNull { w -> w.skills.any { it.id == n.id } }?.id }
    // Aufgeklappte Welten bleiben erhalten, wenn man von einem Level zurückkommt.
    val expanded = rememberSaveable(saver = expandedSaver) { mutableStateMapOf() }

    fun openLevel(skill: Skill) {
        climbViewModel.playTap()
        if (climbViewModel.isUnlocked(progress, skill.id)) {
            navController.navigate("level/${skill.id}")
        } else {
            Toast.makeText(context, "Erst das Level davor schaffen.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            ClimbTopBar(
                title = "Kletterweg",
                navigationIcon = R.drawable.menu_24dp,
                navigationDescription = "Menü",
                onNavigation = {
                    settingsViewModel.playFeedback(FeedbackType.TAP)
                    settingsViewModel.toggleMenuOpen()
                },
                actions = {
                    IconButton(
                        onClick = {
                            climbViewModel.playTap()
                            navController.navigate("free") { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.tune_24dp),
                            contentDescription = "Freies Training",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(dimens.icons.topBarAction),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("climbList"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    OverviewCard(
                        viewModel = climbViewModel,
                        progress = progress,
                        next = next,
                        onContinue = { skill -> openLevel(skill) },
                        onReview = {
                            if (climbViewModel.startReview()) {
                                navController.navigate("climbplay")
                            } else {
                                Toast.makeText(context, "Schaffe zuerst ein Level, dann gibt es etwas zu wiederholen.", Toast.LENGTH_LONG).show()
                            }
                        },
                        onRefresh = { skill ->
                            climbViewModel.playTap()
                            navController.navigate("level/${skill.id}")
                        },
                        onFreeTraining = {
                            climbViewModel.playTap()
                            navController.navigate("free") { launchSingleTop = true }
                        },
                        onHandbook = {
                            climbViewModel.playTap()
                            navController.navigate("handbook") { launchSingleTop = true }
                        },
                    )
                }
                worlds.forEachIndexed { index, world ->
                    item(key = world.id) {
                        val unlocked = climbViewModel.isWorldUnlocked(progress, index)
                        val isOpen = expanded[world.id] ?: (world.id == currentWorldId || (currentWorldId == null && index == worlds.lastIndex))
                        WorldCard(
                            world = world,
                            unlocked = unlocked,
                            progress = progress,
                            isOpen = isOpen,
                            nextId = next?.id,
                            isSkillUnlocked = { climbViewModel.isUnlocked(progress, it) },
                            onToggle = {
                                climbViewModel.playTap()
                                expanded[world.id] = !isOpen
                            },
                            onOpenSkill = { openLevel(it) },
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
            MenuSideSheet(navController = navController)
        }
    }

    BackHandler {
        if (settingsViewModel.isMenuOpen.value) {
            settingsViewModel.toggleMenuOpen()
        } else {
            (context as? android.app.Activity)?.moveTaskToBack(true)
        }
    }
}

@Composable
private fun OverviewCard(
    viewModel: ClimbViewModel,
    progress: ClimbProgress,
    next: Skill?,
    onContinue: (Skill) -> Unit,
    onReview: () -> Unit,
    onRefresh: (Skill) -> Unit,
    onFreeTraining: () -> Unit,
    onHandbook: () -> Unit,
) {
    val dimens = MentalMathTheme.dimens
    val all = viewModel.worlds.flatMap { it.skills }
    val mastered = all.count { progress.of(it.id).mastered }
    val starsTotal = all.sumOf { progress.of(it.id).stars }
    val streak = viewModel.streak(progress)
    val due = viewModel.dueSkills(progress)
    val refresh = viewModel.refreshSkills(progress)

    ClimbCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "$mastered von ${all.size} Levels",
                    fontSize = dimens.fonts.settingsLabel,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                CaptionText("$starsTotal von ${all.size * 3} Sternen · ${com.helddertierwelt.mentalmath.domain.climb.math.Fmt.int(progress.totalSolved)} Aufgaben gelöst")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.whatshot_24dp),
                    contentDescription = "Tage in Folge",
                    tint = if (streak > 0) red else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                    modifier = Modifier.size(dimens.icons.menuItem),
                )
                CaptionText(if (streak == 1) "1 Tag" else "$streak Tage")
            }
        }
        Spacer(Modifier.height(12.dp))
        if (refresh.isNotEmpty()) {
            val first = refresh.first()
            ActionButton(
                text = "Auffrischen: ${first.title}",
                subtitle = if (refresh.size > 1) "Noch ${refresh.size - 1} weitere eingerostet" else "Erst auffrischen, dann geht es weiter",
                icon = R.drawable.refresh_24dp,
                iconTint = yellow,
                onClick = { onRefresh(first) },
            )
            Spacer(Modifier.height(8.dp))
        }
        if (next != null) {
            ActionButton(
                text = "Weiterklettern",
                subtitle = next.title,
                icon = R.drawable.terrain_24dp,
                iconTint = green,
                onClick = { onContinue(next) },
            )
        } else {
            BodyText("Du hast alle Levels geschafft. Wiederhole regelmäßig, damit nichts einrostet.")
        }
        Spacer(Modifier.height(8.dp))
        ActionButton(
            text = if (due.isNotEmpty()) "Wiederholen (${due.size} fällig)" else "Gemischt trainieren",
            subtitle = if (due.isNotEmpty()) "Gelerntes frisch halten" else "Aufgaben aus allen geschafften Levels",
            icon = R.drawable.history_24dp,
            iconTint = blue,
            enabled = mastered > 0,
            onClick = onReview,
        )
        Spacer(Modifier.height(8.dp))
        ActionButton(
            text = "Handbuch",
            subtitle = "Alle Tricks zum Nachschlagen",
            icon = R.drawable.book_24dp,
            iconTint = blue,
            onClick = onHandbook,
        )
        Spacer(Modifier.height(8.dp))
        ActionButton(
            text = "Freies Training",
            subtitle = "Aufgabenarten, Modi und Statistik",
            icon = R.drawable.tune_24dp,
            iconTint = MaterialTheme.colorScheme.onPrimary,
            onClick = onFreeTraining,
        )
    }
}

@Composable
private fun WorldCard(
    world: World,
    unlocked: Boolean,
    progress: ClimbProgress,
    isOpen: Boolean,
    nextId: String?,
    isSkillUnlocked: (String) -> Boolean,
    onToggle: () -> Unit,
    onOpenSkill: (Skill) -> Unit,
) {
    val dimens = MentalMathTheme.dimens
    val stars = world.skills.sumOf { progress.of(it.id).stars }
    val done = world.skills.count { progress.of(it.id).mastered }
    val attention = world.skills.any { progress.of(it.id).let { p -> p.mastered && p.needsAttention } }
    ClimbCard(
        modifier = Modifier
            .alpha(if (unlocked) 1f else 0.6f)
            .testTag("world_${world.id}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimens.components.cornerRadius))
                .clickable { onToggle() }
                .testTag("worldHeader_${world.id}"),
        ) {
            LevelBadge(
                label = world.number.toString(),
                locked = !unlocked,
                mastered = done == world.skills.size,
                attention = attention,
                highlighted = unlocked && done < world.skills.size,
                size = dimens.components.button * 0.75f,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = world.title,
                    fontSize = dimens.fonts.settingsLabel,
                    lineHeight = dimens.fonts.settingsLabel * 1.15f,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                CaptionText("${world.subtitle} · $done/${world.skills.size} · $stars★")
            }
            Icon(
                painter = painterResource(if (isOpen) R.drawable.keyboard_arrow_up_24dp else R.drawable.keyboard_arrow_down_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(dimens.icons.menuItem),
            )
        }
        if (isOpen) {
            Spacer(Modifier.height(10.dp))
            world.skills.forEachIndexed { i, skill ->
                SkillRow(
                    number = i + 1,
                    skill = skill,
                    progress = progress,
                    unlocked = unlocked && isSkillUnlocked(skill.id),
                    isNext = skill.id == nextId,
                    onClick = { onOpenSkill(skill) },
                )
            }
        }
    }
}

@Composable
private fun SkillRow(
    number: Int,
    skill: Skill,
    progress: ClimbProgress,
    unlocked: Boolean,
    isNext: Boolean,
    onClick: () -> Unit,
) {
    val dimens = MentalMathTheme.dimens
    val sp = progress.of(skill.id)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.components.cornerRadius))
            .clickable { onClick() }
            .testTag("skill_${skill.id}")
            .padding(vertical = 6.dp),
    ) {
        LevelBadge(
            label = number.toString(),
            locked = !unlocked,
            mastered = sp.mastered,
            attention = sp.mastered && sp.needsAttention,
            highlighted = isNext,
            size = dimens.components.button * 0.6f,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = skill.title,
                fontSize = dimens.fonts.sideSheetEntry,
                lineHeight = dimens.fonts.sideSheetEntry * 1.2f,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (unlocked) 1f else 0.6f),
            )
            CaptionText(skill.method)
        }
        Spacer(Modifier.width(8.dp))
        StarRow(stars = sp.stars, size = dimens.icons.problemStatus * 0.7f)
    }
}
