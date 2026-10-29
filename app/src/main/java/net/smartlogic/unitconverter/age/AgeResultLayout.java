package net.smartlogic.unitconverter.age;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;

/** One reference-style result panel. Reflows its sections only for short wide windows. */
public final class AgeResultLayout extends ViewGroup {
    private boolean sideBySide, fits;
    private boolean hasResult = true;
    private int inset, gap;
    public AgeResultLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public boolean contentFits() { return fits; }
    public void showResult(boolean value) {
        hasResult = value;
        for (int i = 0; i < 3; i++) getChildAt(i).setVisibility(value ? VISIBLE : GONE);
        getChildAt(3).setVisibility(value ? GONE : VISIBLE);
        requestLayout();
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int width = MeasureSpec.getSize(wSpec), height = MeasureSpec.getSize(hSpec);
        inset = getResources().getDimensionPixelSize(R.dimen.graphy_spacing_md);
        gap = getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        sideBySide = false;
        if (!hasResult) {
            natural(getChildAt(3), width - inset * 2);
            fits = getChildAt(3).getMeasuredHeight() + inset * 2 <= height;
            setMeasuredDimension(width, height | (fits ? 0 : MEASURED_STATE_TOO_SMALL));
            return;
        }
        textRoles(0);
        spacing(0);
        measureSections(width - inset * 2);
        if (requiredHeight() > height) {
            inset = gap = getResources().getDimensionPixelSize(R.dimen.converter_small_spacing);
            textRoles(1);
            spacing(1);
            measureSections(width - inset * 2);
        }
        if (requiredHeight() > height) {
            gap = getResources().getDimensionPixelSize(R.dimen.converter_compact_row_spacing);
            textRoles(2);
            measureSections(width - inset * 2);
        }
        if (requiredHeight() > height) {
            inset = gap = getResources().getDimensionPixelSize(R.dimen.converter_compact_row_spacing);
            textRoles(3);
            spacing(2);
            measureSections(width - inset * 2);
        }
        if (requiredHeight() > height && width >= getResources().getDimensionPixelSize(R.dimen.age_result_split_min_width)) {
            sideBySide = true;
            int sectionWidth = (width - inset * 2 - gap * 2 - line()) / 2;
            natural(getChildAt(0), sectionWidth);
            natural(getChildAt(2), sectionWidth);
        }
        fits = requiredHeight() <= height;
        setMeasuredDimension(width, resolveSizeAndState(Math.max(height, requiredHeight()), hSpec, fits ? 0 : MEASURED_STATE_TOO_SMALL));
    }
    private int line() { return getResources().getDimensionPixelSize(R.dimen.theme_outline); }
    private void spacing(int level) {
        int space = getResources().getDimensionPixelSize(level == 2 ? R.dimen.converter_compact_row_spacing
                : level == 1 ? R.dimen.converter_small_spacing : R.dimen.converter_spacing);
        // The next-birthday column contains its caption, decorative icon and date details.
        View icon = ((ViewGroup) findViewById(R.id.age_next_label).getParent()).getChildAt(1);
        ViewGroup.MarginLayoutParams iconParams = (ViewGroup.MarginLayoutParams) icon.getLayoutParams();
        int iconSize = getResources().getDimensionPixelSize(level == 2 ? R.dimen.timer_icon_medium
                : level == 1 ? R.dimen.timer_icon_large : R.dimen.converter_picker_row_height);
        iconParams.width = iconParams.height = iconSize;
        iconParams.topMargin = iconParams.bottomMargin = space;
        icon.setLayoutParams(iconParams);
        icon.setPadding(space, space, space, space);
        margin(findViewById(R.id.age_birthday_countdown), level == 2 ? 0
                : getResources().getDimensionPixelSize(R.dimen.converter_small_spacing), 0);
        margin(findViewById(R.id.age_summary_title), 0, space);
        margin(((ViewGroup) findViewById(R.id.age_summary)).getChildAt(1), 0, 0);
        margin(((ViewGroup) findViewById(R.id.age_summary)).getChildAt(2), space, 0);
        margin(findViewById(R.id.age_note), space, 0);
    }
    private void margin(View view, int top, int bottom) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.topMargin = top;
        params.bottomMargin = bottom;
        view.setLayoutParams(params);
    }
    private void textRoles(int density) {
        role(R.id.age_years, density >= 2 ? R.dimen.h1 : R.dimen.resultDisplaySize);
        int heading = density >= 3 ? R.dimen.graphy_label_text_size : R.dimen.graphy_value_text_size;
        role(R.id.age_title, heading);
        role(R.id.age_next_label, heading);
        role(R.id.age_summary_title, heading);
        for (int id : new int[]{R.id.age_total_years, R.id.age_total_months, R.id.age_total_weeks,
                R.id.age_total_days, R.id.age_total_hours, R.id.age_total_minutes}) role(id, density >= 2 ? R.dimen.graphy_value_text_size : R.dimen.age_summary_value_size);
    }
    private void role(int id, int dimen) {
        ((TextView) findViewById(id)).setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(dimen));
    }
    private void measureSections(int width) { natural(getChildAt(0), width); natural(getChildAt(2), width); }
    private void natural(View child, int width) {
        child.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
    private int requiredHeight() {
        return inset * 2 + (sideBySide ? Math.max(getChildAt(0).getMeasuredHeight(), getChildAt(2).getMeasuredHeight())
                : getChildAt(0).getMeasuredHeight() + getChildAt(2).getMeasuredHeight() + gap * 2 + line());
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View top = getChildAt(0), divider = getChildAt(1), summary = getChildAt(2);
        int height = b - t;
        if (!hasResult) {
            View empty = getChildAt(3);
            place(empty, inset, Math.max(inset, (height - empty.getMeasuredHeight()) / 2));
            return;
        }
        if (sideBySide) {
            int x = inset + top.getMeasuredWidth() + gap;
            place(top, inset, inset + Math.max(0, height - inset * 2 - top.getMeasuredHeight()) / 2);
            divider.layout(x, inset, x + line(), height - inset);
            place(summary, x + line() + gap, inset + Math.max(0, height - inset * 2 - summary.getMeasuredHeight()) / 2);
        } else {
            int extra = Math.max(0, height - requiredHeight());
            place(top, inset, inset + extra / 4);
            int y = inset + top.getMeasuredHeight() + gap + extra / 2;
            divider.layout(inset, y, r - l - inset, y + line());
            place(summary, inset, y + line() + gap + extra / 4);
        }
    }
    private void place(View child, int x, int y) {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL && sideBySide) x = getMeasuredWidth() - x - child.getMeasuredWidth();
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
}
