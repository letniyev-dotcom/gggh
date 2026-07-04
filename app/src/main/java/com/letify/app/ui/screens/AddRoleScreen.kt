package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letify.app.ui.Field
import com.letify.app.ui.HeaderTextAction
import com.letify.app.ui.Hint
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SecTitle
import com.letify.app.ui.SubHeader
import com.letify.app.ui.components.ColorPickerGrid
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.state.Role
import com.letify.app.ui.theme.ThemePalette

@Composable
fun AddRoleScreen(state: AnketnicaState, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(ThemePalette[0]) }

    fun save() {
        val n = name.trim()
        if (n.isEmpty()) { state.toast("Введите название роли"); return }
        val id = "role${System.currentTimeMillis()}"
        state.roles.add(Role(id, n, chosen.toArgbLong(), "star-bold-duotone", emptyList()))
        state.toast("Роль создана"); onBack()
    }

    ScreenFrame(header = { SubHeader(title = "Новая роль", onBack = onBack, trailing = { HeaderTextAction("Готово") { save() } }) }) { topPad ->
        ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topPad, bottom = 24.dp)) {
            SecTitle("Название роли")
            Field(value = name, onValueChange = { name = it }, placeholder = "Например: Дизайнер")
            SecTitle("Цвет")
            // Same Telegram-style colour picker as the Оформление screen: tap a
            // dot and it shrinks with a stroked ring crossfading in — identical
            // logic, reused 1:1 (the user asked for exactly this behaviour here).
            Box(Modifier.screenHPad().padding(top = 6.dp)) {
                ColorPickerGrid(
                    colors = ThemePalette,
                    selected = chosen,
                    onSelect = { chosen = it },
                )
            }
            Hint("После создания роли можно добавить к ней вопросы для анкеты.")
        }
        }
    }
}

private fun Color.toArgbLong(): Long {
    val a = (alpha * 255).toLong() shl 24
    val r = (red * 255).toLong() shl 16
    val g = (green * 255).toLong() shl 8
    val b = (blue * 255).toLong()
    return a or r or g or b
}