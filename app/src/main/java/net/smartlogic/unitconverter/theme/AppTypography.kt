package net.smartlogic.unitconverter.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.smartlogic.unitconverter.R

/** Compose counterpart of @font/app_text, using the same bundled Oswald weights. */
val AppFontFamily = FontFamily(
    Font(R.font.oswald_regular, FontWeight.Normal),
    Font(R.font.oswald_bold, FontWeight.Bold),
)

private val defaults = Typography()

/** Every Material role uses Oswald, including app bars and dialog headlines. */
val AppTypography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium = defaults.displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall = defaults.displaySmall.copy(fontFamily = AppFontFamily),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = AppFontFamily),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = AppFontFamily),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = AppFontFamily),
    titleLarge = defaults.titleLarge.copy(fontFamily = AppFontFamily),
    titleMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = defaults.titleSmall.copy(fontFamily = AppFontFamily),
    labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = defaults.labelMedium.copy(fontFamily = AppFontFamily),
    labelSmall = defaults.labelSmall.copy(fontFamily = AppFontFamily),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
)
