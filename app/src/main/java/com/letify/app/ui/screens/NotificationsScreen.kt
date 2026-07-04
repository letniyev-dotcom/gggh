package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SecTitle
import com.letify.app.ui.SubHeader
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.AccentSwitch
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.SettingsRow

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    data class Toggle(val icon: String, val color: Long, val title: String, val default: Boolean)
    val rows = remember {
        listOf(
            Toggle("bell-bold", 0xFFF26A5C, "Новая анкета", true),
            Toggle("check-circle-bold-duotone", 0xFF4ECB71, "Решение по анкете", true),
            Toggle("user-id-bold-duotone", 0xFF3DBFA8, "Новая роль", false),
        )
    }
    val values = remember { mutableStateMapOf<String, Boolean>().apply { rows.forEach { put(it.title, it.default) } } }

    ScreenFrame(header = { SubHeader(title = "Уведомления", onBack = onBack) }) { topPad ->
        ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topPad, bottom = 24.dp),
        ) {
        SecTitle("Пуш-уведомления")
        Box(Modifier.padding(horizontal = 16.dp)) {
            SettingsCard {
                rows.forEach { r ->
                    SettingsRow(
                        icon = r.icon,
                        iconTile = Color(r.color),
                        title = r.title,
                        showChevron = false,
                        trailing = {
                            AccentSwitch(
                                checked = values[r.title] == true,
                                onCheckedChange = { values[r.title] = it },
                            )
                        },
                    )
                }
            }
        }
        }
        }
    }
}