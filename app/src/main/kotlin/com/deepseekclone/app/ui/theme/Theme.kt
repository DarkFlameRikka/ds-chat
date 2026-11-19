package com.deepseekclone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LightScheme = lightColorScheme(
    primary = DeepSeekBlue,
    onPrimary = Color.White,
    background = LightPageBg,
    onBackground = LightTextPrimary,
    surface = LightCardBg,
    onSurface = LightTextPrimary,
    surfaceVariant = LightFill,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    outlineVariant = LightCardBorder,
    error = Color(0xFFE5484D),
)

private val DarkScheme = darkColorScheme(
    primary = DeepSeekBlueNight,
    onPrimary = Color.White,
    background = DarkPageBg,
    onBackground = DarkTextPrimary,
    surface = DarkCardBg,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkFill,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    outlineVariant = DarkCardBorder,
    error = Color(0xFFFF6369),
)

data class ExtendedColors(
    val cardBg: Color,
    val cardBorder: Color,
    val chipBg: Color,
    val chipFg: Color,
    val bubbleUser: Color,
    val bubbleAi: Color,
    val textSecondary: Color,
    val divider: Color,
    val inputBar: Color,
    val iconTint: Color,
    val menuBg: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        cardBg = LightCardBg,
        cardBorder = LightCardBorder,
        chipBg = LightChipBg,
        chipFg = DeepSeekBlue,
        bubbleUser = LightBubbleUser,
        bubbleAi = Color.Transparent,
        textSecondary = LightTextSecondary,
        divider = LightDivider,
        inputBar = LightFill,
        iconTint = Color(0xFF3D3D3D),
        menuBg = Color(0xFFF6F4FD),
    )
}

private val LightExtended = ExtendedColors(
    cardBg = LightCardBg,
    cardBorder = LightCardBorder,
    chipBg = LightChipBg,
    chipFg = DeepSeekBlue,
    bubbleUser = LightBubbleUser,
    bubbleAi = Color.Transparent,
    textSecondary = LightTextSecondary,
    divider = LightDivider,
    inputBar = LightFill,
    iconTint = Color(0xFF3D3D3D),
    menuBg = Color(0xFFF6F4FD),
)

private val DarkExtended = ExtendedColors(
    cardBg = DarkCardBg,
    cardBorder = DarkCardBorder,
    chipBg = DarkChipBg,
    chipFg = DeepSeekBlueNight,
    bubbleUser = DarkBubbleUser,
    bubbleAi = Color.Transparent,
    textSecondary = DarkTextSecondary,
    divider = DarkDivider,
    inputBar = DarkFill,
    iconTint = Color(0xFFD6D6D6),
    menuBg = Color(0xFF232329),
)

@Composable
fun DeepSeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val extended = if (darkTheme) DarkExtended else LightExtended
    val baseDensity = LocalDensity.current
    val density = Density(density = baseDensity.density, fontScale = baseDensity.fontScale * fontScale)
    CompositionLocalProvider(LocalExtendedColors provides extended, LocalDensity provides density) {
        MaterialTheme(colorScheme = scheme, typography = DeepSeekTypography, content = content)
    }
}
