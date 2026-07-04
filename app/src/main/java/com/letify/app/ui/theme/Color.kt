package com.letify.app.ui.theme

import androidx.compose.ui.graphics.Color

object LetifyColors {
    // Backgrounds per spec
    val DarkBg = Color(0xFF000000)
    val DarkContainer = Color(0xFF181818)
    val LightBg = Color(0xFFF1F1F3)
    val LightContainer = Color(0xFFFFFFFF)

    // Text
    val DarkText = Color(0xFFF4F4F5)
    val LightText = Color(0xFF18181B)
    val MutedDark = Color(0xFF8A8A92)
    // Darker muted in light theme — the previous #8A8A92 sat too low in
    // contrast on a white surface and made captions / chevrons look
    // washed out. #6B6B73 reads as a confident secondary tone while
    // staying clearly subordinate to the body text #18181B.
    val MutedLight = Color(0xFF6B6B73)

    // Tracks (ring background / progress track)
    val TrackDark = Color(0x14FFFFFF)
    val TrackLight = Color(0x14000000)

    // Semantic palette (theme-independent)
    val Water = Color(0xFF5AA7FF)
    val Cal = Color(0xFFFF8C5A)
    val Protein = Color(0xFFFF6B9D)
    val Fat = Color(0xFFFFD166)
    val Carb = Color(0xFFB084F5)
    val Purple = Color(0xFFB084F5)
    val Orange = Color(0xFFFF8C5A)
    val Pink = Color(0xFFFF6B9D)
    val Mint = Color(0xFF7CD992)
    val Yellow = Color(0xFFFFD166)

    // Telegram-style settings tile colours — saturated, theme-independent
    // squares with a white icon centred inside. Match the screenshot the
    // user shared.
    val TileBlue = Color(0xFF3FA8F5)
    val TileOrange = Color(0xFFFFA63D)
    val TileGreen = Color(0xFF4ECB71)
    val TileRed = Color(0xFFF26A5C)
    val TileCyan = Color(0xFF45C8E8)
    val TileSky = Color(0xFF5BB7FF)
    val TileViolet = Color(0xFFB286FF)
    val TileTeal = Color(0xFF3DBFA8)
    val TilePink = Color(0xFFFF6E97)
    val TileLemon = Color(0xFFEEC53A)

    // Softer accent-toned variants used for "ПЕРИОД" rows where the
    // saturated TileLemon / TileSky read as visually loud against the
    // rest of the screen. These are paired with `iconTint = accent`
    // (not white) and a low-alpha tile fill, so the row reads as a
    // muted glyph chip instead of a brash colour chip.
    val AccentAmber = Color(0xFFFFA94D)
    val AccentTeal = Color(0xFF3DBFA8)

    // Brand blue used as the Telegram tile fill — close to the gradient
    // mid-tone of the official Telegram round-logo so the binding entry
    // reads as "Telegram" at a glance.
    val TelegramBlue = Color(0xFF2AABEE)
    val TelegramBlueDeep = Color(0xFF229ED9)
}

/**
 * 18 accent colours arranged in a smooth hue circle (violet → blue → green
 * → yellow → red → pink). Six per row, three rows. Designed to read
 * pleasantly on both light and dark backgrounds.
 */
val AccentPalette: List<Color> = listOf(
    // Reference palette pulled directly from the user's reference
    // Android build (letify-android-fixed-13.zip) — pleasant,
    // saturated but not glaring. Three smooth hue rows of six: violet
    // → blue → cyan, teal → green → lime, yellow → orange → pink/rose.
    Color(0xFFB084F5), // lavender purple
    Color(0xFF8E7CF0), // violet
    Color(0xFF6E86F2), // periwinkle
    Color(0xFF5AA7FF), // blue
    Color(0xFF4EC4E8), // sky cyan
    Color(0xFF45CBC5), // teal cyan
    Color(0xFF4ABFA0), // teal green
    Color(0xFF7CD992), // mint
    Color(0xFF66CF6E), // green
    Color(0xFFB7D858), // lime
    Color(0xFFEFCF4A), // yellow
    Color(0xFFFFB347), // amber
    Color(0xFFFF8C5A), // orange
    Color(0xFFF77D6D), // coral
    Color(0xFFF06262), // red
    Color(0xFFFF6B9D), // pink
    Color(0xFFE57AC9), // magenta
    Color(0xFFD08CE2), // rose lilac
)

/**
 * Accent picker shown on the *Appearance → Theme* screen. This is the
 * one swatch that tints the entire app (buttons, switches, ring "fill"
 * colour). Eighteen vibrant tints — identical hue rhythm to
 * [AccentPalette] for visual consistency, but tuned slightly brighter
 * so they read as "theme tints" on white. No greys / browns.
 */
val ThemePalette: List<Color> = AccentPalette
