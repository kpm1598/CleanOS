package com.cleanos.storage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CleanOsBackground = Color(0xFF101522)
val CleanOsCard = Color(0xFF1C2534)
val CleanOsCardAlt = Color(0xFF232F45)
val CleanOsPrimary = Color(0xFF567FE7)
val CleanOsDelete = Color(0xFFE66B71)
val CleanOsKeep = Color(0xFF53C99A)
val CleanOsText = Color(0xFFFFFFFF)
val CleanOsTextSecondary = Color(0xFFA9B5C9)

private val CleanOsColors = darkColorScheme(
    primary = CleanOsPrimary,
    onPrimary = Color.White,
    background = CleanOsBackground,
    onBackground = CleanOsText,
    surface = CleanOsCard,
    onSurface = CleanOsText,
    surfaceVariant = CleanOsCardAlt,
    onSurfaceVariant = CleanOsTextSecondary,
    error = CleanOsDelete,
    secondary = CleanOsKeep
)

private val CleanOsTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

@Composable
fun CleanOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CleanOsColors, typography = CleanOsTypography, content = content)
}
