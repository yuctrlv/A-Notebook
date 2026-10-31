package com.demo.smartnote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 纸质手账风配色：米白纸 + 墨黑字 + 一点点封面色。
 * 不跟随系统取色，保证任何手机上看都是同一个样子。
 */
private val Paper = Color(0xFFF7F3EA)
private val PaperDeep = Color(0xFFEFE8D9)
private val Card = Color(0xFFFFFDF7)
private val Ink = Color(0xFF23201C)
private val InkSoft = Color(0xFF6F6659)
private val Rule = Color(0xFFDCD2BE)
private val Seal = Color(0xFF9C4221)     // 印章红
private val Night = Color(0xFF1B1916)
private val NightCard = Color(0xFF262320)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = PaperDeep,
    onPrimaryContainer = Ink,
    secondary = Seal,
    onSecondary = Paper,
    secondaryContainer = Color(0xFFF0E0D6),
    onSecondaryContainer = Color(0xFF5C2A18),
    tertiary = Color(0xFF3F5A44),
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkSoft,
    surfaceContainerLow = Card,
    surfaceContainer = PaperDeep,
    surfaceContainerHigh = PaperDeep,
    surfaceContainerHighest = Color(0xFFE6DDCB),
    outline = Color(0xFF8C8171),
    outlineVariant = Rule,
    error = Color(0xFF8C1D18),
    onError = Paper
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8E0CE),
    onPrimary = Night,
    secondary = Color(0xFFE0A183),
    background = Night,
    onBackground = Color(0xFFE9E4D9),
    surface = NightCard,
    onSurface = Color(0xFFE9E4D9),
    surfaceVariant = NightCard,
    onSurfaceVariant = Color(0xFFB3AA9B),
    surfaceContainerLow = NightCard,
    surfaceContainer = Color(0xFF221F1C),
    surfaceContainerHigh = Color(0xFF2B2724),
    outline = Color(0xFF8F857A),
    outlineVariant = Color(0xFF3A352F),
    error = Color(0xFFE8B4AF),
    onError = Night
)

/** 标题用衬线体，正文用系统无衬线，做出「印刷笔记本」的味道 */
private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp
        )
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
