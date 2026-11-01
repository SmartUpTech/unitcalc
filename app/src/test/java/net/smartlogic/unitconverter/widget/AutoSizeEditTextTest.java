package net.smartlogic.unitconverter.widget;

import android.app.Application;
import android.util.TypedValue;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ContextThemeWrapper;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AutoSizeEditTextTest {
    @Test public void selectedUnitShrinksToEightBeforeWrappingAndRecovers() {
        UnitSelectionTextView view = new UnitSelectionTextView(new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.AppTheme), null);
        view.setText("12345678901234567890");
        measure(view, 100);
        assertEquals(1, view.getMaxLines());
        assertTrue(view.getTextSize() < 14f);
        measure(view, 20);
        assertEquals(8f, view.getTextSize(), 0.1f);
        assertEquals(2, view.getMaxLines());
        measure(view, 1000);
        assertEquals(14f, view.getTextSize(), 0.1f);
        assertEquals(1, view.getMaxLines());
    }

    @Test public void calculatorDisplaysShrinkAndGrow() {
        View root = LayoutInflater.from(new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
                R.style.AppTheme)).inflate(R.layout.pane_calculator_calculate, null);
        for (int id : new int[]{R.id.expression, R.id.result}) {
            TextView view = root.findViewById(id);
            // CalculationLayout normally reserves the result's maximum line height.
            if (id == R.id.result) view.setMinHeight(100);
            view.setText("1");
            measure(view, 160);
            float maximum = view.getTextSize();
            view.setText("12345678901234567890123456789012345678901234567890");
            measure(view, 160);
            assertTrue("display " + id + " maximum=" + maximum + " actual=" + view.getTextSize(),
                    view.getTextSize() < maximum);
            assertTrue(view.getTextSize() >= view.getAutoSizeMinTextSize());
            view.setText("1");
            measure(view, 160);
            assertEquals(maximum, view.getTextSize(), 0.1f);
        }
    }

    @Test public void amountShrinksThenRestoresWithoutChangingTextOrSelection() {
        AutoSizeEditText view = new AutoSizeEditText(new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.AppTheme), null);
        float maximum = view.getTextSize();
        view.setSingleLine(true);
        view.setText("1234567890123456789012345678901234567890");
        view.setSelection(5);
        measure(view, 100);
        float minimum = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8,
                view.getResources().getDisplayMetrics());
        assertEquals(minimum, view.getTextSize(), 0.1f);
        assertEquals(5, view.getSelectionStart());
        assertEquals("1234567890123456789012345678901234567890", view.getText().toString());
        view.setText("1");
        measure(view, 100);
        assertEquals(maximum, view.getTextSize(), 0.1f);
    }

    @Test public void amountRefitsWhenWidthChanges() {
        AutoSizeEditText view = new AutoSizeEditText(new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.AppTheme), null);
        float maximum = view.getTextSize();
        view.setText("1234567890123456");
        measure(view, 100);
        assertTrue(view.getTextSize() < maximum);
        measure(view, 1000);
        assertEquals(maximum, view.getTextSize(), 0.1f);
    }

    private void measure(View view, int width) {
        if (view.getLayoutParams() == null) view.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }
}
