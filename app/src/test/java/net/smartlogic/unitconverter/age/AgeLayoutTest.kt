package net.smartlogic.unitconverter.age

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.theme.CalculatorThemes
import net.smartlogic.unitconverter.theme.ThemeApplier
import net.smartlogic.unitconverter.theme.ThemeManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class AgeLayoutTest {
    private fun render(width: Int, height: Int, scale: Float, themeId: String, verify: Boolean = true, largestRange: Boolean = false, empty: Boolean = false) {
        val app = RuntimeEnvironment.getApplication()
        val previous = Configuration(app.resources.configuration)
        val config = Configuration(previous).apply { fontScale = scale }
        @Suppress("DEPRECATION")
        app.resources.updateConfiguration(config, app.resources.displayMetrics)
        ThemeManager.setProAccessProvider { true }
        ThemeManager.init(app)
        ThemeManager.select(app, themeId)
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        try {
            val activity = controller.get()
            val pane = LayoutInflater.from(activity).inflate(R.layout.pane_age_calculate, null) as AgeCalculateLayout
            activity.setContentView(pane)
            val r = if (largestRange) AgeCalculator.calculate(AgeCalculator.MIN_DATE, AgeCalculator.MAX_DATE)
                else AgeCalculator.calculate(LocalDate.of(2010, 8, 16), LocalDate.of(2020, 1, 21))
            val f = AgeFormatter(activity)
            fun text(id: Int, value: String) { pane.findViewById<TextView>(id).text = value }
            text(R.id.age_birth_button, if (empty) activity.getString(R.string.age_select_date) else f.date(r.birth()))
            text(R.id.age_asof_button, f.date(r.asOf()))
            text(R.id.age_years, f.number(r.age().years().toLong()))
            text(R.id.age_year_unit, activity.resources.getQuantityString(R.plurals.age_year_unit, r.age().years()))
            text(R.id.age_remainder, activity.getString(R.string.age_remaining, f.months(r.age().months()), f.days(r.age().days())))
            text(R.id.age_birthday_details, activity.getString(R.string.age_birthday_when, f.weekday(r.nextBirthday()), f.date(r.nextBirthday())))
            text(R.id.age_birthday_countdown, f.countdown(r))
            pane.findViewById<AgeResultLayout>(R.id.age_result_card).showResult(!empty)
            pane.findViewById<View>(R.id.age_calendar).isEnabled = !empty
            pane.findViewById<View>(R.id.age_share).isEnabled = !empty
            val ids = intArrayOf(R.id.age_total_years, R.id.age_total_months, R.id.age_total_weeks, R.id.age_total_days, R.id.age_total_hours, R.id.age_total_minutes)
            val values = longArrayOf(r.age().years().toLong(), r.totalMonths(), r.totalWeeks(), r.totalDays(), r.totalHours(), r.totalMinutes())
            ids.forEachIndexed { index, id -> text(id, f.number(values[index])) }
            ThemeApplier.apply(pane)
            pane.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            pane.layout(0, 0, width, height)
            fun descendants(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
            val name = "$themeId-${width}x$height-font$scale" + (if (largestRange) "-max" else "") + if (empty) "-empty" else ""
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            pane.draw(Canvas(bitmap))
            val file = File("build/reports/age/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            if (verify) {
                if (!pane.contentFits()) File("build/reports/age/$name-bounds.txt").writeText(
                    descendants(pane).joinToString("\n") { v ->
                        "${v.javaClass.simpleName} ${if (v is TextView) v.text else v.id}: ${v.left},${v.top},${v.right},${v.bottom}; measured=${v.measuredWidth}x${v.measuredHeight}" +
                            if (v is TextView) "; textSize=${v.textSize}; lines=${v.layout?.lineCount}" else ""
                    })
                assertTrue("$name must fit the available pane", pane.contentFits())
                fun shown(view: View): Boolean = view.visibility == View.VISIBLE && (view === pane || shown(view.parent as View))
                for (v in descendants(pane).filter { shown(it) }) {
                    if (v !== pane) {
                        val parent = v.parent as View
                        assertTrue("$name ${if (v is TextView) v.text else v.javaClass.simpleName} fits parent: ${v.left},${v.top},${v.right},${v.bottom} in ${parent.width}x${parent.height}", v.left >= 0 && v.top >= 0 && v.right <= parent.width && v.bottom <= parent.height)
                    }
                    if (v is TextView && v.text.isNotEmpty()) {
                        assertTrue("$name ${v.id} has unclipped text", v.layout.getLineBottom(v.layout.lineCount - 1) <= v.height - v.compoundPaddingTop - v.compoundPaddingBottom)
                        if (v.isClickable) assertTrue("$name ${v.id} touch target", v.width >= 48 && v.height >= 48)
                        if (ids.contains(v.id)) assertEquals("$name summary totals stay on one line", 1, v.layout.lineCount)
                    }
                }
                assertEquals(ThemeManager.get().accentContentColor(), pane.findViewById<TextView>(R.id.age_years).currentTextColor)
            }
        } finally {
            controller.pause().stop().destroy()
            ThemeManager.setProAccessProvider { false }
            @Suppress("DEPRECATION")
            app.resources.updateConfiguration(previous, app.resources.displayMetrics)
        }
    }
    @Test fun allThemesRenderWithSharedTypographyAndVisibleControls() {
        for (theme in CalculatorThemes.all()) {
            render(360, 480, 1f, theme.id())
            render(360, 480, 1f, theme.id(), empty = true)
        }
    }
    @Test fun narrowPortraitKeepsTheWholeWorkflowVisible() = render(320, 432, 1f, "porcelain_white")
    @Test fun scaledPortraitKeepsTheWholeWorkflowVisible() = render(360, 480, 1.3f, "graphite_black")
    @Test fun shortLandscapeKeepsTheWholeWorkflowVisible() = render(640, 300, 1f, "porcelain_white")
    @Test fun scaledLandscapeKeepsTheWholeWorkflowVisible() = render(720, 360, 1.3f, "graphite_black")
    @Test fun extremeAccessibilityViewportsAreRecordedWithoutClaimingFit() {
        render(320, 320, 2f, "porcelain_white", verify = false)
    }
    @Test fun maximumSupportedDateRangeKeepsCompleteTotalsReadable() = render(360, 560, 1f, "porcelain_white", largestRange = true)
    @Test fun maximumSupportedDateRangeKeepsScaledTotalsReadable() = render(360, 560, 1.3f, "graphite_black", largestRange = true)
}
