package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.smartlogic.unitconverter.theme.CalculatorTheme
import net.smartlogic.unitconverter.theme.ThemeManager

object GraphyThemeDefaults {
    fun currentTheme(): CalculatorTheme = ThemeManager.get()

    fun isDarkTheme(): Boolean = !ThemeManager.get().isLightBackground()
}

@Composable
fun GraphyTheme(
    content: @Composable () -> Unit,
) {
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val listener = ThemeManager.Listener { revision++ }
        ThemeManager.addListener(listener)
        onDispose { ThemeManager.removeListener(listener) }
    }
    revision

    val theme = ThemeManager.get()
    val colorScheme = graphyColorScheme(theme)
    val semanticColors = graphySemanticColors(theme)

    CompositionLocalProvider(
        LocalGraphySemanticColors provides semanticColors,
        LocalGraphyTextStyles provides GraphyTextStylesDefault,
        LocalGraphyShapeTokens provides GraphyShapeTokensDefault,
        LocalGraphySpacing provides GraphySpacingDefault,
        LocalGraphyElevation provides GraphyElevationDefault,
        LocalGraphyComponentTokens provides GraphyComponentTokensDefault,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GraphyTypography,
            shapes = GraphyShapes,
            content = content,
        )
    }
}

@Composable
fun isGraphyDarkTheme(): Boolean = GraphyThemeDefaults.isDarkTheme()

object GraphyThemeTokens {
    val semanticColors: GraphySemanticColors
        @Composable get() = LocalGraphySemanticColors.current

    val textStyles: GraphyTextStyles
        @Composable get() = LocalGraphyTextStyles.current

    val shapes: GraphyShapeTokens
        @Composable get() = LocalGraphyShapeTokens.current

    val spacing: GraphySpacing
        @Composable get() = LocalGraphySpacing.current

    val elevation: GraphyElevation
        @Composable get() = LocalGraphyElevation.current

    val components: GraphyComponentTokens
        @Composable get() = LocalGraphyComponentTokens.current
}
