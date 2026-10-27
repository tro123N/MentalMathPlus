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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.presentation.climb.ClimbViewModel
import com.helddertierwelt.mentalmath.presentation.climb.component.ActionButton
import com.helddertierwelt.mentalmath.presentation.climb.component.BodyText
import com.helddertierwelt.mentalmath.presentation.climb.component.CaptionText
import com.helddertierwelt.mentalmath.presentation.climb.component.CardTitle
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbCard
import com.helddertierwelt.mentalmath.presentation.climb.component.ClimbTopBar
import com.helddertierwelt.mentalmath.presentation.climb.component.ExplanationBlocks
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.blue

/** Alles, was in einer Erklärung als Text vorkommt – für die Suche. */
private fun searchText(skill: Skill): String = buildString {
    append(skill.title).append(' ').append(skill.method).append(' ').append(skill.summary).append(' ').append(skill.book.label)
    skill.explanation.forEach { block ->
        when (block) {
            is Block.Text -> append(' ').append(block.text)
            is Block.Steps -> block.items.forEach { append(' ').append(it) }
            is Block.Tip -> append(' ').append(block.text)
            is Block.Formula -> append(' ').append(block.text)
            is Block.Example -> append(' ').append(block.problem.prompt.text)
        }
    }
}.lowercase()

/**
 * Handbuch: alle Tricks und Methoden des Kletterwegs zum Nachschlagen, nach Welten geordnet.
 * Es wird aus dem Lehrplan erzeugt und wächst deshalb mit jedem neuen Level automatisch mit.
 */
@Composable
fun HandbookScreen(navController: NavController) {
    val dimens = MentalMathTheme.dimens
    val owner = LocalActivity.current as ViewModelStoreOwner
    val viewModel: ClimbViewModel = hiltViewModel(owner)
    var query by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf("") }

    fun back() {
        viewModel.playTap()
        if (!navController.popBackStack()) navController.navigate("climb") { launchSingleTop = true }
    }
    BackHandler { back() }

    val words = query.lowercase().split(' ').filter { it.isNotBlank() }
    val worlds = viewModel.worlds.map { world ->
        world to world.skills.filter { skill -> words.isEmpty() || searchText(skill).let { t -> words.all { it in t } } }
    }.filter { it.second.isNotEmpty() }
    val count = worlds.sumOf { it.second.size }

    Scaffold(topBar = { ClimbTopBar(title = "Handbuch", onNavigation = { back() }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("handbookList"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ClimbCard {
                    BodyText("Alle Tricks und Methoden auf einen Blick – zum Nachschlagen, auch für Levels, die noch nicht freigeschaltet sind.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Suchen, z. B. Quadrat, Prozent, Wochentag") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            focusedBorderColor = blue,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("handbookSearch"),
                    )
                    Spacer(Modifier.height(6.dp))
                    CaptionText(if (words.isEmpty()) "$count Methoden" else "$count Treffer")
                }
            }
            worlds.forEach { (world, skills) ->
                item(key = world.id) {
                    ClimbCard {
                        CardTitle("Welt ${world.number} · ${world.title}")
                        CaptionText(world.subtitle)
                        Spacer(Modifier.height(6.dp))
                        skills.forEach { skill ->
                            val isOpen = open == skill.id
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(dimens.components.cornerRadius))
                                    .clickable {
                                        viewModel.playTap()
                                        open = if (isOpen) "" else skill.id
                                    }
                                    .testTag("hb_${skill.id}")
                                    .padding(vertical = 8.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(skill.title, fontSize = dimens.fonts.sideSheetEntry, color = MaterialTheme.colorScheme.onPrimary)
                                    CaptionText(skill.method)
                                }
                                Icon(
                                    painter = painterResource(if (isOpen) R.drawable.keyboard_arrow_up_24dp else R.drawable.keyboard_arrow_down_24dp),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(dimens.icons.menuItem),
                                )
                            }
                            if (isOpen) {
                                CaptionText("Im Buch: ${skill.book.label}")
                                Spacer(Modifier.height(6.dp))
                                BodyText(skill.summary)
                                Spacer(Modifier.height(10.dp))
                                ExplanationBlocks(skill.explanation)
                                ActionButton(
                                    text = "Zum Level",
                                    subtitle = "Üben und Prüfung",
                                    icon = R.drawable.terrain_24dp,
                                    iconTint = blue,
                                    onClick = {
                                        viewModel.playTap()
                                        navController.navigate("level/${skill.id}")
                                    },
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
