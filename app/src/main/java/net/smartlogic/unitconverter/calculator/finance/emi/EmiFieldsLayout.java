package net.smartlogic.unitconverter.calculator.finance.emi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.smartlogic.unitconverter.R;

/** Reflows input rows into two columns in short wide windows, preserving labels. */
public final class EmiFieldsLayout extends ViewGroup {
    private boolean twoColumns;
    private boolean dense;
    public void setDenseSpacing(boolean value) { dense = value; }
    private int spacing() { return getResources().getDimensionPixelSize(dense ? R.dimen.converter_small_spacing : R.dimen.converter_spacing); }
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
                params.bottomMargin = value && !dense && j == 0 ? getResources().getDimensionPixelSize(R.dimen.converter_compact_row_spacing) : 0;
            }
        }
    }
    private boolean compactGrid;
    private final java.util.Map<Integer, CharSequence[]> labelAlternatives = new java.util.HashMap<>();
    public void setLabelAlternatives(int id, CharSequence normal, CharSequence compact) {
        labelAlternatives.put(id, new CharSequence[]{normal, compact});
        ((android.widget.TextView)findViewById(id)).setText(compactGrid ? compact : normal);
    }
    public boolean isCompactGrid() { return compactGrid; }
    public void setCompactGrid(boolean compact) {
        if (compactGrid == compact) return;
        compactGrid = compact;
        for (var entry : labelAlternatives.entrySet()) {
            ((android.widget.TextView)findViewById(entry.getKey())).setText(entry.getValue()[compact ? 1 : 0]);
        }
    }
    private java.util.List<View> visibleRows() {
        java.util.List<View> rows = new java.util.ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) if (getChildAt(i).getVisibility() != GONE) rows.add(getChildAt(i));
        return rows;
    }
    private int columns(int index) { return compactGrid && index == 0 ? 3 : twoColumns ? 2 : 1; }
    @Override protected void onMeasure(int ws, int hs) {
        int width = MeasureSpec.getSize(ws), gap = spacing();
        var rows = visibleRows();
        int height = 0;
        for (int i = 0; i < rows.size();) {
            int columns = columns(i), column = (width - gap * (columns - 1)) / columns;
            int rowHeight = 0;
            for (int j = i; j < Math.min(i + columns, rows.size()); j++) {
                View v = rows.get(j);
                v.measure(MeasureSpec.makeMeasureSpec(Math.max(0, column), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                rowHeight = Math.max(rowHeight, v.getMeasuredHeight());
            }
            height += rowHeight + (i > 0 ? gap : 0);
            i += columns;
        }
        setMeasuredDimension(width, height);
    }
    @Override protected void onLayout(boolean c, int l, int t, int r, int b) {
        var rows = visibleRows();
        int gap = spacing(), y = 0;
        for (int i = 0; i < rows.size();) {
            int columns = columns(i), rowHeight = 0;
            for (int j = i; j < Math.min(i + columns, rows.size()); j++) {
                View v = rows.get(j); int x = (j - i) * (v.getMeasuredWidth() + gap);
                if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - v.getMeasuredWidth();
                v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight()); rowHeight = Math.max(rowHeight, v.getMeasuredHeight());
            }
            y += rowHeight + gap;
            i += columns;
        }
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-1, -2); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
