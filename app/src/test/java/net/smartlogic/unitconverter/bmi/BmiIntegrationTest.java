package net.smartlogic.unitconverter.bmi;

import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.DatabaseHelper;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class BmiIntegrationTest {
    private void drain() throws Exception {
        CountDownLatch idle = new CountDownLatch(1); DatabaseHelper.executeHistoryWork(idle::countDown);
        assertTrue(idle.await(10, TimeUnit.SECONDS)); shadowOf(Looper.getMainLooper()).idle();
    }
    @Test public void historyResetUnitConversionAndRememberedCalculation() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        Preferences.getInstance(app).setBmiState(""); ThemeManager.init(app);
        try (DatabaseHelper db = new DatabaseHelper(app)) { db.clearHistory(); }
        var controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme); controller.setup().visible();
        try {
            var activity = controller.get();
            BmiCalculatorFragment fragment = new BmiCalculatorFragment();
            activity.getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "bmi").commitNow();
            View root = fragment.requireView();
            ((EditText) root.findViewById(R.id.bmi_height_input)).setText("186");
            ((EditText) root.findViewById(R.id.bmi_weight_input)).setText("73");
            assertEquals("21.10", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4)); drain();
            try (DatabaseHelper db = new DatabaseHelper(app)) {
                assertEquals(1, db.getAllHistory().size());
                assertEquals("bmi_calculator", db.getAllHistory().get(0).calculatorId());
                assertTrue(db.getAllHistory().get(0).result().contains("21.10"));
            }
            root.findViewById(R.id.equal).performClick(); drain();
            try (DatabaseHelper db = new DatabaseHelper(app)) { assertEquals(1, db.getAllHistory().size()); }
            root.findViewById(R.id.bmi_ranges).performClick();
            assertNotNull(fragment.getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG));
            ((BmiDialogFragment) fragment.getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG)).dismissNow();
            Bundle unit = new Bundle(); unit.putString("mode", BmiDialogFragment.HEIGHT); unit.putInt("selected", 1);
            fragment.getChildFragmentManager().setFragmentResult(BmiDialogFragment.RESULT, unit);
            assertEquals(View.VISIBLE, root.findViewById(R.id.bmi_inches_input).getVisibility());
            assertEquals("21.10", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            activity.getSupportFragmentManager().beginTransaction().remove(fragment).commitNow();
            fragment = new BmiCalculatorFragment();
            activity.getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "bmi").commitNow();
            root = fragment.requireView();
            assertEquals("21.10", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            assertEquals(View.VISIBLE, root.findViewById(R.id.bmi_inches_input).getVisibility());
            fragment.reset();
            assertEquals("", ((EditText) root.findViewById(R.id.bmi_weight_input)).getText().toString());
            assertEquals(View.VISIBLE, root.findViewById(R.id.bmi_inches_input).getVisibility());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4)); drain();
            try (DatabaseHelper db = new DatabaseHelper(app)) { assertEquals(1, db.getAllHistory().size()); }
        } finally { controller.pause().stop().destroy(); }
    }
}
