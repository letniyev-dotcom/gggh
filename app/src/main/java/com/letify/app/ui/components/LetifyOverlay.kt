package com.letify.app.ui.components

import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.TransitionStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Overlay "at rest" signal ────────────────────────────────────────────────
//
// True only when the hosting overlay/sheet has FINISHED its open animation and
// is NOT being dragged. Heavy, perpetually-animating backgrounds (e.g. the
// «Аура» glow) read this and stay on a single STATIC frame
// while the card is sliding in or being dragged out — so their per-frame draw
// work never competes with the slide/drag and the open feels instant + async.
// Defaults to a constant `true` for any content not under a sheet.
val LocalOverlaySettled: androidx.compose.runtime.ProvidableCompositionLocal<State<Boolean>> =
    compositionLocalOf { mutableStateOf(true) }

// ── True single-driver "push" transition (Material "Shared Axis X" / iOS push) ──
//
// THE WHOLE POINT of this file: the outgoing screen and the incoming screen are
// driven by ONE shared Animatable<Float> (`progress`, in 0..1). There is no
// second animated value and no coroutine that mirrors one value into another.
//
//   progress = 1  → incoming screen fully off the right edge, outgoing centered
//   progress = 0  → incoming screen centered, outgoing fully off the left edge
//
//   • the incoming screen (RoundedSlideOverlay):  translationX =  progress * width
//   • the outgoing screen (OverlayHost):          translationX = -(1 - progress) * width
//
// Both layers read the SAME `progress.value` in their own graphicsLayer, so they
// recompute in the same draw frame and physically cannot desync — the two
// screens move as one continuous strip at the same speed. That is a real push,
// not a parallax/cover (which by definition needs two separate animations).

// Programmatic open / close (tap, back button, predictive-back commit/cancel)
// runs on a fixed-duration ease-out tween — NO spring overshoot, deterministic,
// and the SAME curve+duration as the navbar pill and the tab-switch push so the
// whole app moves on one consistent motion. A spring's tiny terminal bounce is
// exactly the "дёрганость" the user saw on a full-width push.
private val PushEasing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)
// 360ms (was 320): a touch more glide reads noticeably smoother and gives the
// destination screen more time to compose its first frame relative to the
// visible travel, so the very start of the slide no longer feels like a hitch.
private const val PushDurationMs = 360
private val PushSpec = tween<Float>(PushDurationMs, easing = PushEasing)

// Springs are used ONLY for gesture-release (swipe-back fling), where carrying
// the finger's velocity into the settle reads natural. Critically damped
// (DampingRatioNoBouncy = 1.0): the throw already has momentum, so we don't need
// any underdamping — and a dampingRatio < 1 produced a tiny terminal oscillation
// that read as the "подрагивание при остановке" the user felt at the end of a
// swipe-back dismiss. 1.0 settles fast with ZERO overshoot/bounce.
private val OverlaySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 500f,
)

// Snappier spring for the rollback-from-cancel direction.
private val OverlayCancelSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 600f,
)

// Width of the swipe-back "grab" zone measured from the left edge of the overlay.
private val SwipeBackEdgeWidth: Dp = 120.dp

// How far the outgoing (host) screen travels to the LEFT when the incoming
// screen is fully on top, as a fraction of screen width, PER STYLE:
//  - Push  → 1.0: it leaves completely, in lockstep with the incoming screen —
//            one continuous canvas, no parallax gap.
//  - Cover → 0.16: it only shifts a LITTLE (subtle iOS-style parallax) while the
//            new screen slides over it (the classic "наплыв"). iOS' own push
//            parallax is ~0.30, but combined with the dimming scrim below a
//            smaller 0.16 reads as the "совсем чуть-чуть" shift the user wants
//            while still giving a clear sense of depth.
private const val PushHostShiftFraction = 1.0f
private const val CoverHostShiftFraction = 0.16f

// Cover style only: the outgoing screen dims to this peak black alpha as it
// recedes under the incoming screen (iOS' UIParallaxDimmingView). This is the
// real depth cue, so the parallax shift itself can stay subtle.
private const val CoverDimPeakAlpha = 0.16f
private val DimColor = Color.Black

