package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letify.app.ui.IconTile
import com.letify.app.ui.components.AccentSwitch
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SubHeader
import com.letify.app.ui.components.ColorPickerGrid
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.SettingsRow
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.state.TransitionStyle
import com.letify.app.ui.theme.LetifyColors
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.ThemeMode
import com.letify.app.ui.theme.ThemePalette

@Composable
fun AppearanceScreen(onBack: () -> Unit, state: AnketnicaState) {
    val scroll = rememberScrollState()
    ScreenFrame(header = { SubHeader(title = "Оформление", onBack = onBack) }) { topPad ->
        ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = topPad, bottom = 60.dp),
        ) {
            // ── ТЕМА ────────────────────────────────────────────────────────
            SectionLabel("Тема")
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "moon-bold",
                    iconTile = LetifyColors.TileViolet,
                    title = "Тёмная тема",
                    value = if (state.themeMode == ThemeMode.Dark) "Включена" else "Выключена",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = state.themeMode == ThemeMode.Dark,
                            onCheckedChange = { dark ->
                                state.themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light
                            },
                        )
                    },
                )
            }

            // ── АКЦЕНТНЫЙ ЦВЕТ ───────────────────────────────────────────────
            SectionLabel("Акцентный цвет")
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            ) {
                ColorPickerGrid(
                    colors = ThemePalette,
                    selected = state.accent,
                    onSelect = { state.accent = it },
                )
            }

            // ── АНИМАЦИЯ ─────────────────────────────────────────────────────
            SectionLabel("Переход между экранами")
            Row(
                modifier = Modifier.screenHPad().fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TransitionOptionCard(
                    title = "Сдвиг",
                    subtitle = "Единое полотно",
                    selected = state.transitionStyle == TransitionStyle.Push,
                    onSelect = { state.transitionStyle = TransitionStyle.Push },
                    modifier = Modifier.weight(1f),
                )
                TransitionOptionCard(
                    title = "Наплыв",
                    subtitle = "Наезжает поверх",
                    selected = state.transitionStyle == TransitionStyle.Cover,
                    onSelect = { state.transitionStyle = TransitionStyle.Cover },
                    modifier = Modifier.weight(1f),
                )
            }

            SectionLabel("Жесты")
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "arrow-left-bold",
                    iconTile = LetifyColors.TileViolet,
                    title = "Свайп назад",
                    value = if (state.swipeBackEnabled) "Включён" else "Выключен",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = state.swipeBackEnabled,
                            onCheckedChange = { state.swipeBackEnabled = it },
                        )
                    },
                )
            }
            Box(Modifier.screenHPad().padding(top = 8.dp)) {
                Text(
                    "Когда выключено, экраны нельзя закрывать свайпом от левого края — только кнопкой «Назад».",
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }

            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Letify.colors.muted,
        style = Letify.typography.labelSmall,
        modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun TransitionOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    // No border/outline. Selection is shown ONLY by the filled accent check
    // circle on the right (and the accent-tinted title), never a hairline ring.
    Column(
        modifier
            .clip(shape)
            .background(Letify.colors.container)
            .noFeedbackClick(onClick = onSelect)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = if (selected) Letify.colors.accent else Letify.colors.text,
                style = Letify.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) Letify.colors.accent else Letify.colors.track),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) SolarIcon(name = "check-bold", tint = Color.White, size = 14.dp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Letify.colors.muted, style = Letify.typography.bodySmall, maxLines = 1)
    }
}