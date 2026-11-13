package com.example.curio.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun curioCardColors(): CardColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return CardDefaults.cardColors(
        containerColor = if (dark) scheme.surfaceContainer else scheme.surfaceContainerLowest,
        contentColor = scheme.onSurface,
    )
}

@Composable
fun curioSelectedCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
fun curioCardBorder(selected: Boolean): BorderStroke = BorderStroke(
    width = if (selected) 2.dp else 1.dp,
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
)

@Composable
fun curioCardElevation(): CardElevation = CardDefaults.cardElevation(
    defaultElevation = 3.dp,
    pressedElevation = 1.dp,
)
