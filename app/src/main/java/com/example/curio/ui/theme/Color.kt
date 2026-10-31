package com.example.curio.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val LightColors = lightColorScheme(
    primary = Color(0xFF0B6B5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8F0E2),
    onPrimaryContainer = Color(0xFF00201B),
    inversePrimary = Color(0xFF70D9C4),
    secondary = Color(0xFF1A5EA8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5FF),
    onSecondaryContainer = Color(0xFF001C38),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8F0C4),
    onTertiaryContainer = Color(0xFF002105),
    background = Color(0xFFF3FAF8),
    onBackground = Color(0xFF0E1F1C),
    surface = Color(0xFFF3FAF8),
    onSurface = Color(0xFF0E1F1C),
    surfaceVariant = Color(0xFFDCE8E4),
    onSurfaceVariant = Color(0xFF3B4946),
    surfaceTint = Color(0xFF0B6B5F),
    surfaceDim = Color(0xFFD5E1DD),
    surfaceBright = Color(0xFFF3FAF8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFECF5F2),
    surfaceContainer = Color(0xFFE5F0ED),
    surfaceContainerHigh = Color(0xFFDEEAE7),
    surfaceContainerHighest = Color(0xFFD7E4E0),
    outline = Color(0xFF66766F),
    outlineVariant = Color(0xFFBDCBC6),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF223330),
    inverseOnSurface = Color(0xFFE8F3F0),
    scrim = Color(0xFF000000),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD8C4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005045),
    onPrimaryContainer = Color(0xFFB8F0E2),
    inversePrimary = Color(0xFF0B6B5F),
    secondary = Color(0xFFA5C8FF),
    onSecondary = Color(0xFF00315C),
    secondaryContainer = Color(0xFF00478A),
    onSecondaryContainer = Color(0xFFD2E5FF),
    tertiary = Color(0xFF8FD98B),
    onTertiary = Color(0xFF00390C),
    tertiaryContainer = Color(0xFF115318),
    onTertiaryContainer = Color(0xFFC8F0C4),
    background = Color(0xFF0A1514),
    onBackground = Color(0xFFDDE8E5),
    surface = Color(0xFF0A1514),
    onSurface = Color(0xFFDDE8E5),
    surfaceVariant = Color(0xFF3B4946),
    onSurfaceVariant = Color(0xFFBDCBC6),
    surfaceTint = Color(0xFF6FD8C4),
    surfaceDim = Color(0xFF0A1514),
    surfaceBright = Color(0xFF2A3B38),
    surfaceContainerLowest = Color(0xFF06100F),
    surfaceContainerLow = Color(0xFF101D1B),
    surfaceContainer = Color(0xFF142320),
    surfaceContainerHigh = Color(0xFF1B2B28),
    surfaceContainerHighest = Color(0xFF233431),
    outline = Color(0xFF879691),
    outlineVariant = Color(0xFF3B4946),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFDDE8E5),
    inverseOnSurface = Color(0xFF223330),
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
    val surface = if (darkTheme) Color(0xFF0A1514) else Color(0xFFF3FAF8)
    return lerp(surface, accent, factor)
}

fun onAccentContainerFor(container: Color): Color {
    return Color(0xFF0E1F1C)
}
