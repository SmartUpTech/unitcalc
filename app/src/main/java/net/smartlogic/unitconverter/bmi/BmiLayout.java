package net.smartlogic.unitconverter.bmi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.smartlogic.unitconverter.R;

/** Measure after Activity insets/chrome. Never add scrolling or shrink keypad typography. */
public final class BmiLayout extends ViewGroup {
    private boolean wide, fits;
    private int inset, gap, contentWidth;
    public BmiLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public boolean contentFits() { return fits; }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec), height = MeasureSpec.getSize(heightSpec);
        wide = width >= getResources().getDimensionPixelSize(R.dimen.bmi_wide_width) && width > height;
        int required = measureContent(width, height, false);
        if (required > height) required = measureContent(width, height, true);
        fits = required <= height;
        setMeasuredDimension(width, resolveSizeAndState(Math.max(height, required), heightSpec, fits ? 0 : MEASURED_STATE_TOO_SMALL));
    }
    private int measureContent(int width, int height, boolean compact) {
        inset = gap = getResources().getDimensionPixelSize(compact ? R.dimen.converter_small_spacing : R.dimen.converter_spacing);
        contentWidth = wide ? (width - inset * 2 - gap) / 2 : width - inset * 2;
        View controls = getChildAt(0), actions = getChildAt(2), keypad = getChildAt(3);
        int padding = compact ? 0 : inset;
        keypad.setPadding(padding, padding, padding, padding);
        BmiResultLayout results = (BmiResultLayout) getChildAt(1);
        natural(controls, contentWidth); natural(actions, contentWidth);
        int touch = getResources().getDimensionPixelSize(R.dimen.converter_touch_target);
        int minimumKeys = 4 * touch + keypad.getPaddingTop() + keypad.getPaddingBottom();
        int preferredGauge = getResources().getDimensionPixelSize(R.dimen.bmi_gauge_height);
        results.configure(wide, getResources().getDimensionPixelSize(R.dimen.bmi_gauge_min_height));
        natural(results, contentWidth);
        int minimumContent = controls.getMeasuredHeight() + results.getMeasuredHeight() + actions.getMeasuredHeight() + gap * 2;
        int extra = Math.max(0, height - inset * 2 - minimumContent - (wide ? 0 : minimumKeys + gap));
        int growGauge = Math.min(extra / 2, preferredGauge - getResources().getDimensionPixelSize(R.dimen.bmi_gauge_min_height));
        results.configure(wide, getResources().getDimensionPixelSize(R.dimen.bmi_gauge_min_height) + growGauge);
        natural(results, contentWidth);
        int content = controls.getMeasuredHeight() + results.getMeasuredHeight() + actions.getMeasuredHeight() + gap * 2;
        int keysHeight = Math.max(minimumKeys, wide ? height - inset * 2 : height - inset * 2 - content - gap);
        int keysWidth = wide ? width - inset * 2 - gap - contentWidth : contentWidth;
        keypad.measure(MeasureSpec.makeMeasureSpec(keysWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(keysHeight, MeasureSpec.EXACTLY));
        int required = wide ? Math.max(content, keysHeight) : content + gap + keysHeight;
        return required + inset * 2;
    }
    private void natural(View child, int width) {
        child.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int y = inset;
        for (int i = 0; i < 3; i++) {
            View child = getChildAt(i);
            place(child, inset, y);
            y += child.getMeasuredHeight() + gap;
        }
        place(getChildAt(3), wide ? inset + contentWidth + gap : inset, wide ? inset : y);
    }
    private void place(View child, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - child.getMeasuredWidth();
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
