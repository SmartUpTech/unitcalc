package net.smartlogic.unitconverter.bmi;

import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.activity.MainActivity;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.*;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.Duration;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "w360dp-h800dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class BmiNavigationTest {
    private void settle() { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150)); }
    @Test public void toolsEntryVisibleBottomNavigationAndHistoryReturnSurviveRecreation() {
        Application app = RuntimeEnvironment.getApplication(); Preferences.getInstance(app).setBmiState(""); ThemeManager.init(app);
        AppConst.CURRENT_TAG = AppConst.TAG_CALC;
        var controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            MainActivity activity = controller.get(); settle();
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_BMI)); settle();
            assertEquals("BMI Calculator", activity.<TextView>findViewById(R.id.tv_app_bar_brand).getText().toString());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.navigation).getVisibility());
            activity.navigateToEntry(CalculatorCatalog.getById("history")); settle();
            activity.closeHistory(); settle();
            assertEquals(AppConst.TAG_BMI, AppConst.CURRENT_TAG);
            activity.restoreFromHistory(new CalculationHistoryItem("old inputs", "old BMI", 1L, CalculatorCatalog.ID_BMI)); settle();
            assertEquals(AppConst.TAG_BMI, AppConst.CURRENT_TAG);
            activity = controller.recreate().get(); settle();
            assertEquals(AppConst.TAG_BMI, AppConst.CURRENT_TAG);
            assertEquals(View.VISIBLE, activity.findViewById(R.id.navigation).getVisibility());
            activity.getOnBackPressedDispatcher().onBackPressed(); settle();
            assertEquals(AppConst.TAG_CALC, AppConst.CURRENT_TAG);
            assertFalse(activity.isFinishing());
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_AGE)); settle();
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_BMI)); settle();
            activity.getOnBackPressedDispatcher().onBackPressed(); settle();
            assertEquals(AppConst.TAG_AGE, AppConst.CURRENT_TAG);
            activity.getOnBackPressedDispatcher().onBackPressed(); settle();
            assertEquals(AppConst.TAG_CALC, AppConst.CURRENT_TAG);
        } finally { controller.pause().stop().destroy(); }
    }
}
