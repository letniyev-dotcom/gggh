package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.GoalProgressBar
import com.letify.app.ui.components.NoFeedbackButton

import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.SettingsRow
import com.letify.app.ui.components.progressColor
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.AnketaStatus
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

/**
 * Profile — the HOME screen (the bottom navbar is gone). Top-right search button,
 * big centred avatar, name + muted subtitle, the «Разобрано анкет» colour
 * progress bar, then a Telegram-style settings card. «Сценарии» is the first
 * row (the roles/questions scenario editor); then Оформление / Уведомления /
 * Статистика / Списки. The «Новые анкеты» sheet peeks at the bottom (owned by
 * the shell, not this screen).
 */
@Composable
fun ProfileScreen(
    state: AnketnicaState,
    onSearch: () -> Unit,
    onScenarios: () -> Unit,
    onAppearance: () -> Unit,
    onNotifications: () -> Unit,
    onStats: () -> Unit,
    onLists: () -> Unit,
) {
    val total = state.ankety.size
    val processed = state.ankety.count { it.status != AnketaStatus.NEW }
    val progress = if (total == 0) 0f else processed.toFloat() / total

    // No ScreenScaffold here: the profile is the HOME screen behind the «Новые
    // анкеты» sheet, and a vertical drag on it must drive the sheet open (handled
    // by the shell), NOT scroll/overscroll the profile. So the profile is a plain,
    // non-scrolling Column — the content is short and fits — with just the status-
    // bar inset. That removes the rubber-band overscroll the user was hitting.
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 6.dp, bottom = 160.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 0.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            NoFeedbackButton(onClick = onSearch, modifier = Modifier.size(44.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SolarIcon(name = "magnifer-outline", tint = Letify.colors.text, size = 24.dp)
                }
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(state.avatarGradient(1))),
                contentAlignment = Alignment.Center,
            ) {
                Text("Д", color = Color.White, style = Letify.typography.displayLarge)
            }
        }

        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            Text("danya", color = Letify.colors.text, style = Letify.typography.headlineLarge)
        }
        Box(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
            Text("@imrny · Администратор", color = Letify.colors.muted, style = Letify.typography.bodyMedium)
        }

        SettingsCard(
            modifier = Modifier.screenHPad(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Разобрано анкет",
                    color = Letify.colors.text,
                    style = Letify.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$processed / $total",
                    color = progressColor(progress),
                    style = Letify.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(12.dp))
            GoalProgressBar(progress = progress)
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard(
            modifier = Modifier.screenHPad(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            SettingsRow(
                icon = "document-text-bold-duotone",
                iconTile = LetifyColors.TileOrange,
                title = "Сценарии",
                onClick = onScenarios,
            )
            SettingsRow(
                icon = "moon-stars-bold",
                iconTile = LetifyColors.TileViolet,
                title = "Оформление",
                onClick = onAppearance,
            )
            SettingsRow(
                icon = "bell-bold",
                iconTile = LetifyColors.TileRed,
                title = "Уведомления",
                onClick = onNotifications,
            )
            SettingsRow(
                icon = "chart-2-bold-duotone",
                iconTile = LetifyColors.TileBlue,
                title = "Статистика",
                onClick = onStats,
            )
            SettingsRow(
                icon = "list-check-bold-duotone",
                iconTile = LetifyColors.TileGreen,
                title = "Списки",
                onClick = onLists,
            )
        }
    }
}