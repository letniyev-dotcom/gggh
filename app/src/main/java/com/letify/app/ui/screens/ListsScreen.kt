package com.letify.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SubHeader
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.SegItem
import com.letify.app.ui.components.SegmentedTabs
import com.letify.app.ui.state.AnketaStatus
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.theme.Letify

/**
 * «Списки» — decided applications that have left the Анкеты screen. Filtered
 * by outcome: Принятые / Отказы / Все. Opened from Profile → Списки.
 */
@Composable
fun ListsScreen(state: AnketnicaState, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    var tab by remember { mutableStateOf("acc") }
    ScreenFrame(header = { SubHeader(title = "Списки", onBack = onBack) }) { topPad ->
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(topPad))
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SegmentedTabs(
                    items = listOf(
                        SegItem("acc", "Приняты"),
                        SegItem("rej", "Отказы"),
                        SegItem("all", "Все"),
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                )
            }
            val items = when (tab) {
                "acc" -> state.ankety.filter { it.status == AnketaStatus.ACC }
                "rej" -> state.ankety.filter { it.status == AnketaStatus.REJ }
                else -> state.ankety.toList()
            }
            ElasticOverscroll(Modifier.fillMaxSize()) {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                ) {
                    if (items.isEmpty()) {
                        item {
                            Text(
                                "Здесь пока пусто.",
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
    }
}
