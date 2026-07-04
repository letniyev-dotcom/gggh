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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SubHeader
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Anketa
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.theme.Letify

/**
 * Поиск по анкетам — opened from the top-right of the profile (the button that
 * used to be the profile-edit pencil). One query box searches EVERY field of
 * EVERY anketa across ALL statuses (not just new): id, имя, @юзернейм, роль,
 * все поля (возраст, город, опыт…) и содержимое ответов на вопросы. Substring,
 * case-insensitive. Blank query lists everything; tapping a result opens that
 * anketa's detail.
 */
@Composable
fun SearchScreen(state: AnketnicaState, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val results = remember(q, state.ankety.size) {
        val all = state.ankety.sortedBy { it.agox }
        if (q.isEmpty()) all else all.filter { matches(state, it, q) }
    }

    ScreenFrame(header = { SubHeader(title = "Поиск", onBack = onBack) }) { topPad ->
        Column(Modifier.fillMaxSize().padding(top = topPad)) {
            SearchField(value = query, onValueChange = { query = it }, onClear = { query = "" })
            ElasticOverscroll(Modifier.fillMaxSize()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
                ) {
                    if (results.isEmpty()) {
                        item {
                            Text(
                                if (q.isEmpty())
                                    "Начните вводить запрос — имя, @юзернейм, ID, возраст, город или текст ответа."
                                else "Ничего не найдено.",
                                color = Letify.colors.muted,
                                style = Letify.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    } else {
                        items(results, key = { it.id }) { a ->
                            Box(Modifier.padding(horizontal = 10.dp)) {
                                AnketaRow(state, a) { onOpen(a.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** True if [query] appears in ANY field of the anketa (case-insensitive). */
private fun matches(state: AnketnicaState, a: Anketa, query: String): Boolean {
    val needle = query.lowercase()
    val haystack = buildList {
        add(a.id.toString())
        add(a.name)
        add(a.user)
        add(a.role)
        state.role(a.role)?.name?.let { add(it) }
        a.fields.forEach { (k, v) -> add(k); add(v) }
        a.answers.forEach { ans ->
            add(ans.q)
            ans.a?.let { add(it) }
            ans.options.forEach { add(it) }
        }
    }
    return haystack.any { it.lowercase().contains(needle) }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Letify.colors.container)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SolarIcon(name = "magnifer-outline", tint = Letify.colors.muted, size = 20.dp)
        Spacer(Modifier.size(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Поиск анкет", color = Letify.colors.muted, style = Letify.typography.bodyLarge)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = Letify.typography.bodyLarge.copy(color = Letify.colors.text),
                cursorBrush = SolidColor(Letify.colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.size(10.dp))
            Box(
                Modifier.size(22.dp).noFeedbackClick(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                SolarIcon(name = "close-circle-bold-duotone", tint = Letify.colors.muted, size = 20.dp)
            }
        }
    }
}
