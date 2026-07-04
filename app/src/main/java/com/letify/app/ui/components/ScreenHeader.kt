package com.letify.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    trailingIcon: String? = null,
    trailingAccent: Boolean = false,
    trailingGhost: Boolean = true,
    onTrailingClick: () -> Unit = {},
) {
    // Title (and optional subtitle above it) sit dead-centre in a Box;
    // the trailing icon floats on the right edge so it doesn't push the
    // title off-centre. Matches the centred title style the user asked for.
    Box(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .padding(top = 6.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
            Text(
                text = title,
                color = Letify.colors.text,
                style = Letify.typography.displayMedium,
            )
        }
        if (trailingIcon != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButtonRound(
                    icon = trailingIcon,
                    accent = trailingAccent,
                    ghost = trailingGhost && !trailingAccent,
                    onClick = onTrailingClick,
                )
            }
        }
    }
}

@Composable
fun IconButtonRound(
    icon: String,
    accent: Boolean = false,
    ghost: Boolean = false,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    // Default: no backing plate at all (ghost). Only when `accent` is set do
    // we draw the soft circular halo behind the icon. This matches "+" buttons
    // in the prototype that don't have container backgrounds.
    val tint = when {
        accent -> Letify.colors.accent
        else -> Letify.colors.text
    }
    NoFeedbackButton(onClick = onClick, modifier = Modifier.size(size)) {
        Box(
            Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = tint, size = iconSize)
        }
    }
    // ghost intentionally unused — kept for source-compatibility.
    @Suppress("UNUSED_EXPRESSION") ghost
}

@Composable
fun SectionTitle(
    text: String,
    topPadding: Dp = 18.dp,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .padding(start = 4.dp, end = 4.dp, top = topPadding, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text,
            color = Letify.colors.muted,
            style = Letify.typography.titleSmall,
        )
        if (trailing != null) trailing()
    }
}
