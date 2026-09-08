package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

private object GraphyPalette {
    val BackgroundLight = Color(0xFFF7F5F0)
    val BackgroundDark = Color(0xFF111417)
    val PrimaryTextLight = Color(0xFF25282C)
    val PrimaryTextDark = Color(0xFFE2E4E6)
    val SecondaryTextLight = Color(0xFF85827C)
    val SecondaryTextDark = Color(0xFF777D83)
    val NumberLight = Color(0xFF25282C)
    val NumberDark = Color(0xFFE2E4E6)
    val OperatorLight = Color(0xFF52677D)
    val OperatorDark = Color(0xFF7F94AA)
    val FunctionLight = Color(0xFF62676D)
    val FunctionDark = Color(0xFF9A9EA3)
    val UtilityLight = Color(0xFF6B7075)
    val UtilityDark = Color(0xFF8A9096)
    val EqualLight = Color(0xFFD94A3A)
    val EqualDark = Color(0xFFE56A5C)
    val Input = Color(0xFFF4C542)
    val Constant = Color(0xFF9B7EDE)
    val Operation = Color(0xFFF59E0B)
    val DerivedLight = Color(0xFF60A5FA)
    val DerivedDark = Color(0xFF60A5FA)
    val ResultFill = Color(0xFF3B82F6)
    val ResultOn = Color(0xFFFFFFFF)
    val ConnectorLight = Color(0xFF9CA3AF)
    val ConnectorDark = Color(0xFF4B5563)
    val SecondaryLight = Color(0xFF6B7280)
    val SecondaryDark = Color(0xFF9CA3AF)
    val SurfaceLight = Color(0xFFF7F5F0)
    val SurfaceDark = Color(0xFF111417)
}

fun graphyLightColorScheme(): ColorScheme = lightColorScheme(
    primary = GraphyPalette.PrimaryTextLight,
    onPrimary = GraphyPalette.ResultOn,
    secondary = GraphyPalette.SecondaryLight,
    onSecondary = GraphyPalette.ResultOn,
    tertiary = GraphyPalette.Constant,
    onTertiary = GraphyPalette.ResultOn,
    error = GraphyPalette.Operation,
    onError = GraphyPalette.ResultOn,
    background = GraphyPalette.BackgroundLight,
    onBackground = GraphyPalette.PrimaryTextLight,
    surface = GraphyPalette.SurfaceLight,
    onSurface = GraphyPalette.PrimaryTextLight,
    surfaceContainerHigh = GraphyPalette.SurfaceLight,
    onSurfaceVariant = GraphyPalette.SecondaryLight,
    outline = GraphyPalette.ConnectorLight,
)

fun graphyDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFF3F4F6),
    onPrimary = GraphyPalette.SurfaceDark,
    secondary = GraphyPalette.SecondaryDark,
    onSecondary = GraphyPalette.SurfaceDark,
    tertiary = GraphyPalette.Constant,
    onTertiary = GraphyPalette.ResultOn,
    error = GraphyPalette.Operation,
    onError = GraphyPalette.ResultOn,
    background = GraphyPalette.SurfaceDark,
    onBackground = Color(0xFFF3F4F6),
    surface = GraphyPalette.SurfaceDark,
    onSurface = Color(0xFFF3F4F6),
    surfaceContainerHigh = GraphyPalette.SurfaceDark,
    onSurfaceVariant = GraphyPalette.SecondaryDark,
    outline = GraphyPalette.ConnectorDark,
)

fun graphyLightSemanticColors(): GraphySemanticColors = GraphySemanticColors(
    background = GraphyPalette.BackgroundLight,
    primaryText = GraphyPalette.PrimaryTextLight,
    secondaryText = GraphyPalette.SecondaryTextLight,
    number = GraphyPalette.NumberLight,
    operator = GraphyPalette.OperatorLight,
    function = GraphyPalette.FunctionLight,
    utility = GraphyPalette.UtilityLight,
    equal = GraphyPalette.EqualLight,
    input = GraphyPalette.Input,
    constant = GraphyPalette.Constant,
    operation = GraphyPalette.Operation,
    derived = GraphyPalette.DerivedLight,
    resultFill = GraphyPalette.ResultFill,
    resultOn = GraphyPalette.ResultOn,
    connector = GraphyPalette.ConnectorLight,
    connectorHighlight = GraphyPalette.SecondaryLight,
    surfaceElevated = GraphyPalette.SurfaceLight,
    onInput = Color(0xFF1F2937),
    onConstant = Color(0xFFFFFFFF),
    onDerived = Color(0xFFFFFFFF),
    onOperation = Color(0xFFFFFFFF),
    onResult = GraphyPalette.ResultOn,
)

fun graphyDarkSemanticColors(): GraphySemanticColors = GraphySemanticColors(
    background = GraphyPalette.BackgroundDark,
    primaryText = GraphyPalette.PrimaryTextDark,
    secondaryText = GraphyPalette.SecondaryTextDark,
    number = GraphyPalette.NumberDark,
    operator = GraphyPalette.OperatorDark,
    function = GraphyPalette.FunctionDark,
    utility = GraphyPalette.UtilityDark,
    equal = GraphyPalette.EqualDark,
    input = GraphyPalette.Input,
    constant = GraphyPalette.Constant,
    operation = GraphyPalette.Operation,
    derived = GraphyPalette.DerivedDark,
    resultFill = GraphyPalette.ResultFill,
    resultOn = GraphyPalette.ResultOn,
    connector = GraphyPalette.ConnectorDark,
    connectorHighlight = GraphyPalette.SecondaryDark,
    surfaceElevated = GraphyPalette.SurfaceDark,
    onInput = Color(0xFF1F2937),
    onConstant = Color(0xFFFFFFFF),
    onDerived = Color(0xFFFFFFFF),
    onOperation = Color(0xFFFFFFFF),
    onResult = GraphyPalette.ResultOn,
)

val LocalGraphySemanticColors = staticCompositionLocalOf { graphyLightSemanticColors() }
