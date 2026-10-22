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
        View title = findViewById(R.id.exchange_rate_title);
        if (title != null) title.setVisibility(compact ? GONE : VISIBLE);
        forceLayout();
    }

    private TextView rate() {
        TextView unit = findViewById(R.id.unit_rate);
        return unit != null ? unit : findViewById(R.id.txtRate);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        View header = getChildAt(0);
        View metadata = getChildAt(getChildCount() - 1);
        // Both modes use identical font-aware heading/metadata allocations. Compact
        // category tabs put icons beside labels instead of narrowing the strip.
        if (header instanceof com.google.android.material.tabs.TabLayout tabs) {
            tabs.setInlineLabel(compactHeading);
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
        int padding = 2 * getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        int icon = getResources().getDimensionPixelSize(R.dimen.converter_flag_height);
        int contentHeight = compactHeading ? Math.max(icon, textHeight) + padding
                : icon + textHeight + padding + getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        header.setMinimumHeight(Math.max(contentHeight, getResources().getDimensionPixelSize(compactHeading
                ? R.dimen.converter_touch_target : R.dimen.converter_category_header_height)));
        super.onMeasure(widthSpec, heightSpec);
    }
}
