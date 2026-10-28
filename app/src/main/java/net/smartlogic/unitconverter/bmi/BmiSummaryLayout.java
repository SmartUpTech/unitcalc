package net.smartlogic.unitconverter.bmi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.smartlogic.unitconverter.R;

/** Keep the current category, range and difference visible in constrained display sizes. */
public final class BmiSummaryLayout extends ViewGroup {
    private boolean tight, hasResult;
    private int gap, firstRowHeight, leftWidth;
    public BmiSummaryLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    void configure(boolean tight) { this.tight = tight; forceLayout(); }
    public void setHasResult(boolean value) {
        if (hasResult != value) { hasResult = value; requestLayout(); }
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        gap = getResources().getDimensionPixelSize(R.dimen.converter_compact_row_spacing);
        leftWidth = (width - gap) / 2;
        View category = getChildAt(0), range = getChildAt(1), difference = getChildAt(2);
        measure(category, tight && hasResult ? leftWidth : width);
        measure(range, tight && !hasResult ? leftWidth : width);
        measure(difference, tight ? width - leftWidth - gap : width);
        firstRowHeight = tight && hasResult ? Math.max(category.getMeasuredHeight(), difference.getMeasuredHeight()) : category.getMeasuredHeight();
        int height = tight ? firstRowHeight + gap + (hasResult ? range.getMeasuredHeight()
                : Math.max(range.getMeasuredHeight(), difference.getMeasuredHeight()))
                : category.getMeasuredHeight() + range.getMeasuredHeight() + difference.getMeasuredHeight();
        range.setAccessibilityTraversalAfter(tight && hasResult ? difference.getId() : category.getId());
        difference.setAccessibilityTraversalAfter(tight && hasResult ? category.getId() : range.getId());
        setMeasuredDimension(width, height);
    }
    private void measure(View child, int width) {
        child.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View category = getChildAt(0), range = getChildAt(1), difference = getChildAt(2);
        place(category, 0, 0);
        if (tight && hasResult) {
            place(difference, leftWidth + gap, Math.max(0, (firstRowHeight - difference.getMeasuredHeight()) / 2));
            place(range, 0, firstRowHeight + gap);
        } else if (tight) {
            place(range, 0, firstRowHeight + gap);
            place(difference, leftWidth + gap, firstRowHeight + gap);
        } else {
            place(range, 0, firstRowHeight);
            place(difference, 0, firstRowHeight + range.getMeasuredHeight());
        }
    }
    private void place(View child, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - child.getMeasuredWidth();
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
