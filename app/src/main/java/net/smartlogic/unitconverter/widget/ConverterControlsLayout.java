package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.smartlogic.unitconverter.R;

/** Shared converter chrome: retain typography and reflow secondary information when necessary. */
public final class ConverterControlsLayout extends LinearLayout {
    private final Paint measurementPaint = new Paint();
    private boolean compactHeading;

    public ConverterControlsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setCompactHeading(boolean compact) {
        compactHeading = compact;
        forceLayout();
    }

    private TextView rate() {
        TextView unit = findViewById(R.id.unit_rate);
        return unit != null ? unit : findViewById(R.id.txtRate);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        View header = findViewById(R.id.category_selector);
        View metadata = getChildAt(getChildCount() - 1);
        // Keep the reference's stacked tabs normally. Only the last constrained
        // viewport fallback puts icons beside labels to preserve usable key rows.
        if (header instanceof com.google.android.material.tabs.TabLayout tabs) {
            // Custom tab content owns its orientation. setInlineLabel() also
            // revives Material's default icon/text alongside that custom view.
            for (int i = 0; i < tabs.getTabCount(); i++) {
                View custom = tabs.getTabAt(i).getCustomView();
                if (custom instanceof LinearLayout item) {
                    item.setOrientation(compactHeading ? HORIZONTAL : VERTICAL);
                    View iconView = item.findViewById(R.id.converter_category_tab_icon);
                    if (iconView != null) {
                        LayoutParams iconParams = (LayoutParams) iconView.getLayoutParams();
                        iconParams.bottomMargin = compactHeading ? 0 : getResources().getDimensionPixelSize(R.dimen.converter_category_icon_label_gap);
                        iconParams.setMarginEnd(compactHeading ? getResources().getDimensionPixelSize(R.dimen.converter_small_spacing) : 0);
                    }
                }
            }
        }
        Paint status = measurementPaint;
        status.set(rate().getPaint());
        Paint.FontMetricsInt metrics = status.getFontMetricsInt();
        metadata.setMinimumHeight(Math.max(getResources().getDimensionPixelSize(
                R.dimen.converter_metadata_height), metrics.bottom - metrics.top
                + metrics.descent - metrics.ascent));
        status.setTextSize(getResources().getDimension(R.dimen.converter_category_text_size));
        metrics = status.getFontMetricsInt();
        int textHeight = metrics.bottom - metrics.top;
        int padding = 2 * getResources().getDimensionPixelSize(R.dimen.converter_small_spacing);
        int icon = getResources().getDimensionPixelSize(R.dimen.converter_flag_height);
        int contentHeight = compactHeading ? Math.max(icon, textHeight) + padding
                : icon + textHeight + padding + getResources().getDimensionPixelSize(R.dimen.converter_category_icon_label_gap);
        int headerHeight = Math.max(contentHeight, getResources().getDimensionPixelSize(compactHeading
                ? R.dimen.converter_touch_target : R.dimen.converter_category_header_height));
        header.setMinimumHeight(header instanceof ConverterValueRow
                ? getResources().getDimensionPixelSize(R.dimen.converter_touch_target) : headerHeight);
        if (!(header instanceof ConverterValueRow)) header.getLayoutParams().height = headerHeight;
        View[] rows = {findViewById(R.id.converter_source_section),
                findViewById(R.id.converter_target_section), header};
        int rowHeight = getResources().getDimensionPixelSize(R.dimen.converter_row_height);
        // Reset each probe so font/viewport compaction can reduce all rows together.
        for (View row : rows) {
            if (row instanceof ConverterValueRow) row.setMinimumHeight(rowHeight);
        }
        super.onMeasure(widthSpec, heightSpec);
        for (View row : rows) {
            if (row instanceof ConverterValueRow) rowHeight = Math.max(rowHeight, row.getMeasuredHeight());
        }
        boolean needsAlignment = false;
        for (View row : rows) {
            if (row instanceof ConverterValueRow) {
                needsAlignment |= row.getMeasuredHeight() != rowHeight;
                row.setMinimumHeight(rowHeight);
            }
        }
        if (needsAlignment) super.onMeasure(widthSpec, heightSpec);
    }
}