/** Outgoing-screen travel fraction for the given [style]. Shared by [OverlayHost]
 *  and the underlay render in LetifyApp so both shift by the same amount. */
fun overlayHostShiftFraction(style: TransitionStyle): Float =
    if (style == TransitionStyle.Cover) CoverHostShiftFraction else PushHostShiftFraction

// Device physical corner radius fallback (used by the Cover style to round the
// incoming screen's leading corners as it crosses the edge).
private val FallbackDeviceCornerRadius: Dp = 32.dp

// Release thresholds for the swipe-back gesture.
private const val SwipeBackCommitFraction = 0.22f
private val SwipeBackCommitVelocityDpPerSec: Dp = 550.dp

/**
 * A single shared push-progress driver. Create one with [rememberParallaxProgress]
 * and hand the SAME instance to both [OverlayHost] (the screen being pushed away)
 * and [RoundedSlideOverlay] (the screen being pushed in). Both read its `.value`
 * directly, so the two screens animate as one strip.
 */
typealias SlideProgress = Animatable<Float, AnimationVector1D>

/**
 * Renders the host content (the OUTGOING screen) and slides it to the left in
 * lockstep with the incoming overlay, driven by the shared [parallaxProgress].
 * No snapshot/screenshot hack and no separate animation — it just reads the one
 * shared value.
 */
@Composable
fun OverlayHost(
    parallaxProgress: SlideProgress,
    content: @Composable () -> Unit,
) {
    val style = LocalAppState.current.transitionStyle
    val shiftFraction = overlayHostShiftFraction(style)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = parallaxProgress.value.coerceIn(0f, 1f)
                translationX = -(1f - p) * size.width * shiftFraction
            },
    ) {
        content()
        // Cover style: dim the outgoing screen as it recedes (iOS parallax-dim).
        // alpha = peak when fully covered (p=0), 0 when the cover is gone (p=1).
        // drawn in its OWN layer so reading progress.value only re-runs the draw
        // phase, never recomposition. Fully transparent at rest-dismissed, so it
        // never lingers visibly once the overlay is gone.
        if (style == TransitionStyle.Cover) {
            // Drawn with drawBehind (pure draw phase) instead of a child
            // graphicsLayer+background: a solid scrim needs no extra render
            // node, so the recede-dim costs one drawRect per frame with no
            // layer allocation/compositing pass.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val p = parallaxProgress.value.coerceIn(0f, 1f)
                        drawRect(color = DimColor, alpha = (1f - p) * CoverDimPeakAlpha)
                    },
            )
        }
    }
}

/**
 * Animated, gesture-aware overlay that hosts the INCOMING screen on top of the
 * rest of the app. It slides in from the right and the host underneath slides
 * out to the left at the exact same speed — both driven by the single shared
 * [parallaxProgress] Animatable (no second value, no mirroring). Supports a
 * left-edge swipe-back and the predictive-back gesture.
 *
 *   * parallaxProgress = 0f — incoming screen at rest, edge-to-edge
 *   * parallaxProgress = 1f — incoming screen fully off the right edge
 *
 * NonCancellable wraps around suspend cleanup so a mid-animation cancellation
 * (predictive-back release, recomposition, etc.) can't leave the overlay in a
 * half-dismissed state or crash at a suspend point inside a cancelled context.
 */
