package com.letify.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class LetifyColorScheme(
    val bg: Color,
    val container: Color,
    val text: Color,
    val muted: Color,
    val track: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val LocalLetifyColors = staticCompositionLocalOf<LetifyColorScheme> {
    error("LetifyColors not provided")
}

object Letify {
    val colors: LetifyColorScheme
        @Composable
        get() = LocalLetifyColors.current
    val typography
        @Composable
        get() = MaterialTheme.typography
    val shapes = LetifyShapes
}

private fun darkScheme(accent: Color) = LetifyColorScheme(
    bg = LetifyColors.DarkBg,
    container = LetifyColors.DarkContainer,
    text = LetifyColors.DarkText,
    muted = LetifyColors.MutedDark,
    track = LetifyColors.TrackDark,
    accent = accent,
    accentSoft = accent.copy(alpha = 0.16f),
    onAccent = Color(0xFF0C1F12),
    isDark = true,
)

private fun lightScheme(accent: Color) = LetifyColorScheme(
    bg = LetifyColors.LightBg,
    container = LetifyColors.LightContainer,
    text = LetifyColors.LightText,
    muted = LetifyColors.MutedLight,
    track = LetifyColors.TrackLight,
    accent = accent,
    accentSoft = accent.copy(alpha = 0.16f),
    onAccent = Color(0xFF0C1F12),
    isDark = false,
)

enum class ThemeMode { Light, Dark, System }

@Composable
fun LetifyTheme(
    mode: ThemeMode = ThemeMode.Dark,
    accent: Color = LetifyColors.Mint,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> systemDark
    }
    val target = if (dark) darkScheme(accent) else lightScheme(accent)

    // Theme / accent changes apply INSTANTLY - no cross-fade. The user asked to
    // remove the smooth theme transition, so we use the target scheme directly
    // instead of animating each colour over ~420ms.
    val wColors = target

    val material = if (dark) {
        darkColorScheme(
            primary = wColors.accent,
            onPrimary = Color(0xFF0C1F12),
            background = wColors.bg,
            surface = wColors.container,
            onBackground = wColors.text,
            onSurface = wColors.text,
        )
    } else {
        lightColorScheme(
            primary = wColors.accent,
            onPrimary = Color(0xFF0C1F12),
            background = wColors.bg,
            surface = wColors.container,
            onBackground = wColors.text,
            onSurface = wColors.text,
        )
    }

    CompositionLocalProvider(LocalLetifyColors provides wColors) {
        MaterialTheme(
            colorScheme = material,
            typography = LetifyTypography,
            content = content,
        )
    }
}
