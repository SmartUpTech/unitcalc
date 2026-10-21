package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import androidx.appcompat.widget.AppCompatTextView;

/** Keeps a wrap-content display tall enough to grow back to its autosize maximum. */
public final class AutoSizeValueTextView extends AppCompatTextView {
    public AutoSizeValueTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        requestLayout();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (getAutoSizeMaxTextSize() > 0 && getLayoutParams() != null
                && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            Paint probe = new Paint(getPaint());
            probe.setTextSize(getAutoSizeMaxTextSize());
            Paint.FontMetricsInt metrics = probe.getFontMetricsInt();
            int line = getIncludeFontPadding() ? metrics.bottom - metrics.top
                    : metrics.descent - metrics.ascent;
            setMinimumHeight(line + getCompoundPaddingTop() + getCompoundPaddingBottom());
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
