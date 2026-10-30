package net.smartlogic.unitconverter.graphy.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.dimensionResource
import net.smartlogic.unitconverter.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.smartlogic.unitconverter.theme.ThemeManager
import net.smartlogic.unitconverter.theme.appTypography

val LocalCalculatorTheme = androidx.compose.runtime.staticCompositionLocalOf { ThemeManager.get() }

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
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(context) {
        val prefs = net.smartlogic.unitconverter.helper.Preferences.getInstance(context).preferences
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (net.smartlogic.unitconverter.theme.Appearance.isPreference(key)) revision++
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    revision

    val theme = ThemeManager.get()
    val colorScheme = graphyColorScheme(theme)
    val semanticColors = graphySemanticColors(theme)

    val spacing = GraphySpacing(
        dimensionResource(R.dimen.graphy_spacing_xs), dimensionResource(R.dimen.graphy_spacing_sm),
        dimensionResource(R.dimen.graphy_spacing_md), dimensionResource(R.dimen.graphy_spacing_lg))
    val shape = RoundedCornerShape(dimensionResource(R.dimen.graphy_node_corner_radius))
    CompositionLocalProvider(
        LocalCalculatorTheme provides theme,
        LocalGraphySemanticColors provides semanticColors,
        LocalGraphySpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(net.smartlogic.unitconverter.theme.Appearance.font(context)),
            shapes = Shapes(small = shape, medium = shape, large = shape),
            content = content,
        )
    }
}

object GraphyThemeTokens {
    val semanticColors: GraphySemanticColors
        @Composable get() = LocalGraphySemanticColors.current
    val spacing: GraphySpacing
        @Composable get() = LocalGraphySpacing.current
}
