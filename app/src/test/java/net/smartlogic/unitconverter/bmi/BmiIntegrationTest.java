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
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2999)); drain();
            try (DatabaseHelper db = new DatabaseHelper(app)) { assertTrue(db.getAllHistory().isEmpty()); }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1)); drain();
            try (DatabaseHelper db = new DatabaseHelper(app)) {
                assertEquals(1, db.getAllHistory().size());
                assertEquals("bmi_calculator", db.getAllHistory().get(0).calculatorId());
                assertTrue(db.getAllHistory().get(0).result().contains("21.10"));
            }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4)); drain();
            try (DatabaseHelper db = new DatabaseHelper(app)) { assertEquals(1, db.getAllHistory().size()); }
            root.findViewById(R.id.bmi_ranges).performClick();
            assertNotNull(fragment.getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG));
            BmiDialogFragment ranges = (BmiDialogFragment) fragment.getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG);
            String popupText = textTree(ranges.requireDialog().getWindow().getDecorView());
            for (int id : BmiFormatter.CATEGORIES) assertTrue(popupText, popupText.contains(app.getString(id)));
            for (int id : BmiFormatter.RANGES) assertTrue(popupText, popupText.contains(app.getString(id)));
            assertTrue(popupText.contains("Your range"));
            ((BmiDialogFragment) fragment.getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG)).dismissNow();
            Bundle unit = new Bundle(); unit.putString("mode", BmiDialogFragment.HEIGHT); unit.putInt("selected", 1);
            fragment.getChildFragmentManager().setFragmentResult(BmiDialogFragment.RESULT, unit);
            assertEquals(View.VISIBLE, root.findViewById(R.id.bmi_inches_input).getVisibility());
            assertEquals("21.10", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            for (int i = 0; i < 4; i++) {
                unit.putString("mode", BmiDialogFragment.HEIGHT); unit.putInt("selected", i % 2);
                fragment.getChildFragmentManager().setFragmentResult(BmiDialogFragment.RESULT, unit);
                unit.putString("mode", BmiDialogFragment.WEIGHT); unit.putInt("selected", i % 2);
                fragment.getChildFragmentManager().setFragmentResult(BmiDialogFragment.RESULT, unit);
                assertEquals("21.10", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            }
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
            unit.putInt("selected", 0);
            for (String mode : new String[]{BmiDialogFragment.HEIGHT, BmiDialogFragment.WEIGHT}) {
                unit.putString("mode", mode); fragment.getChildFragmentManager().setFragmentResult(BmiDialogFragment.RESULT, unit);
            }
            for (int id : new int[]{R.id.one, R.id.eight, R.id.six}) root.findViewById(id).performClick();
            root.findViewById(R.id.bmi_weight_input).requestFocus();
            for (int id : new int[]{R.id.seven, R.id.three}) root.findViewById(id).performClick();
            assertEquals("21.10", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            root.findViewById(R.id.backspace).performClick();
            assertEquals("7", ((EditText) root.findViewById(R.id.bmi_weight_input)).getText().toString());
            root.findViewById(R.id.ac).performClick();
            assertEquals("", ((EditText) root.findViewById(R.id.bmi_weight_input)).getText().toString());
            assertNull(root.findViewById(R.id.equal));
            assertNull(root.findViewById(R.id.copy));
            assertNull(root.findViewById(R.id.minus));
            EditText heightInput = root.findViewById(R.id.bmi_height_input);
            heightInput.getText().replace(0, heightInput.length(), "1234567890123");
            assertEquals("186", heightInput.getText().toString());
            heightInput.getText().append(".1234");
            assertEquals("186", heightInput.getText().toString());
        } finally { controller.pause().stop().destroy(); }
    }
    private String textTree(View view) {
        StringBuilder result = new StringBuilder(view instanceof TextView text ? text.getText() : "");
        if (view instanceof android.view.ViewGroup group) for (int i = 0; i < group.getChildCount(); i++)
            result.append('\n').append(textTree(group.getChildAt(i)));
        return result.toString();
    }
}
