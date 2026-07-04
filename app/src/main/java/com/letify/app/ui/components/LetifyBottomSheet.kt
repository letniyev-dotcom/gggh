package com.letify.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val ENTER_DURATION_MS = 320
private const val EXIT_DURATION_MS = 240

/**
 * Custom letify-styled modal bottom sheet.
 *
 * The previous implementation wrapped Material 3's `ModalBottomSheet`,
 * which on entry briefly paints the scrim at full intensity *before* the
 * sheet animates up — producing a visible "flash" / lag. This re-write
 * drives both the scrim and the sheet from a single shared animation
 * progress (one [Animatable]) so they appear and disappear in perfect
 * sync, and adds drag-to-dismiss with a snap-back when the gesture
 * isn't decisive enough.
 *
 * The header accepts a [trailingIcon] (e.g. a check glyph) which sits in
 * the top-right corner — used as a one-tap commit for sheets where a
 * dedicated bottom "Save" button would feel heavy.
 */
@Composable
fun LetifyBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    primaryLabel: String? = null,
    primaryEnabled: Boolean = true,
    onPrimary: () -> Unit = {},
    trailingIcon: String? = null,
    trailingEnabled: Boolean = true,
    onTrailing: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Single shared progress: 0 = hidden, 1 = fully presented. Drives both
    // the scrim alpha and the sheet's translationY so they never look out
    // of sync.
    val progress = remember { Animatable(0f) }
    var sheetHeightPx by remember { mutableStateOf(0f) }
    // Drag offset sits on top of the (1 - progress) translation. Snaps
    // back to 0 if the release gesture isn't decisive enough.
    val dragOffset = remember { Animatable(0f) }
    var dismissed by remember { mutableStateOf(false) }
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    // Wait until the sheet has been measured before starting the entry
    // animation. If we started immediately, the animation could advance
    // several frames before the sheet's height is known — causing it to
    // pop in partway through the easing curve. Snapshotting on
    // `sheetHeightPx` keeps the entrance crisp and predictable.
    LaunchedEffect(Unit) {
        snapshotFlow { sheetHeightPx }.first { it > 0f }
        progress.animateTo(1f, tween(ENTER_DURATION_MS, easing = EaseOutCubic))
    }

    val dismiss: () -> Unit = remember {
        {
            if (!dismissed) {
                dismissed = true
                scope.launch {
                    val durLeft = (EXIT_DURATION_MS * progress.value).toInt().coerceAtLeast(60)
                    // Park the drag offset back to 0 in parallel so it
                    // doesn't fight the progress-driven translation.
                    launch { dragOffset.animateTo(0f, tween(durLeft, easing = LinearEasing)) }
                    progress.animateTo(0f, tween(durLeft, easing = EaseInCubic))
                    latestOnDismiss()
                }
            }
        }
    }

    BackHandler(enabled = !dismissed) { dismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim — alpha tracks progress so it fades together with the
        // sheet motion instead of "popping" first.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.42f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { dismiss() })
                },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { sz -> sheetHeightPx = sz.height.toFloat() }
                    // graphicsLayer reads `sheetHeightPx` and `progress` at
                    // draw time. On the very first frame `sheetHeightPx`
                    // is 0, which would make translationY = 0 and briefly
                    // paint the sheet at its rest position — the "flash"
                    // the user reported. Keeping alpha at 0 until the
                    // sheet is measured eliminates that frame. Once the
                    // height is known, alpha is fully opaque and the
                    // shared `progress` Animatable drives both the
                    // translation and the scrim in lock-step.
                    .graphicsLayer {
                        translationY = (1f - progress.value) * sheetHeightPx + dragOffset.value
                        alpha = if (sheetHeightPx > 0f) 1f else 0f
                    }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Letify.colors.container),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    // Drag handle. Drag-to-dismiss is attached HERE only —
                    // not on the whole sheet — so wheel pickers / lists
                    // inside the sheet body can scroll freely without
                    // fighting the parent for the drag gesture.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 6.dp)
                            .pointerInput(Unit) {
                                // Track the drag distance synchronously here
                                // rather than reading dragOffset.value at
                                // release time. The launched snapTo calls
                                // below are queued and may not have flushed
                                // by the time onDragEnd fires — reading the
                                // Animatable in that callback can therefore
                                // see a stale value and snap the sheet back
                                // even when the user pulled it well past the
                                // commit threshold (the "тянется, потом
                                // возвращается" symptom from the bug report).
                                var fingerOffset = 0f
                                // Single tracked job serialises every mutation
                                // of dragOffset (same fix as the «Новые анкеты»
                                // sheet): each drag frame used to queue its own
                                // snapTo through Animatable's mutator mutex, so
                                // on release a still-queued STALE snapTo could
                                // run AFTER the snap-back animateTo and cancel
                                // it — the sheet froze partially dragged
                                // instead of springing back or dismissing.
                                var dragJob: Job? = null
                                detectVerticalDragGestures(
                                    onDragStart = { fingerOffset = 0f },
                                    onDragEnd = {
                                        val current = fingerOffset
                                        val threshold = max(80f, sheetHeightPx * 0.18f)
                                        dragJob?.cancel()
                                        if (current > threshold) {
                                            dismiss()
                                        } else {
                                            dragJob = scope.launch {
                                                dragOffset.snapTo(current)
                                                dragOffset.animateTo(
                                                    0f,
                                                    tween(220, easing = EaseOutCubic),
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        fingerOffset = 0f
                                        dragJob?.cancel()
                                        dragJob = scope.launch {
                                            dragOffset.animateTo(
                                                0f,
                                                tween(220, easing = EaseOutCubic),
                                            )
                                        }
                                    },
                                    onVerticalDrag = { _, dy ->
                                        fingerOffset = (fingerOffset + dy).coerceAtLeast(0f)
                                        val next = fingerOffset
                                        dragJob?.cancel()
                                        dragJob = scope.launch { dragOffset.snapTo(next) }
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .background(
                                    Letify.colors.muted.copy(alpha = 0.45f),
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                    }

                    // Title row with optional trailing icon (e.g. a checkmark "save").
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 8.dp, top = 4.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = title,
                            color = Letify.colors.text,
                            style = Letify.typography.headlineLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (trailingIcon != null) {
                            NoFeedbackButton(
                                onClick = {
                                    if (trailingEnabled) {
                                        onTrailing()
                                        dismiss()
                                    }
                                },
                                modifier = Modifier.size(44.dp),
                                enabled = trailingEnabled,
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SolarIcon(
                                        name = trailingIcon,
                                        tint = if (trailingEnabled) Letify.colors.accent
                                        else Letify.colors.muted,
                                        size = 28.dp,
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                        content()
                        if (primaryLabel != null) {
                            Box(Modifier.height(14.dp))
                            PrimaryActionButton(
                                label = primaryLabel,
                                enabled = primaryEnabled,
                                onClick = {
                                    onPrimary()
                                    dismiss()
                                },
                            )
                        }
                        Box(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}
