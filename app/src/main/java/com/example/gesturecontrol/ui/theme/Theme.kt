package com.example.gesturecontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Background = Color(0xFF0A0E14)
val Surface = Color(0xFF11161F)
val ElevatedSurface = Color(0xFF161C27)
val HairlineBorder = Color(0xFF232B38)
val TextPrimary = Color(0xFFE6EAF0)
val TextSecondary = Color(0xFF8A94A6)
val PrimaryTeal = Color(0xFF2DD4A6)
val SecondaryAmber = Color(0xFFE8A94C)
val InfoViolet = Color(0xFF8B7CF6)
val ErrorRed = Color(0xFFF26D6D)

val GestureTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        color = TextSecondary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = TextSecondary
    )
)

private val DarkColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    surfaceVariant = ElevatedSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    primary = PrimaryTeal,
    secondary = SecondaryAmber,
    tertiary = InfoViolet,
    error = ErrorRed,
    outline = HairlineBorder
)

@Composable
fun GestureControlTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = GestureTypography,
        content = content
    )
}