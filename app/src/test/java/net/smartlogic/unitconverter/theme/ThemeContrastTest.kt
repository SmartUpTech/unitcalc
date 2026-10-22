package net.smartlogic.unitconverter.theme

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.graphyColorScheme
import net.smartlogic.unitconverter.helper.Preferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ThemeContrastTest {
    @Test fun catalogInkAndFilledControlContentHaveNormalTextContrast() {
        for (theme in CalculatorThemes.all()) {
            // Raw palette roles must be usable without a renderer silently substituting black/white.
            for (ink in listOf(theme.mainText(), theme.functions(), theme.equal(), theme.operatorContentColor())) {
                assertTrue(theme.id(), ColorUtils.calculateContrast(ink, theme.background()) >= 4.5)
            }
            assertTrue(theme.id(), ColorUtils.calculateContrast(theme.onAccentColor(), theme.accentContentColor()) >= 4.5)
        }
    }

    @Test fun selectedNavigationTabsComposeAndDrawerHeadingsShareThemeRoles() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        val context = ContextThemeWrapper(app, R.style.AppTheme)
        val bottom = BottomNavigationView(context)
        val drawer = NavigationView(context)
        drawer.menu.addSubMenu("Section").add("Entry")
        val tabs = TabLayout(context)
        for (theme in CalculatorThemes.all()) {
            ThemeManager.select(app, theme.id())
            ThemeApplier.applyNavigation(bottom, drawer)
            ThemeApplier.applyTabLayout(tabs)
            val selected = theme.accentContentColor()
            val checkedState = intArrayOf(android.R.attr.state_checked)
            val selectedState = intArrayOf(android.R.attr.state_selected)
            assertEquals(theme.id(), selected, bottom.itemIconTintList!!.getColorForState(checkedState, 0))
            assertEquals(theme.id(), selected, bottom.itemTextColor!!.getColorForState(checkedState, 0))
            assertEquals(theme.id(), selected, tabs.tabTextColors!!.getColorForState(selectedState, 0))
            assertEquals(theme.id(), selected, drawer.itemTextColor!!.getColorForState(checkedState, 0))
            val title = drawer.menu.getItem(0).title as Spanned
            assertEquals(theme.secondaryContentColor(), title.getSpans(0, title.length, ForegroundColorSpan::class.java).single().foregroundColor)
            val compose = graphyColorScheme(theme)
            assertEquals(theme.id(), selected, compose.primary.toArgb())
            assertEquals(theme.id(), theme.primaryContentColor(), compose.onSurface.toArgb())
            assertEquals(theme.id(), theme.secondaryContentColor(), compose.onSurfaceVariant.toArgb())
            assertEquals(theme.id(), theme.onAccentColor(), compose.onPrimary.toArgb())
            assertEquals(theme.id(), theme.background(), compose.surfaceContainerHighest.toArgb())
        }
    }

    @Test fun appliedSystemBarIconModeChoosesTheHigherContrastInk() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            for (theme in CalculatorThemes.all()) {
                ThemeManager.select(app, theme.id())
                WindowChrome.apply(controller.get())
                val flags = controller.get().window.decorView.systemUiVisibility
                val darkIcons = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0
                val chosen = if (darkIcons) Color.BLACK else Color.WHITE
                val other = if (darkIcons) Color.WHITE else Color.BLACK
                assertTrue(theme.id(), ColorUtils.calculateContrast(chosen, theme.background()) >=
                    ColorUtils.calculateContrast(other, theme.background()))
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
