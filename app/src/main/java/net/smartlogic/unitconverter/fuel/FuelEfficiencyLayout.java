package net.smartlogic.unitconverter.fuel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.widget.AutoSizeEditText;

/** Inputs stay anchored while the keypad replaces the centered result. */
public final class FuelEfficiencyLayout extends ViewGroup {
    private boolean wide, fits, open;
    private int inset, gap, controlsWidth, resultWidth, resultX, resultY;

    public FuelEfficiencyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean contentFits() {
        return fits;
    }

    @Override protected void onMeasure(int ws, int hs) {
        int width = MeasureSpec.getSize(ws), height = MeasureSpec.getSize(hs);
        open = getChildAt(2).getVisibility() != GONE;
        wide = width >= dim(R.dimen.bmi_wide_width) && width > height;
        int required = measureContent(width, height, false);
        if (required > height) required = measureContent(width, height, true);
        fits = required <= height;
        setMeasuredDimension(width, height | (fits ? 0 : MEASURED_STATE_TOO_SMALL));
    }

    private int measureContent(int width, int height, boolean compact) {
        inset = gap = dim(compact ? R.dimen.converter_compact_row_spacing : R.dimen.converter_spacing);
        int available = width - inset * 2;
        int columns = wide ? 2 : 1;
        int column = (available - gap * (columns - 1)) / columns;
        controlsWidth = Math.min(column, dim(R.dimen.bmi_controls_max_width));
        resultWidth = column;
        LinearLayout controls = (LinearLayout) getChildAt(0);
        setCompactValues(controls, compact);
        natural(controls, controlsWidth);
        View results = getChildAt(1);
        natural(results, resultWidth);
        int keysHeight = 0;
        natural(getChildAt(2), column);
        keysHeight = Math.max(dim(R.dimen.converter_touch_target) * 4, getChildAt(2).getMeasuredHeight());
        getChildAt(2).measure(MeasureSpec.makeMeasureSpec(column, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(keysHeight, MeasureSpec.EXACTLY));
        resultX = wide ? inset + column + gap : inset;
        int regionTop = wide ? inset : inset + controls.getMeasuredHeight() + gap;
        int regionHeight = Math.max(0, height - inset - regionTop);
        resultY = regionTop + Math.max(0, (regionHeight - results.getMeasuredHeight()) / 2);
        int regionRequired = open ? keysHeight : results.getMeasuredHeight();
        return inset * 2 + (wide ? Math.max(controls.getMeasuredHeight(), regionRequired)
                : controls.getMeasuredHeight() + gap + regionRequired);
    }

    private int dim(int id) {
        return getResources().getDimensionPixelSize(id);
    }

    private void natural(View child, int width) {
        child.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }

    private void setCompactValues(View root, boolean compact) {
        if (root instanceof AutoSizeEditText amount) {
            amount.setCompactValue(compact);
            amount.forceLayout();
        } else if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                setCompactValues(group.getChildAt(i), compact);
            }
        }
    }

    @Override protected void onLayout(boolean c, int l, int t, int r, int b) {
        int column = wide ? resultWidth : getMeasuredWidth() - inset * 2;
        place(getChildAt(0), inset + (column - controlsWidth) / 2, inset);
        place(getChildAt(1), resultX, resultY);
        if (open) {
            place(getChildAt(2), wide ? resultX : inset,
                    wide ? inset : getMeasuredHeight() - inset - getChildAt(2).getMeasuredHeight());
        }
    }

    private void place(View child, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            x = getMeasuredWidth() - x - child.getMeasuredWidth();
        }
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }

    @Override protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }
}
