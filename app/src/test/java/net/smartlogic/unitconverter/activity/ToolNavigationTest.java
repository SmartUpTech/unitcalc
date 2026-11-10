package net.smartlogic.unitconverter.activity;

import android.app.Application;
import android.content.Intent;
import android.os.Looper;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ToolNavigationTest {
    private void reset() {
        var app = RuntimeEnvironment.getApplication();
        Preferences.getInstance(app).getPreferences().edit().clear().commit();
        ThemeManager.init(app);
        AppConst.CURRENT_TAG = AppConst.TAG_CALC;
    }
    private void settle() { Shadows.shadowOf(Looper.getMainLooper()).idle(); }
    private void open(MainActivity activity, String id) {
        activity.navigateToEntry(CalculatorCatalog.getById(id)); settle();
    }
    private void back(MainActivity activity) { activity.getOnBackPressedDispatcher().onBackPressed(); settle(); }

    @Test public void everyDetailToolReturnsToConverterWithSystemAndToolbarBack() {
        reset();
        var controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            var activity = controller.get();
            for (var entry : CalculatorCatalog.getStartupEntries()) {
                if (entry.id.equals(CalculatorCatalog.ID_BASIC) || entry.id.equals(CalculatorCatalog.ID_UNIT)
                        || entry.id.equals(CalculatorCatalog.ID_CURRENCY)) continue;
                open(activity, CalculatorCatalog.ID_UNIT); open(activity, entry.id);
                if (entry.id.equals(CalculatorCatalog.ID_TIMER)) assertTrue(activity.isTimerScreenVisible());
                else assertEquals(entry.id, AppConst.CURRENT_TAG);
                activity.onPostResume(); settle(); back(activity);
                assertEquals(entry.id, AppConst.TAG_CONVERTER, AppConst.CURRENT_TAG);
                assertFalse(activity.isTimerScreenVisible());
                if (entry.id.equals(CalculatorCatalog.ID_TIMER)) continue; // Timer retains its drawer menu.
                open(activity, entry.id);
                activity.findViewById(net.smartlogic.unitconverter.R.id.btn_nav_menu).performClick(); settle();
                assertEquals(entry.id, AppConst.TAG_CONVERTER, AppConst.CURRENT_TAG);
                assertFalse(activity.isTimerScreenVisible());
            }
        } finally { controller.pause().stop().destroy(); }
    }

    @Test public void financeTrailAndHistorySurviveRecreationWithoutReturnCycles() {
        reset();
        var controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            var activity = controller.get();
            open(activity, CalculatorCatalog.ID_UNIT);
            open(activity, CalculatorCatalog.ID_FD); open(activity, CalculatorCatalog.ID_RD);
            open(activity, CalculatorCatalog.ID_SIP); open(activity, "history");
            controller.recreate(); activity = controller.get(); settle();
            for (String expected : new String[]{CalculatorCatalog.ID_SIP, CalculatorCatalog.ID_RD,
                    CalculatorCatalog.ID_FD, AppConst.TAG_CONVERTER}) {
                back(activity); assertEquals(expected, AppConst.CURRENT_TAG);
            }
            open(activity, CalculatorCatalog.ID_AGE); open(activity, CalculatorCatalog.ID_BMI);
            open(activity, CalculatorCatalog.ID_AGE); back(activity);
            assertEquals(AppConst.TAG_CONVERTER, AppConst.CURRENT_TAG);
        } finally { controller.pause().stop().destroy(); }
    }

    @Test public void allStartupToolsLaunchAndSavedScreenAndExplicitTimerTakePriority() {
        reset();
        var prefs = Preferences.getInstance(RuntimeEnvironment.getApplication());
        for (var entry : CalculatorCatalog.getStartupEntries()) {
            prefs.setStartupScreen(entry.id);
            var controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
            try {
                settle(); var activity = controller.get();
                if (entry.id.equals(CalculatorCatalog.ID_TIMER)) assertTrue(activity.isTimerScreenVisible());
                else assertEquals(entry.id, expectedTag(entry.id), AppConst.CURRENT_TAG);
                open(activity, CalculatorCatalog.ID_BMI);
                prefs.setStartupScreen(CalculatorCatalog.ID_FD);
                controller.recreate(); settle();
                assertEquals(CalculatorCatalog.ID_BMI, AppConst.CURRENT_TAG);
            } finally { controller.pause().stop().destroy(); }
        }
        var intent = new Intent(RuntimeEnvironment.getApplication(), MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_TIMER, true);
        var controller = Robolectric.buildActivity(MainActivity.class, intent).setup().visible();
        try { settle(); assertTrue(controller.get().isTimerScreenVisible());
            controller.recreate(); settle(); assertTrue(controller.get().isTimerScreenVisible());
        } finally { controller.pause().stop().destroy(); }
    }
    private String expectedTag(String id) {
        if (id.equals(CalculatorCatalog.ID_BASIC)) return AppConst.TAG_CALC;
        if (id.equals(CalculatorCatalog.ID_UNIT) || id.equals(CalculatorCatalog.ID_CURRENCY)) return AppConst.TAG_CONVERTER;
        return id;
    }

    @Test public void usageRoutingCoversStartupCategoriesModesAndReturnsWithoutResumeDuplicates() {
        reset();
        var controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            var activity = controller.get(); settle();
            assertEquals(CalculatorCatalog.ID_BASIC, lastTool(activity));
            for (var entry : CalculatorCatalog.getStartupEntries()) {
                open(activity, entry.id);
                if (entry.id.equals(CalculatorCatalog.ID_UNIT)) assertTrue(lastTool(activity).startsWith("unit:"));
                else assertEquals(entry.id, lastTool(activity));
                activity.onPostResume(); settle();
                if (!entry.id.equals(CalculatorCatalog.ID_UNIT)) assertEquals(entry.id, lastTool(activity));
            }
            open(activity, CalculatorCatalog.unitId(net.smartlogic.unitconverter.model.Conversion.MASS));
            assertEquals(CalculatorCatalog.unitId(net.smartlogic.unitconverter.model.Conversion.MASS), lastTool(activity));
            open(activity, CalculatorCatalog.ID_CURRENCY);
            assertEquals(CalculatorCatalog.ID_CURRENCY, lastTool(activity));
            open(activity, "history"); assertNull(lastTool(activity));
            back(activity); assertEquals(CalculatorCatalog.ID_CURRENCY, lastTool(activity));
            controller.recreate(); settle();
            assertEquals(CalculatorCatalog.ID_CURRENCY, lastTool(controller.get()));
        } finally { controller.pause().stop().destroy(); }
    }
    private String lastTool(MainActivity activity) {
        return org.robolectric.util.ReflectionHelpers.getField(activity, "lastOpenedTool");
    }
}
