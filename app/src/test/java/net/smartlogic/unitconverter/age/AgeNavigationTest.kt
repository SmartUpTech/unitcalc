package net.smartlogic.unitconverter.age

import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.activity.MainActivity
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
@Config(application = Application::class, sdk = [34], qualifiers = "w360dp-h800dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class AgeNavigationTest {
    private fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
    private fun choose(fragment: AgeCalculatorFragment, mode: String, date: String) {
        fragment.childFragmentManager.setFragmentResult(AgeDialogFragment.RESULT,
            Bundle().apply { putString("mode", mode); putString("date", date) })
        settle()
    }
    @Test fun ageRestoresDatesAndGraphyAndBackReturnsToTheOriginalDestination() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        AppConst.CURRENT_TAG = AppConst.TAG_CALC
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            var activity = controller.get()
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_UNIT)!!)
            settle()
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_AGE)!!)
            settle()
            assertEquals(View.GONE, activity.findViewById<View>(R.id.navigation).visibility)
            assertEquals("Age Calculator", activity.findViewById<TextView>(R.id.tv_app_bar_brand).text.toString())
            var age = activity.supportFragmentManager.findFragmentByTag(AppConst.TAG_AGE) as AgeCalculatorFragment
            choose(age, AgeDialogFragment.AS_OF, "2020-01-21")
            choose(age, AgeDialogFragment.BIRTH, "2010-08-16")
            assertEquals("9", age.requireView().findViewById<TextView>(R.id.age_years).text.toString())
            age.requireView().findViewById<TabLayout>(R.id.workspace_tabs).getTabAt(1)!!.select()
            settle()
            activity = controller.recreate().get()
            settle()
            age = activity.supportFragmentManager.findFragmentByTag(AppConst.TAG_AGE) as AgeCalculatorFragment
            assertEquals(AppConst.TAG_AGE, AppConst.CURRENT_TAG)
            assertEquals(1, age.requireView().findViewById<TabLayout>(R.id.workspace_tabs).selectedTabPosition)
            assertEquals("9", age.requireView().findViewById<TextView>(R.id.age_years).text.toString())
            activity.onBackPressedDispatcher.onBackPressed()
            settle()
            assertEquals(0, age.requireView().findViewById<TabLayout>(R.id.workspace_tabs).selectedTabPosition)
            activity.onBackPressedDispatcher.onBackPressed()
            settle()
            assertEquals(AppConst.TAG_CONVERTER, AppConst.CURRENT_TAG)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.navigation).visibility)
            assertFalse(activity.isFinishing)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun invalidInputClearsResultAndCalendarAlwaysAsksForFrequency() {
        val app = RuntimeEnvironment.getApplication()
        Preferences.getInstance(app).preferences.edit().clear().commit()
        ThemeManager.init(app)
        AppConst.CURRENT_TAG = AppConst.TAG_CALC
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_AGE)!!)
            settle()
            val age = activity.supportFragmentManager.findFragmentByTag(AppConst.TAG_AGE) as AgeCalculatorFragment
            choose(age, AgeDialogFragment.AS_OF, "2020-01-21")
            choose(age, AgeDialogFragment.BIRTH, "2010-08-16")
            val calendar = age.requireView().findViewById<View>(R.id.age_calendar)
            calendar.performClick()
            settle()
            val dialog = age.childFragmentManager.findFragmentByTag(AgeDialogFragment.TAG) as AgeDialogFragment
            val alert = dialog.requireDialog() as androidx.appcompat.app.AlertDialog
            assertEquals(activity.getString(R.string.age_calendar_yearly), alert.getButton(-1).text.toString())
            assertEquals(activity.getString(R.string.age_calendar_once), alert.getButton(-3).text.toString())
            alert.getButton(-2).performClick()
            settle()
            calendar.performClick()
            settle()
            assertNotNull(age.childFragmentManager.findFragmentByTag(AgeDialogFragment.TAG))
            (age.childFragmentManager.findFragmentByTag(AgeDialogFragment.TAG) as AgeDialogFragment).dismissNow()
            choose(age, AgeDialogFragment.AS_OF, "2009-01-01")
            assertEquals(activity.getString(R.string.age_placeholder), age.requireView().findViewById<TextView>(R.id.age_years).text.toString())
            assertFalse(calendar.isEnabled)
            assertFalse(age.requireView().findViewById<View>(R.id.age_share).isEnabled)
            assertEquals(activity.getString(R.string.age_invalid), age.requireView().findViewById<TextView>(R.id.age_note).text.toString())
        } finally { controller.pause().stop().destroy() }
    }
}
