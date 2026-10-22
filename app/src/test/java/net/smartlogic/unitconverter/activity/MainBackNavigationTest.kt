package net.smartlogic.unitconverter.activity

import android.app.Application
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.app.AppConst
import net.smartlogic.unitconverter.helper.Preferences
import net.smartlogic.unitconverter.model.CalculatorCatalog
import net.smartlogic.unitconverter.theme.ThemeManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class MainBackNavigationTest {
    @Test fun rootBackRequiresSecondPressWithinTwoSeconds() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        AppConst.CURRENT_TAG = AppConst.TAG_CALC
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            activity.onBackPressedDispatcher.onBackPressed()
            assertFalse(activity.isFinishing)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2001))
            activity.onBackPressedDispatcher.onBackPressed()
            assertFalse(activity.isFinishing)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1999))
            activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(activity.isFinishing)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun drawerTakesPriorityThenTimerReturnsToWorkspace() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        AppConst.CURRENT_TAG = AppConst.TAG_CALC
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-mdpi")
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            settle()
            // Simulate a workspace handler that must lose priority to the open drawer.
            var workspaceBacks = 0
            val workspace = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { workspaceBacks++ }
            }
            activity.onBackPressedDispatcher.addCallback(activity, workspace)
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_TIMER)!!)
            settle()
            val timer = activity.supportFragmentManager.findFragmentByTag(AppConst.TAG_TIMER)!!
            assertFalse(timer.isHidden)
            val drawer = activity.findViewById<DrawerLayout>(R.id.drawer_layout)
            drawer.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY))
            drawer.layout(0, 0, 360, 640)
            drawer.openDrawer(GravityCompat.START, false)
            settle()
            activity.onBackPressedDispatcher.onBackPressed()
            settle()
            assertFalse(timer.isHidden)
            assertEquals(0, workspaceBacks)
            // Complete the drawer animation explicitly; Robolectric does not render frames.
            drawer.closeDrawer(GravityCompat.START, false)
            assertFalse(drawer.isDrawerOpen(GravityCompat.START))
            activity.onBackPressedDispatcher.onBackPressed()
            settle()
            assertTrue(timer.isHidden)
            assertFalse(activity.isFinishing)
            activity.onBackPressedDispatcher.onBackPressed()
            assertEquals(1, workspaceBacks)
            workspace.remove()
            activity.onBackPressedDispatcher.onBackPressed()
            assertFalse(activity.isFinishing)
            activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(activity.isFinishing)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
