package com.letify.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify

/**
 * Reusable fully-rounded progress bar with smooth colour interpolation
 * from red -> orange -> yellow -> mint as the value approaches 100 %.
 * Ported 1:1 from Letify's GoalProgressBar (previews dropped). The colour
 * gradient communicates "health" (red = low, mint = done), independent of
 * the app accent, so it always reads intuitively.
 */
@Composable
fun GoalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = 18.dp,
    animated: Boolean = true,
) {
    val target = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animated) tween(durationMillis = 700, easing = EaseOutCubic)
                        else tween(durationMillis = 0),
        label = "goal-progress-width",
    )
    val targetColor = progressColor(target)
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = if (animated) tween(durationMillis = 500) else tween(durationMillis = 0),
        label = "goal-progress-color",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(RoundedCornerShape(50))
            .background(Letify.colors.track),
    ) {
        if (animatedProgress > 0.005f) {
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                animatedColor,
                                lerp(animatedColor, Color.White, 0.18f),
                            ),
                        ),
                    ),
            )
        }
    }
}

/** Maps progress (0..1) to the bar colour via piecewise-linear interpolation. */
fun progressColor(progress: Float): Color {
    val p = progress.coerceIn(0f, 1f)
    val red = Color(0xFFF06262)
    val amber = Color(0xFFFFB347)
    val yellow = Color(0xFFEFCF4A)
    val mint = Color(0xFF7CD992)
    return when {
        p <= 0.35f -> lerp(red, amber, p / 0.35f)
        p <= 0.55f -> lerp(amber, yellow, (p - 0.35f) / 0.20f)
        else -> lerp(yellow, mint, (p - 0.55f) / 0.45f)
    }
}
