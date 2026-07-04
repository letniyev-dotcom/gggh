package com.letify.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify

/**
 * Pill-shaped toggle in the spirit of Telegram / iOS settings.
 *
 *   * Track colour fades through accent (tween 220 ms) — no abrupt
 *     swap on tap.
 *   * Thumb glides with a soft underdamped spring so it visibly
 *     *slides* and gently overshoots at the end of travel.
 *   * The thumb briefly elongates in the direction of motion on every
 *     toggle (scaleX 1 → 1.12 → 1) so it reads as physical, like the
 *     iOS system switch.
 */
@Composable
fun AccentSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = 52.dp
    val height = 32.dp
    val thumbSize = 26.dp
    val padding = 3.dp
    val trackOff = Letify.colors.text.copy(alpha = 0.18f)
    val trackOn = Letify.colors.accent
    val trackColor by animateColorAsState(
        if (checked) trackOn else trackOff,
        animationSpec = tween(durationMillis = 280),
        label = "switchtrack",
    )
    val offsetX by animateDpAsState(
        if (checked) width - thumbSize - padding else padding,
        // Softer spring — visibly slides ~260 ms with a small overshoot
        // at the end. R17 lowered stiffness 240→180 because users
        // reported the previous setting "didn't animate" on the
        // Notifications screen (the slide was finishing in <100 ms,
        // basically a snap).
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f),
        label = "switchthumb",
    )
    // Per-toggle "squish" pulse. We skip the initial composition so
    // every freshly-opened screen doesn't kick off a phantom pulse on
    // each switch — the very first call to LaunchedEffect just records
    // the initial state, and only subsequent toggles trigger the
    // snapTo(1) → spring-back animation.
    val pulse = remember { Animatable(0f) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(checked) {
        if (firstComposition) {
            firstComposition = false
            return@LaunchedEffect
        }
        pulse.snapTo(1f)
        pulse.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 180f),
        )
    }
    Box(
        modifier
            .width(width)
            .height(height)
            .background(trackColor, RoundedCornerShape(999.dp))
            .noFeedbackClick { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = offsetX, y = padding)
                .size(thumbSize)
                .graphicsLayer {
                    val s = 1f + pulse.value * 0.18f
                    scaleX = s
                    scaleY = 2f - s
                }
                .background(Color.White, RoundedCornerShape(999.dp)),
        )
    }
}
