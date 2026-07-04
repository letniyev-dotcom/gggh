package com.letify.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.tanh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// iOS "rubber band" coefficient. The derivative of UIKit's rubber-band
// displacement curve at offset=0 is exactly this constant — so the first
// pixel of finger drag past the edge produces 0.55 px of overscroll, NOT a
// full pixel. With c=1.0 (the previous value) the very start of the pull
// felt 1:1 and then the quadratic damping kicked in hard, giving a subtle
// but unmistakable "abrupt then sticky" feel. c=0.55 makes the response
// uniform from the first pixel onward and is what gives the iOS pull its
// characteristically gentle, gradual resistance.
private const val RubberBandC = 0.55f

/**
 * iOS-style elastic translation overscroll for Compose.
 *
 * Replaces the system stretch-overscroll on Android 12+: when a scrollable
 * inside this scope hits its edge and the user keeps dragging, the leftover
 * delta is accumulated into a per-axis [MutableFloatState] with quadratic
 * damping (each extra pixel contributes less the more it is already pulled).
 * On finger lift the value springs back to 0 with a slightly bouncy spring.
 *
 * No graphical deformation — the content just slides a touch further and
 * pings back.
 *
 * Vertical and horizontal axes are independent so a horizontal carousel and
 * its containing vertical scroller each get their own elastic feel.
 */
class ElasticOverscrollState internal constructor(
    val verticalOverscroll: MutableFloatState,
    val horizontalOverscroll: MutableFloatState,
    val connection: NestedScrollConnection,
    // Most-recently observed fling velocity (pixels-per-second). Captured
    // by the nested-scroll connection in onPreFling — i.e. at the EXACT
    // moment the finger leaves the screen, before the inner fling has had
    // any time to decay. Snap logic should read this rather than sampling
    // scrollState.value via snapshotFlow, because by the time the inner
    // fling decays and isScrollInProgress flips to false the residual
    // velocity is already (by definition) almost zero — that's what made
    // the position-based 55% snap reverse the user's motion on a sharp
    // flick.
    val lastFlingVelocityY: MutableFloatState,
    val lastFlingVelocityX: MutableFloatState,
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun rememberElasticOverscroll(
    maxVertical: Dp = 64.dp,
    maxHorizontal: Dp = 48.dp,
): ElasticOverscrollState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maxV = with(density) { maxVertical.toPx() }
    val maxH = with(density) { maxHorizontal.toPx() }
    val verticalOverscrollY = remember { mutableFloatStateOf(0f) }
    val horizontalOverscrollX = remember { mutableFloatStateOf(0f) }
    val flingVelocityY = remember { mutableFloatStateOf(0f) }
    val flingVelocityX = remember { mutableFloatStateOf(0f) }
    val connection = remember(maxV, maxH) {
        ElasticOverscrollConnection(
            verticalOverscroll = verticalOverscrollY,
            horizontalOverscroll = horizontalOverscrollX,
            lastFlingVelocityY = flingVelocityY,
            lastFlingVelocityX = flingVelocityX,
            maxVerticalPx = maxV,
            maxHorizontalPx = maxH,
            scope = scope,
        )
    }
    return remember(connection) {
        ElasticOverscrollState(
            verticalOverscrollY,
            horizontalOverscrollX,
            connection,
            flingVelocityY,
            flingVelocityX,
        )
    }
}

