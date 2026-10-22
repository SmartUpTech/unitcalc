package net.smartlogic.unitconverter.activity

import android.app.Application
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.widget.PopupMenu
import com.google.android.material.tabs.TabLayout
import net.smartlogic.unitconverter.fragment.CalculatorFragment
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
    @Test fun historyBackReturnsToConverterAndRestoresNavigation() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        AppConst.CURRENT_TAG = AppConst.TAG_CALC
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_UNIT)!!)
            shadowOf(Looper.getMainLooper()).idle()
            val previousTag = AppConst.CURRENT_TAG
            val menu = PopupMenu(activity, activity.findViewById<View>(android.R.id.content)).menu
            activity.onCreateOptionsMenu(menu)
            activity.onOptionsItemSelected(menu.findItem(R.id.menu_history))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(AppConst.TAG_HISTORY, AppConst.CURRENT_TAG)
            assertEquals("History", activity.findViewById<android.widget.TextView>(R.id.tv_app_bar_brand).text.toString())
            assertEquals(View.GONE, activity.findViewById<View>(R.id.navigation).visibility)
            val restoredActivity = controller.recreate().get()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(AppConst.TAG_HISTORY, AppConst.CURRENT_TAG)
            restoredActivity.onBackPressedDispatcher.onBackPressed()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(previousTag, AppConst.CURRENT_TAG)
            assertFalse(restoredActivity.isFinishing)
            assertEquals(View.VISIBLE, restoredActivity.findViewById<View>(R.id.navigation).visibility)
            assertEquals(restoredActivity.getString(R.string.app_bar_brand), restoredActivity.findViewById<android.widget.TextView>(R.id.tv_app_bar_brand).text.toString())
        } finally { controller.pause().stop().destroy() }
    }
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

    @Test fun hiddenCalculatorDoesNotConsumeConverterBackAndHistoryIsTheOnlyAction() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        AppConst.CURRENT_TAG = AppConst.TAG_CALC
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-mdpi")
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            settle()
            val calculator = activity.supportFragmentManager.findFragmentByTag(AppConst.TAG_CALC) as CalculatorFragment
            for (id in intArrayOf(R.id.one, R.id.plus, R.id.two, R.id.equal)) {
                calculator.requireView().findViewById<View>(id).performClick()
                settle()
            }
            val tabs = calculator.requireView().findViewById<TabLayout>(R.id.workspace_tabs)
            tabs.getTabAt(1)!!.select()
            settle()
            assertEquals(1, tabs.selectedTabPosition)
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_UNIT)!!)
            settle()
            assertTrue(calculator.isHidden)
            activity.onBackPressedDispatcher.onBackPressed()
            assertFalse(activity.isFinishing)
            // Root Back must not alter a hidden workspace's selected tab.
            assertEquals(1, tabs.selectedTabPosition)
            activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(activity.isFinishing)
            val menu = PopupMenu(activity, activity.findViewById<View>(android.R.id.content)).menu
            activity.onCreateOptionsMenu(menu)
            activity.onPrepareOptionsMenu(menu)
            assertEquals(listOf(R.id.menu_history), (0 until menu.size())
                .map { menu.getItem(it) }.filter { it.isVisible }.map { it.itemId })
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
