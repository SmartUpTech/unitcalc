package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GraphySemanticColors(
    val input: Color,
    val operation: Color,
    val result: Color,
    val warning: Color,
    val time: Color,
    val connector: Color,
    val connectorHighlight: Color,
    val surfaceElevated: Color,
    val onInput: Color,
    val onOperation: Color,
    val onResult: Color,
)

private object GraphyPalette {
    val PrimaryLight = Color(0xFF4F46E5)
    val InputLight = Color(0xFFEEF2FF)
    val OperationLight = Color(0xFFF59E0B)
    val ResultLight = Color(0xFF10B981)
    val WarningLight = Color(0xFFEF4444)
    val TimeLight = Color(0xFF6366F1)
    val SecondaryLight = Color(0xFF64748B)
    val ConnectorLight = Color(0xFFCBD5E1)
    val ConnectorHighlightLight = Color(0xFF4F46E5)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceElevatedLight = Color(0xFFF8FAFC)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val OnInputLight = Color(0xFF1E293B)
    val OnOperationLight = Color(0xFFFFFFFF)
    val OnResultLight = Color(0xFFFFFFFF)

    val PrimaryDark = Color(0xFF818CF8)
    val InputDark = Color(0xFF1E293B)
    val OperationDark = Color(0xFFFBBF24)
    val ResultDark = Color(0xFF34D399)
    val WarningDark = Color(0xFFF87171)
    val TimeDark = Color(0xFFA5B4FC)
    val SecondaryDark = Color(0xFF94A3B8)
    val ConnectorDark = Color(0xFF475569)
    val ConnectorHighlightDark = Color(0xFF818CF8)
    val SurfaceDark = Color(0xFF0F172A)
    val SurfaceElevatedDark = Color(0xFF1E293B)
    val OnPrimaryDark = Color(0xFF0F172A)
    val OnInputDark = Color(0xFFE2E8F0)
    val OnOperationDark = Color(0xFF0F172A)
    val OnResultDark = Color(0xFF0F172A)
}

fun graphyLightColorScheme(): ColorScheme = lightColorScheme(
    primary = GraphyPalette.PrimaryLight,
    onPrimary = GraphyPalette.OnPrimaryLight,
    secondary = GraphyPalette.SecondaryLight,
    onSecondary = GraphyPalette.OnPrimaryLight,
    tertiary = GraphyPalette.TimeLight,
    onTertiary = GraphyPalette.OnPrimaryLight,
    error = GraphyPalette.WarningLight,
    onError = GraphyPalette.OnPrimaryLight,
    background = GraphyPalette.SurfaceLight,
    onBackground = GraphyPalette.OnInputLight,
    surface = GraphyPalette.SurfaceLight,
    onSurface = GraphyPalette.OnInputLight,
    surfaceContainerHigh = GraphyPalette.SurfaceElevatedLight,
    onSurfaceVariant = GraphyPalette.SecondaryLight,
    outline = GraphyPalette.ConnectorLight,
)

fun graphyDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = GraphyPalette.PrimaryDark,
    onPrimary = GraphyPalette.OnPrimaryDark,
    secondary = GraphyPalette.SecondaryDark,
    onSecondary = GraphyPalette.OnPrimaryDark,
    tertiary = GraphyPalette.TimeDark,
    onTertiary = GraphyPalette.OnPrimaryDark,
    error = GraphyPalette.WarningDark,
    onError = GraphyPalette.OnPrimaryDark,
    background = GraphyPalette.SurfaceDark,
    onBackground = GraphyPalette.OnInputDark,
    surface = GraphyPalette.SurfaceDark,
    onSurface = GraphyPalette.OnInputDark,
    surfaceContainerHigh = GraphyPalette.SurfaceElevatedDark,
    onSurfaceVariant = GraphyPalette.SecondaryDark,
    outline = GraphyPalette.ConnectorDark,
)

fun graphyLightSemanticColors(): GraphySemanticColors = GraphySemanticColors(
    input = GraphyPalette.InputLight,
    operation = GraphyPalette.OperationLight,
    result = GraphyPalette.ResultLight,
    warning = GraphyPalette.WarningLight,
    time = GraphyPalette.TimeLight,
    connector = GraphyPalette.ConnectorLight,
    connectorHighlight = GraphyPalette.ConnectorHighlightLight,
    surfaceElevated = GraphyPalette.SurfaceElevatedLight,
    onInput = GraphyPalette.OnInputLight,
    onOperation = GraphyPalette.OnOperationLight,
    onResult = GraphyPalette.OnResultLight,
)

fun graphyDarkSemanticColors(): GraphySemanticColors = GraphySemanticColors(
    input = GraphyPalette.InputDark,
    operation = GraphyPalette.OperationDark,
    result = GraphyPalette.ResultDark,
    warning = GraphyPalette.WarningDark,
    time = GraphyPalette.TimeDark,
    connector = GraphyPalette.ConnectorDark,
    connectorHighlight = GraphyPalette.ConnectorHighlightDark,
    surfaceElevated = GraphyPalette.SurfaceElevatedDark,
    onInput = GraphyPalette.OnInputDark,
    onOperation = GraphyPalette.OnOperationDark,
    onResult = GraphyPalette.OnResultDark,
)

val LocalGraphySemanticColors = staticCompositionLocalOf { graphyLightSemanticColors() }
