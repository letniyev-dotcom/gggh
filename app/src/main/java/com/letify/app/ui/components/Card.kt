package com.letify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify

@Composable
fun WCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    shape: RoundedCornerShape = Letify.shapes.Card,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Letify.colors.container, shape)
            .padding(contentPadding)
    ) { content() }
}
