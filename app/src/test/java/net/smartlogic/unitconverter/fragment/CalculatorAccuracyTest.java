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
            click(fragment, R.id.one);
            click(fragment, R.id.six);
            click(fragment, R.id.sqrt);
            assertEquals("sqrt(16)", ReflectionHelpers.getField(fragment, "expression"));
            TextView expressionView = ReflectionHelpers.getField(fragment, "tvExpression");
            assertFalse(expressionView.getText().toString().contains("("));
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
            click(fragment, R.id.sqrt); // Reject the complete root wrapping atomically.
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

    @Test public void squareRootStandardScenarios() {
        var prefs = Preferences.getInstance(RuntimeEnvironment.getApplication());
        prefs.getPreferences().edit().clear().putString(Preferences.PREFS_NUMBER_OF_DECIMALS, "8").commit();
        var controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        var activity = controller.get();
        var container = new FrameLayout(activity);
        container.setId(12345);
        activity.setContentView(container);
        var fragment = new CalculatorFragment();
        activity.getSupportFragmentManager().beginTransaction().add(12345, fragment).commitNow();
        shadowOf(Looper.getMainLooper()).idle();
        try {
            // Exercise actual keypad clicks for whole, decimal, small and irrational roots.
            rootCase(fragment, new int[]{R.id.zero}, "0");
            rootCase(fragment, new int[]{R.id.one}, "1");
            rootCase(fragment, new int[]{R.id.nine}, "3");
            rootCase(fragment, new int[]{R.id.one, R.id.four, R.id.four}, "12");
            rootCase(fragment, new int[]{R.id.two, R.id.dot, R.id.two, R.id.five}, "1.5");
            rootCase(fragment, new int[]{R.id.zero, R.id.dot, R.id.zero, R.id.four}, "0.2");
            rootCase(fragment, new int[]{R.id.two}, "1.41421356");

            int[] operators = {R.id.plus, R.id.minus, R.id.multiply, R.id.divide, R.id.power};
            String[] continued = {"5", "1", "6", "1.5", "9"};
            for (int i = 0; i < operators.length; i++) {
                ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
                click(fragment, R.id.nine); click(fragment, R.id.sqrt);
                click(fragment, operators[i]); click(fragment, R.id.two);
                click(fragment, R.id.equal);
                TextView value = ReflectionHelpers.getField(fragment, "tvResult");
                // 3+2, 3-2, 3×2, 3÷2, 3².
                assertEquals(continued[i], value.getText().toString());
            }

            ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
            click(fragment, R.id.two); click(fragment, R.id.plus);
            click(fragment, R.id.nine); click(fragment, R.id.sqrt);
            assertExpressionResult(fragment, "2+sqrt(9)", "5");
            click(fragment, R.id.equal); click(fragment, R.id.sqrt);
            assertExpressionResult(fragment, "sqrt(5)", "2.23606798");

            ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
            click(fragment, R.id.two); click(fragment, R.id.five); click(fragment, R.id.six);
            click(fragment, R.id.sqrt); click(fragment, R.id.sqrt);
            assertExpressionResult(fragment, "sqrt(sqrt(256))", "4");

            ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
            click(fragment, R.id.parenthesis_open); click(fragment, R.id.nine);
            click(fragment, R.id.plus); click(fragment, R.id.seven);
            click(fragment, R.id.parenthesis_close); click(fragment, R.id.sqrt);
            assertExpressionResult(fragment, "sqrt((9+7))", "4");

            ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
            click(fragment, R.id.two); click(fragment, R.id.five); click(fragment, R.id.percent);
            click(fragment, R.id.sqrt);
            assertExpressionResult(fragment, "sqrt(25%)", "0.5");

            ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
            click(fragment, R.id.sqrt);
            assertEquals("", ReflectionHelpers.getField(fragment, "expression"));
            click(fragment, R.id.nine); click(fragment, R.id.plus); click(fragment, R.id.sqrt);
            assertEquals("9+", ReflectionHelpers.getField(fragment, "expression"));

            ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
            click(fragment, R.id.nine); click(fragment, R.id.sign_toggle); click(fragment, R.id.sqrt);
            assertEquals("sqrt(-9)", ReflectionHelpers.getField(fragment, "expression"));
            assertFalse(net.smartlogic.unitconverter.utils.ExpressionEvaluator.evaluate("sqrt(-9)").isValid());
            TextView result = ReflectionHelpers.getField(fragment, "tvResult");
            assertEquals("", result.getText().toString());
            assertNull(ReflectionHelpers.getField(fragment, "latestGraphyOutput"));
            click(fragment, R.id.equal);
            assertEquals("", result.getText().toString());
        } finally {
            controller.pause().stop().destroy();
        }
    }

    private void rootCase(CalculatorFragment fragment, int[] digits, String expected) {
        ReflectionHelpers.callInstanceMethod(fragment, "clearAll");
        for (int id : digits) click(fragment, id);
        click(fragment, R.id.sqrt);
        TextView result = ReflectionHelpers.getField(fragment, "tvResult");
        assertEquals(expected, result.getText().toString());
        click(fragment, R.id.equal);
        assertEquals(expected, result.getText().toString());
    }

    private void assertExpressionResult(CalculatorFragment fragment, String expectedExpression, String expectedResult) {
        assertEquals(expectedExpression, ReflectionHelpers.getField(fragment, "expression"));
        TextView result = ReflectionHelpers.getField(fragment, "tvResult");
        assertEquals(expectedResult, result.getText().toString());
    }

    private void click(CalculatorFragment fragment, int id) {
        android.view.View button = fragment.requireView().findViewById(id);
        assertNotNull(button);
        button.performClick();
    }
}
