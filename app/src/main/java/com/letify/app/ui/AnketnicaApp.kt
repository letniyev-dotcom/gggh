package com.letify.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Job
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.OverlayHost
import com.letify.app.ui.components.RoundedSlideOverlay
import com.letify.app.ui.components.overlayHostShiftFraction
import com.letify.app.ui.components.rememberParallaxProgress
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.screens.AnketaRow
import com.letify.app.ui.screens.AnketaDetailScreen
import com.letify.app.ui.screens.ProfileScreen
import com.letify.app.ui.state.AnketaStatus
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.state.TransitionStyle
import com.letify.app.ui.theme.Letify
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Smooth, natural sheet motion. The old tween(340, cubic-bezier) made the
// sheet snap in — the user's "открывается слишком резко". A critically-
// damped spring (NO bounce) glides it in/out and settles organically, so it
// reads as smooth, not slower. Same spec drives the profile receding, the
// header tap-toggle, the back gesture and the drag-release settle.
// stiffness = 380 → ~20 % faster than the old 260 while keeping the same
// critically-damped (no-bounce) character. At 260 the sheet felt sluggish
// near the top; 380 reaches 90 % of its target in ≈ 200 ms and feels
// responsive without being sharp.
private val SheetSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 380f,
)

// Position-specific spec for sheetProgress (0–1 value that gets multiplied by
// travelPx to produce translationY). The default spring visibilityThreshold is
// 0.01f, which is "1% of total travel". With travelPx ≈ 1500 px that means
// the spring terminates — and SNAPS to its target — while still 15 px away.
// That single-frame teleport is the "тормозит вверху → резко поднимается"
// the user sees: the spring decelerates naturally (correct), then at the
// snap point jumps the remaining pixels in one frame (incorrect).
//
// visibilityThreshold = 0.0005f → 0.05% of travel → ≈ 0.75 px on a 1500 px
// screen — completely imperceptible. The spring physics (curve, feel, timing)
// are identical; it just runs through the invisible tail instead of snapping.
//
// SheetSpec is intentionally kept at the default threshold: sheetChrome and
// sheetFull animate 0–1 alpha/scale values where 0.01 = invisible.
private val SheetPositionSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 380f,
    visibilityThreshold = 0.0005f,
)

// The in-sheet push-in (RoundedSlideOverlay) takes ~360ms + 2 warm-up frames.
// The sheet only climbs to fullscreen AFTER that, so opening an anketa reads as
// two clean beats: (1) the detail slides in WITHIN the small resting sheet,
// then (2) the sheet expands to fullscreen — never both at once.
private const val PushInDelayMs = 380L

private val PeekDp = 96.dp

/**
 * Root shell. The bottom navbar is GONE — the home screen is the Profile, and
 * the applications list lives in a bottom sheet that peeks at the bottom of the
 * profile and pulls up to (almost) fullscreen. As it opens, the profile behind
 * it recedes (scale + dim + rounded corners), iOS-modal style.
 *
 * Layering (bottom → top):
 *   1. Receding layer: Profile + its pushed sub-screens (Сценарии, Оформление…)
 *   2. «Новые анкеты» sheet (peek ↔ expanded), only on the home profile
 *   3. Anketa detail — top overlay, slides in from the right
 */
