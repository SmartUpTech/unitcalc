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
    public BmiResultLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    void configure(boolean wide, int gaugeHeight) {
        this.wide = wide; this.gaugeHeight = gaugeHeight; forceLayout();
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int gap = getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        View gauge = getChildAt(0), summary = getChildAt(1);
        int gaugeWidth = wide ? width / 3 : width;
        summary.measure(MeasureSpec.makeMeasureSpec(wide ? width - gaugeWidth - gap : width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        View text = findViewById(R.id.bmi_gauge_text);
        text.measure(MeasureSpec.makeMeasureSpec(gaugeWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int minGauge = Math.max(getResources().getDimensionPixelSize(R.dimen.bmi_gauge_min_height),
                text.getMeasuredHeight() + getResources().getDimensionPixelSize(R.dimen.bmi_gauge_stroke) * 2);
        int gh = Math.max(minGauge, gaugeHeight);
        gauge.measure(MeasureSpec.makeMeasureSpec(gaugeWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(gh, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, wide ? Math.max(gh, summary.getMeasuredHeight()) : gh + gap + summary.getMeasuredHeight());
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View gauge = getChildAt(0), summary = getChildAt(1);
        place(gauge, 0, 0);
        place(summary, wide ? gauge.getMeasuredWidth() + getResources().getDimensionPixelSize(R.dimen.converter_spacing) : 0,
                wide ? Math.max(0, (getMeasuredHeight() - summary.getMeasuredHeight()) / 2) : gauge.getMeasuredHeight() + getResources().getDimensionPixelSize(R.dimen.converter_spacing));
    }
    private void place(View child, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - child.getMeasuredWidth();
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
