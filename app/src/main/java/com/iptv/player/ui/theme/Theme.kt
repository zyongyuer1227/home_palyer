package com.iptv.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A63FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = Color(0xFF06306F),
    secondary = Color(0xFF22A85A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7F8EE),
    onSecondaryContainer = Color(0xFF0B4F28),
    tertiary = Color(0xFF6C6F7A),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF252528),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF252528),
    surfaceVariant = Color(0xFFE9EAEE),
    onSurfaceVariant = Color(0xFF70717A),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFDFDFE),
    surfaceContainerHighest = Color(0xFFF8F8FA),
    outline = Color(0xFFD8DAE0),
    outlineVariant = Color(0xFFE7E8ED),
    error = Color(0xFFFF4D2E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFECE8),
    onErrorContainer = Color(0xFF7D1200),
    scrim = Color(0xFF000000),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun IptvTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val appDensity = Density(density = density.density, fontScale = density.fontScale.coerceAtMost(1.15f))

    CompositionLocalProvider(LocalDensity provides appDensity) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
