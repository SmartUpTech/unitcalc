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
    private boolean compactHeading;
    private int headingHeight;

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
        // Both modes reserve the same font-aware two-line status slot. Wrapped
        // timestamps must not move the keypad when switching converter modes.
        Paint status = new Paint(rate().getPaint());
        Paint.FontMetricsInt metrics = status.getFontMetricsInt();
        metadata.setMinimumHeight(Math.max(getResources().getDimensionPixelSize(
                R.dimen.converter_metadata_height), metrics.bottom - metrics.top
                + metrics.descent - metrics.ascent));
        if (!compactHeading) {
            super.onMeasure(widthSpec, heightSpec);
            return;
        }
        int width = MeasureSpec.getSize(widthSpec);
        LayoutParams headerParams = (LayoutParams) header.getLayoutParams();
        int available = Math.max(0, width - getPaddingLeft() - getPaddingRight()
                - headerParams.leftMargin - headerParams.rightMargin);
        int headerWidth = findViewById(R.id.refresh) == null ? available / 2
                : getResources().getDimensionPixelSize(R.dimen.converter_touch_target);
        header.measure(MeasureSpec.makeMeasureSpec(headerWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        metadata.measure(MeasureSpec.makeMeasureSpec(Math.max(0, available - headerWidth), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        headingHeight = Math.max(Math.max(header.getMeasuredHeight(), metadata.getMeasuredHeight()),
                rate().getLineHeight() * 2);
        View card = findViewById(R.id.converter_card);
        measureChildWithMargins(card, widthSpec, 0,
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0);
        LayoutParams cardParams = (LayoutParams) card.getLayoutParams();
        setMeasuredDimension(width, getPaddingTop() + getPaddingBottom() + headingHeight
                + card.getMeasuredHeight() + cardParams.topMargin + cardParams.bottomMargin);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!compactHeading) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        View header = getChildAt(0);
        View metadata = getChildAt(getChildCount() - 1);
        LayoutParams params = (LayoutParams) header.getLayoutParams();
        int start = getPaddingLeft() + params.leftMargin;
        int end = getWidth() - getPaddingRight() - params.rightMargin;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            layoutHeading(header, end - header.getMeasuredWidth());
            layoutHeading(metadata, start);
        } else {
            layoutHeading(header, start);
            layoutHeading(metadata, start + header.getMeasuredWidth());
        }
        View card = findViewById(R.id.converter_card);
        LayoutParams cardParams = (LayoutParams) card.getLayoutParams();
        int cardTop = getPaddingTop() + headingHeight + cardParams.topMargin;
        card.layout(getPaddingLeft() + cardParams.leftMargin, cardTop,
                getPaddingLeft() + cardParams.leftMargin + card.getMeasuredWidth(),
                cardTop + card.getMeasuredHeight());
    }

    private void layoutHeading(View child, int left) {
        int top = getPaddingTop() + (headingHeight - child.getMeasuredHeight()) / 2;
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
    }
}
