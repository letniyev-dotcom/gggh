package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.state.Anketa
import com.letify.app.ui.state.AnketaStatus
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.theme.Letify

/**
 * Анкеты — shows ONLY pending (new) applications, most recent first. Once an
 * application is accepted/rejected it leaves this screen and lives in «Списки»
 * (Profile → Списки). The old sort bar (Новые/Все/Приняты/Отказы) + swipe were
 * removed per design.
 */
@Composable
fun AnketkiScreen(state: AnketnicaState, topPad: Dp, onOpen: (Int) -> Unit) {
    val items = state.ankety.filter { it.status == AnketaStatus.NEW }.sortedBy { it.agox }
    ElasticOverscroll(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPad, bottom = 120.dp),
        ) {
            if (items.isEmpty()) {
                item {
                    Text(
                        "Новых анкет нет.",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }
            } else {
                items(items, key = { it.id }) { a ->
                    Box(Modifier.padding(horizontal = 10.dp)) {
                        AnketaRow(state, a) { onOpen(a.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun AnketaRow(state: AnketnicaState, a: Anketa, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .noFeedbackClick(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Plain gradient avatar — the little NEW status dot was removed per
        // feedback ("у аватарки убери точки").
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(Brush.linearGradient(state.avatarGradient(a.id))),
            contentAlignment = Alignment.Center,
        ) {
            Text(a.name.take(1), color = Color.White, style = Letify.typography.titleLarge)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    a.name,
                    color = Letify.colors.text,
                    style = Letify.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(a.time, color = Letify.colors.muted, style = Letify.typography.bodySmall)
            }
            Spacer(Modifier.size(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val role = state.role(a.role)
                Box(Modifier.size(7.dp).clip(CircleShape).background(role?.color ?: Letify.colors.muted))
                Spacer(Modifier.size(6.dp))
                Text(
                    role?.name ?: a.role,
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(a.status)
            }
        }
    }
}

@Composable
fun StatusBadge(status: AnketaStatus) {
    val (label, color) = when (status) {
        AnketaStatus.NEW -> "Новая" to Letify.colors.accent
        AnketaStatus.ACC -> "Принят" to Color(0xFF4ECB71)
        AnketaStatus.REJ -> "Отклонён" to Color(0xFFF26A5C)
        AnketaStatus.HOLD -> "Доп. рассмотрение" to Color(0xFF5AA7FF)
    }
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(label, color = color, style = Letify.typography.labelSmall)
    }
}