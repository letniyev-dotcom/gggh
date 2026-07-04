package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.letify.app.ui.ScreenFrame
import com.letify.app.ui.SecTitle
import com.letify.app.ui.SubHeader
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Anketa
import com.letify.app.ui.state.AnketaStatus
import com.letify.app.ui.state.AnketnicaState
import com.letify.app.ui.state.Answer
import com.letify.app.ui.state.QType
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

/**
 * Anketa detail. NO bottom action bar anymore. Under the name sit two buttons:
 *  • «Статус» — opens a rounded dropdown of status rows (icon + label), tap sets
 *    the application's status.
 *  • «Написать в ТГ» — Telegram-blue, opens a chat with the candidate in Telegram
 *    (wired later; toasts for now).
 */
@Composable
fun AnketaDetailScreen(state: AnketnicaState, a: Anketa, onBack: () -> Unit, topInsetOverride: Dp? = null) {
    ScreenFrame(header = { SubHeader(title = "Анкета", onBack = onBack) }, topInsetOverride = topInsetOverride) { topPad ->
        ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topPad, bottom = 40.dp),
            ) {
                // Hero: avatar, name, subtitle, then the two action buttons.
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(state.avatarGradient(a.id))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(a.name.take(1), color = Color.White, style = Letify.typography.displayLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(a.name, color = Letify.colors.text, style = Letify.typography.headlineLarge)
                    Text(a.user, color = Letify.colors.muted, style = Letify.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))

                    StatusAndTelegramButtons(state, a)
                }

                SecTitle("Информация из Telegram")
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SettingsCard {
                        a.fields.forEach { (k, v) ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(k, color = Letify.colors.text, style = Letify.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text(v, color = Letify.colors.muted, style = Letify.typography.bodyMedium)
                            }
                        }
                    }
                }

                SecTitle("Ответы на вопросы")
                Column(Modifier.padding(horizontal = 16.dp)) {
                    a.answers.forEachIndexed { i, ans ->
                        AnswerCard(state, ans)
                        if (i != a.answers.lastIndex) Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/** Status option shown in the dropdown. Maps a label+icon+tile to a concrete status. */
private data class StatusOption(val label: String, val dot: Color, val status: AnketaStatus)

// Status is shown as a solid coloured DOT (no icon tiles):
//   green = принято, red = отклонено, yellow = вернуть в новые,
//   blue = доп. рассмотрение (HOLD — «дополнительное рассмотрение»).
private val StatusGreen = Color(0xFF4ECB71)
private val StatusRed = Color(0xFFF26A5C)
private val StatusAmber = Color(0xFFEFCF4A)
private val StatusBlue = Color(0xFF5AA7FF)

private val STATUS_OPTIONS = listOf(
    StatusOption("Принято", StatusGreen, AnketaStatus.ACC),
    StatusOption("Отклонено", StatusRed, AnketaStatus.REJ),
    StatusOption("Вернуть в новые", StatusAmber, AnketaStatus.NEW),
    StatusOption("Доп. рассмотрение", StatusBlue, AnketaStatus.HOLD),
)

@Composable
private fun StatusAndTelegramButtons(state: AnketnicaState, a: Anketa) {
    var menuOpen by remember { mutableStateOf(false) }
    // Measured width of the «Статус» button — the dropdown is sized to match it.
    var btnWidthPx by remember { mutableStateOf(0) }
    // Chevron flips 180° while the menu is open (list-style expander).
    val chevronRot by animateFloatAsState(if (menuOpen) 180f else 0f, label = "statusChevron")

    Box(Modifier.fillMaxWidth().widthIn(max = 360.dp).padding(horizontal = 24.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // «Статус» button — measures its own width so the dropdown matches it.
            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .onSizeChanged { btnWidthPx = it.width }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Letify.colors.container)
                    .noFeedbackClick { menuOpen = !menuOpen },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val current = STATUS_OPTIONS.firstOrNull { it.status == a.status }
                StatusDot(current?.dot ?: Letify.colors.muted, size = 12.dp)
                Spacer(Modifier.width(8.dp))
                Text("Статус", color = Letify.colors.text, style = Letify.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                SolarIcon(
                    name = "alt-arrow-down-outline",
                    tint = Letify.colors.muted,
                    size = 16.dp,
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRot },
                )
            }

            // «Написать в ТГ» button — Telegram blue with the supplied logo.
            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LetifyColors.TelegramBlue)
                    .noFeedbackClick { state.toast("Открываю Telegram…") },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SolarIcon(name = "telegram", tint = Color.White, size = 20.dp)
                Spacer(Modifier.width(8.dp))
                Text("Написать в ТГ", color = Color.White, style = Letify.typography.titleSmall)
            }
        }

        // Dropdown menu — a Popup so it expands OVER content (nothing below
        // shifts), theme-coloured, NO border. Anchored just under «Статус».
        val menuState = remember { MutableTransitionState(false) }
        menuState.targetState = menuOpen
        if (menuState.currentState || menuState.targetState) {
            val density = LocalDensity.current
            // Expand DIRECTLY from the button: anchored at the button's own top-
            // left (offset 0), so the list grows down from the button with no gap.
            // Width is pinned to the measured button width so the menu is exactly
            // as wide as «Статус», not full-width.
            val menuWidthDp = with(density) { btnWidthPx.toDp() }
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 48.dp.roundToPx() }),
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = menuState,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    Column(
                        Modifier
                            .then(if (menuWidthDp > 0.dp) Modifier.width(menuWidthDp) else Modifier.widthIn(min = 200.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Letify.colors.container)
                            .padding(6.dp),
                    ) {
                        STATUS_OPTIONS.forEach { opt ->
                            val selected = a.status == opt.status
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(11.dp))
                                    .let { if (selected) it.background(Letify.colors.text.copy(alpha = 0.06f)) else it }
                                    .noFeedbackClick {
                                        a.status = opt.status
                                        state.toast("Статус: ${opt.label}")
                                        menuOpen = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusDot(opt.dot, size = 12.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(opt.label, color = Letify.colors.text, style = Letify.typography.bodyMedium, modifier = Modifier.weight(1f))
                                if (selected) SolarIcon(name = "check-bold", tint = Letify.colors.accent, size = 18.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Solid coloured status dot (green/red/yellow/blue). Replaces the old icon tiles. */
@Composable
private fun StatusDot(color: Color, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun AnswerCard(state: AnketnicaState, ans: Answer) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Letify.colors.container).padding(16.dp)) {
        Column {
            Text(ans.q, color = Letify.colors.muted, style = Letify.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            when (ans.type) {
                QType.TEXT -> Text(ans.a ?: "", color = Letify.colors.text, style = Letify.typography.bodyLarge)
                QType.VOICE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Letify.colors.accent)
                            .noFeedbackClick { state.toast("▶ Голосовое сообщение…") },
                        contentAlignment = Alignment.Center,
                    ) {
                        SolarIcon(name = "play-bold", tint = Color.White, size = 18.dp)
                    }
                    Spacer(Modifier.size(12.dp))
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Letify.colors.track))
                    Spacer(Modifier.size(12.dp))
                    Text(ans.dur ?: "", color = Letify.colors.muted, style = Letify.typography.bodyMedium)
                }
                QType.QUIZ -> Column {
                    ans.options.forEachIndexed { i, opt ->
                        val isCorrect = i == ans.correct
                        val isChosenWrong = i == ans.chosen && ans.chosen != ans.correct
                        val green = Color(0xFF4ECB71)
                        val red = Color(0xFFF26A5C)
                        val border = when {
                            isCorrect -> green
                            isChosenWrong -> red
                            else -> Letify.colors.track
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                                .background(border.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(opt, color = Letify.colors.text, style = Letify.typography.bodyMedium, modifier = Modifier.weight(1f))
                            if (isCorrect) SolarIcon(name = "check-circle-bold-duotone", tint = green, size = 20.dp)
                            else if (isChosenWrong) SolarIcon(name = "close-circle-bold-duotone", tint = red, size = 20.dp)
                        }
                    }
                }
            }
        }
    }
}