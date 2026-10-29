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
            assertEquals("6", ((EditText) root.findViewById(R.id.bmi_height_input)).getText().toString());
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
            assertEquals("25", ((EditText) root.findViewById(R.id.bmi_age_input)).getText().toString());
            assertEquals("", ((EditText) root.findViewById(R.id.bmi_weight_input)).getText().toString());
            assertEquals(View.VISIBLE, root.findViewById(R.id.bmi_inches_input).getVisibility());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4)); drain();
            try (DatabaseHelper db = new DatabaseHelper(app)) { assertEquals(1, db.getAllHistory().size()); }
            unit.putInt("selected", 0);
            for (String mode : new String[]{BmiDialogFragment.HEIGHT, BmiDialogFragment.WEIGHT}) {
                unit.putString("mode", mode); fragment.getChildFragmentManager().setFragmentResult(BmiDialogFragment.RESULT, unit);
            }
            root.findViewById(R.id.bmi_height_input).requestFocus();
            for (int id : new int[]{R.id.one, R.id.eight, R.id.six}) root.findViewById(id).performClick();
            root.findViewById(R.id.bmi_weight_input).requestFocus();
            for (int id : new int[]{R.id.seven, R.id.three}) root.findViewById(id).performClick();
            assertEquals("21.10", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            root.findViewById(R.id.backspace).performClick();
            assertEquals("7", ((EditText) root.findViewById(R.id.bmi_weight_input)).getText().toString());
            root.findViewById(R.id.ac).performClick();
            assertEquals("", ((EditText) root.findViewById(R.id.bmi_weight_input)).getText().toString());
            assertNotNull(root.findViewById(R.id.equal));
            root.findViewById(R.id.equal).performClick();
            assertFalse(fragment.isKeypadExpanded());
            root.findViewById(R.id.bmi_weight_input).performClick();
            assertTrue(fragment.isKeypadExpanded());
            activity.getOnBackPressedDispatcher().onBackPressed();
            assertFalse(fragment.isKeypadExpanded());
            assertNull(root.findViewById(R.id.copy));
            assertNull(root.findViewById(R.id.minus));
            EditText heightInput = root.findViewById(R.id.bmi_height_input);
            heightInput.getText().replace(0, heightInput.length(), "1234567890123");
            assertEquals("186", heightInput.getText().toString());
            heightInput.getText().append(".1234");
            assertEquals("186", heightInput.getText().toString());
            root.findViewById(R.id.bmi_gender_female).performClick();
            assertTrue(Preferences.getInstance(app).getBmiState().contains("\"gender\":\"FEMALE\""));
            ((EditText) root.findViewById(R.id.bmi_age_input)).setText("19");
            ((EditText) root.findViewById(R.id.bmi_weight_input)).setText("73");
            assertTrue(((TextView) root.findViewById(R.id.bmi_range_value)).getText().toString().contains("Reference BMI"));
            heightInput.performClick();
            heightInput.setText("123456789012");
            heightInput.setSelection(heightInput.length());
            root.findViewById(R.id.nine).performClick();
            assertEquals("123456789012", heightInput.getText().toString());
            heightInput.setText("186"); heightInput.setSelection(0, 3);
            root.findViewById(R.id.one).performClick();
            assertEquals("1", heightInput.getText().toString());
        } finally { controller.pause().stop().destroy(); }
    }
    @Test public void genderAndAgePersistAcrossRecreation() {
        Application app = RuntimeEnvironment.getApplication();
        Preferences.getInstance(app).setBmiState("");
        ThemeManager.init(app);
        var controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme); controller.setup().visible();
        try {
            var activity = controller.get();
            BmiCalculatorFragment fragment = new BmiCalculatorFragment();
            activity.getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "bmi").commitNow();
            View root = fragment.requireView();
            ((EditText) root.findViewById(R.id.bmi_age_input)).setText("30");
            root.findViewById(R.id.bmi_gender_female).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            String saved = Preferences.getInstance(app).getBmiState();
            assertTrue(saved.contains("\"age\":\"30\""));
            assertTrue(saved.contains("\"gender\":\"FEMALE\""));
            activity.getSupportFragmentManager().beginTransaction().remove(fragment).commitNow();
            fragment = new BmiCalculatorFragment();
            activity.getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "bmi").commitNow();
            root = fragment.requireView();
            assertEquals("30", ((EditText) root.findViewById(R.id.bmi_age_input)).getText().toString());
            assertEquals(R.id.bmi_gender_female, ((com.google.android.material.button.MaterialButtonToggleGroup) root.findViewById(R.id.bmi_gender_group)).getCheckedButtonId());
        } finally { controller.pause().stop().destroy(); }
    }
    @Test public void childMonthsReferenceSectionsAndKeypadSurviveRecreation() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        Preferences.getInstance(app).setBmiState(""); ThemeManager.init(app);
        var controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme); controller.setup().visible();
        try {
            BmiCalculatorFragment fragment = new BmiCalculatorFragment();
            controller.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "bmi").commitNow();
            View root = fragment.requireView();
            ((EditText) root.findViewById(R.id.bmi_age_input)).setText("9");
            ((EditText) root.findViewById(R.id.bmi_height_input)).setText("100");
            ((EditText) root.findViewById(R.id.bmi_weight_input)).setText("21.2");
            root.findViewById(R.id.bmi_gender_female).performClick();
            Bundle choice = new Bundle(); choice.putString("mode", BmiDialogFragment.MONTHS); choice.putInt("selected", 6);
            fragment.getChildFragmentManager().setFragmentResult(BmiDialogFragment.RESULT, choice);
            assertEquals("Overweight", ((TextView) root.findViewById(R.id.bmi_category_value)).getText().toString());
            assertFalse(((TextView) root.findViewById(R.id.bmi_range_value)).getText().toString().contains("kg"));
            root.findViewById(R.id.bmi_ranges).performClick();
            BmiDialogFragment popup = (BmiDialogFragment) fragment.getChildFragmentManager().findFragmentByTag(BmiDialogFragment.TAG);
            com.google.android.material.tabs.TabLayout tabs = popup.requireDialog().findViewById(R.id.bmi_reference_tabs);
            assertEquals(1, tabs.getSelectedTabPosition());
            String details = textTree(popup.requireDialog().getWindow().getDecorView());
            assertTrue(details, details.contains("9 years 6 months"));
            assertTrue(details, details.contains("Female"));
            assertTrue(details, details.contains("22.40"));
            tabs.selectTab(tabs.getTabAt(0));
            assertTrue(textTree(popup.requireDialog().getWindow().getDecorView()).contains("WHO"));
            tabs.selectTab(tabs.getTabAt(2));
            assertTrue(textTree(popup.requireDialog().getWindow().getDecorView()).contains("18.5"));
            popup.dismissNow();
            root.findViewById(R.id.bmi_weight_input).performClick();
            assertTrue(fragment.isKeypadExpanded());
            controller.recreate();
            fragment = (BmiCalculatorFragment) controller.get().getSupportFragmentManager().findFragmentByTag("bmi");
            root = fragment.requireView();
            assertTrue(fragment.isKeypadExpanded());
            assertTrue(((TextView) root.findViewById(R.id.bmi_age_months)).getText().toString().contains("6"));
            root.findViewById(R.id.equal).performClick();
            assertFalse(fragment.isKeypadExpanded());
            assertEquals("21.20", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            ((EditText) root.findViewById(R.id.bmi_age_input)).setText("");
            assertEquals("—", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            ((EditText) root.findViewById(R.id.bmi_age_input)).setText("1");
            assertEquals("—", ((TextView) root.findViewById(R.id.bmi_value)).getText().toString());
            assertTrue(((TextView) root.findViewById(R.id.bmi_message)).getText().toString().contains("WHO"));
            ((EditText) root.findViewById(R.id.bmi_age_input)).setText("20");
            assertEquals("Healthy weight", ((TextView) root.findViewById(R.id.bmi_category_value)).getText().toString());
        } finally { controller.pause().stop().destroy(); }
    }

    private String textTree(View view) {
        StringBuilder result = new StringBuilder(view instanceof TextView text ? text.getText() : "");
        if (view instanceof android.view.ViewGroup group) for (int i = 0; i < group.getChildCount(); i++)
            result.append('\n').append(textTree(group.getChildAt(i)));
        return result.toString();
    }
}
