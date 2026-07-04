package com.letify.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify

/**
 * Telegram-style vivid rounded tile with a centred white icon — the SAME
 * treatment Letify's SettingsRow uses (30dp, radius 11, vertical sheen),
 * factored out so role/question/stat rows can reuse it 1:1, including the crisp
 * semi-transparent white top-edge rim (the glossy Telegram/iOS look).
 */
@Composable
fun IconTile(
    tile: Color,
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    tint: Color = Color.White,
) {
    val top = lerp(tile, Color.White, 0.18f)
    val bottom = lerp(tile, Color.Black, 0.13f)
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to top,
                    0.34f to tile,
                    1f to bottom,
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.42f),
                    0.6f to Color.Transparent,
                ),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SolarIcon(name = icon, tint = tint, size = size * 0.66f)
    }
}

/**
 * Muted, uppercase section caption sitting above a settings card group —
 * matches Letify's section labels exactly (labelSmall, muted, start = 28dp).
 */
@Composable
fun SecTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = Letify.colors.muted,
        style = Letify.typography.labelSmall,
        modifier = modifier.padding(start = 28.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Small muted hint paragraph under a card group. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Letify.colors.muted,
        style = Letify.typography.bodySmall,
        modifier = modifier.padding(horizontal = 28.dp, vertical = 10.dp),
    )
}

/**
 * Pinned screen header — a 1:1 copy of Letify's SettingsHeader: a ghost
 * back-arrow (alt-arrow-left-outline, 28dp, no plate) + the title in
 * headlineMedium, with an optional trailing slot. Used by every sub-screen so
 * the header size/typography is identical everywhere.
 */
@Composable
fun SubHeader(
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SolarIcon(name = "alt-arrow-left-outline", tint = Letify.colors.text, size = 28.dp)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            color = Letify.colors.text,
            style = Letify.typography.headlineLarge,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

/**
 * Height of the pinned title bar that [ScreenFrame] reserves at the top of
 * every screen. The bar itself is TRANSPARENT — content scrolls underneath it
 * and a soft [TopScrim] keeps the title legible without any hard plate.
 */
val HeaderBarHeight: Dp = 52.dp

/**
 * A soft, multi-stop top fade drawn behind the pinned header.
 */
@Composable
fun TopScrim(topInset: Dp) {
    val bg = Letify.colors.bg
    Box(
        Modifier
            .fillMaxWidth()
            .height(topInset + HeaderBarHeight + 10.dp)
            .background(
                Brush.verticalGradient(
                    0.00f to bg.copy(alpha = 0.80f),
                    0.30f to bg.copy(alpha = 0.66f),
                    0.50f to bg.copy(alpha = 0.48f),
                    0.66f to bg.copy(alpha = 0.30f),
                    0.80f to bg.copy(alpha = 0.16f),
                    0.90f to bg.copy(alpha = 0.06f),
                    1.00f to Color.Transparent,
                ),
            ),
    )
}

/**
 * Shared screen chrome for EVERY screen (sections and sub-screens alike).
 */
@Composable
fun ScreenFrame(
    header: @Composable () -> Unit,
    topInsetOverride: Dp? = null,
    body: @Composable (topPad: Dp) -> Unit,
) {
    // When embedded inside the «Новые анкеты» sheet (which already sits below the
    // status bar) the caller passes a small topInsetOverride so we don't add the
    // full status-bar inset a SECOND time (that was the extra gap at the top).
    val topInset = topInsetOverride
        ?: WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPad = topInset + HeaderBarHeight + 6.dp
    Box(Modifier.fillMaxSize()) {
        body(topPad)
        TopScrim(topInset)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = topInset)
                .height(HeaderBarHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            header()
        }
    }
}

/** Accent text action used in the top-right of some sub-screen headers ("Готово"). */
@Composable
fun HeaderTextAction(text: String, onClick: () -> Unit) {
    NoFeedbackButton(onClick = onClick, modifier = Modifier.padding(end = 8.dp)) {
        Text(text, color = Letify.colors.accent, style = Letify.typography.titleMedium)
    }
}

/** Rounded input field — container-filled, accent cursor, single style everywhere. */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Letify.colors.container, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, color = Letify.colors.muted, style = Letify.typography.bodyLarge)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = Letify.typography.bodyLarge.copy(color = Letify.colors.text),
            cursorBrush = SolidColor(Letify.colors.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}