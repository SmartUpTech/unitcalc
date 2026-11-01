package net.smartlogic.unitconverter.bmi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import net.smartlogic.unitconverter.R;

/** Inputs stay anchored while the keypad replaces the centered result. */
public final class BmiLayout extends ViewGroup {
    private boolean wide, fits, open;
    private int inset, gap, controlsWidth, resultWidth, resultX, resultY;
    public BmiLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public boolean contentFits() { return fits; }
    @Override protected void onMeasure(int ws, int hs) {
        int width = MeasureSpec.getSize(ws), height = MeasureSpec.getSize(hs);
        open = getChildAt(2).getVisibility() != GONE;
        wide = width >= dim(R.dimen.bmi_wide_width) && width > height;
        int required = measureContent(width, height, false, false);
        if (required > height) required = measureContent(width, height, true, false);
        if (required > height) required = measureContent(width, height, true, true);
        fits = required <= height;
        setMeasuredDimension(width, height | (fits ? 0 : MEASURED_STATE_TOO_SMALL));
    }
    private int measureContent(int width, int height, boolean compact, boolean tight) {
        inset = gap = dim(compact ? R.dimen.converter_compact_row_spacing : R.dimen.converter_spacing);
        int available = width - inset * 2;
        int columns = wide ? 2 : 1;
        int column = (available - gap * (columns - 1)) / columns;
        controlsWidth = Math.min(column, dim(R.dimen.bmi_controls_max_width));
        resultWidth = column;
        LinearLayout controls = (LinearLayout) getChildAt(0);
        // Units still identify these measurements; spoken field labels remain available.
        ((ViewGroup) findViewById(R.id.bmi_height_column)).getChildAt(0).setVisibility(tight ? GONE : VISIBLE);
        ((ViewGroup) findViewById(R.id.bmi_weight_column)).getChildAt(0).setVisibility(tight ? GONE : VISIBLE);
        ((ViewGroup) findViewById(R.id.bmi_gauge_text)).getChildAt(0).setVisibility(tight ? GONE : VISIBLE);
        LinearLayout demographics = (LinearLayout) controls.getChildAt(0);
        boolean stackDemographics = controlsWidth < dim(R.dimen.bmi_demographics_row_min_width);
        demographics.setOrientation(stackDemographics ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        for (int i = 0; i < demographics.getChildCount(); i++) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) demographics.getChildAt(i).getLayoutParams();
            params.width = stackDemographics ? LayoutParams.MATCH_PARENT : 0;
            params.weight = stackDemographics ? 0 : 1;
        }
        LinearLayout measurements = (LinearLayout) controls.getChildAt(1);
        boolean vertical = controlsWidth < dim(R.dimen.bmi_measurement_row_min_width);
        measurements.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        for (int i = 0; i < measurements.getChildCount(); i++) {
            View child = measurements.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
            params.width = vertical ? LayoutParams.MATCH_PARENT : 0;
            params.weight = vertical ? 0 : i == 0 && findViewById(R.id.bmi_inches_input).getVisibility() == VISIBLE ? 1.2f
                    : i == 1 && findViewById(R.id.bmi_inches_input).getVisibility() == VISIBLE ? 0.8f : 1f;
        }
        natural(controls, controlsWidth);
        BmiResultLayout results = (BmiResultLayout) getChildAt(1);
        // In a short portrait window, the arc and values share a row.
        boolean sideResult = compact && !wide;
        results.configure(sideResult, dim(compact ? R.dimen.bmi_gauge_tight_height : R.dimen.bmi_gauge_height), compact, compact);
        natural(results, resultWidth);
        int keysHeight = 0;
        {
            // Measure both modes so opening the keypad cannot rearrange inputs.
            // Respect enlarged key text as well as the shared minimum touch target.
            natural(getChildAt(2), column);
            keysHeight = Math.max(dim(R.dimen.converter_touch_target) * 4, getChildAt(2).getMeasuredHeight());
            getChildAt(2).measure(MeasureSpec.makeMeasureSpec(column, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(keysHeight, MeasureSpec.EXACTLY));
        }
        resultX = wide ? inset + column + gap : inset;
        int regionTop = wide ? inset : inset + controls.getMeasuredHeight() + gap;
        int regionHeight = Math.max(0, height - inset - regionTop);
        resultY = regionTop + Math.max(0, (regionHeight - results.getMeasuredHeight()) / 2);
        return inset * 2 + (wide ? Math.max(controls.getMeasuredHeight(), Math.max(keysHeight, results.getMeasuredHeight()))
                : controls.getMeasuredHeight() + gap + Math.max(results.getMeasuredHeight(), keysHeight));
    }
    private int dim(int id) { return getResources().getDimensionPixelSize(id); }
    private void natural(View child, int width) {
        child.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
    @Override protected void onLayout(boolean c, int l, int t, int r, int b) {
        int column = wide ? resultWidth : getMeasuredWidth() - inset * 2;
        place(getChildAt(0), inset + (column - controlsWidth) / 2, inset);
        place(getChildAt(1), resultX, resultY);
        if (open) place(getChildAt(2), wide ? resultX : inset,
                wide ? inset : getMeasuredHeight() - inset - getChildAt(2).getMeasuredHeight());
    }
    private void place(View child, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - child.getMeasuredWidth();
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
