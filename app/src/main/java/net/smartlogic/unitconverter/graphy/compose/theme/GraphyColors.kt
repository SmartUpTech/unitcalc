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
    val PrimaryLight = Color(0xFFC23A2B)
    val InputLight = Color(0xFFE8DFD2)
    val OperationLight = Color(0xFFC23A2B)
    val ResultLight = Color(0xFF1A1612)
    val WarningLight = Color(0xFFC23A2B)
    val TimeLight = Color(0xFF8A8174)
    val SecondaryLight = Color(0xFF6B6358)
    val ConnectorLight = Color(0xFFC4B8A8)
    val ConnectorHighlightLight = Color(0xFFC23A2B)
    val SurfaceLight = Color(0xFFF3EDE3)
    val SurfaceElevatedLight = Color(0xFFEDE6DA)
    val OnPrimaryLight = Color(0xFFF3EDE3)
    val OnInputLight = Color(0xFF1A1612)
    val OnOperationLight = Color(0xFFF3EDE3)
    val OnResultLight = Color(0xFFF3EDE3)

    val PrimaryDark = Color(0xFFE08A3C)
    val InputDark = Color(0xFF2A2C30)
    val OperationDark = Color(0xFFE08A3C)
    val ResultDark = Color(0xFFE8E4DE)
    val WarningDark = Color(0xFFE06A4A)
    val TimeDark = Color(0xFF9A9590)
    val SecondaryDark = Color(0xFF9A9590)
    val ConnectorDark = Color(0xFF3A3D42)
    val ConnectorHighlightDark = Color(0xFFE08A3C)
    val SurfaceDark = Color(0xFF1A1C1F)
    val SurfaceElevatedDark = Color(0xFF22252A)
    val OnPrimaryDark = Color(0xFF1A1C1F)
    val OnInputDark = Color(0xFFE8E4DE)
    val OnOperationDark = Color(0xFF1A1C1F)
    val OnResultDark = Color(0xFF1A1C1F)
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
