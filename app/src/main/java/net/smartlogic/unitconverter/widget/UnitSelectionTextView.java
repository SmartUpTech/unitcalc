package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import androidx.appcompat.widget.AppCompatTextView;
import net.smartlogic.unitconverter.R;

/** Shrink a selected unit to the minimum before allowing a second line. */
public final class UnitSelectionTextView extends AppCompatTextView {
    private final TypedValue maximumTextSize = new TypedValue();
    private final TextPaint measurementPaint = new TextPaint();
    public UnitSelectionTextView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        requestLayout();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        int width = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        if (width <= 0) return;
        float minimum = getResources().getInteger(R.integer.value_text_min_sp);
        float step = getResources().getInteger(R.integer.value_text_step_sp);
        // Selected unit text uses the shared 14sp role, independently of the amount size.
        TypedValue maximum = maximumTextSize;
        getResources().getValue(R.dimen.converter_unit_text_size, maximum, true);
        float size = TypedValue.complexToFloat(maximum.data);
        TextPaint probe = measurementPaint;
        probe.set(getPaint());
        CharSequence text = getText();
        boolean fits;
        while (true) {
            probe.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size,
                    getResources().getDisplayMetrics()));
            fits = probe.measureText(text, 0, text.length()) <= width;
            if (fits || size <= minimum) break;
            size = Math.max(minimum, size - step);
        }
        int lines = fits ? 1 : 2;
        if (getMaxLines() != lines) setMaxLines(lines);
        if (Math.abs(getTextSize() - probe.getTextSize()) > 0.01f) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, probe.getTextSize());
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
