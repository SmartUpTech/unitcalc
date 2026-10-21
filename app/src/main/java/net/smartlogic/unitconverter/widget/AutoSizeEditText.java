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
    private final float maximumSp;

    public AutoSizeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        maximumSp = Build.VERSION.SDK_INT >= 34
                ? TypedValue.deriveDimension(TypedValue.COMPLEX_UNIT_SP, getTextSize(),
                        getResources().getDisplayMetrics())
                : getTextSize() / getResources().getDisplayMetrics().scaledDensity;
    }

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
        TextPaint probe = new TextPaint(getPaint());
        CharSequence text = getText();
        if (getTransformationMethod() != null) {
            text = getTransformationMethod().getTransformation(text, this);
        }
        float size = maximumSp;
        while (true) {
            probe.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size,
                    getResources().getDisplayMetrics()));
            if (size <= minimum || probe.measureText(text, 0, text.length()) <= width) break;
            size = Math.max(minimum, size - step);
        }
        if (Math.abs(getTextSize() - probe.getTextSize()) > 0.01f) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, probe.getTextSize());
            super.onMeasure(widthSpec, heightSpec);
        }
    }
}