@Composable
fun RoundedSlideOverlay(
    parallaxProgress: SlideProgress,
    onDismissed: () -> Unit,
    animateIn: Boolean = true,
    content: @Composable (animatedBack: () -> Unit) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val appState = LocalAppState.current
    val style = appState.transitionStyle
    val swipeBackEnabled = appState.swipeBackEnabled

    val edgeWidthPx = with(density) { SwipeBackEdgeWidth.toPx() }
    val velocityCommitPx = with(density) { SwipeBackCommitVelocityDpPerSec.toPx() }
    // Only resolved/used in Cover mode — the incoming screen rounds its leading
    // corners to match the device curve as it crosses the edge.
    val cornerRadiusPx = remember(view, style) {
        if (style == TransitionStyle.Cover) {
            with(density) { resolveDeviceCornerRadius(view, density).toPx() }
        } else 0f
    }

    // The ONE driver. Both this overlay (incoming) and the OverlayHost (outgoing)
    // read parallaxProgress.value directly — there is no second Animatable.
    val progress = parallaxProgress
    val currentOnDismissed by rememberUpdatedState(onDismissed)

    // Slide in from the right on first composition. animateIn=false means this
    // overlay was already on screen as an underlay (a child just popped off), so
    // it should appear at its final position without replaying the entry slide.
    //
    // ASYNC READINESS: we hold the incoming screen fully off-screen (progress=1)
    // and wait for TWO real frames via withFrameNanos before starting the slide.
    // That guarantees the destination has been composed, measured and laid out
    // (its first heavy frame is already drawn off-screen) BEFORE any motion — so
    // nothing is "still loading" while the screen is gliding in. This is what
    // made entry feel less smooth than it should: previously the animateTo fired
    // the same frame the screen first composed, so the first measure landed mid-
    // slide and dropped frames.
    LaunchedEffect(Unit) {
        if (animateIn) {
            progress.snapTo(1f)
            withFrameNanos {}
            withFrameNanos {}
            progress.animateTo(targetValue = 0f, animationSpec = PushSpec)
        } else {
            progress.snapTo(0f)
        }
    }

    val animatedBack: () -> Unit = remember(scope) {
        {
            scope.launch {
                try {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    progress.animateTo(targetValue = 1f, animationSpec = PushSpec)
                } finally {
                    withContext(NonCancellable) { currentOnDismissed() }
                }
            }
        }
    }

    PredictiveBackHandler(enabled = true) { progressFlow ->
        try {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            progressFlow.collect { event ->
                if (event.progress < 0.999f) {
                    progress.snapTo(event.progress)
                }
            }
            withContext(NonCancellable) {
                progress.animateTo(targetValue = 1f, animationSpec = PushSpec)
                currentOnDismissed()
            }
        } catch (_: CancellationException) {
            withContext(NonCancellable) {
                progress.animateTo(targetValue = 0f, animationSpec = PushSpec)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Manual follow-finger swipe-back on the left edge — runs in parallel
            // to PredictiveBackHandler so we cover both gesture stacks. Gated by
            // the user's "Свайп назад" toggle: when off we ignore every gesture
            // here (without consuming the down) so it can't drag the screen, but
            // the back button / system back still works via PredictiveBackHandler.
            .pointerInput(swipeBackEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!swipeBackEnabled) return@awaitEachGesture
                    if (down.position.x > edgeWidthPx) return@awaitEachGesture

                    var consumedRight = false
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, dragAmount ->
                        if (dragAmount > 0) {
                            consumedRight = true
                            change.consume()
                        }
                    } ?: return@awaitEachGesture
                    if (!consumedRight) return@awaitEachGesture

                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    velocityTracker.addPosition(drag.uptimeMillis, drag.position)

                    val width = size.width.toFloat()
                    val initialDx = (drag.position.x - down.position.x).coerceAtLeast(0f)
                    // awaitEachGesture is a restricted-suspension scope: Animatable.snapTo
                    // isn't on its dispatch surface, so we launch it on the composable
                    // scope. Track the latest finger progress synchronously here so the
                    // release threshold reflects where the finger actually was on lift.
                    var fingerProgress = (initialDx / width).coerceIn(0f, 1f)
                    scope.launch { progress.snapTo(fingerProgress) }

                    var released = false
                    while (!released) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            released = true
                        } else {
                            val dx = (change.position.x - down.position.x).coerceAtLeast(0f)
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            fingerProgress = (dx / width).coerceIn(0f, 1f)
                            scope.launch { progress.snapTo(fingerProgress) }
                        }
                    }

                    val velocity = velocityTracker.calculateVelocity().x
                    val shouldDismiss =
                        fingerProgress >= SwipeBackCommitFraction ||
                            velocity >= velocityCommitPx
                    if (shouldDismiss) {
                        scope.launch {
                            try {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                progress.snapTo(fingerProgress)
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = OverlaySpring,
                                    initialVelocity = velocity / width,
                                )
                            } finally {
                                withContext(NonCancellable) { currentOnDismissed() }
                            }
                        }
                    } else {
                        scope.launch {
                            progress.snapTo(fingerProgress)
                            progress.animateTo(
                                targetValue = 0f,
                                animationSpec = OverlayCancelSpring,
                                initialVelocity = velocity / width,
                            )
                        }
                    }
                }
            }
            .graphicsLayer {
                // Spring physics can briefly overshoot to negative values when the
                // overlay settles back through zero — clamp before translating.
                val p = progress.value.coerceAtLeast(0f)
                translationX = p * size.width
                // Cover style: the leading (left) corners round up to the device
                // curve as the screen moves out; right corners stay 0 (it's at or
                // past the right edge). Push style leaves cornerRadiusPx == 0 so
                // the slide is flat edge-to-edge.
                //
                // The radius reaches FULL by ~28% of travel and holds full for the
                // rest of the slide, then eases back to 0 only as the screen settles
                // flush to the edge. CRITICAL: we use a smoothstep ramp instead of a
                // clamped-linear one. Smoothstep has ZERO slope at both ends, so as
                // the screen settles (p → 0) the corner radius' rate of change → 0
                // too. Combined with the ease-out tween (whose velocity also → 0 at
                // the end), there is no perceptible corner "morph" right at the edge
                // — that late linear corner collapse was the little twitch the user
                // felt "при прилегании к краю". Now the corners are clearly round the
                // whole time the screen is in motion and dissolve imperceptibly into
                // the flat full-screen state at rest.
                if (cornerRadiusPx > 0f && p > 0.001f) {
                    val t = (p / 0.28f).coerceIn(0f, 1f)
                    val ramp = t * t * (3f - 2f * t) // smoothstep: f'(0)=f'(1)=0
                    val r = ramp * cornerRadiusPx
                    // Use a SYMMETRIC rounded rect, not asymmetric corners.
                    // An asymmetric RoundedCornerShape compiles to an
                    // Outline.Generic (a Path), and clipping a full-screen
                    // layer to a Path forces an off-screen buffer + clipPath
                    // every single frame — the dropped frames that made the
                    // "наплыв" read as laggy/abrupt. A uniform radius compiles
                    // to Outline.Rounded, which clips via the GPU's
                    // setClipToOutline fast path (no off-screen buffer).
                    // While p>0 the screen is shifted right, so its trailing
                    // (right) corners sit off-screen — rounding all four is
                    // visually identical to rounding only the leading pair.
                    // We also skip the clip entirely at rest (p≈0) so the
                    // settled full-screen state stays a plain rectangle.
                    shape = RoundedCornerShape(r)
                    clip = true
                }
            },
    ) {
        content(animatedBack)
    }
}

/**
 * Creates a single shared push-progress driver. Pass the SAME instance to both
 * [OverlayHost] and [RoundedSlideOverlay] so the outgoing and incoming screens
 * move as one strip. Starts at 1f (incoming off-screen / nothing covering).
 */
@Composable
fun rememberParallaxProgress(): SlideProgress = remember { Animatable(1f) }

/** Best-effort lookup of the device's physical display corner radius (Cover style). */
private fun resolveDeviceCornerRadius(view: android.view.View, density: Density): Dp {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val insets = view.rootWindowInsets
        if (insets != null) {
            val positions = intArrayOf(
                RoundedCorner.POSITION_TOP_LEFT,
                RoundedCorner.POSITION_TOP_RIGHT,
                RoundedCorner.POSITION_BOTTOM_LEFT,
                RoundedCorner.POSITION_BOTTOM_RIGHT,
            )
            var maxPx = 0
            for (p in positions) {
                val r = insets.getRoundedCorner(p)
                if (r != null && r.radius > maxPx) maxPx = r.radius
            }
            if (maxPx > 0) {
                return with(density) { maxPx.toFloat().toDp() }
            }
        }
    }
    return FallbackDeviceCornerRadius
}
