package net.smartlogic.unitconverter.graphy.compose

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.graphy.compose.ui.GraphyDownloadButton
import net.smartlogic.unitconverter.graphy.compose.ui.GraphyDownloadState
import net.smartlogic.unitconverter.graphy.theme.GraphySemanticStyle
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
    @org.junit.After fun resetAccess() { ThemeManager.setProAccessProvider { false } }
    @Test fun downloadIconTracksThemeWhileCompositionRemainsOpen() {
        val app = RuntimeEnvironment.getApplication()
        ThemeManager.setProAccessProvider { true }
        RuntimeEnvironment.setFontScale(1f)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().visible()
        val view = ComposeView(controller.get())
        controller.get().setContentView(view)
        view.setContent {
            GraphyTheme {
                Box(Modifier.fillMaxSize().background(GraphyThemeTokens.semanticColors.background)) {
                    GraphyDownloadButton(GraphyDownloadState(true, false) {})
                }
            }
        }
        try {
            for (theme in CalculatorThemes.all()) {
                ThemeManager.select(app, theme.id())
                settle()
                view.measure(View.MeasureSpec.makeMeasureSpec(96, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(96, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, 96, 96)
                settle()
                val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val ink = GraphySemanticStyle.connector(theme)
                var matching = 0
                for (y in 0 until 96) for (x in 0 until 96) if (bitmap.getPixel(x, y) == ink) matching++
                assertTrue("Download must use visible theme ink in ${theme.id()}", matching > 10)
                bitmap.recycle()
            }
        } finally { controller.pause().stop().destroy() }
    }
    private fun expression(text: String, result: String) = GraphyBridge().build(
        CalculationSnapshot.create("basic", text, text, result, ExpressionEvaluator.evaluate(text)))

    @Test fun graphsAllowBothAxesAndResetExplorationInLightDarkAndLargeText() {
        val app = RuntimeEnvironment.getApplication()
        ThemeManager.setProAccessProvider { true }
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
                        // AndroidView updates request a later native layout pass after recomposition.
                        repeat(3) {
                            settle()
                            view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
                            view.layout(0, 0, 320, 400)
                        }
                        settle()
                    }
                    measure()
                    val nodes = nodes(view)
                    val scroller = nodes.single { it.config.contains(SemanticsProperties.VerticalScrollAxisRange) }
                    val range = scroller.config[SemanticsProperties.VerticalScrollAxisRange]
                    assertTrue("Long graph must remain reachable", range.maxValue() > 0)
                    // Folding survives reconstruction of an equivalent semantic output.
                    val initialGraph = descendants(view).filterIsInstance<FlowchartGraphView>().single()
                    val firstFoldId = output.value.nodes.size
                    assertTrue(initialGraph.accessibilityNodeProvider.performAction(firstFoldId,
                        android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null))
                    settle()
                    assertEquals(setOf(1), initialGraph.collapsedStages)
                    output.value = expression("((12+8)*3-15)/5+7", "16")
                    measure()
                    val retainedGraph = descendants(view).filterIsInstance<FlowchartGraphView>().single()
                    assertEquals(setOf(1), retainedGraph.collapsedStages)
                    val horizontal = nodes.single { it.config.contains(SemanticsProperties.HorizontalScrollAxisRange) }
                    assertTrue(horizontal.config[SemanticsProperties.HorizontalScrollAxisRange].maxValue() >= 0)
                    val graph = descendants(view).filterIsInstance<FlowchartGraphView>().single()
                    assertTrue(graph.measuredHeight > 400)
                    assertTrue(graph.measuredWidth >= 280)
                    // Independent branches require horizontal exploration at this viewport.
                    output.value = expression("((12+8)*(9-4)+(18/3-2))/(7-3)", "26")
                    measure()
                    val wideScroller = nodes(view).single { it.config.contains(SemanticsProperties.HorizontalScrollAxisRange) }
                    val wideRange = wideScroller.config[SemanticsProperties.HorizontalScrollAxisRange]
                    val measuredGraph = descendants(view).filterIsInstance<FlowchartGraphView>().single()
                    assertEquals(output.value.expression,
                        org.robolectric.util.ReflectionHelpers.getField<net.smartlogic.unitconverter.graphy.model.GraphyOutput>(measuredGraph, "output").expression)
                    assertTrue("Wide graph must overflow: measured=" + measuredGraph.measuredWidth
                        + " content=" + org.robolectric.util.ReflectionHelpers.getField<Int>(measuredGraph, "contentWidth")
                        + " expression=" + org.robolectric.util.ReflectionHelpers.getField<net.smartlogic.unitconverter.graphy.model.GraphyOutput>(measuredGraph, "output").expression,
                        wideRange.maxValue() > 0)
                    // Real native pointer movement must pan, not just semantic ScrollBy.
                    val wideGraph = descendants(view).filterIsInstance<FlowchartGraphView>().single()
                    val now = android.os.SystemClock.uptimeMillis()
                    for ((action, x, offset) in listOf(Triple(android.view.MotionEvent.ACTION_DOWN, 260f, 0L),
                        Triple(android.view.MotionEvent.ACTION_MOVE, 40f, 400L),
                        Triple(android.view.MotionEvent.ACTION_UP, 40f, 450L))) {
                        val event = android.view.MotionEvent.obtain(now, now + offset, action, x, 80f, 0)
                        assertTrue(wideGraph.onTouchEvent(event))
                        event.recycle()
                    }
                    settle()
                    assertTrue("Pointer drag must move the graph", wideRange.value() > 0)
                    val currentVertical = nodes(view).single { it.config.contains(SemanticsProperties.VerticalScrollAxisRange) }
                    assertTrue(currentVertical.config[SemanticsActions.ScrollBy].action!!.invoke(0f, 10000f))
                    settle()
                    assertTrue(currentVertical.config[SemanticsProperties.VerticalScrollAxisRange].value() > 0)
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
                    val newHorizontal = nodes(view).single { it.config.contains(SemanticsProperties.HorizontalScrollAxisRange) }
                        .config[SemanticsProperties.HorizontalScrollAxisRange]
                    assertEquals(0f, newHorizontal.value(), 0f)
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

    private fun settle() {
        // State writes occur on the test thread; deliver snapshot notifications before frames.
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
    }
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    private fun nodes(view: ComposeView): List<SemanticsNode> {
        val root = view.getChildAt(0)
        val owner = root.javaClass.getMethod("getSemanticsOwner").invoke(root) as SemanticsOwner
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return walk(owner.rootSemanticsNode)
    }
}
