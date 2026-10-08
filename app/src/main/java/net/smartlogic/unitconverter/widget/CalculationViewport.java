package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.core.widget.NestedScrollView;

/** Supplies the actual post-chrome viewport; scrolling is only an accessibility overflow fallback. */
public final class CalculationViewport extends NestedScrollView {
    public CalculationViewport(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFillViewport(false);
        setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() == 1 && getChildAt(0) instanceof CalculationLayout) {
            ((CalculationLayout) getChildAt(0)).setViewportHeight(
                    MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED ? 0
                            : Math.max(0, MeasureSpec.getSize(heightMeasureSpec)
                                    - getPaddingTop() - getPaddingBottom()));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
