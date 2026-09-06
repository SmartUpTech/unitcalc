package net.smartlogic.unitconverter.graphy.compose.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

object GraphyThemeDefaults {
    fun isDarkTheme(context: Context): Boolean {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode == Configuration.UI_MODE_NIGHT_YES
    }
}

@Composable
fun GraphyTheme(
    darkTheme: Boolean = isGraphyDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) graphyDarkColorScheme() else graphyLightColorScheme()
    val semanticColors = if (darkTheme) graphyDarkSemanticColors() else graphyLightSemanticColors()

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
fun isGraphyDarkTheme(): Boolean {
    val context = LocalContext.current
    return when {
        GraphyThemeDefaults.isDarkTheme(context) -> true
        else -> isSystemInDarkTheme()
    }
}

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