@Composable
fun AnketnicaApp(state: AnketnicaState) {
    val subStack = remember { mutableStateListOf<SubRoute>() }
    var lastAction by remember { mutableStateOf("init") }
    val overlay: SubRoute? = subStack.lastOrNull()
    val underlay: SubRoute? = if (subStack.size >= 2) subStack[subStack.size - 2] else null
    val push: (SubRoute) -> Unit = { r -> subStack.add(r); lastAction = "push" }
    val pop: () -> Unit = { if (subStack.isNotEmpty()) subStack.removeAt(subStack.lastIndex); lastAction = "pop" }
    val overlayStateHolder = rememberSaveableStateHolder()

    val parallax = rememberParallaxProgress()
    val nestedParallax = remember(subStack.size, lastAction) {
        Animatable(if (lastAction == "pop") 0f else 1f)
    }

    // Anketa detail is a TOP overlay (over sheet + profile). Opened both from the
    // ankety sheet and from Profile → Списки.
    var detailId by remember { mutableStateOf<Int?>(null) }
    val openAnketa: (Int) -> Unit = { detailId = it }

    // Single driver for both the sheet translation and the profile receding.
    // Using MutableState<Float> instead of Animatable so drag updates are fully
    // synchronous — no coroutine launch per gesture frame, no mutex contention,
    // position written in the SAME call-stack as the touch event so graphicsLayer
    // always reads the current-frame value. The settle animation uses the
    // standalone `animate()` function which drives the same state on each frame.
    val sheetProgressState = remember { mutableStateOf(0f) }
    // Chrome: 1 on the home profile, 0 when a sub-screen covers it — lets the
    // sheet fade/slide away smoothly instead of popping in/out.
    val sheetChrome = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var rootHeightPx by remember { mutableStateOf(0f) }
    val topInsetDp: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInsetDp: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val peekPx = with(density) { (PeekDp + navInsetDp).toPx() }
    val topGapPx = with(density) { (topInsetDp + 12.dp).toPx() }
    val travelPx = (rootHeightPx - peekPx - topGapPx).coerceAtLeast(1f)
    val measured = rootHeightPx > 0f

    // Settle job — only ONE running at a time. sheetSettling guards against the
    // list's fling delivering Drag-source NSC deltas after the finger has lifted,
    // which used to cancel the settle mid-flight. Genuine user gestures (grabber
    // and profile pointerInput) call sheetOnGrabStart first, which cancels the
    // settle and clears the flag so subsequent drag frames go straight through.
    var sheetSettleJob by remember { mutableStateOf<Job?>(null) }
    var sheetSettling by remember { mutableStateOf(false) }

    // Settle: run spring animation via the standalone animate() function, which
    // drives sheetProgressState on every frame without needing Animatable or a
    // mutator-mutex. initialVelocity carries the finger's release speed so the
    // spring continues from actual swipe momentum instead of always cold-starting.
    val settleSheetTo: (Float, Float) -> Unit = { target, initialVelocity ->
        sheetSettleJob?.cancel()
        sheetSettling = true
        val startValue = sheetProgressState.value
        var job: Job? = null
        job = scope.launch {
            try {
                animate(
                    initialValue = startValue,
                    targetValue = target,
                    initialVelocity = initialVelocity,
                    animationSpec = SheetPositionSpec,
                ) { value, _ -> sheetProgressState.value = value }
            } finally {
                if (sheetSettleJob === job) sheetSettling = false
            }
        }
        sheetSettleJob = job
    }

    // Drag — fully synchronous direct state write. No coroutine, no mutex.
    // Compute travel LIVE from rootHeightPx (a snapshot state) so even a
    // stale-captured lambda always divides by the real post-measure travel.
    val sheetOnDrag: (Float) -> Unit = { dyPx ->
        if (!sheetSettling) {
            val tp = (rootHeightPx - peekPx - topGapPx).coerceAtLeast(1f)
            sheetProgressState.value = (sheetProgressState.value - dyPx / tp).coerceIn(0f, 1f)
        }
    }

    // Called at the START of a genuine user gesture (grabber / profile drag).
    // Cancels any running settle immediately so the finger takes control with
    // no delay — the sheet tracks 1:1 from the very first drag frame. The
    // sheetSettling guard is intentionally kept for NestedScrollConnection
    // Drag-source events (post-lift fling echoes from the list) so they cannot
    // restart a cancelled settle.
    val sheetOnGrabStart: () -> Unit = {
        if (sheetSettling) {
            sheetSettleJob?.cancel()
            sheetSettling = false
        }
    }

    val sheetOnDragEnd: (Float) -> Unit = { vy ->
        val target = when {
            vy < -700f -> 1f
            vy > 700f -> 0f
            else -> if (sheetProgressState.value > 0.4f) 1f else 0f
        }
        // Convert pixel/s velocity to 0-1 progress velocity and carry it into
        // the spring so a fast fling settles with natural momentum.
        val tp = (rootHeightPx - peekPx - topGapPx).coerceAtLeast(1f)
        settleSheetTo(target, -vy / tp)
    }

    // Live handles for gesture blocks: pointerInput(Unit) captures its lambdas
    // at FIRST composition, when the window insets can still be 0 — captured
    // peekPx/topGapPx then stayed stale forever. Routing every call through
    // rememberUpdatedState makes the gesture always invoke the LATEST handler.
    val currentSheetOnDrag by rememberUpdatedState(sheetOnDrag)
    val currentSheetOnDragEnd by rememberUpdatedState(sheetOnDragEnd)
    val currentSheetOnGrabStart by rememberUpdatedState(sheetOnGrabStart)

    val sheetExpanded by remember { derivedStateOf { sheetProgressState.value > 0.5f } }
    BackHandler(enabled = sheetExpanded && detailId == null && subStack.isEmpty()) {
        settleSheetTo(0f, 0f)
    }
    val onHome = subStack.isEmpty()
    LaunchedEffect(onHome) {
        sheetChrome.animateTo(if (onHome) 1f else 0f, SheetSpec)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { rootHeightPx = it.height.toFloat() },
    ) {
        // ── Receding layer ───────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = sheetProgressState.value.coerceIn(0f, 1f)
                    val s = 1f - 0.06f * p
                    scaleX = s
                    scaleY = s
                    translationY = p * 14.dp.toPx()
                    if (p > 0.001f) {
                        shape = RoundedCornerShape(p * 22.dp.toPx())
                        clip = true
                    }
                }
                .background(Letify.colors.bg),
        ) {
            OverlayHost(parallaxProgress = parallax) {
                // On the home profile, a vertical drag anywhere drives the sheet
                // (up = open, down = close) instead of the old rubber-band
                // overscroll. detectVerticalDragGestures only claims the pointer
                // after the touch-slop, so taps on the pencil / settings rows are
                // unaffected. Disabled once a sub-screen is on top.
                val profileDrag = if (subStack.isEmpty()) {
                    Modifier.pointerInput(Unit) {
                        val vt = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = { vt.resetTracking(); currentSheetOnGrabStart() },
                            onDragEnd = { currentSheetOnDragEnd(vt.calculateVelocity().y) },
                            onDragCancel = { currentSheetOnDragEnd(0f) },
                            onVerticalDrag = { change, dy ->
                                vt.addPosition(change.uptimeMillis, change.position)
                                change.consume(); currentSheetOnDrag(dy)
                            },
                        )
                    }
                } else Modifier
                Box(Modifier.fillMaxSize().then(profileDrag)) {
                    ProfileScreen(
                        state = state,
                        onSearch = { push(SubRoute.Search) },
                        onScenarios = { push(SubRoute.Scenarios) },
                        onAppearance = { push(SubRoute.Appearance) },
                        onNotifications = { push(SubRoute.Notifications) },
                        onStats = { push(SubRoute.Stats) },
                        onLists = { push(SubRoute.Lists) },
                    )
                }
            }

            underlay?.let { u ->
                val style = state.transitionStyle
                val shiftFraction = overlayHostShiftFraction(style)
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = nestedParallax.value.coerceIn(0f, 1f)
                            translationX = -(1f - p) * size.width * shiftFraction
                        }
                        .background(Letify.colors.bg),
                ) {
                    overlayStateHolder.SaveableStateProvider(u.stateKey()) {
                        RenderSubRoute(state = state, route = u, push = {}, dismiss = {}, openAnketa = openAnketa)
                    }
                    if (style == TransitionStyle.Cover) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = (1f - nestedParallax.value.coerceIn(0f, 1f)) * 0.16f }
                                .background(Color.Black),
                        )
                    }
                }
            }

            overlay?.let { current ->
                val animateInTop = lastAction != "pop"
                val topParallax = if (subStack.size >= 2) nestedParallax else parallax
                key(current, animateInTop) {
                    RoundedSlideOverlay(
                        parallaxProgress = topParallax,
                        onDismissed = { pop() },
                        animateIn = animateInTop,
                    ) { animatedBack ->
                        Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
                            overlayStateHolder.SaveableStateProvider(current.stateKey()) {
                                RenderSubRoute(
                                    state = state,
                                    route = current,
                                    push = { push(it) },
                                    dismiss = animatedBack,
                                    openAnketa = openAnketa,
                                )
                            }
                        }
                    }
                }
            }

            // Dim scrim over the receding layer — fades in with the sheet.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = sheetProgressState.value.coerceIn(0f, 1f) * 0.5f }
                    .background(Color.Black),
            )
        }

        // ── Bottom sheet — always mounted; fades/slides away behind sub-screens. ─────
        NewAnketySheet(
            state = state,
            progress = sheetProgressState,
            chrome = sheetChrome,
            travelPx = travelPx,
            topGapPx = topGapPx,
            peekPx = peekPx,
            visible = measured,
            interactive = subStack.isEmpty(),
            onOpenAnketa = openAnketa,
            onToggle = {
                settleSheetTo(if (sheetProgressState.value < 0.5f) 1f else 0f, 0f)
            },
            onDrag = sheetOnDrag,
            onDragEnd = sheetOnDragEnd,
            onDragStart = sheetOnGrabStart,
            onRequestExpand = { settleSheetTo(1f, 0f) },
        )

        // ── Anketa detail — top-most overlay, slides in from the right. ───────
        detailId?.let { id ->
            val a = state.anketa(id)
            if (a == null) {
                detailId = null
            } else {
                val detailParallax = remember(id) { Animatable(1f) }
                key(id) {
                    RoundedSlideOverlay(
                        parallaxProgress = detailParallax,
                        onDismissed = { detailId = null },
                        animateIn = true,
                    ) { animatedBack ->
                        Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
                            AnketaDetailScreen(state, a, onBack = animatedBack)
                        }
                    }
                }
            }
        }

        Toast(state)
    }
}

