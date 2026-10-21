package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewParent;
import android.widget.LinearLayout;
import net.smartlogic.unitconverter.R;

/** Reserve a stable quarter of the tool viewport for the selected unit/currency. */
public final class ConverterValueRow extends LinearLayout {
    public ConverterValueRow(Context context, AttributeSet attrs) { super(context, attrs); }

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
                R.fraction.converter_selector_screen_width, 1, 1));
        amount.width = Math.max(0, available - picker.width - amount.leftMargin - amount.rightMargin);
        picker.weight = 0;
        amount.weight = 0;
        super.onMeasure(widthSpec, heightSpec);
    }
}
