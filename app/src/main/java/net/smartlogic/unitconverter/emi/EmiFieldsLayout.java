package net.smartlogic.unitconverter.emi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.smartlogic.unitconverter.R;

/** Reflows input rows into two columns in short wide windows, preserving labels. */
public final class EmiFieldsLayout extends ViewGroup {
    private boolean twoColumns;
    public EmiFieldsLayout(Context c, AttributeSet a) { super(c, a); }
    public void setTwoColumns(boolean value) {
        twoColumns = value;
        for (int i = 0; i < getChildCount(); i++) {
            android.widget.LinearLayout row = (android.widget.LinearLayout) getChildAt(i);
            row.setOrientation(value ? android.widget.LinearLayout.VERTICAL : android.widget.LinearLayout.HORIZONTAL);
            for (int j = 0; j < row.getChildCount(); j++) {
                var params = (android.widget.LinearLayout.LayoutParams) row.getChildAt(j).getLayoutParams();
                params.width = value ? LayoutParams.MATCH_PARENT : 0;
                params.weight = value ? 0 : 1;
            }
        }
    }
    @Override protected void onMeasure(int ws, int hs) {
        int width = MeasureSpec.getSize(ws), gap = getResources().getDimensionPixelSize(R.dimen.converter_small_spacing);
        int columns = twoColumns ? 2 : 1, column = (width - gap * (columns - 1)) / columns;
        int height = 0;
        for (int i = 0; i < getChildCount(); i += columns) {
            int rowHeight = 0;
            for (int j = i; j < Math.min(i + columns, getChildCount()); j++) {
                View v = getChildAt(j);
                v.measure(MeasureSpec.makeMeasureSpec(Math.max(0, column), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                rowHeight = Math.max(rowHeight, v.getMeasuredHeight());
            }
            height += rowHeight + (i > 0 ? gap : 0);
        }
        setMeasuredDimension(width, height);
    }
    @Override protected void onLayout(boolean c, int l, int t, int r, int b) {
        int columns = twoColumns ? 2 : 1, gap = getResources().getDimensionPixelSize(R.dimen.converter_small_spacing), y = 0;
        for (int i = 0; i < getChildCount(); i += columns) {
            int rowHeight = 0;
            for (int j = i; j < Math.min(i + columns, getChildCount()); j++) {
                View v = getChildAt(j); int x = (j - i) * (v.getMeasuredWidth() + gap);
                if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - v.getMeasuredWidth();
                v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight()); rowHeight = Math.max(rowHeight, v.getMeasuredHeight());
            }
            y += rowHeight + gap;
        }
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-1, -2); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
