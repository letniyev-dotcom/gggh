package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import com.letify.app.ui.components.ElasticOverscroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.letify.app.ui.Hint
import com.letify.app.ui.IconTile
import com.letify.app.ui.SecTitle
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.state.Role
import com.letify.app.ui.theme.Letify

@Composable
fun RolesScreen(state: AnketnicaState, topPad: Dp, onRole: (String) -> Unit, onAddRole: () -> Unit) {
    ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = topPad, bottom = 24.dp),
        ) {
        SecTitle("Роли проекта")
        Box(Modifier.padding(horizontal = 16.dp)) {
            SettingsCard {
                state.roles.forEach { r -> RoleRow(r) { onRole(r.id) } }
                AddRow("Добавить роль", onAddRole)
            }
        }
        Hint("Нажмите на роль, чтобы настроить вопросы анкеты. Вопросы бывают трёх типов: текстовой, голосовой и викторина.")
        Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun RoleRow(r: Role, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().noFeedbackClick(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(tile = r.color, icon = r.icon)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(r.name, color = Letify.colors.text, style = Letify.typography.titleSmall)
            Text(
                "${r.questions.size} вопрос(ов)",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }
        SolarIcon(name = "alt-arrow-right-outline", tint = Letify.colors.muted, size = 20.dp)
    }
}

@Composable
fun AddRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().noFeedbackClick(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(11.dp)).background(Letify.colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = "add-bold", tint = Letify.colors.accent, size = 19.dp)
        }
        Spacer(Modifier.size(14.dp))
        Text(label, color = Letify.colors.accent, style = Letify.typography.titleSmall)
    }
}