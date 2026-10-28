package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatEditText;

import net.smartlogic.unitconverter.R;

/** Single-line amount editor; refits from its styled maximum on every measurement. */
public final class AutoSizeEditText extends AppCompatEditText {
    private final TextPaint measurementPaint = new TextPaint();
    private float maximumSp;
    private final float styledMaximumSp;
    private final int styledMinimumHeight;

    public AutoSizeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        maximumSp = Build.VERSION.SDK_INT >= 34
                ? TypedValue.deriveDimension(TypedValue.COMPLEX_UNIT_SP, getTextSize(),
                        getResources().getDisplayMetrics())
                : getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        styledMaximumSp = maximumSp;
        styledMinimumHeight = getMinimumHeight();
    }

    void setCompactValue(boolean compact) {
        float compactSp = getResources().getInteger(R.integer.converter_compact_value_max_sp);
        float maximum = compact ? Math.min(styledMaximumSp, compactSp) : styledMaximumSp;
        if (maximumSp != maximum) {
            maximumSp = maximum;
            forceLayout();
        }
    }

    @Override protected void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        requestLayout();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        // The row's geometry belongs to the maximum text role, not the current
        // fitted size. Recompute from the current typeface/scaling and padding so
        // resizing, deleting and clearing cannot pull the keypad upward.
        TextPaint probe = measurementPaint;
        probe.set(getPaint());
        probe.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, maximumSp,
                getResources().getDisplayMetrics()));
        android.graphics.Paint.FontMetricsInt metrics = probe.getFontMetricsInt();
        int lineHeight = getIncludeFontPadding() ? metrics.bottom - metrics.top
                : metrics.descent - metrics.ascent;
        setMinimumHeight(Math.max(styledMinimumHeight,
                lineHeight + getCompoundPaddingTop() + getCompoundPaddingBottom()));
        super.onMeasure(widthSpec, heightSpec);
        int width = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        if (width <= 0) return;
        float minimum = getResources().getInteger(R.integer.value_text_min_sp);
        float step = getResources().getInteger(R.integer.value_text_step_sp);
        CharSequence text = getText();
        if (getTransformationMethod() != null) {
            text = getTransformationMethod().getTransformation(text, this);
        }
        float size = maximumSp;
        while (true) {
            probe.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size,
                    getResources().getDisplayMetrics()));
            boolean fits = probe.measureText(text, 0, text.length()) <= width;
            if (size <= minimum || fits) break;
            size = Math.max(minimum, size - step);
        }
        if (Math.abs(getTextSize() - probe.getTextSize()) > 0.01f) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, probe.getTextSize());
            super.onMeasure(widthSpec, heightSpec);
        }
    }
}
