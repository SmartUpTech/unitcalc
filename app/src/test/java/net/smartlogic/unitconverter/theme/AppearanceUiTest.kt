package net.smartlogic.unitconverter.theme

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.helper.Preferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w320dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceUiTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication()
    private lateinit var renderContext: android.content.Context
    private var dialog by mutableStateOf("")
    private var scale by mutableStateOf(1f)
    private var direction by mutableStateOf(LayoutDirection.Ltr)
    private lateinit var restoration: StateRestorationTester
    @Before fun setup() {
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        restoration = StateRestorationTester(compose)
        restoration.setContent {
            renderContext = androidx.compose.ui.platform.LocalContext.current
            renderContext.setTheme(R.style.AppTheme)
            CompositionLocalProvider(LocalDensity provides Density(1f, scale), LocalLayoutDirection provides direction) {
                GraphyTheme {
                    when (dialog) {
                        "buttons" -> ButtonsDialog { dialog = "" }
                        "fonts" -> TextFontDialog { dialog = "" }
                    }
                }
            }
        }
    }
    private fun open(kind: String) {
        compose.runOnIdle { dialog = kind }
        compose.waitForIdle()
    }
    private fun tap(label: Int, scroll: Boolean = true) = tapText(app.getString(label), scroll)
    private fun tapText(label: String, scroll: Boolean = true) {
        val node = compose.onNodeWithText(label)
        if (scroll) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }
    @Test fun draftApplyCancelDismissReopenAndShapeMemory() {
        open("buttons")
        compose.onNodeWithText("Rounded").assertIsNotEnabled()
        tap(R.string.button_style_satin)
        tap(R.string.button_shape_circle)
        tap(R.string.button_style_flat)
        compose.onNodeWithText("Circle").assertIsSelected().assertIsNotEnabled()
        tap(R.string.button_style_outline)
        compose.onNodeWithText("Circle").assertIsSelected().assertIsEnabled()
        tap(android.R.string.cancel, false)
        assertEquals(Appearance.Style.FLAT, Appearance.style(app))
        assertEquals(Appearance.Shape.ROUNDED, Appearance.shape(app))
        open("buttons")
        tap(R.string.button_style_bevel); tap(R.string.button_shape_squircle)
        tap(R.string.appearance_apply, false)
        assertEquals(Appearance.Style.BEVEL, Appearance.style(app))
        assertEquals(Appearance.Shape.SQUIRCLE, Appearance.shape(app))
        open("buttons")
        compose.onNodeWithText("Bevel").assertIsSelected()
        compose.onNodeWithText("Squircle").assertIsSelected()
        tap(R.string.button_style_tonal)
        // Removing the dialog, like navigation or window dismissal, never commits its draft.
        compose.runOnIdle { dialog = "" }
        compose.waitForIdle()
        assertEquals(Appearance.Style.BEVEL, Appearance.style(app))
    }
    @Test fun everyStyleAndShapeUpdatesExactlyOnePreviewRowWithoutSavingDrafts() {
        open("buttons")
        var remembered = Appearance.Shape.ROUNDED
        for (style in Appearance.Style.entries) {
            tap(style.label)
            if (style != Appearance.Style.FLAT) {
                for (shape in Appearance.Shape.entries) {
                    tap(shape.label)
                    remembered = shape
                    assertSinglePreview(style, shape)
                }
            } else assertSinglePreview(style, remembered)
        }
        assertEquals(Appearance.Style.FLAT, Appearance.style(app))
        assertEquals(Appearance.Shape.ROUNDED, Appearance.shape(app))
        tap(android.R.string.cancel, false)
    }
    private fun assertSinglePreview(style: Appearance.Style, shape: Appearance.Shape) {
        compose.runOnIdle {
            val preview = singlePreview()
            assertEquals(android.widget.LinearLayout.HORIZONTAL, preview.orientation)
            assertEquals(5, preview.childCount)
            for (i in 0 until preview.childCount) {
                val key = preview.getChildAt(i)
                assertEquals(0, key.top)
                assertEquals(preview.height, key.bottom)
                assertEquals(style, org.robolectric.util.ReflectionHelpers.getField<Appearance.Style>(key.background, "style"))
                assertEquals(shape, org.robolectric.util.ReflectionHelpers.getField<Appearance.Shape>(key.background, "shape"))
            }
            assertTrue(preview.getChildAt(3) is android.widget.ImageView)
            assertEquals("=", (preview.getChildAt(4) as android.widget.TextView).text.toString())
        }
    }
    private fun singlePreview(): KeypadPreviewView {
        fun previews(v: android.view.View): List<KeypadPreviewView> =
            if (v is KeypadPreviewView) listOf(v) else if (v is android.view.ViewGroup)
                (0 until v.childCount).flatMap { previews(v.getChildAt(it)) } else emptyList()
        return previews(ShadowDialog.getLatestDialog().window!!.decorView).single()
    }
    private fun pressSystemBack() {
        compose.runOnIdle {
            (ShadowDialog.getLatestDialog() as androidx.activity.ComponentDialog)
                .onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()
    }
    private fun tapOutsideDialog() {
        compose.runOnIdle {
            val window = ShadowDialog.getLatestDialog()
            val downTime = android.os.SystemClock.uptimeMillis()
            for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                val event = android.view.MotionEvent.obtain(downTime, downTime + action, action, -10f, -10f, 0)
                try { window.dispatchTouchEvent(event) } finally { event.recycle() }
            }
        }
        compose.waitForIdle()
    }
    @Test fun backAndOutsideTapDiscardDrafts() {
        open("buttons")
        tap(R.string.button_style_satin)
        pressSystemBack()
        compose.onNodeWithText(app.getString(R.string.appearance_buttons)).assertDoesNotExist()
        assertEquals(Appearance.Style.FLAT, Appearance.style(app))
        open("buttons")
        tap(R.string.button_style_bevel)
        tapOutsideDialog()
        compose.onNodeWithText(app.getString(R.string.appearance_buttons)).assertDoesNotExist()
        assertEquals(Appearance.Style.FLAT, Appearance.style(app))
        assertEquals(Appearance.Shape.ROUNDED, Appearance.shape(app))
    }
    @Test fun restoredDraftKeepsStyleAndShapeWithoutApplyingThem() {
        open("buttons")
        tap(R.string.button_style_bevel)
        tap(R.string.button_shape_squircle)
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.onNodeWithText("Bevel").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithText("Squircle").assertIsSelected().assertIsDisplayed()
        assertSinglePreview(Appearance.Style.BEVEL, Appearance.Shape.SQUIRCLE)
        assertEquals(Appearance.Style.FLAT, Appearance.style(app))
        tap(R.string.appearance_apply, false)
        assertEquals(Appearance.Style.BEVEL, Appearance.style(app))
        assertEquals(Appearance.Shape.SQUIRCLE, Appearance.shape(app))
    }
    @Test fun reopeningBringsBothAppliedSelectionsIntoViewInRtl() {
        compose.runOnIdle {
            direction = LayoutDirection.Rtl
            Appearance.applyButtons(app, Appearance.Style.SATIN, Appearance.Shape.CIRCLE)
        }
        open("buttons")
        compose.onNodeWithText("Satin").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithText("Circle").assertIsSelected().assertIsDisplayed()
        assertSinglePreview(Appearance.Style.SATIN, Appearance.Shape.CIRCLE)
        tap(R.string.button_style_flat)
        compose.onNodeWithText("Circle").assertIsSelected().assertIsNotEnabled()
        tap(R.string.button_style_outline)
        compose.onNodeWithText("Circle").assertIsSelected().assertIsEnabled()
        tap(android.R.string.cancel, false)
    }
    @Test fun allFontsKeepPreviewAndActionsVisibleAtDoubleTextScale() {
        for (font in Appearance.Font.entries) {
            compose.runOnIdle {
                val config = android.content.res.Configuration(renderContext.resources.configuration).apply { fontScale = 2f }
                @Suppress("DEPRECATION")
                renderContext.resources.updateConfiguration(config, renderContext.resources.displayMetrics)
                scale = 2f
                Appearance.applyFont(app, font)
                Appearance.applyButtons(app, Appearance.Style.INSET, Appearance.Shape.CIRCLE)
            }
            open("buttons")
            compose.onNodeWithText(app.getString(R.string.appearance_preview)).assertIsDisplayed()
            compose.onNodeWithText(app.getString(R.string.appearance_apply)).assertIsDisplayed()
            compose.onNodeWithText(app.getString(android.R.string.cancel)).assertIsDisplayed()
            assertSinglePreview(Appearance.Style.INSET, Appearance.Shape.CIRCLE)
            compose.runOnIdle {
                val preview = singlePreview()
                for (i in 0 until preview.childCount) {
                    val key = preview.getChildAt(i)
                    if (key is android.widget.TextView) {
                        assertTrue("$font clipped preview label", key.paint.measureText(key.text.toString()) <= key.width)
                        assertTrue("$font clipped preview height", key.layout.height <= key.height)
                    }
                }
            }
            tap(android.R.string.cancel, false)
        }
    }
    @Test fun fontsApplyOnlyOnConfirmationAndReopenWithTheSelection() {
        open("fonts"); tapText("Lato")
        assertEquals(Appearance.Font.OSWALD, Appearance.font(app))
        tap(android.R.string.cancel, false)
        assertEquals(Appearance.Font.OSWALD, Appearance.font(app))
        open("fonts"); tapText("Roboto Mono"); tap(R.string.appearance_apply, false)
        assertEquals(Appearance.Font.ROBOTO_MONO, Appearance.font(app))
        open("fonts")
        compose.onNodeWithText("Roboto Mono").assertIsSelected()
        tap(android.R.string.cancel, false)
    }
    private fun assertTextFamily(label: String, font: Appearance.Font) {
        val node = compose.onNodeWithText(label)
        val visibleHeight = node.fetchSemanticsNode().boundsInRoot.height
        node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            assertTrue(action(layouts))
            assertEquals(appFontFamily(font), layouts.single().layoutInput.style.fontFamily)
            assertTrue("Clipped text: $label", layouts.single().size.height <= visibleHeight + 1)
        }
    }
    private fun assertFontPreview(font: Appearance.Font) {
        val text = app.getString(R.string.appearance_font_preview_text, font.label)
        compose.onNodeWithText(text).assertIsDisplayed()
        assertTextFamily(text, font)
        assertTextFamily(font.label, font)
        assertSinglePreview(Appearance.style(app), Appearance.shape(app))
        compose.runOnIdle {
            val preview = singlePreview()
            val expected = androidx.core.content.res.ResourcesCompat.getFont(renderContext, font.resource)
            for (i in 0 until preview.childCount) {
                val key = preview.getChildAt(i)
                if (key is android.widget.TextView) {
                    assertEquals(expected, key.typeface)
                    assertFalse(key.typeface.isBold)
                    assertTrue("$font clipped numeric preview", key.paint.measureText(key.text.toString()) <= key.width)
                    assertTrue("$font clipped numeric height", key.layout.height <= key.height)
                }
            }
        }
    }
    @Test fun fontNamesAndLiveTextAndKeysUseEachDraftFontWithoutSaving() {
        compose.runOnIdle { Appearance.applyButtons(app, Appearance.Style.SATIN, Appearance.Shape.CIRCLE) }
        open("fonts")
        for (font in Appearance.Font.entries) {
            tapText(font.label)
            compose.onNodeWithText(font.label).assertIsSelected()
            assertFontPreview(font)
            assertEquals(Appearance.Font.OSWALD, Appearance.font(app))
        }
        tapText("Oswald")
        compose.onNodeWithText("Roboto Mono").performScrollTo()
        assertTextFamily("Roboto Mono", Appearance.Font.ROBOTO_MONO)
        tap(android.R.string.cancel, false)
        assertEquals(Appearance.Font.OSWALD, Appearance.font(app))
        assertEquals(Appearance.Style.SATIN, Appearance.style(app))
        assertEquals(Appearance.Shape.CIRCLE, Appearance.shape(app))
    }
    @Test fun fontDraftRestoresAndBackAndOutsideTapDiscardIt() {
        open("fonts"); tapText("Lato")
        restoration.emulateSavedInstanceStateRestore(); compose.waitForIdle()
        compose.onNodeWithText("Lato").assertIsSelected().assertIsDisplayed()
        assertFontPreview(Appearance.Font.LATO)
        assertEquals(Appearance.Font.OSWALD, Appearance.font(app))
        pressSystemBack()
        compose.onNodeWithText(app.getString(R.string.appearance_font)).assertDoesNotExist()
        open("fonts"); tapText("Roboto")
        assertFontPreview(Appearance.Font.ROBOTO)
        tapOutsideDialog()
        compose.onNodeWithText(app.getString(R.string.appearance_font)).assertDoesNotExist()
        assertEquals(Appearance.Font.OSWALD, Appearance.font(app))
    }
    @Test fun fontPreviewAndActionsFitAtDoubleTextInBothDirections() {
        for (layoutDirection in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            compose.runOnIdle {
                direction = layoutDirection
                val config = android.content.res.Configuration(renderContext.resources.configuration).apply { fontScale = 2f }
                @Suppress("DEPRECATION")
                renderContext.resources.updateConfiguration(config, renderContext.resources.displayMetrics)
                scale = 2f
                Appearance.applyFont(app, Appearance.Font.ROBOTO_MONO)
            }
            open("fonts")
            compose.onNodeWithText("Roboto Mono").assertIsSelected().assertIsDisplayed()
            for (font in Appearance.Font.entries) {
                tapText(font.label)
                assertFontPreview(font)
                compose.onNodeWithText(app.getString(R.string.appearance_apply)).assertIsDisplayed()
                compose.onNodeWithText(app.getString(android.R.string.cancel)).assertIsDisplayed()
            }
            tap(android.R.string.cancel, false)
            assertEquals(Appearance.Font.ROBOTO_MONO, Appearance.font(app))
        }
    }
    @Test fun nativeReviewScreenshotsInLightDarkAndLargeText() {
        for (theme in listOf(CalculatorThemes.PORCELAIN_WHITE, CalculatorThemes.GRAPHITE_BLACK)) {
            compose.runOnIdle { ThemeManager.select(app, theme.id()) }
            for (fontScale in listOf(1f, 1.3f, 2f)) {
                compose.runOnIdle {
                    val config = android.content.res.Configuration(renderContext.resources.configuration).apply {
                        this.fontScale = fontScale
                    }
                    @Suppress("DEPRECATION")
                    renderContext.resources.updateConfiguration(config, renderContext.resources.displayMetrics)
                    scale = fontScale
                    Appearance.applyButtons(app, Appearance.Style.TONAL, Appearance.Shape.SQUIRCLE)
                }
                open("buttons")
                screenshot("buttons-${theme.id()}-320-$fontScale")
                compose.onNodeWithText("Circle").performScrollTo()
                screenshot("button-shapes-${theme.id()}-320-$fontScale")
                tap(android.R.string.cancel, false)
                open("fonts")
                screenshot("fonts-${theme.id()}-320-$fontScale")
                tapText("Roboto Mono")
                screenshot("fonts-live-${theme.id()}-320-$fontScale")
                assertEquals(Appearance.Font.OSWALD, Appearance.font(app))
                tap(android.R.string.cancel, false)
            }
        }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val root = ShadowDialog.getLatestDialog().window!!.decorView
            assertTrue(root.width > 100 && root.height > 100)
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            val file = File("../.artifacts/appearance/$name.png"); file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
