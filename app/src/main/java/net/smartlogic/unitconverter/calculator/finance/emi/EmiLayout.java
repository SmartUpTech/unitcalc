package net.smartlogic.unitconverter.calculator.finance.emi;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.widget.ViewportContent;

/** Inputs, both totals and keys coexist. Only optional actions leave the editing page. */
public final class EmiLayout extends ViewGroup implements ViewportContent {
    private boolean fits, wide;
    private int resultRowGap, compactResultRowGap;
    public void setResultRowSpacing(int regular, int compact) { resultRowGap = regular; compactResultRowGap = compact; requestLayout(); }
    private int inset, gap, column, summaryMargin;
    private int previousWidth = -1, previousHeight = -1, previousKeys = -1;
    private float previousFontScale;
    public EmiLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        resultRowGap = dim(R.dimen.converter_spacing);
        compactResultRowGap = dim(R.dimen.converter_compact_row_spacing);
    }
    @Override public void setViewportHeight(int height) { }
    @Override public boolean fitsViewport() { return fits; }
    @Override protected void onMeasure(int ws, int hs) {
        int width = MeasureSpec.getSize(ws), height = MeasureSpec.getSize(hs);
        wide = width >= dim(R.dimen.emi_wide_min_width) && width > height;
        EmiFieldsLayout fields = (EmiFieldsLayout)getChildAt(0);
        int keyVisibility = getChildAt(2).getVisibility();
        float fontScale = getResources().getConfiguration().fontScale;
        if (width != previousWidth || height != previousHeight || keyVisibility != previousKeys || fontScale != previousFontScale) {
            fields.setCompactGrid(false);
        }
        previousWidth = width; previousHeight = height; previousKeys = keyVisibility; previousFontScale = fontScale;
        fits = false;
        if (!fields.isCompactGrid()) {
            fits = measureContent(width, height, false, false);
            if (!fits) fits = measureContent(width, height, true, false);
            if (!fits) fits = measureContent(width, height, true, true);
        }
        if (!fits) { fields.setCompactGrid(true); fits = measureContent(width, height, true, false); }
        if (!fits) fits = measureContent(width, height, true, true);
        setMeasuredDimension(width, height | (fits ? 0 : MEASURED_STATE_TOO_SMALL));
    }
    private boolean measureContent(int width, int height, boolean compact, boolean dense) {
        inset = dim(dense ? R.dimen.converter_compact_row_spacing : R.dimen.converter_spacing);
        gap = dim(dense ? R.dimen.converter_compact_row_spacing : compact ? R.dimen.converter_small_spacing : R.dimen.converter_spacing);
        summaryMargin = dim(dense ? R.dimen.emi_summary_dense_margin
                : compact ? R.dimen.emi_summary_compact_margin : R.dimen.emi_summary_margin);
        int content = Math.min(width - inset * 2, dim(R.dimen.emi_max_content_width) * (wide ? 2 : 1));
        inset = (width - content) / 2;
        column = wide ? (content - gap) / 2 : content;
        ((EmiFieldsLayout) getChildAt(0)).setDenseSpacing(dense);
        ((EmiFieldsLayout) getChildAt(0)).setTwoColumns(wide || compact);
        android.widget.LinearLayout heading = findViewById(R.id.emi_result_heading);
        heading.setOrientation(compact ? android.widget.LinearLayout.HORIZONTAL : android.widget.LinearLayout.VERTICAL);
        for (int i = 0; i < heading.getChildCount(); i++) {
            var params = (android.widget.LinearLayout.LayoutParams) heading.getChildAt(i).getLayoutParams();
            params.width = compact ? LayoutParams.WRAP_CONTENT : LayoutParams.MATCH_PARENT;
            params.setMarginEnd(compact && i == 0 ? dim(R.dimen.converter_spacing) : 0);
            params.resolveLayoutDirection(getLayoutDirection());
        }
        android.widget.LinearLayout summary = (android.widget.LinearLayout)getChildAt(1);
        for (int i = 1; i < summary.getChildCount(); i++) {
            ((android.widget.LinearLayout.LayoutParams)summary.getChildAt(i).getLayoutParams()).topMargin = dense ? compactResultRowGap : resultRowGap;
        }
        TextView result = findViewById(R.id.emi_value);
        if (result != null) result.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(compact ? R.dimen.emi_result_compact_size : R.dimen.emi_result_size));
        TextView caption = findViewById(R.id.emi_value_label);
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.graphy_value_text_size));
        TextView guidance = findViewById(R.id.emi_message);
        guidance.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(dense ? R.dimen.h3_5 : R.dimen.graphy_value_text_size));
        for (int id : new int[]{R.id.emi_total_interest, R.id.emi_total_payment, R.id.investment_receipts}) {
            TextView supporting = findViewById(id);
            if (supporting != null) supporting.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(compact ? R.dimen.emi_summary_compact_text_size : R.dimen.emi_summary_text_size));
        }
        for (int i = 0; i < 4; i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            v.measure(MeasureSpec.makeMeasureSpec(Math.max(0, column), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }
        View keys = getChildAt(2);
        if (keys.getVisibility() != GONE) {
            int keyHeight = Math.max(dim(R.dimen.converter_touch_target) * 4, keys.getMeasuredHeight());
            keys.measure(MeasureSpec.makeMeasureSpec(Math.max(0, column), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(keyHeight, MeasureSpec.EXACTLY));
        }
        int left = getChildAt(0).getMeasuredHeight() + summaryMargin * 2 + getChildAt(1).getMeasuredHeight();
        int bottom = keys.getVisibility() != GONE ? keys.getMeasuredHeight() : getChildAt(3).getMeasuredHeight();
        return insetVertical() * 2 + (wide ? Math.max(left, bottom) : left + bottom) <= height;
    }
    private int insetVertical() { return gap; }
    private int dim(int id) { return getResources().getDimensionPixelSize(id); }
    private void place(View v, int x, int y) {
        if (v.getVisibility() == GONE) return;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - v.getMeasuredWidth();
        v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
    }
    @Override protected void onLayout(boolean c, int l, int t, int r, int b) {
        View inputs = getChildAt(0), summary = getChildAt(1), bottom = getChildAt(getChildAt(2).getVisibility() != GONE ? 2 : 3);
        place(inputs, inset, gap);
        int free = getMeasuredHeight() - gap * 2 - summaryMargin * 2
                - inputs.getMeasuredHeight() - summary.getMeasuredHeight() - bottom.getMeasuredHeight();
        place(summary, inset, gap + inputs.getMeasuredHeight() + summaryMargin + (wide ? 0 : Math.max(0, free / 2)));
        place(bottom, wide ? inset + column + gap : inset, getMeasuredHeight() - gap - bottom.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-1, -2); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
