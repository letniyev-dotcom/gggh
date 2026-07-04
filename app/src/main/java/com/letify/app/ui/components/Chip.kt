package com.letify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify

@Composable
fun Chip(
    text: String,
    active: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val bg = if (active) Letify.colors.accentSoft else Letify.colors.track
    val color = if (active) Letify.colors.accent else Letify.colors.text
    NoFeedbackButton(onClick = onClick, modifier = modifier) {
        Row(
            Modifier
                .background(bg, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, color = color, style = Letify.typography.labelMedium)
        }
    }
}
