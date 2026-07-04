package com.letify.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default horizontal page padding. Sections opt in via [Modifier.screenHPad].
 * The scaffold itself has NO horizontal padding so horizontal scrollers and
 * "bleed" content can extend past the screen edges and back.
 */
val ScreenHorizontalPadding: Dp = 18.dp

val LocalScreenHPad = staticCompositionLocalOf { ScreenHorizontalPadding }

fun Modifier.screenHPad(pad: Dp = ScreenHorizontalPadding): Modifier =
    this.padding(horizontal = pad)

@Composable
fun ScreenScaffold(
    horizontalPadding: Dp = ScreenHorizontalPadding,
    scrollState: ScrollState = rememberScrollState(),
    topPadding: Dp = 6.dp,
    pinnedHeader: (@Composable () -> Unit)? = null,
    // When the pinned header has a known, constant height the caller can
    // pass it here. The scaffold then reserves that height directly with a
    // plain Box + leading Spacer, skipping the SubcomposeLayout below.
    // SubcomposeLayout composes its body DURING the layout phase, which is
    // measurably heavier on the first frame — and that first frame coincides
    // with the overlay slide-in spring, so the extra work shows up as a
    // hitch ("подлагивает") right when the screen opens. A constant height
    // also means the body's top spacer is correct on frame 0 with no
    // measure-readback, so we keep the jitter-free property the
    // SubcomposeLayout path was introduced for, without its cost.
    pinnedHeaderHeight: Dp? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalScreenHPad provides horizontalPadding) {
        if (pinnedHeader == null) {
            // ElasticOverscroll replaces the system stretch with a damped
            // iOS-style translation: scrolls slide a touch further past the
            // edge and ping back — nothing gets visually deformed.
            ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = topPadding, bottom = 160.dp)
                ) {
                    content()
                }
            }
        } else if (pinnedHeaderHeight != null) {
            // Fast path: header height is a known constant, so we reserve it
            // with a leading Spacer and pin the header in a plain Box overlay.
            // No SubcomposeLayout → no body composition during layout → a
            // cheaper, jank-free first frame when the screen slides in.
            val topInset =
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + topPadding
            Box(Modifier.fillMaxSize()) {
                ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = 160.dp)
                    ) {
                        Spacer(Modifier.height(topInset + pinnedHeaderHeight))
                        content()
                    }
                }
                // Pinned header on top, glued to the shared top inset.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = topInset)
                ) {
                    pinnedHeader()
                }
            }
        } else {
            // Pinned (sticky, transparent, no-plate) header. We measure the
            // header and lay out the scrolling body in the SAME pass via
            // SubcomposeLayout, so the body's top spacer is correct on the
            // very first frame. (The old approach measured the header into a
            // state and read it back a frame later — every fresh mount, e.g.
            // this screen re-appearing as the underlay when a sheet/overlay
            // opens, briefly rendered with a 0-height spacer and then jumped
            // down by the header height. That one-frame jump was the
            // "дёргается" jitter. Measuring synchronously removes it.)
            // Top inset that BOTH the header and the body share, so the
            // header lines up pixel-for-pixel with where the first item
            // would otherwise sit. Read here (composable scope) — it can't
            // be read inside the SubcomposeLayout measure lambda.
            val topInset =
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + topPadding

            SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
                val looseW = constraints.copy(minHeight = 0, maxHeight = Int.MAX_VALUE)

                // 1) Measure the pinned header first.
                val headerPlaceables = subcompose("pinnedHeader") {
                    Box(Modifier.fillMaxWidth()) { pinnedHeader() }
                }.map { it.measure(looseW) }
                val headerPx = headerPlaceables.maxOfOrNull { it.height } ?: 0
                val headerDp = headerPx.toDp()

                // 2) Measure the scrolling body, reserving exactly
                //    topInset + headerHeight at the top so the first item
                //    rests right below the title and content scrolls UNDER it.
                val bodyPlaceables = subcompose("body") {
                    ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(bottom = 160.dp)
                        ) {
                            Spacer(Modifier.height(topInset + headerDp))
                            content()
                        }
                    }
                }.map { it.measure(constraints) }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    bodyPlaceables.forEach { it.place(0, 0) }
                    // Header sits on top, pinned at the shared top inset.
                    val headerY = topInset.roundToPx()
                    headerPlaceables.forEach { it.place(0, headerY) }
                }
            }
        }
    }
}
