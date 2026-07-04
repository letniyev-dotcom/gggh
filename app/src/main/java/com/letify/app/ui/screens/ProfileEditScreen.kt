package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letify.app.ui.Field
import com.letify.app.ui.HeaderTextAction
import com.letify.app.ui.IconTile
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SecTitle
import com.letify.app.ui.SubHeader
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.theme.Letify

@Composable
fun ProfileEditScreen(state: AnketnicaState, onBack: () -> Unit) {
    var name by remember { mutableStateOf("danya") }
    var username by remember { mutableStateOf("@imrny") }

    ScreenFrame(header = { SubHeader(title = "Мой профиль", onBack = onBack, trailing = { HeaderTextAction("Готово") { state.toast("Профиль сохранён"); onBack() } }) }) { topPad ->
        ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topPad, bottom = 24.dp)) {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(Brush.linearGradient(state.avatarGradient(1))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Д", color = Color.White, style = Letify.typography.displayLarge)
                }
            }
            SecTitle("Имя")
            Field(value = name, onValueChange = { name = it })
            SecTitle("Username")
            Field(value = username, onValueChange = { username = it })
            SecTitle("Роль в проекте")
            Box(Modifier.padding(horizontal = 16.dp)) {
                SettingsCard {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(tile = Color(0xFFF26A5C), icon = "shield-user-bold-duotone")
                        Spacer(Modifier.size(14.dp))
                        Text("Администратор", color = Letify.colors.text, style = Letify.typography.titleSmall)
                    }
                }
            }
        }
        }
    }
}