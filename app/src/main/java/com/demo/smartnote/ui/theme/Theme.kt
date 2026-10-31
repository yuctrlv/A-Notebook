package com.demo.smartnote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive 风格配色：
 * 高饱和主色 + 大块浅色容器 + 大圆角，不再用纸张质感。
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E0FF),
    onPrimaryContainer = Color(0xFF17105C),
    secondary = Color(0xFF0F9C8F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC9F3EC),
    onSecondaryContainer = Color(0xFF04352F),
    tertiary = Color(0xFFF2831B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE2C4),
    onTertiaryContainer = Color(0xFF4A2400),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF14131A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14131A),
    surfaceVariant = Color(0xFFE9E8F0),
    onSurfaceVariant = Color(0xFF5B5966),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F4FA),
    surfaceContainer = Color(0xFFEFEEF6),
    surfaceContainerHigh = Color(0xFFE8E7F1),
    surfaceContainerHighest = Color(0xFFE1E0EB),
    outline = Color(0xFF77758A),
    outlineVariant = Color(0xFFD6D4E0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBFC1FF),
    onPrimary = Color(0xFF221B78),
    primaryContainer = Color(0xFF3A32A0),
    onPrimaryContainer = Color(0xFFE2E0FF),
    secondary = Color(0xFF8FE3D6),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF0B5D55),
    onSecondaryContainer = Color(0xFFC9F3EC),
    tertiary = Color(0xFFFFB86B),
    onTertiary = Color(0xFF4A2400),
    tertiaryContainer = Color(0xFF6A3A05),
    onTertiaryContainer = Color(0xFFFFE2C4),
    background = Color(0xFF121118),
    onBackground = Color(0xFFE6E4EE),
    surface = Color(0xFF1A1922),
    onSurface = Color(0xFFE6E4EE),
    surfaceVariant = Color(0xFF2A2933),
    onSurfaceVariant = Color(0xFFB4B2C2),
    surfaceContainerLowest = Color(0xFF0D0C12),
    surfaceContainerLow = Color(0xFF191821),
    surfaceContainer = Color(0xFF1E1D27),
    surfaceContainerHigh = Color(0xFF26252F),
    surfaceContainerHighest = Color(0xFF302F3A),
    outline = Color(0xFF918FA3),
    outlineVariant = Color(0xFF3D3B49),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/** 字重对比拉开：标题很粗，辅助信息很轻 */
private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontWeight = FontWeight.Normal, lineHeight = 24.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    )
}

@Composable
fun SmartNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
