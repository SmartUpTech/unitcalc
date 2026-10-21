package net.smartlogic.unitconverter.graphy.compose

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.graphy.compose.ui.GraphyPanel
import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot
import net.smartlogic.unitconverter.graphy.integration.GraphyBridge
import net.smartlogic.unitconverter.graphy.renderer.FlowchartGraphView
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme
import net.smartlogic.unitconverter.helper.Preferences
import net.smartlogic.unitconverter.theme.CalculatorThemes
import net.smartlogic.unitconverter.theme.ThemeManager
import net.smartlogic.unitconverter.utils.ExpressionEvaluator
import org.junit.Assert.*
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class GraphyPanelUiTest {
    private fun expression(text: String, result: String) = GraphyBridge().build(
        CalculationSnapshot.create("basic", text, text, result, ExpressionEvaluator.evaluate(text)))

    @Test fun longGraphsRemainFullHeightAndScrollOnlyVerticallyInLightDarkAndLargeText() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit()
            .putInt(Preferences.PREFS_THEME_USAGE_DAYS, Int.MAX_VALUE).commit()
        for (theme in listOf(CalculatorThemes.TITANIUM_GRAY, CalculatorThemes.GRAPHITE_BLACK)) {
            for (scale in listOf(1f, 2f)) {
                ThemeManager.select(app, theme.id())
                RuntimeEnvironment.setFontScale(scale)
                val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().visible()
                val activity = controller.get()
                val output = mutableStateOf(expression("((12+8)*3-15)/5+7", "16"))
                val view = ComposeView(activity)
                activity.setContentView(view)
                view.setContent { GraphyTheme { GraphyPanel(output.value, GraphyViewTheme(activity)) } }
                try {
                    fun measure() {
                        settle()
                        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
                        view.layout(0, 0, 320, 400)
                        settle()
                    }
                    measure()
                    val nodes = nodes(view)
                    val scroller = nodes.single { it.config.contains(SemanticsProperties.VerticalScrollAxisRange) }
                    val range = scroller.config[SemanticsProperties.VerticalScrollAxisRange]
                    assertTrue("Long graph must remain reachable", range.maxValue() > 0)
                    assertFalse(nodes.any { it.config.contains(SemanticsProperties.HorizontalScrollAxisRange) })
                    val graph = descendants(view).filterIsInstance<FlowchartGraphView>().single()
                    assertTrue(graph.measuredHeight > 400)
                    assertTrue(graph.measuredWidth <= 320)
                    assertTrue(scroller.config[SemanticsActions.ScrollBy].action!!.invoke(0f, 10000f))
                    settle()
                    assertTrue(range.value() > 0)
                    val directory = File("build/reports/graphy-panel").apply { mkdirs() }
                    val bitmap = Bitmap.createBitmap(320, 400, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    File(directory, "${theme.id()}-${scale}-end.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                    // A new calculation must not inherit the previous graph's scroll offset.
                    output.value = expression("2", "2")
                    measure()
                    val newRange = nodes(view).single { it.config.contains(SemanticsProperties.VerticalScrollAxisRange) }
                        .config[SemanticsProperties.VerticalScrollAxisRange]
                    assertEquals(0f, newRange.value(), 0f)
                    val visibleCopy = nodes(view).flatMap { it.config.getOrNull(SemanticsProperties.Text) ?: emptyList() }
                        .joinToString { it.text }
                    assertFalse(visibleCopy.contains("How this answer was calculated"))
                    assertFalse(visibleCopy.contains("Tap a node"))
                    assertFalse(visibleCopy.contains("Show formula"))
                } finally {
                    view.disposeComposition()
                    controller.pause().stop().destroy()
                    RuntimeEnvironment.setFontScale(1f)
                    ThemeManager.select(app, CalculatorThemes.DEFAULT_ID)
                }
            }
        }
    }

    private fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    private fun nodes(view: ComposeView): List<SemanticsNode> {
        val root = view.getChildAt(0)
        val owner = root.javaClass.getMethod("getSemanticsOwner").invoke(root) as SemanticsOwner
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return walk(owner.rootSemanticsNode)
    }
}
