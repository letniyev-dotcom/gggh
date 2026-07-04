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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.Hint
import com.letify.app.ui.IconTile
import com.letify.app.ui.SecTitle
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SubHeader
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.state.QTYPE_META
import com.letify.app.ui.state.QType
import com.letify.app.ui.state.Question
import com.letify.app.ui.state.Role
import com.letify.app.ui.theme.Letify

@Composable
fun RoleQuestionsScreen(
    state: AnketnicaState,
    role: Role,
    onBack: () -> Unit,
    onEditQuestion: (Question?) -> Unit,
) {
    ScreenFrame(header = { SubHeader(title = role.name, onBack = onBack) }) { topPad ->
        ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topPad, bottom = 24.dp),
        ) {
            SecTitle("Вопросы анкеты")
            Box(Modifier.padding(horizontal = 16.dp)) {
                SettingsCard {
                    if (role.questions.isEmpty()) {
                        Text(
                            "Вопросов пока нет.",
                            color = Letify.colors.muted,
                            style = Letify.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                    role.questions.forEach { q -> QuestionRow(q) { onEditQuestion(q) } }
                    AddRow("Добавить вопрос") { onEditQuestion(null) }
                }
            }
            Hint("Кандидат на роль «${role.name}» ответит на эти вопросы при подаче.")
            Spacer(Modifier.size(24.dp))
        }
        }
    }
}

@Composable
private fun QuestionRow(q: Question, onClick: () -> Unit) {
    val meta = QTYPE_META.getValue(q.type)
    Row(
        Modifier.fillMaxWidth().noFeedbackClick(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(tile = meta.color, icon = meta.icon)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                q.text.ifBlank { "(без текста)" },
                color = Letify.colors.text,
                style = Letify.typography.titleSmall,
            )
            val sub = meta.short + if (q.type == QType.QUIZ) " · ${q.options.size} вар." else ""
            Text(sub, color = Letify.colors.muted, style = Letify.typography.bodySmall)
        }
        SolarIcon(name = "alt-arrow-right-outline", tint = Letify.colors.muted, size = 20.dp)
    }
}