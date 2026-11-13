package com.example.curio.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

fun Modifier.bubbles(color: Color): Modifier = drawBehind {
    val base = size.minDimension
    drawCircle(color.copy(alpha = 0.35f), radius = base * 0.55f, center = Offset(size.width * 0.95f, size.height * 0.05f))
    drawCircle(color.copy(alpha = 0.25f), radius = base * 0.30f, center = Offset(size.width * 0.78f, size.height * 0.95f))
    drawCircle(color.copy(alpha = 0.20f), radius = base * 0.18f, center = Offset(size.width * 0.08f, size.height * 0.80f))
}
