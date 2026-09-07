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
    val BackgroundLight = Color(0xFFFAF8F3)
    val PrimaryTextLight = Color(0xFF1F2937)
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
    val SurfaceLight = Color(0xFFFAF8F3)
    val SurfaceDark = Color(0xFF101418)
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
    background = GraphyPalette.SurfaceDark,
    primaryText = Color(0xFFF3F4F6),
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