/**
 * Wraps [content] in a [Box] that:
 *  1. Provides a null [LocalOverscrollConfiguration] so inner scroll
 *     containers don't paint the system stretch effect.
 *  2. Attaches a [NestedScrollConnection] that converts leftover scroll
 *     deltas into a damped translation overscroll.
 *  3. Applies the resulting translation via [graphicsLayer] to the entire
 *     subtree so everything shifts together.
 *
 * Use this on screens whose ENTIRE content should slide together. For
 * screens with pinned UI that must NOT slide (e.g. a collapsing header
 * cluster), use the lower-level [rememberElasticOverscroll] and apply
 * the translation only where appropriate, plus a counter-translation on
 * the pinned element.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ElasticOverscroll(
    modifier: Modifier = Modifier,
    maxVertical: Dp = 64.dp,
    maxHorizontal: Dp = 48.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val elastic = rememberElasticOverscroll(maxVertical, maxHorizontal)
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(
            modifier = modifier
                .nestedScroll(elastic.connection)
                .graphicsLayer {
                    translationY = elastic.verticalOverscroll.floatValue
                    translationX = elastic.horizontalOverscroll.floatValue
                },
            content = content,
        )
    }
}

internal class ElasticOverscrollConnection(
    private val verticalOverscroll: MutableFloatState,
    private val horizontalOverscroll: MutableFloatState,
    private val lastFlingVelocityY: MutableFloatState,
    private val lastFlingVelocityX: MutableFloatState,
    private val maxVerticalPx: Float,
    private val maxHorizontalPx: Float,
    private val scope: CoroutineScope,
) : NestedScrollConnection {

    // Active spring-back animation jobs, cancelled when the user grabs again.
    private var verticalJob: Job? = null
    private var horizontalJob: Job? = null

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.Drag) return Offset.Zero
        // New drag input → cancel any running spring-back ONLY when there
        // is actually still overscroll to fight with. Cancelling on every
        // tiny drag delta used to interrupt the spring at sub-pixel residue
        // and produce a perceptible "stutter" right after lift, even on
        // screens where the overscroll value had effectively settled.
        if (available.y != 0f && verticalOverscroll.floatValue != 0f) cancelVertical()
        if (available.x != 0f && horizontalOverscroll.floatValue != 0f) cancelHorizontal()
        val cx = consumeAxis(horizontalOverscroll, available.x)
        val cy = consumeAxis(verticalOverscroll, available.y)
        return Offset(cx, cy)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.Drag) return Offset.Zero
        val ax = applyAxis(horizontalOverscroll, available.x, maxHorizontalPx)
        val ay = applyAxis(verticalOverscroll, available.y, maxVerticalPx)
        return Offset(ax, ay)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        // Capture the raw lift-time velocity BEFORE doing anything else.
        // This is the user's intent velocity; the inner scrollable's fling
        // will then decay from this value, but we expose the original here
        // so screen-level snap logic can decide direction based on what
        // the finger ACTUALLY did, not what's left after the fling decays.
        lastFlingVelocityY.floatValue = available.y
        lastFlingVelocityX.floatValue = available.x
        // Finger lifted. If we accumulated any overscroll, spring it back
        // to 0 and consume the matching velocity so the inner scrollable
        // doesn't double-fling on top of the spring animation.
        val springY = verticalOverscroll.floatValue != 0f
        val springX = horizontalOverscroll.floatValue != 0f
        if (springY) {
            verticalJob = scope.launch {
                springBack(verticalOverscroll, available.y, maxVerticalPx)
            }
        }
        if (springX) {
            horizontalJob = scope.launch {
                springBack(horizontalOverscroll, available.x, maxHorizontalPx)
            }
        }
        return Velocity(
            x = if (springX) available.x else 0f,
            y = if (springY) available.y else 0f,
        )
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        // [available] is the fling velocity the inner scrollable COULD NOT
        // consume — i.e. the fling reached the top/bottom (or left/right)
        // edge with this much speed left over. Previously we ignored it, so
        // a flung list slammed to a dead stop at the edge while a slow finger
        // *drag* past the same edge produced the nice rubber-band stretch.
        // Convert that leftover velocity into the SAME elastic bounce: shoot
        // the overscroll outward proportionally to the speed, then spring it
        // back to 0 (this is what iOS does on a hard flick at the edge).
        var consumedY = 0f
        var consumedX = 0f
        if (available.y != 0f) {
            cancelVertical()
            verticalJob = scope.launch {
                flingBounce(verticalOverscroll, available.y, maxVerticalPx)
            }
            consumedY = available.y
        } else if (verticalOverscroll.floatValue != 0f && verticalJob?.isActive != true) {
            verticalJob = scope.launch { springBack(verticalOverscroll, 0f, maxVerticalPx) }
        }
        if (available.x != 0f) {
            cancelHorizontal()
            horizontalJob = scope.launch {
                flingBounce(horizontalOverscroll, available.x, maxHorizontalPx)
            }
            consumedX = available.x
        } else if (horizontalOverscroll.floatValue != 0f && horizontalJob?.isActive != true) {
            horizontalJob = scope.launch { springBack(horizontalOverscroll, 0f, maxHorizontalPx) }
        }
        return Velocity(consumedX, consumedY)
    }

    /**
     * Elastic edge bounce driven by a leftover FLING velocity (px/s).
     *
     * Starts from the current overscroll value (usually ~0 when a fling first
     * reaches the edge) and feeds [velocity] into a critically-damped spring
     * whose equilibrium is 0. A no-bounce spring released at rest with a
     * non-zero initial velocity travels OUT to a single peak (∝ velocity /
     * √stiffness) and glides back to 0 with no oscillation — exactly the
     * "stretch out then snap back" the rubber-band drag produces, but sourced
     * from fling momentum instead of finger displacement. Each tick is clamped
     * to the cap so a very hard flick can't punch past the stretch limit.
     */
    private suspend fun flingBounce(
        state: MutableFloatState,
        velocity: Float,
        maxPx: Float,
    ) {
        if (velocity == 0f || maxPx <= 0f) return
        // A critically-damped spring released from rest with initial velocity v
        // reaches a single peak of  x_peak ≈ v / (√stiffness · e)  and then
        // glides back to 0 with no oscillation — the clean "stretch out, snap
        // back". The catch: if x_peak overshoots the cap we used to hard-clamp
        // every tick, which pinned the content FLAT at the edge for the frames
        // the (unclamped) trajectory spent above the cap. That plateau read as
        // a hang / "лаг" at the end of a flick.
        //
        // Fix: keep the response PROPORTIONAL to the flick for normal speeds
        // but smoothly SATURATE the input velocity so the natural peak just
        // kisses the cap instead of overshooting it — no clamp plateau, so the
        // whole motion is one continuous, smooth out-and-back. tanh gives a
        // soft knee: linear for gentle flings, asymptotic for hard ones.
        val stiffness = 220f
        // velocity that would peak exactly at the cap (the saturation point)
        val vCap = maxPx * kotlin.math.sqrt(stiffness) * 2.718281828f
        // 0.4 = the response slope at zero velocity. tanh(x)≈x near 0, so a
        // plain tanh(velocity/vCap) reacts at FULL strength to gentle flicks —
        // which is what made small finger swipes feel "резко" (a small swipe
        // still kicked out a sizable bounce). Flattening the origin slope to
        // 0.4 makes light swipes give only a small, gradual stretch while a
        // hard flick still saturates smoothly toward the cap.
        val v = vCap * tanh(0.4f * velocity / vCap)
        val animatable = Animatable(state.floatValue)
        try {
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    // Medium stiffness + critically damped → a gentle, smooth
                    // give that settles in ~300ms (smoother than the previous
                    // 380 snap) with no overshoot or oscillation.
                    stiffness = stiffness,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    visibilityThreshold = 0.5f,
                ),
                initialVelocity = v,
            ) {
                // Soft safety clamp only — with the velocity saturated above the
                // trajectory stays under the cap, so this almost never bites and
                // there is no flat plateau.
                state.floatValue = value.coerceIn(-maxPx, maxPx)
            }
            state.floatValue = 0f
        } catch (_: CancellationException) {
            // user re-grabbed mid-bounce — leave the value where it is
        }
    }

    private fun cancelVertical() {
        verticalJob?.cancel()
    }

    private fun cancelHorizontal() {
        horizontalJob?.cancel()
    }

    private fun consumeAxis(state: MutableFloatState, available: Float): Float {
        // Drag direction OPPOSITE the current overscroll → eat PART of it
        // into the overscroll (reduce magnitude toward 0) while letting the
        // rest pass through to the inner scrollable. The previous version
        // consumed the ENTIRE reverse-direction delta until the rubber band
        // hit 0 — so on a screen with a large pull, the very first frames
        // after a direction-reversal moved the rubber band but NOT the
        // content. That dead window manifested as "everything jerks instead
        // of scrolling smoothly" — most noticeable on dense screens (e.g.
        // Питание) where the user reflexively flicks up-down-up-down.
        //
        // Letting ~45% of the reverse delta flow into the scrollable in
        // parallel keeps the content moving from frame 1 of the reversal.
        // The rubber band still snaps closed because the spring-back from
        // onPreFling does the rest the moment the finger lifts.
        if (available == 0f) return 0f
        val current = state.floatValue
        if (current == 0f) return 0f
        if (sign(available) == sign(current)) return 0f
        val elasticShare = 0.55f
        val useForElastic = available * elasticShare
        val newVal = (current + useForElastic).let {
            if (sign(it) != sign(current)) 0f else it
        }
        val consumedSigned = newVal - current
        // Synchronous write — no coroutine hop, no 1-frame lag.
        state.floatValue = newVal
        return consumedSigned
    }

    private fun applyAxis(state: MutableFloatState, available: Float, maxPx: Float): Float {
        // Inner scroll couldn't consume this delta (at an edge) → roll it
        // into the overscroll with rubber-band damping.
        //
        // Damping factor is the derivative of the iOS UIKit rubber-band
        // displacement curve:
        //   d = c · (1 − |current|/maxPx)^2,   c ≈ 0.55
        // At rest (current = 0) the damping is `c` — the first pixel of
        // finger movement gives a 0.55 px overscroll. That gentle initial
        // ratio is what makes the pull feel uniform; with c=1.0 the very
        // first pixel of drag jumped 1:1 and then the quadratic falloff
        // hit hard, giving a noticeable "snap then resist" feel.
        if (available == 0f || maxPx <= 0f) return 0f
        val current = state.floatValue
        val ratio = (abs(current) / maxPx).coerceIn(0f, 1f)
        val damping = RubberBandC * (1f - ratio) * (1f - ratio)
        val delta = available * damping
        val newVal = (current + delta).coerceIn(-maxPx, maxPx)
        state.floatValue = newVal
        return available
    }

    private suspend fun springBack(
        state: MutableFloatState,
        initialVelocity: Float,
        maxPx: Float,
    ) {
        val startValue = state.floatValue
        if (startValue == 0f) return
        // Damp the finger velocity through the same rubber-band curve we used
        // to absorb finger displacement. Without this, a sharp finger flick
        // past the edge feeds its raw finger velocity (e.g. 3000 px/s) into
        // a spring whose displacement only moved 0.55× the finger — so the
        // spring's solver overshoots the cap by tens of pixels before bouncing
        // back, producing the visible "jerk on sharp swipe" reported by the
        // user. Damping with the same factor keeps velocity in the same
        // scale as the overscroll displacement and the return stays smooth.
        val ratio = (abs(startValue) / maxPx).coerceIn(0f, 1f)
        // Scale the lift-off velocity by the rubber-band curve AND by how far
        // the content is actually pulled (ratio). A SMALL pull released with a
        // fast finger used to feed a big velocity into a spring that only had a
        // few px to travel → it snapped back instantly = the "резко на
        // небольших свайпах" feel. Multiplying by `ratio` means a tiny pull
        // eases back almost purely on the (gentle) spring, while a near-cap
        // pull still keeps its momentum.
        val velocityScale = RubberBandC * (1f - ratio) * (1f - ratio)
        val dampedVelocity = initialVelocity * velocityScale * ratio
        val animatable = Animatable(startValue)
        try {
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    // Critically damped — NO overshoot, no oscillation.
                    // iOS-style stiffness (~300) settles in ~220–260ms which
                    // feels like a confident snap-back without slamming.
                    // The previous build used StiffnessLow (~50), so the
                    // release crawled for ~500ms and the user perceived it
                    // as “дёрганный” — the content lagged the finger by
                    // several frames after lift. With stiffness=300 the
                    // surface tracks the finger’s release energy cleanly
                    // and there’s no visible lag between touch-up and the
                    // start of the spring.
                    stiffness = 230f,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    visibilityThreshold = 0.25f,
                ),
                initialVelocity = dampedVelocity,
            ) {
                // Hard-clamp every spring tick to the cap. Even with the
                // velocity damping above, a near-cap pull combined with a
                // small extra impulse can mathematically over-run by a pixel
                // or two — clamping here guarantees the visible content
                // never punches past the stretch limit. Spring solver still
                // computes the un-clamped trajectory internally, so the
                // motion remains smooth (we just hide the over-run tail).
                state.floatValue = value.coerceIn(-maxPx, maxPx)
            }
            // animateTo's visibilityThreshold can leave a sub-pixel residue
            // — nail it to 0 to avoid the GraphicsLayer recording a fractional
            // translation forever.
            state.floatValue = 0f
        } catch (_: CancellationException) {
            // user re-grabbed mid-spring — leave the value where it is
        }
    }
}
