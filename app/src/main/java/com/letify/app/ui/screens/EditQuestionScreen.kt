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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letify.app.ui.Field
import com.letify.app.ui.IconTile
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SecTitle
import com.letify.app.ui.SubHeader
import com.letify.app.ui.HeaderTextAction
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
fun EditQuestionScreen(
    state: AnketnicaState,
    role: Role,
    existing: Question?,
    onBack: () -> Unit,
) {
    val isNew = existing == null
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: QType.TEXT) }
    val options = remember { mutableStateListOf<String>().also { it.addAll(existing?.options ?: listOf("", "")) } }
    var correct by remember { mutableStateOf(existing?.correct ?: 0) }

    fun save() {
        val t = text.trim()
        if (t.isEmpty()) { state.toast("Введите текст вопроса"); return }
        val trimmed = options.map { it.trim() }
        val opts = trimmed.filter { it.isNotEmpty() }
        if (type == QType.QUIZ && opts.size < 2) { state.toast("Минимум 2 варианта"); return }
        // Re-map the marked-correct index through the blank-option filter:
        // dropping an empty option ABOVE the marked one shifted every later
        // index, so the saved question could mark the WRONG option as correct.
        val safeCorrect = trimmed.getOrNull(correct)
            ?.takeIf { it.isNotEmpty() }
            ?.let { trimmed.take(correct).count { o -> o.isNotEmpty() } }
            ?: 0
        if (isNew) {
            role.questions.add(Question("q${System.currentTimeMillis()}", type, t, opts.ifEmpty { listOf("", "") }, safeCorrect))
        } else {
            existing!!.text = t
            existing.type = type
            existing.options.clear(); existing.options.addAll(opts.ifEmpty { listOf("", "") })
            existing.correct = safeCorrect
        }
        state.toast("Сохранено"); onBack()
    }

    ScreenFrame(header = { SubHeader(title = if (isNew) "Новый вопрос" else "Вопрос", onBack = onBack, trailing = { HeaderTextAction("Готово") { save() } }) }) { topPad ->
        ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topPad, bottom = 24.dp)) {
            SecTitle("Текст вопроса")
            Field(value = text, onValueChange = { text = it }, placeholder = "Введите вопрос…", singleLine = false)

            SecTitle("Тип ответа")
            Box(Modifier.padding(horizontal = 16.dp)) {
                SettingsCard {
                    QType.values().forEach { t ->
                        TypeOption(t, selected = t == type) { type = t }
                    }
                }
            }

            if (type == QType.QUIZ) {
                SecTitle("Варианты ответа · отметьте верный")
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SettingsCard {
                        options.forEachIndexed { i, opt ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val green = Color(0xFF4ECB71)
                                // Filled radio dot — no hairline ring/outline.
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape)
                                        .background(if (correct == i) green else Letify.colors.track)
                                        .noFeedbackClick { correct = i },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (correct == i) SolarIcon(name = "check-bold", tint = Color.White, size = 14.dp)
                                }
                                Spacer(Modifier.size(12.dp))
                                Box(Modifier.weight(1f)) {
                                    Field(
                                        value = opt,
                                        onValueChange = { options[i] = it },
                                        placeholder = "Вариант ${i + 1}",
                                    )
                                }
                                Spacer(Modifier.size(8.dp))
                                if (options.size > 2) {
                                    Box(
                                        Modifier.size(30.dp).noFeedbackClick {
                                            options.removeAt(i)
                                            // Keep the checkmark on the SAME
                                            // option: removing a row above the
                                            // marked one used to shift the mark
                                            // onto the option below it.
                                            when {
                                                correct == i -> correct = 0
                                                correct > i -> correct -= 1
                                            }
                                            if (correct >= options.size) correct = 0
                                        },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        SolarIcon(name = "close-circle-bold-duotone", tint = Letify.colors.muted, size = 20.dp)
                                    }
                                }
                            }
                        }
                        AddRow("Добавить вариант") { options.add("") }
                    }
                }
            }

            if (!isNew) {
                Spacer(Modifier.size(22.dp))
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SettingsCard {
                        Row(
                            Modifier.fillMaxWidth().noFeedbackClick {
                                role.questions.removeAll { it.id == existing!!.id }
                                state.toast("Вопрос удалён"); onBack()
                            }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(tile = Color(0xFFF26A5C), icon = "trash-bin-trash-bold")
                            Spacer(Modifier.size(14.dp))
                            Text("Удалить вопрос", color = Color(0xFFF26A5C), style = Letify.typography.titleSmall)
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun TypeOption(type: QType, selected: Boolean, onClick: () -> Unit) {
    val meta = QTYPE_META.getValue(type)
    Row(
        Modifier.fillMaxWidth().noFeedbackClick(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(tile = meta.color, icon = meta.icon)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(meta.label, color = Letify.colors.text, style = Letify.typography.titleSmall)
            Text(meta.desc, color = Letify.colors.muted, style = Letify.typography.bodySmall)
        }
        // Filled radio dot — no hairline ring/outline.
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background(if (selected) Letify.colors.accent else Letify.colors.track),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) SolarIcon(name = "check-bold", tint = Color.White, size = 14.dp)
        }
    }
}