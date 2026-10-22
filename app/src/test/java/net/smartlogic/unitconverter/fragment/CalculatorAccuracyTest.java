package net.smartlogic.unitconverter.fragment;

import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class CalculatorAccuracyTest {
    @Test public void sqrtTypingAndContinuationKeepCanonicalPrecision() {
        var prefs = Preferences.getInstance(RuntimeEnvironment.getApplication());
        prefs.getPreferences().edit().clear().putString(Preferences.PREFS_NUMBER_OF_DECIMALS, "2").commit();
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        var activity = controller.get();
        var container = new FrameLayout(activity);
        container.setId(12345);
        activity.setContentView(container);
        var fragment = new CalculatorFragment();
        activity.getSupportFragmentManager().beginTransaction().add(12345, fragment).commitNow();
        activity.getWindow().getDecorView().measure(
                android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY));
        activity.getWindow().getDecorView().layout(0, 0, 1080, 1920);
        shadowOf(Looper.getMainLooper()).idle();
        try {
            assertNotNull(ReflectionHelpers.getField(fragment, "tvResult"));
            click(fragment, R.id.sqrt); // Previously crashed immediately on "sqrt(".
            click(fragment, R.id.one);
            click(fragment, R.id.six);
            click(fragment, R.id.equal);
            TextView result = ReflectionHelpers.getField(fragment, "tvResult");
            assertEquals("4", result.getText().toString());
            click(fragment, R.id.minus);
            click(fragment, R.id.six);
            click(fragment, R.id.equal);
            assertEquals("-2", result.getText().toString());
            click(fragment, R.id.power);
            click(fragment, R.id.two);
            click(fragment, R.id.equal);
            assertEquals("4", result.getText().toString());
            ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
            click(fragment, R.id.one);
            click(fragment, R.id.divide);
            click(fragment, R.id.eight);
            click(fragment, R.id.equal);
            assertEquals("0.125", ReflectionHelpers.getField(fragment, "expression"));
            click(fragment, R.id.multiply);
            click(fragment, R.id.eight);
            click(fragment, R.id.equal);
            assertEquals("1", result.getText().toString());
            Bundle state = new Bundle();
            fragment.onSaveInstanceState(state);
            assertEquals("1", state.getString("calculator_expression"));
        } finally {
            controller.pause().stop().destroy();
        }
    }
    @Test public void expressionLimitRejectsWholeEditAndAllowsDeleteAndEvaluate() {
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        var activity = controller.get();
        var container = new FrameLayout(activity);
        container.setId(12345);
        activity.setContentView(container);
        var fragment = new CalculatorFragment();
        activity.getSupportFragmentManager().beginTransaction().add(12345, fragment).commitNow();
        activity.getWindow().getDecorView().measure(
                android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY));
        activity.getWindow().getDecorView().layout(0, 0, 1080, 1920);
        shadowOf(Looper.getMainLooper()).idle();
        try {
            String boundary = "1".repeat(30);
            for (int i = 0; i < 30; i++) click(fragment, R.id.one);
            assertEquals(boundary, ReflectionHelpers.getField(fragment, "expression"));
            TextView expression = ReflectionHelpers.getField(fragment, "tvExpression");
            assertFalse(expression.getText().toString().isEmpty());
            click(fragment, R.id.two);
            click(fragment, R.id.dot);
            click(fragment, R.id.sign_toggle);
            click(fragment, R.id.sqrt); // Reject sqrt and implicit multiplication atomically.
            assertEquals(boundary, ReflectionHelpers.getField(fragment, "expression"));
            click(fragment, R.id.backspace);
            assertEquals(29, ((String) ReflectionHelpers.getField(fragment, "expression")).length());
            click(fragment, R.id.plus);
            click(fragment, R.id.minus); // Replacing an operator is permitted at the limit.
            assertEquals("1".repeat(29) + "-", ReflectionHelpers.getField(fragment, "expression"));
            click(fragment, R.id.backspace);
            click(fragment, R.id.equal);
            assertTrue((boolean) ReflectionHelpers.getField(fragment, "isResultDisplayed"));
            click(fragment, R.id.two); // A new calculation starts normally after equals.
            assertEquals("2", ReflectionHelpers.getField(fragment, "expression"));
        } finally {
            controller.pause().stop().destroy();
        }
    }

    private void click(CalculatorFragment fragment, int id) {
        android.view.View button = fragment.requireView().findViewById(id);
        assertNotNull(button);
        button.performClick();
    }
}
