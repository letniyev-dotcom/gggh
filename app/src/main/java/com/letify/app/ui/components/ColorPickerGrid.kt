package com.letify.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Three-row colour grid (default six per row) with a Telegram-style selection
 * ring and continuous drag-swipe selection — drag your finger across the grid
 * and the active colour follows under the pointer without lifting.
 *
 * Cells are strict 1:1 squares (aspectRatio) so the inner disc is never
 * stretched into an oval. The outer cell footprint stays constant on
 * selection — only the inner disc shrinks, exposing a stroked ring of the
 * same colour around it.
 */
@Composable
fun ColorPickerGrid(
    colors: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 6,
) {
    val density = LocalDensity.current
    val rowGap = 10.dp
    val colGap = 10.dp
    val rows = (colors.size + columns - 1) / columns

    var size by remember { mutableStateOf(IntSize.Zero) }
    val rowGapPx = with(density) { rowGap.toPx() }
    val colGapPx = with(density) { colGap.toPx() }

    // Resolve the cell index under a given local pointer. Returns null if the
    // pointer is outside any cell (e.g. in a gap or past the edge).
    fun cellIndex(pos: Offset): Int? {
        if (size.width <= 0 || size.height <= 0) return null
        // Cells are square: their height equals their width.
        val cellW = (size.width - colGapPx * (columns - 1)) / columns
        val cellH = cellW

        val x = pos.x.coerceIn(0f, size.width.toFloat())
        val y = pos.y.coerceIn(0f, size.height.toFloat())

        val col = (x / (cellW + colGapPx)).toInt().coerceIn(0, columns - 1)
        val row = (y / (cellH + rowGapPx)).toInt().coerceIn(0, rows - 1)
        // Ignore the gap strips between cells so drag doesn't re-emit at
        // every pixel along a row/col boundary.
        val colStart = col * (cellW + colGapPx)
        val rowStart = row * (cellH + rowGapPx)
        if (x - colStart > cellW + 0.5f) return null
        if (y - rowStart > cellH + 0.5f) return null
        val idx = row * columns + col
        return idx.takeIf { it in colors.indices }
    }

    var lastEmitted by remember { mutableIntStateOf(-1) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size = it }
            .pointerInput(colors) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                    cellIndex(down.position)?.let {
                        if (it != lastEmitted) {
                            lastEmitted = it
                            onSelect(colors[it])
                        }
                    }
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        cellIndex(change.position)?.let {
                            if (it != lastEmitted) {
                                lastEmitted = it
                                onSelect(colors[it])
                            }
                        }
                        change.consume()
                    }
                    lastEmitted = -1
                }
            },
        verticalArrangement = Arrangement.spacedBy(rowGap),
    ) {
        repeat(rows) { r ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(colGap),
            ) {
                repeat(columns) { c ->
                    val idx = r * columns + c
                    if (idx < colors.size) {
                        ColorDot(
                            color = colors[idx],
                            selected = colors[idx].value == selected.value,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, modifier: Modifier = Modifier) {
    // Square outer slot — width comes from the weighted Row, height matches
    // via aspectRatio. The inner disc fills the slot and shrinks to ~70%
    // when selected so the slot-wide stroked ring becomes visible around it.
    //
    // The previous implementation used a stiff spring (StiffnessMedium=1500,
    // dampingRatio=0.65) which gave a noticeable "snap" feel — during the
    // drag-swipe selection it looked twitchy because every cell crossing
    // triggered an instant under-damped pulse. We swap to a longer, smooth
    // tween for the inner disc and animate the ring's alpha/stroke colour
    // in tandem so the unselected → selected handoff fades rather than
    // pops, even when the finger is sliding fast across the grid.
    // Telegram-style colour pip: unselected dot fills ~84% of the slot
    // (solid colour, no transparency anywhere), selected dot shrinks
    // down to ~58% so a clearly visible gap opens up between the disc
    // and the stroked ring around it. R17 bumps the gap a touch wider
    // — outer ring 0.86, inner disc 0.58 — because the previous 0.82/
    // 0.64 looked nearly touching at small cell sizes.
    val innerFraction by animateFloatAsState(
        targetValue = if (selected) 0.58f else 0.84f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "colorinner",
    )
    // Cross-fade the ring with the dot itself: alpha = 1 when selected,
    // 0 otherwise, with the same smooth tween. The Stroke is then drawn
    // with the per-frame alpha so the ring "swells in" instead of popping
    // on the same frame the inner disc starts shrinking.
    val ringAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "colorring",
    )
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawBehind {
                if (ringAlpha > 0f) {
                    val sw = 2.dp.toPx()
                    // Selection ring sits a touch outside the unselected
                    // disc (~0.86 of the slot). When a dot is picked, the
                    // disc shrinks to ~0.58 and the ring stays large —
                    // produces a visibly wider gap between disc and ring,
                    // like iOS / Telegram colour pickers.
                    val ringRadius = (this.size.minDimension * 0.86f - sw) / 2f
                    drawCircle(
                        color = color.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = Offset(this.size.width / 2f, this.size.height / 2f),
                        style = Stroke(width = sw),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize(innerFraction)
                .background(color, CircleShape),
        )
    }
}
