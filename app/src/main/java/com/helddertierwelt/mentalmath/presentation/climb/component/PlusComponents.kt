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

package com.helddertierwelt.mentalmath.presentation.climb.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.accent

/** Die drei Bereiche der App: Kletterweg, freies Training und der klassische Modus der Original-App. */
enum class PlusTab(val route: String, val title: String, val icon: Int, val tag: String) {
    CLIMB("climb", "Kletterweg", R.drawable.terrain_24dp, "tab_climb"),
    FREE("free", "Freies Training", R.drawable.tune_24dp, "tab_free"),
    CLASSIC("settings", "Klassisch", R.drawable.calculate_24dp, "tab_classic"),
}

/** Zu einem Bereich wechseln: der Kletterweg bleibt unten im Verlauf, „Zurück“ führt immer dorthin. */
fun NavController.openTab(tab: PlusTab) {
    val hasClimb = try {
        getBackStackEntry(PlusTab.CLIMB.route)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
    if (!hasClimb) {
        navigate(PlusTab.CLIMB.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
    if (tab == PlusTab.CLIMB) {
        popBackStack(PlusTab.CLIMB.route, inclusive = false)
        return
    }
    navigate(tab.route) {
        popUpTo(PlusTab.CLIMB.route) { inclusive = false }
        launchSingleTop = true
    }
}

/** War „Klassisch“ auf der vorigen Seite ausgeklappt? */
private var classicShownBefore = false

/**
 * Leiste unten zum Wechseln zwischen den Bereichen. „Klassisch“ gehört zum freien Üben: Der Knopf
 * fährt erst neben „Freies Training“ heraus, wenn man dort ist.
 */
@Composable
fun PlusBottomBar(navController: NavController, current: PlusTab, onTap: () -> Unit = {}) {
    val showClassic = current != PlusTab.CLIMB
    // Startet im Zustand der vorigen Seite, damit der Knopf nur beim Wechsel hinein- oder herausfährt.
    var revealed by remember { mutableStateOf(classicShownBefore) }
    LaunchedEffect(showClassic) {
        revealed = showClassic
        classicShownBefore = showClassic
    }
    val classicWeight by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.001f,
        animationSpec = tween(durationMillis = 350),
        label = "klassisch",
    )
    Surface(color = MaterialTheme.colorScheme.primary) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 8.dp),
        ) {
            PlusTab.entries.forEach { tab ->
                val weight = if (tab == PlusTab.CLASSIC) classicWeight else 1f
                BarItem(
                    tab = tab,
                    selected = tab == current,
                    onClick = {
                        if (tab != current) {
                            onTap()
                            navController.openTab(tab)
                        }
                    },
                    modifier = Modifier
                        .weight(weight)
                        .alpha(if (tab == PlusTab.CLASSIC) classicWeight.coerceIn(0f, 1f) else 1f),
                )
            }
        }
    }
}

@Composable
private fun BarItem(tab: PlusTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (selected) accent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clipToBounds()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(tab.tag),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent)
                .padding(horizontal = 18.dp, vertical = 4.dp),
        ) {
            Icon(
                painter = painterResource(tab.icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = tab.title,
            fontSize = 12.sp,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

/** Kurze Erklärung oben in einem Bereich: wofür er da ist. */
@Composable
fun AreaIntro(icon: Int, title: String, text: String, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.components.cornerRadius))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(dimens.icons.menuItem),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = dimens.fonts.sideSheetEntry,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            CaptionText(text)
        }
    }
}

/** Kleiner Knopf mit Symbol und Beschriftung, z. B. drei nebeneinander. */
@Composable
fun MiniAction(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = accent,
) {
    val dimens = MentalMathTheme.dimens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(dimens.components.cornerRadius))
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(dimens.icons.menuItem),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = dimens.fonts.caption,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Schalter in den Farben des Farbthemas. */
@Composable
fun PlusSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = accent,
            checkedBorderColor = accent,
            uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
            uncheckedTrackColor = MaterialTheme.colorScheme.background,
            uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
        ),
        modifier = modifier,
    )
}

/** Zeile mit Text links und Schalter rechts. */
@Composable
fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dimens = MentalMathTheme.dimens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.components.cornerRadius))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = dimens.fonts.sideSheetEntry,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (enabled) 1f else 0.5f),
            )
            if (subtitle != null) CaptionText(subtitle)
        }
        Spacer(Modifier.width(12.dp))
        PlusSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
