package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewParent;
import android.widget.LinearLayout;
import net.smartlogic.unitconverter.R;

/** Reserve a stable quarter of the viewport plus the space reclaimed beside swap. */
public final class ConverterValueRow extends LinearLayout {
    public ConverterValueRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBaselineAligned(false);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int available = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
        LayoutParams amount = (LayoutParams) getChildAt(0).getLayoutParams();
        LayoutParams picker = (LayoutParams) getChildAt(1).getLayoutParams();
        int screenWidth = 0;
        for (ViewParent parent = getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof CalculationLayout layout) {
                screenWidth = layout.getViewportWidth();
                break;
            }
        }
        if (screenWidth == 0) screenWidth = getResources().getDisplayMetrics().widthPixels;
        picker.width = Math.round(screenWidth * getResources().getFraction(
                R.fraction.converter_selector_screen_width, 1, 1))
                + getResources().getDimensionPixelSize(R.dimen.converter_selector_extra_width);
        amount.width = Math.max(0, available - picker.width - amount.leftMargin - amount.rightMargin);
        picker.weight = 0;
        amount.weight = 0;
        // Both bordered fields share the tallest value/selector's measured row.
        amount.height = picker.height = LayoutParams.MATCH_PARENT;
        super.onMeasure(widthSpec, heightSpec);
    }
}
