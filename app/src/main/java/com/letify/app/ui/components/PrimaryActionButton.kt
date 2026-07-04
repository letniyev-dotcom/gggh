package com.letify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify

@Composable
fun PrimaryActionButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.5f
    NoFeedbackButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        enabled = enabled,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Letify.colors.accent.copy(alpha = alpha), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = Color(0xFF0C1F12), style = Letify.typography.titleMedium)
        }
    }
}
