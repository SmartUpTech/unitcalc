package net.smartlogic.unitconverter.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTypographyTest {
    @Test fun everyMaterialTextRoleUsesTheAppFamilyIncludingAppBarsAndDialogs() {
        with(AppTypography) {
            listOf(displayLarge, displayMedium, displaySmall,
                headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall,
                bodyLarge, bodyMedium, bodySmall,
                labelLarge, labelMedium, labelSmall).forEach { role ->
                assertEquals(AppFontFamily, role.fontFamily)
            }
        }
    }
}
