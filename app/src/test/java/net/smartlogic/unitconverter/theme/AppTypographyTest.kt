package net.smartlogic.unitconverter.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTypographyTest {
    @Test fun everyMaterialTextRoleUsesTheAppFamilyIncludingAppBarsAndDialogs() {
        Appearance.Font.entries.forEach { font ->
        with(appTypography(font)) {
            listOf(displayLarge, displayMedium, displaySmall,
                headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall,
                bodyLarge, bodyMedium, bodySmall,
                labelLarge, labelMedium, labelSmall).forEach { role ->
                assertEquals(appFontFamily(font), role.fontFamily)
                assertEquals(androidx.compose.ui.text.font.FontWeight.Normal, role.fontWeight)
            }
        }
        }
    }
}
