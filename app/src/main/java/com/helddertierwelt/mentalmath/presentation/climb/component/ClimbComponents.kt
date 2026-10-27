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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.MentalMathTheme
import com.helddertierwelt.mentalmath.presentation.theme.yellow

/** Obere Leiste im Stil der App mit optionalem Zurück-Pfeil. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimbTopBar(
    title: String,
    navigationIcon: Int? = R.drawable.arrow_back_24dp,
    navigationTint: Color = MaterialTheme.colorScheme.onPrimary,
    onNavigation: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val dimens = MentalMathTheme.dimens
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = dimens.fonts.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        navigationIcon = {
            if (navigationIcon != null && onNavigation != null) {
                IconButton(
                    onClick = onNavigation,
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    Icon(
                        painter = painterResource(navigationIcon),
                        contentDescription = "Zurück",
                        tint = navigationTint,
                        modifier = Modifier.size(dimens.icons.topBarAction),
                    )
                }
            }
        },
        actions = actions,
    )
}

/** Abgerundete Karte wie in der Ergebnisansicht der App. */
@Composable
fun ClimbCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = MentalMathTheme.dimens
    Card(
        shape = RoundedCornerShape(dimens.components.cardCornerRadius),
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            content = content,
        )
    }
}

/** Großer Knopf über die ganze Breite, Symbol links – wie der Start-Knopf der App. */
@Composable
fun ActionButton(
    text: String,
    icon: Int,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
) {
    val dimens = MentalMathTheme.dimens
    val contentAlpha = if (enabled) 1f else 0.38f
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(dimens.components.cornerRadius * 2),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.components.button * 0.8f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint.copy(alpha = contentAlpha * iconTint.alpha),
                modifier = Modifier.size(dimens.icons.menuItem),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    fontSize = dimens.fonts.settingsLabel,
                    lineHeight = dimens.fonts.settingsLabel * 1.2f,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = contentAlpha),
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = dimens.fonts.caption,
                        lineHeight = dimens.fonts.caption * 1.3f,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f * contentAlpha),
                    )
                }
            }
        }
    }
}

/** Drei Sterne, erreichte gelb, fehlende blass. */
@Composable
fun StarRow(stars: Int, size: Dp, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            Icon(
                painter = painterResource(R.drawable.star_24dp),
                contentDescription = null,
                tint = if (i < stars) yellow else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(size),
            )
        }
    }
}

/** Runde Plakette eines Levels: Nummer, Schloss oder Haken. */
@Composable
fun LevelBadge(
    label: String,
    locked: Boolean,
    mastered: Boolean,
    attention: Boolean,
    highlighted: Boolean,
    size: Dp,
) {
    val ringColor = when {
        attention -> yellow
        highlighted -> com.helddertierwelt.mentalmath.presentation.theme.blue
        mastered -> com.helddertierwelt.mentalmath.presentation.theme.green
        else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .alpha(if (locked) 0.45f else 1f)
            .border(width = 3.dp, color = ringColor, shape = CircleShape)
            .background(MaterialTheme.colorScheme.background, CircleShape),
    ) {
        when {
            locked -> Icon(
                painter = painterResource(R.drawable.lock_24dp),
                contentDescription = "gesperrt",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(size * 0.45f),
            )

            attention -> Icon(
                painter = painterResource(R.drawable.refresh_24dp),
                contentDescription = "auffrischen",
                tint = yellow,
                modifier = Modifier.size(size * 0.5f),
            )

            else -> Text(
                text = label,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = MentalMathTheme.dimens.fonts.sideSheetEntry,
            )
        }
    }
}

/** Kleine Überschrift innerhalb einer Karte. */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    Text(
        text = text,
        fontSize = dimens.fonts.settingsLabel,
        lineHeight = dimens.fonts.settingsLabel * 1.2f,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

/** Fließtext in einer Karte. */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onPrimary) {
    val dimens = MentalMathTheme.dimens
    Text(
        text = text,
        fontSize = dimens.fonts.sideSheetEntry,
        lineHeight = dimens.fonts.sideSheetEntry * 1.35f,
        color = color,
        modifier = modifier,
    )
}

/** Kleine graue Zusatzzeile. */
@Composable
fun CaptionText(text: String, modifier: Modifier = Modifier) {
    val dimens = MentalMathTheme.dimens
    Text(
        text = text,
        fontSize = dimens.fonts.caption,
        lineHeight = dimens.fonts.caption * 1.35f,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        modifier = modifier,
    )
}

/** Abstand zwischen Karten. */
@Composable
fun CardGap() = Spacer(Modifier.height(16.dp))

/** Hintergrund-Fläche aller Kletterweg-Seiten. */
@Composable
fun ScreenBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) { content() }
}
