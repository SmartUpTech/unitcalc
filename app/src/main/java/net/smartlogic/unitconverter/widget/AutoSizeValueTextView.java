package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;

/** Keeps a wrap-content display tall enough to grow back to its autosize maximum. */
public final class AutoSizeValueTextView extends AppCompatTextView {
    private final Paint measurementPaint = new Paint();
    private final int styledMinimumHeight;
    public AutoSizeValueTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        styledMinimumHeight = getMinimumHeight();
    }

    @Override protected void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        requestLayout();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (TextViewCompat.getAutoSizeMaxTextSize(this) > 0 && getLayoutParams() != null
                && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            Paint probe = measurementPaint;
        probe.set(getPaint());
            probe.setTextSize(TextViewCompat.getAutoSizeMaxTextSize(this));
            Paint.FontMetricsInt metrics = probe.getFontMetricsInt();
            int line = getIncludeFontPadding() ? metrics.bottom - metrics.top
                    : metrics.descent - metrics.ascent;
            setMinimumHeight(Math.max(styledMinimumHeight,
                    line + getCompoundPaddingTop() + getCompoundPaddingBottom()));
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
