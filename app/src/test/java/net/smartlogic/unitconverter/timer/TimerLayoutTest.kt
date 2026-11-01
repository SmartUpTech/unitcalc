package net.smartlogic.unitconverter.timer

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.*
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.theme.CalculatorThemes
import net.smartlogic.unitconverter.theme.ThemeManager
import net.smartlogic.unitconverter.timer.compose.*
import net.smartlogic.unitconverter.timer.model.TimerDraft
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
@Config(application = Application::class, sdk = [34], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class TimerLayoutTest {
    private fun render(width: Int, height: Int, scale: Float, name: String,
        content: @Composable () -> Unit, check: (List<SemanticsNode>) -> Unit) {
        val app = RuntimeEnvironment.getApplication()
        val configuration = Configuration(app.resources.configuration).apply { fontScale = scale }
        @Suppress("DEPRECATION")
        app.resources.updateConfiguration(configuration, app.resources.displayMetrics)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val view = ComposeView(controller.get())
        controller.get().setContentView(view)
        var renderedFontScale = 0f
        view.setContent { GraphyTheme { Surface {
            renderedFontScale = LocalDensity.current.fontScale
            content()
        } } }
        try {
            fun settle() {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, width, height)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
            }
            settle(); settle()
            assertEquals("Renderer uses requested font scale", scale, renderedFontScale, 0.01f)
            val owner = view.getChildAt(0).javaClass.getMethod("getSemanticsOwner")
                .invoke(view.getChildAt(0)) as SemanticsOwner
            fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output = File("build/reports/timer/$name.png")
            output.parentFile?.mkdirs()
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            check(descendants(owner.rootSemanticsNode))
        } finally { view.disposeComposition(); controller.pause().stop().destroy() }
    }
    private fun action(nodes: List<SemanticsNode>, label: String): SemanticsNode = nodes.first {
        it.config.contains(SemanticsActions.OnClick) &&
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true
    }
    private fun fits(node: SemanticsNode, width: Int, height: Int) {
        assertTrue("Action touch width ${node.config} ${node.boundsInRoot}", node.size.width >= 48)
        assertTrue("Action touch height ${node.boundsInRoot}", node.size.height >= 48)
        assertTrue("Action fits $width x $height: ${node.boundsInRoot}",
            node.boundsInRoot.left >= 0 && node.boundsInRoot.top >= 0 &&
            node.boundsInRoot.right <= width && node.boundsInRoot.bottom <= height)
    }
    @Test fun allThemesAndPhasesFitAndRender() {
        val app = RuntimeEnvironment.getApplication()
        ThemeManager.init(app)
        for (theme in CalculatorThemes.all()) {
            ThemeManager.select(app, theme.id())
            for (phase in listOf("setup", "running", "paused", "completed")) {
                render(360, 560, 1.3f, "${theme.id()}-$phase", {
                    Column(Modifier.fillMaxSize().padding(horizontal = dimensionResource(R.dimen.graphy_spacing_md))) {
                        Box(Modifier.weight(1f)) {
                            when (phase) {
                                "setup" -> TimerSettingSection(TimerDraft(minutes = 5, label = "Tea"), {}, {})
                                "completed" -> TimerCompletedSection("Tea", {}, {})
                                else -> TimerRunningSection("Tea", 272_000L, 300_000L, phase == "paused", {}, {}, {}, {})
                            }
                        }
                        TextButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Recent timers") }
                    }
                }) { nodes ->
                    val labels = when (phase) {
                        "setup" -> listOf(R.string.timer_start)
                        "completed" -> listOf(R.string.timer_dismiss, R.string.timer_restart)
                        "paused" -> listOf(R.string.timer_resume, R.string.timer_reset, R.string.timer_restart)
                        else -> listOf(R.string.timer_pause, R.string.timer_reset, R.string.timer_restart)
                    }
                    labels.forEach { fits(action(nodes, app.getString(it)), 360, 560) }
                    if (phase == "setup") listOf(1, 5, 10, 15, 25, 30).forEach {
                        fits(action(nodes, app.getString(R.string.timer_preset_minutes, it)), 360, 560)
                    }
                }
            }
        }
    }
    @Test fun setupFitsConstrainedWideTabletAndLargeTextViewports() {
        val app = RuntimeEnvironment.getApplication()
        ThemeManager.init(app)
        for ((width, height, scale) in listOf(Triple(320, 360, 1f), Triple(320, 360, 1.3f),
            Triple(600, 240, 1f), Triple(600, 240, 1.3f),
            Triple(800, 600, 1.3f), Triple(360, 640, 2f))) {
            render(width, height, scale, "setup-$width-$height-$scale", {
                Column(Modifier.fillMaxSize().padding(horizontal = dimensionResource(R.dimen.graphy_spacing_md))) {
                    Box(Modifier.weight(1f)) { TimerSettingSection(TimerDraft(), {}, {}) }
                    TextButton(onClick = {}) { Text("Recent timers") }
                }
            }) { nodes ->
                fits(action(nodes, app.getString(R.string.timer_start)), width, height)
                assertTrue(nodes.any { it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) != null })
            }
        }
    }
    @Test fun presetClickKeepsLabelAndDoesNotStart() {
        val app = RuntimeEnvironment.getApplication()
        ThemeManager.init(app)
        var draft = TimerDraft(label = "Tea")
        var starts = 0
        render(360, 560, 1f, "preset-interaction", {
            var current by remember { mutableStateOf(draft) }
            TimerSettingSection(current, { current = it; draft = it }, { starts++ })
        }) { nodes ->
            val preset = action(nodes, app.getString(R.string.timer_preset_minutes, 25))
            assertTrue(preset.config[SemanticsActions.OnClick].action!!.invoke())
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
            assertEquals(TimerDraft(minutes = 25, label = "Tea"), draft)
            assertEquals(0, starts)
        }
    }
    @Test fun physicallyImpossibleHeightReportsConstraint() {
        val app = RuntimeEnvironment.getApplication()
        ThemeManager.init(app)
        render(320, 180, 2f, "unresolved-extreme", { TimerSettingSection(TimerDraft(), {}, {}) }) { nodes ->
            assertTrue(nodes.any { it.config.getOrNull(SemanticsProperties.Text)?.any { text ->
                text.text == app.getString(R.string.tool_more_space) } == true })
        }
    }

    @Test fun maximumCountdownStaysReadableAtLargeText() {
        val app = RuntimeEnvironment.getApplication()
        ThemeManager.init(app)
        render(320, 560, 2f, "maximum-duration-large-text", {
            Box(Modifier.fillMaxSize().padding(horizontal = dimensionResource(R.dimen.graphy_spacing_md))) {
                TimerRunningSection("A longer Focus timer name", 359_999_000L, 359_999_000L, false, {}, {}, {}, {})
            }
        }) { nodes ->
            val time = nodes.first { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "99:59:59" } == true }
            val layouts = mutableListOf<TextLayoutResult>()
            assertTrue(time.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts))
            assertFalse("Full countdown fits at 2x text", layouts.single().didOverflowWidth)
            fits(action(nodes, app.getString(R.string.timer_pause)), 320, 560)
        }
    }

    @Test fun fullScreenFitsActualLargerDisplayViewport() {
        val app = RuntimeEnvironment.getApplication()
        ThemeManager.init(app)
        TimerEngine.resetForTests()
        val engine = TimerEngine.getInstance(app)
        engine.setBackgroundCoordinatorForTests(net.smartlogic.unitconverter.timer.service.NoOpTimerBackgroundCoordinator())
        engine.setAlarmPlayerEnabledForTests(false)
        engine.reset()
        engine.updateDraft(TimerDraft(minutes = 5, label = "Focus"))
        val model = TimerViewModel(engine)
        render(308, 426, 1.3f, "actual-large-display-full-screen", { TimerScreen(model) }) { nodes ->
            fits(action(nodes, app.getString(R.string.timer_start)), 308, 426)
            listOf(1, 5, 10, 15, 25, 30).forEach {
                fits(action(nodes, app.getString(R.string.timer_preset_minutes, it)), 308, 426)
            }
        }
    }
}