/**
 * The «Новые анкеты» bottom sheet. Fills the screen and is translated DOWN so
 * only its header peeks; [progress] 0→1 pulls it up to just below the top inset.
 * A single [Animatable] drives its translation and (in the parent) the profile
 * receding, so they stay in perfect lockstep — no second animation, no lag.
 */
@Composable
private fun NewAnketySheet(
    state: AnketnicaState,
    progress: State<Float>,
    chrome: Animatable<Float, AnimationVector1D>,
    travelPx: Float,
    topGapPx: Float,
    peekPx: Float,
    visible: Boolean,
    interactive: Boolean,
    onOpenAnketa: (Int) -> Unit,
    onToggle: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
    onDragStart: () -> Unit,
    onRequestExpand: () -> Unit,
) {
    val newItems = state.ankety.filter { it.status == AnketaStatus.NEW }.sortedBy { it.agox }
    val expanded by remember { derivedStateOf { progress.value > 0.5f } }
    // Opening an anketa happens INSIDE the sheet (not a new top screen): a
    // nested OverlayHost + RoundedSlideOverlay pair on their OWN sheetPush
    // progress reproduce the app-wide push/«сдвиг» transition, contained within
    // the sheet bounds.
    var sheetDetailId by remember { mutableStateOf<Int?>(null) }
    val sheetPush = rememberParallaxProgress()
    // Live handles — the grabber's pointerInput(Unit) and the nested-scroll
    // connection capture their lambdas once; route them through
    // rememberUpdatedState so they always call the latest handlers.
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnRequestExpand by rememberUpdatedState(onRequestExpand)

    // Dimension parameters (travelPx, topGapPx, peekPx) are derived from
    // window-inset snapshot states in the parent and arrive here as plain Floats.
    // Android can update window insets (status bar, nav bar) during or just
    // before the open animation — even after the first measure — which triggers a
    // recomposition with new values. Because a plain Float captured in a
    // graphicsLayer closure only updates on the NEXT recomposition draw pass,
    // there was a 1-frame gap where the old formula ran at p≈0.98 and then the
    // corrected formula snapped to the right final position: the "тормозит вверху
    // → резко поднимается" the user reported. Wrapping each dimension in
    // rememberUpdatedState makes it a snapshot State<Float>, so reading .value
    // inside graphicsLayer subscribes the draw phase directly to it — the layer
    // re-evaluates in the SAME frame the state changes, with no recomposition
    // lag, and the position formula stays smooth all the way to the end.
    val travelState = rememberUpdatedState(travelPx)
    val topGapState = rememberUpdatedState(topGapPx)
    val peekState = rememberUpdatedState(peekPx)

    // When an anketa is opened, the sheet climbs the last bit from its resting
    // position (just below the status bar) to the VERY top of the screen, so the
    // pushed-in detail gets the full height. The sheet KEEPS its rounded top
    // corners throughout — so the opened detail reads as a rounded iOS-style card
    // over the black backdrop (the user asked for rounded corners ONLY on the
    // opened screen, not a flat full-bleed rectangle).
    val sheetFull = remember { Animatable(0f) }
    LaunchedEffect(sheetDetailId != null) {
        if (sheetDetailId != null) {
            // Two beats: let the detail slide in within the small resting sheet
            // FIRST, THEN climb the sheet to fullscreen.
            delay(PushInDelayMs)
            // If the row was tapped while the sheet was only PART-way up, pull
            // the sheet the rest of the way open too — otherwise only the
            // top-gap collapsed and the opened detail stayed parked mid-screen.
            if (progress.value < 1f) currentOnRequestExpand()
            sheetFull.animateTo(1f, SheetSpec)
        } else {
            // Closing: the detail has already pushed out to the right (via
            // animatedBack) before sheetDetailId cleared, so now just descend the
            // sheet back to its small resting height.
            sheetFull.animateTo(0f, SheetSpec)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value.coerceIn(0f, 1f)
                val c = chrome.value.coerceIn(0f, 1f)
                val f = sheetFull.value.coerceIn(0f, 1f)
                // Read dimensions from snapshot-state wrappers so this block
                // re-evaluates in the same draw frame whenever insets change —
                // no recomposition lag, no position jump at the end of the
                // open animation.
                val tPx = travelState.value
                val tgPx = topGapState.value
                val pkPx = peekState.value
                // f pulls the top gap to 0 → the sheet climbs the last bit to the
                // very top of the screen when a detail is open.
                translationY = tgPx * (1f - f) + tPx * (1f - p) + (1f - c) * (pkPx + 48.dp.toPx())
                alpha = if (visible) c else 0f
            }
            .clipToTopRounded()
            .background(Letify.colors.container),
    ) {

        // NestedScroll glue so the list scroll and the open/close sheet drag stop
        // fighting: an upward drag while the sheet isn't fully open pulls the sheet
        // up first (the list stays put); once the sheet is open and the list is
        // scrolled to the very top, a further downward drag collapses the sheet.
        val sheetNsc = remember(interactive) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // Only a USER DRAG (not fling momentum) moves the sheet here.
                    // The list's post-release fling keeps delivering scroll frames
                    // through nested-scroll; if those also drove the sheet they
                    // fought the settle animation and made it jitter/"улетать".
                    // Fling is handled cleanly in onPreFling/onPostFling → onDragEnd.
                    if (interactive && source == NestedScrollSource.Drag && available.y < 0f && progress.value < 1f) {
                        // Consume ONLY the vertical axis — returning the full
                        // `available` also claimed any horizontal component.
                        currentOnDrag(available.y); return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (interactive && source == NestedScrollSource.Drag && available.y > 0f && progress.value > 0f) {
                        currentOnDrag(available.y); return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (interactive && available.y < 0f && progress.value < 1f) {
                        currentOnDragEnd(available.y); return Velocity(0f, available.y)
                    }
                    return Velocity.Zero
                }
                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (interactive && available.y > 0f && progress.value > 0f) {
                        currentOnDragEnd(available.y); return Velocity(0f, available.y)
                    }
                    return Velocity.Zero
                }
            }
        }

        OverlayHost(parallaxProgress = sheetPush) {
        Column(Modifier.fillMaxSize()) {
            // Header = the peeking part. Tap toggles; vertical drag pulls it.
            val headerGestures = if (interactive) {
                Modifier
                    .noFeedbackClick { currentOnToggle() }
                    .pointerInput(Unit) {
                        val vt = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = { vt.resetTracking(); currentOnDragStart() },
                            onDragEnd = { currentOnDragEnd(vt.calculateVelocity().y) },
                            onDragCancel = { currentOnDragEnd(0f) },
                            onVerticalDrag = { change, dy ->
                                vt.addPosition(change.uptimeMillis, change.position)
                                change.consume(); currentOnDrag(dy)
                            },
                        )
                    }
            } else Modifier
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(headerGestures),
            ) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clipToPill()
                            .background(Letify.colors.muted.copy(alpha = 0.5f)),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (expanded) "Анкеты" else "Новые анкеты",
                        color = Letify.colors.text,
                        style = Letify.typography.headlineLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .clipToPill()
                            .background(Letify.colors.accent)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${newItems.size}", color = Color(0xFF0C1F12), style = Letify.typography.labelMedium)
                    }
                }
            }

            Box(Modifier.fillMaxSize().nestedScroll(sheetNsc)) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    if (newItems.isEmpty()) {
                        item {
                            Text(
                                "Новых анкет нет.",
                                color = Letify.colors.muted,
                                style = Letify.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    } else {
                        items(newItems, key = { it.id }) { a ->
                            Box(Modifier.padding(horizontal = 10.dp)) {
                                AnketaRow(state, a) { sheetDetailId = a.id }
                            }
                        }
                    }
                }
            }
        }
        }

        // In-sheet anketa detail — slides in from the right OVER the list with the
        // same push transition, and the list host recedes left. Back / swipe-back
        // dismisses it back into the list, all within the sheet.
        sheetDetailId?.let { id ->
            val a = state.anketa(id)
            if (a == null) {
                sheetDetailId = null
            } else {
                key(id) {
                    RoundedSlideOverlay(
                        parallaxProgress = sheetPush,
                        onDismissed = { sheetDetailId = null },
                        animateIn = true,
                    ) { animatedBack ->
                        // Paint the in-sheet detail with the base background
                        // (NOT container): the cards inside are container-coloured,
                        // so a container backdrop merged everything into one flat
                        // slab (the "чёрный оверлей" / "контейнеры слились с фоном").
                        // Using bg — exactly like every other screen — gives the
                        // container cards their contrast back.
                        // Sheet is now fullscreen (under the status bar) while a
                        // detail is open, so the detail uses the REAL status-bar
                        // inset (default null) instead of the small 8dp override
                        // used when the sheet rested below the status bar.
                        Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
                            AnketaDetailScreen(state, a, onBack = animatedBack)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.clipToTopRounded(): Modifier =
    this.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))

private fun Modifier.clipToPill(): Modifier =
    this.clip(RoundedCornerShape(999.dp))

@Composable
private fun Toast(state: AnketnicaState) {
    val msg = state.toastMsg
    LaunchedEffect(msg) {
        if (msg != null) {
            delay(1800)
            state.toastMsg = null
        }
    }
    AnimatedVisibility(visible = msg != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().systemBarsPadding().padding(bottom = 40.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier.padding(horizontal = 32.dp).clipToPill()
                    .background(Color(0xFF2A2A2E)).padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(msg ?: "", color = Color.White, style = Letify.typography.bodyMedium)
            }
        }
    }
}