package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import net.smartlogic.unitconverter.theme.CalculatorTheme
import net.smartlogic.unitconverter.theme.ThemeManager
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType
import net.smartlogic.unitconverter.graphy.theme.GraphySemanticStyle

@Immutable
data class GraphySemanticColors(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val number: Color,
    val operator: Color,
    val function: Color,
    val utility: Color,
    val equal: Color,
    val input: Color,
    val constant: Color,
    val operation: Color,
    val derived: Color,
    val resultFill: Color,
    val resultOn: Color,
    val connector: Color,
    val connectorHighlight: Color,
    val surfaceElevated: Color,
    val onInput: Color,
    val onConstant: Color,
    val onDerived: Color,
    val onOperation: Color,
    val onResult: Color,
)

fun graphySemanticColors(theme: CalculatorTheme): GraphySemanticColors {
    fun fill(type: GraphyNodeType) = Color(GraphySemanticStyle.fill(theme, type))
    fun content(type: GraphyNodeType) = Color(GraphySemanticStyle.content(theme, type))
    val connector = Color(GraphySemanticStyle.connector(theme))
    return GraphySemanticColors(
        background = Color(theme.background), primaryText = content(GraphyNodeType.EXPLANATION),
        secondaryText = connector, number = content(GraphyNodeType.INPUT),
        operator = content(GraphyNodeType.OPERATION), function = connector, utility = connector,
        equal = fill(GraphyNodeType.RESULT), input = fill(GraphyNodeType.INPUT),
        constant = fill(GraphyNodeType.CONSTANT), operation = fill(GraphyNodeType.OPERATION),
        derived = fill(GraphyNodeType.DERIVED), resultFill = fill(GraphyNodeType.RESULT),
        resultOn = content(GraphyNodeType.RESULT), connector = connector,
        connectorHighlight = connector, surfaceElevated = fill(GraphyNodeType.CONSTANT),
        onInput = content(GraphyNodeType.INPUT), onConstant = content(GraphyNodeType.CONSTANT),
        onDerived = content(GraphyNodeType.DERIVED), onOperation = content(GraphyNodeType.OPERATION),
        onResult = content(GraphyNodeType.RESULT),
    )
}

fun graphyColorScheme(theme: CalculatorTheme): ColorScheme {
    val background = Color(theme.background)
    val main = Color(theme.primaryContentColor())
    val functions = Color(theme.secondaryContentColor())
    val accent = Color(theme.accentContentColor())
    val onAccent = Color(theme.onAccentColor())
    val onSecondary = Color(CalculatorTheme.readableColor(theme.background, theme.secondaryContentColor()))
    val base = if (theme.isLightBackground) lightColorScheme() else darkColorScheme()
    // Every used Material surface/state follows the app palette, including switches and radios.
    return base.copy(
        primary = accent, onPrimary = onAccent,
        primaryContainer = accent, onPrimaryContainer = onAccent,
        secondary = functions, onSecondary = onSecondary,
        secondaryContainer = background, onSecondaryContainer = main,
        tertiary = accent, onTertiary = onAccent,
        tertiaryContainer = accent, onTertiaryContainer = onAccent,
        background = background, onBackground = main,
        surface = background, onSurface = main, onSurfaceVariant = functions,
        surfaceVariant = background, surfaceTint = Color.Transparent,
        surfaceDim = background, surfaceBright = background,
        surfaceContainerLowest = background, surfaceContainerLow = background,
        surfaceContainer = background, surfaceContainerHigh = background,
        surfaceContainerHighest = background,
        inverseSurface = main, inverseOnSurface = background, inversePrimary = background,
        outline = functions, outlineVariant = functions,
    )
}

fun graphySemanticColorsFromManager(): GraphySemanticColors =
    graphySemanticColors(ThemeManager.get())

fun graphyColorSchemeFromManager(): ColorScheme =
    graphyColorScheme(ThemeManager.get())

val LocalGraphySemanticColors = staticCompositionLocalOf { graphySemanticColorsFromManager() }
