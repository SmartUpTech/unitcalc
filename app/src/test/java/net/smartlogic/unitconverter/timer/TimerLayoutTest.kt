package net.smartlogic.unitconverter.timer

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyTheme
import net.smartlogic.unitconverter.theme.ThemeManager
import net.smartlogic.unitconverter.timer.compose.TimerSettingSection
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
    @Test fun pickerAndStartRemainVisibleAtConstrainedSizesAndLargeText() {
        for (fontScale in listOf(1f, 2f)) {
            for (height in if (fontScale == 1f) listOf(212, 320, 480) else listOf(320, 480)) {
                val app = RuntimeEnvironment.getApplication()
                val configuration = Configuration(app.resources.configuration).apply { this.fontScale = fontScale }
                @Suppress("DEPRECATION")
                app.resources.updateConfiguration(configuration, app.resources.displayMetrics)
                ThemeManager.init(app)
                val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
                val view = ComposeView(controller.get())
                controller.get().setContentView(view)
                view.setContent { GraphyTheme { Surface {
                    TimerSettingSection(TimerDraft(), {}, {})
                } } }
                try {
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
                    view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    view.layout(0, 0, 320, height)
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
                    val owner = view.getChildAt(0).javaClass.getMethod("getSemanticsOwner")
                        .invoke(view.getChildAt(0)) as SemanticsOwner
                    fun descendants(node: SemanticsNode): List<SemanticsNode> =
                        listOf(node) + node.children.flatMap(::descendants)
                    val nodes = descendants(owner.rootSemanticsNode)
                    for (unit in listOf(R.string.timer_hours, R.string.timer_min, R.string.timer_sec)) {
                        val text = controller.get().getString(unit)
                        val label = nodes.first { node ->
                            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true
                        }
                        assertTrue("$text has usable bounds", label.size.width > 0 && label.size.height > 0)
                        assertTrue("$text fits height", label.boundsInRoot.bottom <= height)
                    }
                    val start = nodes.first { it.config.getOrNull(SemanticsProperties.ContentDescription)
                        ?.contains(controller.get().getString(R.string.timer_start)) == true }
                    assertTrue(start.size.width >= 48 && start.size.height >= 48)
                    assertTrue(start.boundsInRoot.bottom <= height)
                    assertTrue("Wheel values must not collapse", nodes.any {
                        it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { label -> label.startsWith("0 ") } == true
                            && it.size.height >= 48
                    })
                    val bitmap = Bitmap.createBitmap(320, height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    val output = File("build/reports/production-review/timer-320-${height}-font$fontScale.png")
                    output.parentFile?.mkdirs()
                    output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                } finally {
                    view.disposeComposition()
                    controller.pause().stop().destroy()
                }
            }
        }
    }
}

