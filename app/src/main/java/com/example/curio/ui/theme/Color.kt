package com.example.curio.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val LightColors = lightColorScheme(
    primary = Color(0xFF138134),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8F5CF),
    onPrimaryContainer = Color(0xFF00240D),
    inversePrimary = Color(0xFF7BDC8B),
    secondary = Color(0xFF0E7C66),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBFF0E3),
    onSecondaryContainer = Color(0xFF002019),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE3A3),
    onTertiaryContainer = Color(0xFF2B1A00),
    background = Color(0xFFF2FBF3),
    onBackground = Color(0xFF102016),
    surface = Color(0xFFF2FBF3),
    onSurface = Color(0xFF102016),
    surfaceVariant = Color(0xFFDDEEE0),
    onSurfaceVariant = Color(0xFF3A4D3F),
    surfaceTint = Color(0xFF138134),
    surfaceDim = Color(0xFFD6E8D9),
    surfaceBright = Color(0xFFF2FBF3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEAF7EC),
    surfaceContainer = Color(0xFFE1F1E4),
    surfaceContainerHigh = Color(0xFFD9EBDC),
    surfaceContainerHighest = Color(0xFFD1E6D5),
    outline = Color(0xFF5E7664),
    outlineVariant = Color(0xFFC2D8C6),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF213A27),
    inverseOnSurface = Color(0xFFE4F3E7),
    scrim = Color(0xFF000000),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF7EE08F),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF14572B),
    onPrimaryContainer = Color(0xFFC8F5CF),
    inversePrimary = Color(0xFF138134),
    secondary = Color(0xFF7FDCC6),
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF005144),
    onSecondaryContainer = Color(0xFFBFF0E3),
    tertiary = Color(0xFFF5C453),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4200),
    onTertiaryContainer = Color(0xFFFFE3A3),
    background = Color(0xFF0C1710),
    onBackground = Color(0xFFDCEBDF),
    surface = Color(0xFF0C1710),
    onSurface = Color(0xFFDCEBDF),
    surfaceVariant = Color(0xFF3A4D3F),
    onSurfaceVariant = Color(0xFFBFD3C3),
    surfaceTint = Color(0xFF7EE08F),
    surfaceDim = Color(0xFF0C1710),
    surfaceBright = Color(0xFF2C4032),
    surfaceContainerLowest = Color(0xFF07100A),
    surfaceContainerLow = Color(0xFF111F16),
    surfaceContainer = Color(0xFF16271B),
    surfaceContainerHigh = Color(0xFF1D3123),
    surfaceContainerHighest = Color(0xFF253A2C),
    outline = Color(0xFF86A08C),
    outlineVariant = Color(0xFF3A4D3F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFDCEBDF),
    inverseOnSurface = Color(0xFF213A27),
    scrim = Color(0xFF000000),
)

data class CurioColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val reward: Color,
    val onReward: Color,
    val rewardContainer: Color,
    val onRewardContainer: Color,
)

val LightCurioColors = CurioColors(
    success = Color(0xFF1B7A45),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFCDEFD9),
    onSuccessContainer = Color(0xFF00210F),
    warning = Color(0xFF8A5100),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDDB3),
    onWarningContainer = Color(0xFF2C1700),
    info = Color(0xFF1A5EA8),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFD2E5FF),
    onInfoContainer = Color(0xFF001C38),
    reward = Color(0xFF8A5A00),
    onReward = Color(0xFFFFFFFF),
    rewardContainer = Color(0xFFFFE19E),
    onRewardContainer = Color(0xFF2B1D00),
)

val DarkCurioColors = CurioColors(
    success = Color(0xFF7FDB9F),
    onSuccess = Color(0xFF00391C),
    successContainer = Color(0xFF00522B),
    onSuccessContainer = Color(0xFFCDEFD9),
    warning = Color(0xFFFFB95C),
    onWarning = Color(0xFF482900),
    warningContainer = Color(0xFF693C00),
    onWarningContainer = Color(0xFFFFDDB3),
    info = Color(0xFFA5C8FF),
    onInfo = Color(0xFF00315C),
    infoContainer = Color(0xFF00478A),
    onInfoContainer = Color(0xFFD2E5FF),
    reward = Color(0xFFF5C453),
    onReward = Color(0xFF3F2E00),
    rewardContainer = Color(0xFF5B4200),
    onRewardContainer = Color(0xFFFFE19E),
)

val LocalCurioColors = staticCompositionLocalOf { LightCurioColors }

val ScrimStart = Color(0x00000000)
val ScrimEnd = Color(0xB8000000)
val OnScrim = Color(0xFFFFFFFF)
val OnScrimMuted = Color(0xFFE8F1EE)
val IconBackdrop = Color(0x52000000)

fun accentContainerFor(accent: Color, darkTheme: Boolean): Color {
    val factor = if (darkTheme) 0.30f else 0.18f
    val surface = if (darkTheme) Color(0xFF0C1710) else Color(0xFFF2FBF3)
    return lerp(surface, accent, factor)
}

fun onAccentContainerFor(container: Color): Color {
    return Color(0xFF102016)
}
