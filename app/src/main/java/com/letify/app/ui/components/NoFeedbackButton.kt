package com.letify.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/** Clickable that produces zero visual feedback: no ripple, no scale, no color change. */
@Composable
fun Modifier.noFeedbackClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
    )
}

/**
 * Like [noFeedbackClick] but with a long-press affordance — still zero visual
 * feedback (no ripple/scale/colour). A short haptic tick fires on long-press so
 * the gesture is discoverable. Used e.g. by selectable cards: tap selects,
 * long-press opens that item's settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.noFeedbackCombinedClick(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    return this.then(
        Modifier.combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick?.let {
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    it()
                }
            },
        )
    )
}

/**
 * Long-press "peek" gesture used by the Plan cards. Combines a plain tap with a
 * drag-after-long-press so we can drive iOS-style drag-to-select on the context
 * menu:
 *   • tap            → [onTap]
 *   • long-press     → a LongPress haptic fires and [onPeekStart] is called
 *                      with the finger position in WINDOW coordinates
 *   • move (no lift) → [onPeekMove] with the live window position
 *   • lift/cancel    → [onPeekEnd]
 *
 * Window coordinates are produced by adding the element's own window origin
 * (captured via onGloballyPositioned) to the gesture's element-local offset, so
 * the overlay can hit-test its menu rows regardless of where the card sits.
 */
@Composable
fun Modifier.peekGesture(
    longPressEnabled: Boolean = true,
    onTap: () -> Unit,
    onPeekStart: (Offset) -> Unit,
    onPeekMove: (Offset) -> Unit,
    onPeekEnd: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    // The pointerInput blocks below are keyed on a constant (Unit) /
    // longPressEnabled, so their lambdas capture the callback closures from the
    // composition that first launched them. Cards composed BEFORE entering
    // selection mode (or before a state like `expanded` changed) would then keep
    // firing a STALE onTap — which is why selecting some tasks and expanding
    // subtask cards silently stopped working. rememberUpdatedState keeps a live
    // handle to the latest callbacks without restarting the gesture detector.
    val tap by rememberUpdatedState(onTap)
    val peekStart by rememberUpdatedState(onPeekStart)
    val peekMove by rememberUpdatedState(onPeekMove)
    val peekEnd by rememberUpdatedState(onPeekEnd)
    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .pointerInput(Unit) {
            detectTapGestures(onTap = { tap() })
        }
        .pointerInput(longPressEnabled) {
            if (longPressEnabled) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { off ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        peekStart(origin + off)
                    },
                    onDrag = { change, _ -> peekMove(origin + change.position) },
                    onDragEnd = { peekEnd() },
                    onDragCancel = { peekEnd() },
                )
            }
        }
}

/**
 * Discrete tappable button. By default it now gives a subtle, consistent
 * press "squat" (gentle scale-down on press, springs back) so every button in
 * the app reacts to touch the same way — matches the Profile quick-action
 * tiles. Pass `feedback = false` to keep the perfectly-still behaviour (used by
 * the navbar, where only the pill underneath is supposed to move).
 *
 * The squat is a pure transform (no layout reflow), so it's safe to apply to
 * any sized button — icon chips, tiles or full-width CTAs alike.
 */
@Composable
fun NoFeedbackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    feedback: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (feedback && pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "buttonSquat",
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        content()
    }
}
