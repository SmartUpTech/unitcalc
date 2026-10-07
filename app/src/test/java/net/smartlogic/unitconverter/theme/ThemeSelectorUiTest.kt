package net.smartlogic.unitconverter.theme

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Looper
import android.view.View
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.activity.SettingsActivity
import net.smartlogic.unitconverter.activity.MainActivity
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.helper.Preferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.Duration

/** Native Compose measurement and semantics, without an additional UI-test dependency. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ThemeSelectorUiTest {
    private lateinit var preferences: Preferences

    @Before fun setup() {
        preferences = Preferences.getInstance(RuntimeEnvironment.getApplication())
        preferences.preferences.edit().clear().commit()
        ThemeManager.init(RuntimeEnvironment.getApplication())
    }

    @Test fun itemsHaveEqualBoundsAcrossStatesScreenSizesAndFontScales() {
        for (width in listOf(320, 800)) {
            for (fontScale in listOf(1f, 2f)) {
                render(width, fontScale) { view, _ ->
                    val cards = cards(view)
                    assertTrue(cards.size >= 2)
                    val size = cards.first().size
                    cards.forEach {
                        assertEquals(size, it.size)
                        assertTrue(it.size.width >= 48 && it.size.height >= 48)
                        assertTrue(it.config[SemanticsProperties.ContentDescription].single().isNotBlank())
                        assertTrue(it.config[SemanticsProperties.StateDescription].isNotBlank())
                    }
                    assertEquals(1, cards.count { it.config[SemanticsProperties.Selected] })
                }
            }
        }
    }

    @Test fun nativeClicksSelectAvailableThemesAndRejectLockedThemes() {
        render(800, 1f) { view, model ->
            fun tap(nameRes: Int) {
                val name = view.context.getString(nameRes)
                val node = cards(view).single { it.config[SemanticsProperties.ContentDescription].single() == name }
                assertTrue(node.config[SemanticsActions.OnClick].action!!.invoke())
                settle()
            }
            tap(CalculatorThemes.OYSTER_CREAM.nameRes())
            assertEquals(CalculatorThemes.OYSTER_CREAM.id(), preferences.selectedThemeId)
            tap(CalculatorThemes.GRAPHITE_BLACK.nameRes())
            assertEquals(CalculatorThemes.OYSTER_CREAM.id(), preferences.selectedThemeId)
            assertEquals(CalculatorThemes.GRAPHITE_BLACK.nameRes(), model.uiState.value.lockedThemeNameRes)
            assertEquals(3, model.uiState.value.daysUntilTappedUnlock)
        }
    }

    @Test fun horizontalOverflowAndLastSelectedThemeAreAccessible() {
        render(320, 1f) { view, _ ->
            val scroll = nodes(view).single { it.config.contains(SemanticsProperties.HorizontalScrollAxisRange) }
            val range = scroll.config[SemanticsProperties.HorizontalScrollAxisRange]
            assertTrue(range.maxValue() > range.value())
            assertTrue(scroll.config.contains(SemanticsActions.ScrollToIndex))
            assertEquals(CalculatorThemes.DEFAULT_ID, preferences.selectedThemeId)
        }
        preferences.preferences.edit().clear()
            .putString(Preferences.PREFS_SELECTED_THEME, CalculatorThemes.GOLDEN_POPPY.id()).commit()
        ThemeManager.init(RuntimeEnvironment.getApplication())
        render(320, 1f) { view, _ ->
            val selected = cards(view).single { it.config[SemanticsProperties.Selected] }
            assertEquals(listOf(view.context.getString(CalculatorThemes.GOLDEN_POPPY.nameRes())),
                selected.config[SemanticsProperties.ContentDescription])
        }
    }

    @Test fun renderLightAndDarkSettingsSnapshots() {
        for (theme in listOf(CalculatorThemes.TITANIUM_GRAY, CalculatorThemes.GRAPHITE_BLACK)) {
            // Exercise the existing legacy-selected-theme path to preview an available dark theme.
            preferences.preferences.edit().clear()
                .putString(Preferences.PREFS_SELECTED_THEME, theme.id()).commit()
            ThemeManager.init(RuntimeEnvironment.getApplication())
            preferences.recordThemeUsage(java.time.LocalDate.of(2026, 10, 7))
            RuntimeEnvironment.setQualifiers("w360dp-h800dp-mdpi")
            val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup().visible()
            try {
                settle()
                val view = controller.get().window.decorView
                view.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                settle()
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val file = File("../.artifacts/settings-${theme.id()}.png")
                file.parentFile?.mkdirs()
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                assertTrue(file.length() > 0)
                assertSettingsAppBar(controller.get())
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }

    @Test fun settingsAppBarUpdatesTitleAndIconsWhenSelectingATheme() {
        // Restore an earned dark theme, then select available themes through the real Settings row.
        preferences.preferences.edit().clear()
            .putString(Preferences.PREFS_SELECTED_THEME, CalculatorThemes.GRAPHITE_BLACK.id()).commit()
        ThemeManager.init(RuntimeEnvironment.getApplication())
        ThemeManager.select(RuntimeEnvironment.getApplication(), CalculatorThemes.TITANIUM_GRAY.id())
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-mdpi")
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            val toolbar = activity.findViewById<Toolbar>(androidx.appcompat.R.id.action_bar)
            // Settings currently has no overflow actions. Exercise AppCompat's standard overflow
            // drawable without introducing an unrelated production menu item.
            toolbar.menu.add("Test action").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            WindowChrome.apply(activity)
            settle()
            val decor = activity.window.decorView
            decor.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
            decor.layout(0, 0, decor.measuredWidth, decor.measuredHeight)
            settle()
            assertSettingsAppBar(activity, checkOverflow = true)
            val selector = activity.findViewById<ComposeView>(R.id.theme_selector)
            for (theme in listOf(CalculatorThemes.GRAPHITE_BLACK, CalculatorThemes.TITANIUM_GRAY)) {
                val node = cards(selector).single {
                    it.config[SemanticsProperties.ContentDescription].single() == activity.getString(theme.nameRes())
                }
                assertTrue(node.config[SemanticsActions.OnClick].action!!.invoke())
                settle()
                assertEquals(theme.id(), ThemeManager.get().id())
                assertSettingsAppBar(activity, checkOverflow = true)
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private fun assertSettingsAppBar(activity: SettingsActivity, checkOverflow: Boolean = false) {
        val bar = activity.supportActionBar!!
        assertTrue(bar.displayOptions and ActionBar.DISPLAY_SHOW_TITLE != 0)
        assertTrue(bar.displayOptions and ActionBar.DISPLAY_HOME_AS_UP != 0)
        val toolbar = activity.findViewById<Toolbar>(androidx.appcompat.R.id.action_bar)
        val title = (0 until toolbar.childCount).map { toolbar.getChildAt(it) }
            .filterIsInstance<TextView>().single { it.text == activity.getString(R.string.title_activity_settings) }
        assertEquals(View.VISIBLE, title.visibility)
        assertTrue(title.width > 0 && title.height > 0)
        assertEquals(ThemeManager.get().mainText(), title.currentTextColor)
        assertIconColor("back", toolbar.navigationIcon!!, ThemeManager.get().mainText())
        if (checkOverflow) {
            assertIconColor("overflow", toolbar.overflowIcon!!, ThemeManager.get().mainText())
        }
    }

    @Test fun mainOverflowKeepsThemeColorAfterMenuInvalidation() {
        for (theme in listOf(CalculatorThemes.TITANIUM_GRAY, CalculatorThemes.GRAPHITE_BLACK)) {
            preferences.preferences.edit().clear()
                .putString(Preferences.PREFS_SELECTED_THEME, theme.id()).commit()
            ThemeManager.init(RuntimeEnvironment.getApplication())
            RuntimeEnvironment.setQualifiers("w360dp-h800dp-mdpi")
            val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
            try {
                val activity = controller.get()
                settle()
                val toolbar = activity.findViewById<Toolbar>(androidx.appcompat.R.id.action_bar)
                assertFalse(activity.supportActionBar!!.displayOptions and ActionBar.DISPLAY_SHOW_TITLE != 0)
                assertIconColor("main overflow", toolbar.overflowIcon!!, theme.mainText())
                activity.invalidateOptionsMenu()
                settle()
                assertIconColor("main overflow after invalidation", toolbar.overflowIcon!!, theme.mainText())
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }

    private fun assertIconColor(name: String, drawable: Drawable, color: Int) {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val bounds = android.graphics.Rect(drawable.bounds)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        drawable.bounds = bounds
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val visible = pixels.filter { Color.alpha(it) == 255 }
        assertTrue(visible.isNotEmpty())
        assertTrue("$name expected ${Integer.toHexString(color)}, got ${visible.toSet().map(Integer::toHexString)}",
            visible.all { (it and 0x00ffffff) == (color and 0x00ffffff) })
        bitmap.recycle()
    }

    private fun render(width: Int, fontScale: Float, inspect: (ComposeView, ThemeSelectorViewModel) -> Unit) {
        RuntimeEnvironment.setQualifiers("w${width}dp-h640dp-mdpi")
        val resources = RuntimeEnvironment.getApplication().resources
        val configuration = Configuration(resources.configuration).apply { this.fontScale = fontScale }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().visible()
        val activity = controller.get()
        val model = ThemeSelectorViewModel(preferences)
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            val state by model.uiState.collectAsState()
            GraphyTheme {
                Surface {
                    Column {
                        Text(stringResource(R.string.prefs_title_theme))
                        ThemeSelector(state, { id ->
                            if (model.requestTheme(id)) ThemeManager.select(activity, id)
                        }, model::dismissLockedTheme)
                    }
                }
            }
        }
        try {
            settle()
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.AT_MOST))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            settle()
            inspect(view, model)
        } finally {
            view.disposeComposition()
            androidx.lifecycle.ViewModelStore().apply { put("theme", model); clear() }
            controller.pause().stop().destroy()
        }
    }

    private fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))

    private fun cards(view: ComposeView): List<SemanticsNode> = nodes(view).filter {
        it.config.contains(SemanticsProperties.Role) && it.config[SemanticsProperties.Role] == Role.RadioButton
    }

    private fun nodes(view: ComposeView): List<SemanticsNode> {
        val rootView = view.getChildAt(0)
        val owner = rootView.javaClass.getMethod("getSemanticsOwner").invoke(rootView) as SemanticsOwner
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return descendants(owner.rootSemanticsNode)
    }
}
