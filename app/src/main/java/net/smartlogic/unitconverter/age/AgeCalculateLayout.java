package net.smartlogic.unitconverter.age;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.smartlogic.unitconverter.R;

/** Keeps the two date rows, shared result panel and bottom actions in one viewport. */
public final class AgeCalculateLayout extends ViewGroup {
    private boolean wide, fits;
    private int inset, gap, leftWidth;
    public AgeCalculateLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public boolean contentFits() { return fits; }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int width = MeasureSpec.getSize(wSpec), height = MeasureSpec.getSize(hSpec);
        inset = getResources().getDimensionPixelSize(R.dimen.graphy_spacing_md);
        gap = getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        wide = false;
        measurePortrait(width, height);
        if (!fits) {
            inset = gap = getResources().getDimensionPixelSize(R.dimen.converter_small_spacing);
            measurePortrait(width, height);
        }
        if (!fits && width >= getResources().getDimensionPixelSize(R.dimen.calculation_pane_min_width) * 2) {
            wide = true;
            leftWidth = (width - inset * 2 - gap) / 3;
            natural(getChildAt(0), leftWidth);
            natural(getChildAt(2), width - inset * 2);
            int available = Math.max(0, height - inset * 2 - gap - getChildAt(2).getMeasuredHeight());
            getChildAt(1).measure(MeasureSpec.makeMeasureSpec(width - inset * 2 - gap - leftWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY));
            fits = getChildAt(0).getMeasuredHeight() <= available && ((AgeResultLayout) getChildAt(1)).contentFits();
        }
        setMeasuredDimension(width, height | (fits ? 0 : MEASURED_STATE_TOO_SMALL));
    }
    private void measurePortrait(int width, int height) {
        int contentWidth = width - inset * 2;
        natural(getChildAt(0), contentWidth);
        natural(getChildAt(2), contentWidth);
        int available = Math.max(0, height - inset * 2 - gap * 2 - getChildAt(0).getMeasuredHeight() - getChildAt(2).getMeasuredHeight());
        getChildAt(1).measure(MeasureSpec.makeMeasureSpec(Math.max(0, contentWidth), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY));
        fits = ((AgeResultLayout) getChildAt(1)).contentFits();
    }
    private void natural(View child, int width) {
        child.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View inputs = getChildAt(0), card = getChildAt(1), actions = getChildAt(2);
        place(inputs, inset, inset);
        place(card, wide ? inset + leftWidth + gap : inset, wide ? inset : inset + inputs.getMeasuredHeight() + gap);
        place(actions, inset, b - t - inset - actions.getMeasuredHeight());
    }
    private void place(View child, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getMeasuredWidth() - x - child.getMeasuredWidth();
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
