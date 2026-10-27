package net.smartlogic.unitconverter.age;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.smartlogic.unitconverter.R;

/** Measures complete controls against the available pane; never scrolls or clips overflow. */
public final class AgeCalculateLayout extends ViewGroup {
    private boolean wide;
    private int gap, inset, leftWidth;
    private boolean fits;

    public AgeCalculateLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public boolean contentFits() { return fits; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec), height = MeasureSpec.getSize(heightSpec);
        int normal = getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        int compact = getResources().getDimensionPixelSize(R.dimen.converter_small_spacing);
        wide = false;
        inset = gap = normal;
        measureStack(width - inset * 2);
        if (stackHeight() + inset * 2 > height) {
            inset = gap = compact;
            measureStack(width - inset * 2);
        }
        if (stackHeight() + inset * 2 > height && width >= getResources().getDimensionPixelSize(R.dimen.calculation_pane_min_width) * 2) {
            wide = true;
            leftWidth = (width - inset * 2 - gap) / 2;
            measureNatural(getChildAt(0), leftWidth);
            measureNatural(getChildAt(1), leftWidth);
            measureNatural(getChildAt(2), width - inset * 2 - gap - leftWidth);
            measureNatural(getChildAt(3), width - inset * 2);
        }
        int required = wide ? Math.max(getChildAt(0).getMeasuredHeight() + gap + getChildAt(1).getMeasuredHeight(),
                getChildAt(2).getMeasuredHeight()) + gap + getChildAt(3).getMeasuredHeight() : stackHeight();
        fits = required + inset * 2 <= height;
        // Expose physical limits to QA; retain full measured controls, never fake a passing fit.
        setMeasuredDimension(width, resolveSizeAndState(Math.max(height, required + inset * 2), heightSpec,
                fits ? 0 : MEASURED_STATE_TOO_SMALL));
    }

    private void measureStack(int width) {
        for (int i = 0; i < getChildCount(); i++) measureNatural(getChildAt(i), width);
    }
    private void measureNatural(View child, int width) {
        child.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
    private int stackHeight() {
        int total = gap * (getChildCount() - 1);
        for (int i = 0; i < getChildCount(); i++) total += getChildAt(i).getMeasuredHeight();
        return total;
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        if (wide) {
            place(getChildAt(0), inset, inset);
            place(getChildAt(1), inset, inset + getChildAt(0).getMeasuredHeight() + gap);
            place(getChildAt(2), inset + leftWidth + gap, inset);
        } else {
            int y = inset;
            int free = Math.max(0, b - t - inset * 2 - stackHeight());
            int spread = free / (getChildCount() - 1);
            for (int i = 0; i < 3; i++) {
                View child = getChildAt(i);
                place(child, inset, y);
                y += child.getMeasuredHeight() + gap + spread;
            }
        }
        View actions = getChildAt(3);
        int contentBottom = wide ? Math.max(getChildAt(1).getBottom(), getChildAt(2).getBottom()) : getChildAt(2).getBottom();
        place(actions, inset, Math.max(contentBottom + gap, b - t - inset - actions.getMeasuredHeight()));
    }
    private void place(View child, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - child.getMeasuredWidth();
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
