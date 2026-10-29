package net.smartlogic.unitconverter.bmi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;

/** Reflow secondary labels instead of truncating calculated values. */
public final class BmiSummaryLayout extends ViewGroup {
    private boolean stacked;
    private boolean hasResult;
    private int gap, rowHeight;
    public BmiSummaryLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    void configure(boolean tight) { forceLayout(); }
    public void setHasResult(boolean value) { if (hasResult != value) { hasResult = value; requestLayout(); } }
    @Override protected void onMeasure(int ws, int hs) {
        int width = MeasureSpec.getSize(ws);
        gap = getResources().getDimensionPixelSize(R.dimen.converter_compact_row_spacing);
        for (int i = 0; i < 3; i++) getChildAt(i).setVisibility(hasResult ? VISIBLE : GONE);
        stacked = width < getResources().getDimensionPixelSize(R.dimen.bmi_summary_columns_width);
        int half = (width - gap) / 2;
        measure(getChildAt(0), stacked ? width : half);
        measure(getChildAt(1), stacked ? width : width - half - gap);
        measure(getChildAt(2), width);
        View message = getChildAt(3);
        message.setVisibility(((TextView) message).length() == 0 ? GONE : VISIBLE);
        measure(message, width);
        rowHeight = stacked ? getChildAt(0).getMeasuredHeight() + gap + getChildAt(1).getMeasuredHeight()
                : Math.max(getChildAt(0).getMeasuredHeight(), getChildAt(1).getMeasuredHeight());
        int height = rowHeight + gap + getChildAt(2).getMeasuredHeight();
        if (message.getVisibility() == VISIBLE) height += gap + message.getMeasuredHeight();
        setMeasuredDimension(width, height);
    }
    private void measure(View view, int width) {
        if (view.getVisibility() == GONE) {
            view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY));
            return;
        }
        view.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
    @Override protected void onLayout(boolean c, int l, int t, int r, int b) {
        place(getChildAt(0), 0, 0);
        place(getChildAt(1), stacked ? 0 : getChildAt(0).getMeasuredWidth() + gap,
                stacked ? getChildAt(0).getMeasuredHeight() + gap : 0);
        place(getChildAt(2), 0, rowHeight + gap);
        place(getChildAt(3), 0, rowHeight + gap * 2 + getChildAt(2).getMeasuredHeight());
    }
    private void place(View view, int x, int y) {
        if (view.getVisibility() == GONE) return;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - view.getMeasuredWidth();
        view.layout(x, y, x + view.getMeasuredWidth(), y + view.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
