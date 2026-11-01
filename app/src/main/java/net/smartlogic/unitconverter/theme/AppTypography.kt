package net.smartlogic.unitconverter.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.smartlogic.unitconverter.R

/** Compose and XML share the bundled font registry and Android language fallback. */
fun appFontFamily(font: Appearance.Font) = FontFamily(Font(font.resource, FontWeight.Normal))
val AppFontFamily = appFontFamily(Appearance.Font.OSWALD)

private val defaults = Typography()

/** Oswald remains the default; the selected family covers every Material role. */
val AppTypography = appTypography(Appearance.Font.OSWALD)
fun appTypography(font: Appearance.Font): Typography {
    val family = appFontFamily(font)
    return Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    displayMedium = defaults.displayMedium.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    displaySmall = defaults.displaySmall.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    titleLarge = defaults.titleLarge.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    titleSmall = defaults.titleSmall.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = defaults.labelMedium.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    labelSmall = defaults.labelSmall.copy(fontFamily = family, fontWeight = FontWeight.Normal),
    bodyLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
)

}
