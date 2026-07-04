package com.letify.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.IconTile
import com.letify.app.ui.SecTitle
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.state.AnketaStatus
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.theme.Letify

@Composable
fun StatsScreen(state: AnketnicaState, topPad: Dp) {
    val total = state.ankety.size
    val acc = state.ankety.count { it.status == AnketaStatus.ACC }
    val rej = state.ankety.count { it.status == AnketaStatus.REJ }
    val green = Color(0xFF4ECB71)
    val red = Color(0xFFF26A5C)

    ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = topPad, bottom = 40.dp),
        ) {
        Spacer(Modifier.size(6.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("$total", "Всего", "document-text-bold-duotone", Color(0xFF3FA8F5), Modifier.weight(1f))
            StatTile("$acc", "Принято", "check-circle-bold-duotone", green, Modifier.weight(1f))
            StatTile("$rej", "Отклонено", "close-circle-bold-duotone", red, Modifier.weight(1f))
        }

        SecTitle("Соотношение решений")
        Box(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Letify.colors.container).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val newC = total - acc - rej
                Donut(
                    values = listOf(acc.toFloat(), rej.toFloat(), newC.toFloat()),
                    colors = listOf(green, red, Letify.colors.accent),
                    total = total,
                )
                Spacer(Modifier.width(20.dp))
                Column {
                    LegendRow(green, "Принято", acc)
                    LegendRow(red, "Отклонено", rej)
                    LegendRow(Letify.colors.accent, "Новые", newC)
                }
            }
        }

        SecTitle("Заявки по ролям")
        Box(Modifier.padding(horizontal = 16.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Letify.colors.container).padding(18.dp)) {
                val perRole = state.roles.map { r -> r to state.ankety.count { it.role == r.id } }
                val mx = (perRole.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                perRole.forEachIndexed { i, (r, c) ->
                    if (i != 0) Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.name, color = Letify.colors.text, style = Letify.typography.bodyMedium, modifier = Modifier.width(150.dp))
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(999.dp)).background(Letify.colors.track)) {
                            Box(Modifier.fillMaxWidth(c.toFloat() / mx).height(10.dp).clip(RoundedCornerShape(999.dp)).background(r.color))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("$c", color = Letify.colors.muted, style = Letify.typography.labelMedium)
                    }
                }
            }
        }

        SecTitle("За неделю")
        Box(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp)).background(Letify.colors.container).padding(18.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                val vals = listOf(4, 7, 3, 8, 6, 9, 5)
                val wmx = vals.max()
                days.forEachIndexed { i, d ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight((vals[i].toFloat() / wmx).coerceIn(0.05f, 1f))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Letify.colors.accent.copy(alpha = 0.5f + 0.5f * vals[i] / wmx)),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(d, color = Letify.colors.muted, style = Letify.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, icon: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Letify.colors.container).padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(tile = color, icon = icon, size = 38.dp)
        Spacer(Modifier.height(10.dp))
        Text(value, color = Letify.colors.text, style = Letify.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold))
        Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall)
    }
}

@Composable
private fun LegendRow(color: Color, label: String, count: Int) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(999.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text("$label · $count", color = Letify.colors.text, style = Letify.typography.bodyMedium)
    }
}

@Composable
private fun Donut(values: List<Float>, colors: List<Color>, total: Int) {
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        val sum = values.sum().coerceAtLeast(1f)
        Canvas(Modifier.size(120.dp)) {
            var start = -90f
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            values.forEachIndexed { i, v ->
                val sweep = 360f * (v / sum)
                drawArc(
                    color = colors[i],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$total", color = Letify.colors.text, style = Letify.typography.headlineLarge)
            Text("всего", color = Letify.colors.muted, style = Letify.typography.bodySmall)
        }
    }
}

