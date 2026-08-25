package dev.xichen.wodtimer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF846200),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE9A3),
    onPrimaryContainer = Color(0xFF2A2000),
    secondary = Color(0xFF286A84),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0EEFF),
    onSecondaryContainer = Color(0xFF082F40),
    background = Color(0xFFF7F7FA),
    surface = Color.White,
    surfaceContainer = Color(0xFFF0F1F5),
    surfaceVariant = Color(0xFFE5E7ED),
    outline = Color(0xFF747A85),
    outlineVariant = Color(0xFFD3D6DE),
    onBackground = Color(0xFF17191E),
    onSurface = Color(0xFF17191E),
    onSurfaceVariant = Color(0xFF5C626E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFCE45),
    onPrimary = Color(0xFF231B00),
    secondary = Color(0xFF72D2FF),
    background = Color(0xFF0C0E13),
    surface = Color(0xFF11141B),
    surfaceContainer = Color(0xFF191D26),
    surfaceVariant = Color(0xFF252A35),
    outline = Color(0xFF777E8E),
    outlineVariant = Color(0xFF363C49),
    onBackground = Color(0xFFF5F6FA),
    onSurface = Color(0xFFF5F6FA),
    onSurfaceVariant = Color(0xFFADB3C1),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 40.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable fun WodTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
