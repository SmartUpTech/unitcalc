package net.smartlogic.unitconverter.bmi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.smartlogic.unitconverter.R;

/** Keep results below the gauge in portrait; put them beside it in short, wide windows. */
public final class BmiResultLayout extends ViewGroup {
    private boolean wide;
    private int gaugeHeight;
    private boolean tight;
    private int headerHeight;
    public BmiResultLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    void configure(boolean wide, int gaugeHeight, boolean compact, boolean tight) {
        this.wide = wide; this.gaugeHeight = gaugeHeight; this.tight = tight;
        int padding = getResources().getDimensionPixelSize(compact ? R.dimen.converter_compact_row_spacing : R.dimen.converter_spacing);
        setPadding(padding, padding, padding, padding);
        ((BmiSummaryLayout) getChildAt(1)).configure(tight);
        forceLayout();
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
        int gap = gap();
        View ranges = getChildAt(2);
        ranges.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        headerHeight = ranges.getMeasuredHeight() + gap;
        View gauge = getChildAt(0), summary = getChildAt(1);
        int gaugeWidth = wide ? width / 3 : width;
        summary.measure(MeasureSpec.makeMeasureSpec(wide ? width - gaugeWidth - gap : width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        View text = findViewById(R.id.bmi_gauge_text);
        text.measure(MeasureSpec.makeMeasureSpec(gaugeWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int minimum = ((ViewGroup) text).getChildAt(0).getVisibility() == GONE ? R.dimen.bmi_gauge_edit_height
                : tight ? R.dimen.bmi_gauge_tight_height : R.dimen.bmi_gauge_min_height;
        int minGauge = Math.max(getResources().getDimensionPixelSize(minimum),
                text.getMeasuredHeight() + getResources().getDimensionPixelSize(R.dimen.bmi_gauge_stroke));
        int gh = Math.max(minGauge, gaugeHeight);
        gauge.measure(MeasureSpec.makeMeasureSpec(gaugeWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(gh, MeasureSpec.EXACTLY));
        int contentHeight = wide ? Math.max(gh, headerHeight + summary.getMeasuredHeight()) : headerHeight + gh + gap + summary.getMeasuredHeight();
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), contentHeight + getPaddingTop() + getPaddingBottom());
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int padL = getPaddingLeft(), padT = getPaddingTop();
        int innerWidth = getMeasuredWidth() - padL - getPaddingRight();
        View gauge = getChildAt(0), summary = getChildAt(1);
        int gaugeWidth = wide ? innerWidth / 3 : innerWidth;
        int gap = gap();
        int gx = padL;
        if (wide && getLayoutDirection() == LAYOUT_DIRECTION_RTL) gx = padL + innerWidth - gaugeWidth;
        int gy = padT + (wide ? 0 : headerHeight);
        gauge.layout(gx, gy, gx + gaugeWidth, gy + gauge.getMeasuredHeight());
        int sx = wide ? padL + gaugeWidth + gap : padL;
        if (wide && getLayoutDirection() == LAYOUT_DIRECTION_RTL) sx = padL + innerWidth - gaugeWidth - gap - summary.getMeasuredWidth();
        int sy = wide ? padT + headerHeight : gy + gauge.getMeasuredHeight() + gap;
        summary.layout(sx, sy, sx + summary.getMeasuredWidth(), sy + summary.getMeasuredHeight());
        View ranges = getChildAt(2);
        int rx = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? padL : getMeasuredWidth() - getPaddingRight() - ranges.getMeasuredWidth();
        ranges.layout(rx, padT, rx + ranges.getMeasuredWidth(), padT + ranges.getMeasuredHeight());
    }
    private int gap() { return getResources().getDimensionPixelSize(tight ? R.dimen.converter_compact_row_spacing : R.dimen.converter_spacing); }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
