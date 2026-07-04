package com.letify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify

/**
 * Telegram-style row used inside the settings container. Each row owns a
 * vivid coloured rounded square with a white icon centred inside, a label
 * and an optional trailing value / chevron.
 */
@Composable
fun SettingsRow(
    icon: String,
    iconTile: Color,
    title: String,
    iconTint: Color = Color.White,
    value: String? = null,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.noFeedbackClick(onClick = onClick) else it }
        .padding(horizontal = 12.dp, vertical = 8.dp)
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        // Telegram-style tile: vivid base with a subtle vertical sheen PLUS a
        // crisp semi-transparent white top-edge rim (the glossy iOS/Telegram
        // look) — brought over 1:1 from the Letify SettingsRow tile.
        val tileTop = lerp(iconTile, Color.White, 0.18f)
        val tileBottom = lerp(iconTile, Color.Black, 0.13f)
        val tileShape = RoundedCornerShape(11.dp)
        Box(
            Modifier
                .size(30.dp)
                .clip(tileShape)
                .background(
                    Brush.verticalGradient(
                        0f to tileTop,
                        0.34f to iconTile,
                        1f to tileBottom,
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.42f),
                        0.6f to Color.Transparent,
                    ),
                    shape = tileShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = iconTint, size = 20.dp)
        }
        Box(Modifier.width(14.dp))
        Text(
            title,
            color = Letify.colors.text,
            style = Letify.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Box(Modifier.width(8.dp))
            trailing()
            if (showChevron) Box(Modifier.width(8.dp))
        } else if (value != null) {
            Box(Modifier.width(8.dp))
            Text(
                value,
                color = Letify.colors.muted,
                style = Letify.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 170.dp),
            )
            if (showChevron) Box(Modifier.width(8.dp))
        }
        if (showChevron) {
            SolarIcon(
                name = "alt-arrow-right-outline",
                tint = Letify.colors.muted,
                size = 20.dp,
            )
        }
    }
}

/**
 * Thin 1px-ish divider sitting under settings rows, indented to align with
 * the row text (i.e. starts after the icon tile).
 */
@Composable
fun SettingsRowDivider(insetStart: Dp = 56.dp) {
    // Dividers are permanently disabled across the whole app — settings rows
    // always sit flush with no grey separator line. Kept as a no-op so existing
    // call sites stay valid.
}

/** Card container shared by all settings groups (Telegram-style ~22dp radius). */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Letify.colors.container, RoundedCornerShape(22.dp))
            .padding(contentPadding),
    ) {
        Column { content() }
    }
}

/**
 * Standard sticky-style header used on every settings screen — a thin outline
 * chevron tap-target on the left and the page title next to it. No filled
 * background plate around the arrow so it reads as a proper Telegram-style
 * back button rather than a chunky chip.
 */
@Composable
fun SettingsHeader(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoFeedbackButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SolarIcon(
                    name = "alt-arrow-left-outline",
                    tint = Letify.colors.text,
                    size = 28.dp,
                )
            }
        }
        Box(Modifier.width(4.dp))
        Text(
            title,
            color = Letify.colors.text,
            style = Letify.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

/**
 * Compact accent checkmark button suitable for the top-right slot of
 * a [SettingsHeader] — replaces a heavier pinned bottom CTA. Rendered
 * as a soft accent pill with a bold check glyph; disabled state fades
 * to muted so the user can still see the affordance.
 */
@Composable
fun HeaderCheckButton(enabled: Boolean = true, onClick: () -> Unit) {
    // No backing pill — just the glyph. Accent when actionable, muted when disabled.
    val tint = if (enabled) Letify.colors.accent else Letify.colors.muted
    NoFeedbackButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SolarIcon(name = "check-bold", tint = tint, size = 26.dp)
        }
    }
}