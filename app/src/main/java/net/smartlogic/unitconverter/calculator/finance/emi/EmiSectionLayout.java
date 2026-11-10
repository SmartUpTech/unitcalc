package net.smartlogic.unitconverter.calculator.finance.emi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.widget.ViewportContent;

/** Fixed header/body/footer; table/chart bodies consume exactly the remaining measured viewport. */
public final class EmiSectionLayout extends ViewGroup implements ViewportContent {
    private int inset, gap, leftWidth; private boolean fits, wide;
    public EmiSectionLayout(Context c, AttributeSet a) { super(c, a); }
    @Override public void setViewportHeight(int h) { }
    @Override public boolean fitsViewport() { return fits; }
    @Override protected void onMeasure(int ws, int hs) {
        int width = MeasureSpec.getSize(ws), height = MeasureSpec.getSize(hs);
        gap = getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        wide = width >= getResources().getDimensionPixelSize(R.dimen.emi_wide_min_width) && width > height;
        int content = Math.min(width - gap * 2, getResources().getDimensionPixelSize(R.dimen.emi_max_content_width) * (wide ? 2 : 1));
        inset = (width - content) / 2;
        leftWidth = wide ? (content - gap) / 2 : content;
        for (int i : new int[]{0, 2}) getChildAt(i).measure(MeasureSpec.makeMeasureSpec(Math.max(0, leftWidth), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int available = Math.max(0, wide ? height - gap * 2 : height - gap * 4 - getChildAt(0).getMeasuredHeight() - getChildAt(2).getMeasuredHeight());
        int bodyWidth = wide ? content - leftWidth - gap : content;
        getChildAt(1).measure(MeasureSpec.makeMeasureSpec(Math.max(0, bodyWidth), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY));
        fits = available > 0 && (!wide || getChildAt(0).getMeasuredHeight() + getChildAt(2).getMeasuredHeight() + gap * 3 <= height);
        ViewGroup body = (ViewGroup) getChildAt(1);
        if (body.getChildCount() > 0) {
            View child = body.getChildAt(0);
            if (child instanceof ViewportContent viewport) fits &= viewport.fitsViewport();
            else {
                child.measure(MeasureSpec.makeMeasureSpec(Math.max(0, bodyWidth), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                fits &= child.getMeasuredHeight() <= available;
            }
        }
        setMeasuredDimension(width, height | (fits ? 0 : MEASURED_STATE_TOO_SMALL));
    }
    @Override protected void onLayout(boolean c, int l, int t, int r, int b) {
        if (wide) {
            place(getChildAt(0), inset, gap);
            place(getChildAt(2), inset, getMeasuredHeight() - gap - getChildAt(2).getMeasuredHeight());
            place(getChildAt(1), inset + leftWidth + gap, gap);
            return;
        }
        int y = gap;
        for (int i = 0; i < 3; i++) { View v = getChildAt(i); if (i == 2) y = getMeasuredHeight() - gap - v.getMeasuredHeight(); place(v, inset, y); y += v.getMeasuredHeight() + gap; }
    }
    private void place(View v, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - v.getMeasuredWidth();
        v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-1, -2); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
