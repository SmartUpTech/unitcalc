package net.smartlogic.unitconverter.age;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.provider.CalendarContract;
import androidx.appcompat.app.AlertDialog;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.activity.MainActivity;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import java.time.Duration;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "w360dp-h800dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class AgeActionsTest {
    @Test public void shareAndBothCalendarChoicesLaunchTheCurrentAgeSnapshot() {
        var app = RuntimeEnvironment.getApplication();
        Preferences.getInstance(app).getPreferences().edit().clear().commit();
        ThemeManager.init(app);
        AppConst.CURRENT_TAG = AppConst.TAG_CALC;
        var controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            var activity = controller.get();
            activity.navigateToEntry(CalculatorCatalog.getById(CalculatorCatalog.ID_AGE));
            settle();
            var age = (AgeCalculatorFragment) activity.getSupportFragmentManager().findFragmentByTag(AppConst.TAG_AGE);
            choose(age, AgeDialogFragment.AS_OF, "2020-01-21");
            choose(age, AgeDialogFragment.BIRTH, "2010-08-16");
            age.requireView().findViewById(R.id.age_share).performClick();
            settle();
            Intent chooser = shadowOf(activity).getNextStartedActivity();
            assertNotNull(chooser);
            assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
            Intent share = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            assertNotNull(share);
            assertEquals(Intent.ACTION_SEND, share.getAction());
            assertEquals("text/plain", share.getType());
            assertTrue(share.getStringExtra(Intent.EXTRA_TEXT).contains("9 years, 5 months, 5 days"));
            assertTrue(share.getStringExtra(Intent.EXTRA_TEXT).contains("4,960,800"));
            for (boolean yearly : new boolean[]{true, false}) {
                age.requireView().findViewById(R.id.age_calendar).performClick();
                settle();
                var popup = (AgeDialogFragment) age.getChildFragmentManager().findFragmentByTag(AgeDialogFragment.TAG);
                ((AlertDialog) popup.requireDialog()).getButton(yearly ? -1 : -3).performClick();
                settle();
                Intent calendar = shadowOf(activity).getNextStartedActivity();
                assertNotNull(calendar);
                assertEquals(Intent.ACTION_INSERT, calendar.getAction());
                assertEquals(CalendarContract.Events.CONTENT_URI, calendar.getData());
                assertTrue(calendar.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false));
                assertFalse(calendar.hasExtra(CalendarContract.EXTRA_EVENT_END_TIME));
                assertEquals(yearly, calendar.hasExtra(CalendarContract.Events.RRULE));
                if (yearly) assertEquals("FREQ=YEARLY;BYMONTH=8;BYMONTHDAY=16", calendar.getStringExtra(CalendarContract.Events.RRULE));
            }
        } finally { controller.pause().stop().destroy(); }
    }

    private void choose(AgeCalculatorFragment age, String mode, String date) {
        Bundle result = new Bundle();
        result.putString("mode", mode);
        result.putString("date", date);
        age.getChildFragmentManager().setFragmentResult(AgeDialogFragment.RESULT, result);
        settle();
    }
    private void settle() { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150)); }
}
