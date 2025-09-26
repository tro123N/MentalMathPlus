package com.helddertierwelt.mentalmath.presentation.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.helddertierwelt.mentalmath.presentation.theme.red

@Composable
fun ButtonRow(
    iconResId: Int,
    contentDescription: String,
    text: String,
    fontSize: TextUnit,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,

    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = red
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = fontSize,
            lineHeight = fontSize,
            color = red
        )
    }
}