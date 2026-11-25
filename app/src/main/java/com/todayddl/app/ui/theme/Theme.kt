package com.todayddl.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class UrgencyPalette(
    val overdueContainer: Color,
    val overdueContent: Color,
    val overdueBorder: Color,
    val urgentContainer: Color,
    val urgentContent: Color,
    val urgentBorder: Color,
    val soonContainer: Color,
    val soonContent: Color,
    val neutralContainer: Color,
    val neutralContent: Color
)

private val LightUrgencyPalette = UrgencyPalette(
    overdueContainer = OverdueContainerLight,
    overdueContent = OverdueContentLight,
    overdueBorder = OverdueBorderLight,
    urgentContainer = UrgentContainerLight,
    urgentContent = UrgentContentLight,
    urgentBorder = UrgentBorderLight,
    soonContainer = SoonContainerLight,
    soonContent = SoonContentLight,
    neutralContainer = SurfaceVariantLight,
    neutralContent = OnSurfaceVariantLight
)

private val DarkUrgencyPalette = UrgencyPalette(
    overdueContainer = OverdueContainerDark,
    overdueContent = OverdueContentDark,
    overdueBorder = OverdueBorderDark,
    urgentContainer = UrgentContainerDark,
    urgentContent = UrgentContentDark,
    urgentBorder = UrgentBorderDark,
    soonContainer = SoonContainerDark,
    soonContent = SoonContentDark,
    neutralContainer = SurfaceVariantDark,
    neutralContent = OnSurfaceVariantDark
)

val LocalUrgencyPalette = staticCompositionLocalOf { LightUrgencyPalette }

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val TodayDdlTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineSmall = TextStyle(
        fontSize = 25.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = TextStyle(
        fontSize = 23.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
)

@Composable
fun TodayDdlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val urgencyPalette = if (darkTheme) DarkUrgencyPalette else LightUrgencyPalette

    CompositionLocalProvider(LocalUrgencyPalette provides urgencyPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TodayDdlTypography,
            content = content
        )
    }
}
