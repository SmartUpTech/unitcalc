package net.smartlogic.unitconverter.activity;

import android.app.Application;
import android.os.Looper;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
@LooperMode(LooperMode.Mode.PAUSED)
public class FinanceNavigationTest {
    @Test public void investmentDestinationsSurviveResumeAndBackReturnsToPreviousWorkspace() {
        var app = RuntimeEnvironment.getApplication();
        Preferences.getInstance(app).getPreferences().edit().clear().commit();
        ThemeManager.init(app);
        AppConst.CURRENT_TAG = AppConst.TAG_CALC;
        var controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            var activity = controller.get();
            for (String id : new String[]{CalculatorCatalog.ID_FD, CalculatorCatalog.ID_RD, CalculatorCatalog.ID_SIP}) {
                activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_UNIT));
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                activity.navigateToEntry(CalculatorCatalog.getById(id));
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(id, AppConst.CURRENT_TAG);
                activity.onPostResume();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(id, AppConst.CURRENT_TAG);
                assertFalse(activity.getSupportFragmentManager().findFragmentByTag(id).isHidden());
                activity.getOnBackPressedDispatcher().onBackPressed();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(AppConst.TAG_CONVERTER, AppConst.CURRENT_TAG);
            }
        } finally { controller.pause().stop().destroy(); }
    }
}
