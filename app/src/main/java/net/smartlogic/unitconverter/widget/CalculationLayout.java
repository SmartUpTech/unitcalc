package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Allocates the measured workspace to controls first, then equal keypad rows.
 * No screen/OEM breakpoints: measure normal spacing, compact spacing, then inline
 * selector/value rows before reducing keys to their font-aware touch minimum.
 * If even that cannot fit, report the unsupported viewport to the fixed host.
 */
public final class CalculationLayout extends LinearLayout {
    private int viewportHeight;
    private ViewGroup keypad;
    private View controls;
    private boolean sideBySide;
    private boolean fitsViewport;
    private final List<Spacing> spacing = new ArrayList<>();
    private final List<LinearLayout> sections = new ArrayList<>();
    private final List<View> currencyDetails = new ArrayList<>();

    public CalculationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setViewportHeight(int height) {
        if (viewportHeight != height) {
            viewportHeight = height;
            // The available viewport is also used during intrinsic-height probes.
            forceLayout();
        }
    }

    boolean fitsViewport() { return fitsViewport; }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        keypad = findViewById(R.id.keypad);
        controls = findViewById(R.id.calculation_controls);
        collect(this);
    }

    private void collect(View view) {
        if (view == keypad) return;
        // Compact structural whitespace, never drawable/text padding inside controls.
        if (view == this || view == controls || view.getId() == R.id.converter_card
                || view.getId() == R.id.category_selector || view.getId() == R.id.display_values) {
            spacing.add(new Spacing(view));
        }
        if (view instanceof LinearLayout && "calculation_section".equals(view.getTag())) {
            sections.add((LinearLayout) view);
        }
        if ("currency_detail".equals(view.getTag()) || view.getId() == R.id.fromCurrency
                || view.getId() == R.id.toCurrency) currencyDetails.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i));
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (keypad == null || viewportHeight == 0) {
            fitsViewport = false;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int minimumRow = Math.max(dimension(R.dimen.converter_touch_target), textHeight(keypad));
        int preferredRow = Math.max(dimension(R.dimen.converter_key_min_height), minimumRow);
        arrangePanes(false);
        configure(false, false);
        int needed = measureRequired(widthMeasureSpec, preferredRow);
        if (needed > viewportHeight) {
            configure(true, false);
            needed = measureRequired(widthMeasureSpec, preferredRow);
        }
        if (needed > viewportHeight && MeasureSpec.getSize(widthMeasureSpec)
                >= 2 * dimension(R.dimen.calculation_pane_min_width)) {
            arrangePanes(true);
            needed = measureRequired(widthMeasureSpec, preferredRow);
        }
        if (needed > viewportHeight && !sections.isEmpty() && canInlineSelectors()) {
            configure(true, true);
            needed = measureRequired(widthMeasureSpec, preferredRow);
        }
        if (needed > viewportHeight) needed = measureRequired(widthMeasureSpec, minimumRow);
        fitsViewport = needed <= viewportHeight;

        // The keypad's measured minimum is preserved; weight only distributes spare space.
        LayoutParams keys = (LayoutParams) keypad.getLayoutParams();
        keys.weight = 1;
        if (sideBySide) keys.height = LayoutParams.MATCH_PARENT;
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                viewportHeight, MeasureSpec.EXACTLY));
        fitsViewport = fitsViewport && keysFitWidth(keypad);
    }

    private boolean keysFitWidth(View view) {
        if (view.getVisibility() == GONE) return true;
        if (view instanceof android.widget.Button || view instanceof android.widget.ImageButton) {
            int minimum = dimension(R.dimen.converter_touch_target);
            if (view instanceof TextView) {
                TextView text = (TextView) view;
                minimum = Math.max(minimum, (int) Math.ceil(text.getPaint().measureText(
                        text.getText().toString())) + text.getCompoundPaddingLeft()
                        + text.getCompoundPaddingRight());
            }
            return view.getMeasuredWidth() >= minimum;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (!keysFitWidth(group.getChildAt(i))) return false;
            }
        }
        return true;
    }

    private int measureRequired(int widthSpec, int rowHeight) {
        LayoutParams keys = (LayoutParams) keypad.getLayoutParams();
        keys.weight = sideBySide ? 1 : 0;
        keys.height = keypad.getChildCount() * rowHeight
                + keypad.getPaddingTop() + keypad.getPaddingBottom();
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return getMeasuredHeight();
    }

    private void arrangePanes(boolean horizontal) {
        sideBySide = horizontal;
        setOrientation(horizontal ? HORIZONTAL : VERTICAL);
        LayoutParams header = (LayoutParams) controls.getLayoutParams();
        header.width = horizontal ? 0 : LayoutParams.MATCH_PARENT;
        header.weight = horizontal ? 1 : 0;
        LayoutParams keys = (LayoutParams) keypad.getLayoutParams();
        keys.width = horizontal ? 0 : LayoutParams.MATCH_PARENT;
    }

    private void configure(boolean compact, boolean inline) {
        for (Spacing item : spacing) item.apply(compact);
        // ISO codes still identify selected currencies; full names remain in their pickers
        // and the existing selector content descriptions. Keep flags at icon size.
        for (View detail : currencyDetails) detail.setVisibility(inline ? GONE : VISIBLE);
        for (int id : new int[]{R.id.fromFlag, R.id.toFlag}) {
            View flag = findViewById(id);
            if (flag != null) flag.getLayoutParams().width = dimension(inline
                    ? R.dimen.converter_flag_height : R.dimen.converter_flag_width);
        }
        for (LinearLayout section : sections) {
            section.setOrientation(inline ? HORIZONTAL : VERTICAL);
            section.setGravity(android.view.Gravity.CENTER_VERTICAL);
            for (int i = 0; i < section.getChildCount(); i++) {
                LayoutParams params = (LayoutParams) section.getChildAt(i).getLayoutParams();
                params.width = inline ? 0 : LayoutParams.MATCH_PARENT;
                params.weight = inline ? 1 : 0;
            }
        }
    }

    private boolean canInlineSelectors() {
        TextView iso = findViewById(R.id.fromCurrencyISO);
        TextView from = findViewById(R.id.converter_from_label);
        TextView to = findViewById(R.id.converter_to_label);
        float labelWidth = Math.max(from.getPaint().measureText(from.getText().toString()),
                to.getPaint().measureText(to.getText().toString()));
        int needed = (int) Math.ceil(labelWidth)
                + dimension(R.dimen.converter_spacing)
                + dimension(R.dimen.converter_touch_target)
                + dimension(R.dimen.converter_chevron_size);
        if (iso != null) {
            TextView toIso = findViewById(R.id.toCurrencyISO);
            float codeWidth = Math.max(iso.getPaint().measureText(iso.getText().toString()),
                    toIso.getPaint().measureText(toIso.getText().toString()));
            needed = (int) Math.ceil(labelWidth + codeWidth)
                    + dimension(R.dimen.converter_flag_height)
                + dimension(R.dimen.converter_chevron_size)
                + 3 * dimension(R.dimen.converter_spacing);
        }
        for (LinearLayout section : sections) {
            if (section.getMeasuredWidth() / 2 < needed) return false;
        }
        return true;
    }

    private int dimension(int resource) { return getResources().getDimensionPixelSize(resource); }

    private int textHeight(View view) {
        int height = 0;
        if (view instanceof TextView && view.getVisibility() != GONE) {
            TextView text = (TextView) view;
            android.graphics.Paint.FontMetricsInt metrics = text.getPaint().getFontMetricsInt();
            height = metrics.bottom - metrics.top + text.getCompoundPaddingTop()
                    + text.getCompoundPaddingBottom();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                height = Math.max(height, textHeight(group.getChildAt(i)));
            }
        }
        return height;
    }

    /** Preserve horizontal gutters and restore original spacing on every resize. */
    private static final class Spacing {
        final View view;
        final int left, top, right, bottom, marginTop, marginBottom;
        Spacing(View view) {
            this.view = view;
            left = view.getPaddingLeft(); top = view.getPaddingTop();
            right = view.getPaddingRight(); bottom = view.getPaddingBottom();
            ViewGroup.LayoutParams params = view.getLayoutParams();
            marginTop = params instanceof MarginLayoutParams ? ((MarginLayoutParams) params).topMargin : 0;
            marginBottom = params instanceof MarginLayoutParams ? ((MarginLayoutParams) params).bottomMargin : 0;
        }
        void apply(boolean compact) {
            view.setPadding(left, compact ? 0 : top, right, compact ? 0 : bottom);
            if (view.getLayoutParams() instanceof MarginLayoutParams) {
                MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
                params.topMargin = compact ? 0 : marginTop;
                params.bottomMargin = compact ? 0 : marginBottom;
            }
        }
    }
}
