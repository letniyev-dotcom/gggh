package com.letify.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify

/**
 * Scaffolding for a full-screen overlay screen (Edit Profile, Goals, Add* …).
 *
 * Layout (top → bottom):
 *   1. Pinned header — back arrow + title. Stays glued to the top edge of
 *      the screen while content scrolls underneath it. Opaque page bg so
 *      scrolling content never bleeds through.
 *   2. Scrollable content column. Sits BELOW the pinned header (top padding
 *      reserves the header height) and ABOVE the floating primary button
 *      (bottom padding reserves the button area). `imePadding()` is applied
 *      to the scroll container so the scroll viewport shrinks above the
 *      keyboard when it opens — this is what allows BasicTextField's
 *      built-in bring-into-view to actually scroll a focused field into the
 *      visible portion instead of leaving it hidden behind the IME or the
 *      floating button.
 *   3. Floating primary action button — anchored to the bottom edge with
 *      `imePadding()` + `navigationBarsPadding()` so it rises with the
 *      keyboard and clears the gesture nav.
 *
 * Elastic overscroll is kept on the scroll column only — the pinned header
 * and the floating button stay perfectly still.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverlayScreen(
    title: String,
    onBack: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val elastic = rememberElasticOverscroll()
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Letify.colors.bg)
                .nestedScroll(elastic.connection),
        ) {
            // ── 1. Scrollable content ────────────────────────────────────
            //   IMPORTANT: the scroll viewport occupies the FULL height of
            //   the screen — no top padding for status bar or header here.
            //   The header-clearance gap is supplied as a leading Spacer
            //   INSIDE the scroll list so that when the user scrolls up,
            //   the actual content rises into the header area and becomes
            //   visible THROUGH the transparent pinned header. If we
            //   reserved the gap with `Modifier.padding(top = …)` instead,
            //   the scroll viewport would start below the header and any
            //   content scrolled past that boundary would be clipped — the
            //   header zone would just show empty page background.
            //
            //   `imePadding()` shrinks the viewport above the keyboard so
            //   BasicTextField's built-in bring-into-view can lift the
            //   focused field above the IME.
            val density = LocalDensity.current
            val imeBottomPx = WindowInsets.ime.getBottom(density)
            val isImeVisible = imeBottomPx > 0
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scroll)
                    .graphicsLayer {
                        // While the keyboard is up we DON'T paint the elastic
                        // translation. Reason: a focused BasicTextField has
                        // a BringIntoViewRequester that constantly re-aligns
                        // the field above the IME. If we translate the column
                        // every time the user swipes past the edge, the
                        // requester fires a counter-scroll, which we then
                        // partially absorb into the rubber band, which the
                        // requester fights again — perceived as the cursor
                        // and the entire field "shaking" or "twitching".
                        // The overscroll value still accumulates internally
                        // and springs back on lift, we just don't render it.
                        translationY = if (isImeVisible) 0f else elastic.verticalOverscroll.floatValue
                    }
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header-clearance: status-bar inset + the 54dp header band.
                // Both are inside the scroll list so they slide off-screen
                // when the user scrolls up, letting content reach the
                // transparent header zone.
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(HEADER_HEIGHT_DP))
                content()
                // Reserve space so the last input row can scroll above the
                // floating button (and above the IME when it's open).
                Spacer(modifier = Modifier.height(120.dp))
            }

            // ── 2. Pinned header ────────────────────────────────────────
            //   Transparent — content visibly slides UNDER the title/back
            //   arrow (per design). `windowInsetsPadding(statusBars)`
            //   keeps it under the notch / status icons.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HEADER_HEIGHT_DP)
                        .padding(top = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NoFeedbackButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            SolarIcon(
                                name = "alt-arrow-left-outline",
                                tint = Letify.colors.text,
                                size = 28.dp,
                            )
                        }
                    }
                    Box(Modifier.width(4.dp))
                    Text(
                        text = title,
                        color = Letify.colors.text,
                        style = Letify.typography.headlineMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── 3. Floating primary action button ───────────────────────
            //   imePadding lifts it above the keyboard; navigationBarsPadding
            //   keeps it above the gesture pill when keyboard is closed.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                PrimaryActionButton(
                    label = primaryLabel,
                    enabled = primaryEnabled,
                    onClick = onPrimary,
                )
            }
        }
    }
}

/**
 * Height of the pinned header bar (excluding the status-bar inset). Has to
 * match the bottom-of-back-button position so the first scrollable row
 * doesn't tuck under it.
 */
private val HEADER_HEIGHT_DP = 54.dp
