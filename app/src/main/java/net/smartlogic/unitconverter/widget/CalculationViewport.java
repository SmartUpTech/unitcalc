package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;

/** Fixed workspace: never exposes a scrolling or clipped interactive surface. */
public final class CalculationViewport extends FrameLayout {
    private CalculationLayout content;
    private TextView spaceMessage;

    public CalculationViewport(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        content = (CalculationLayout) getChildAt(0);
        spaceMessage = new TextView(getContext());
        spaceMessage.setId(R.id.tool_space_message);
        spaceMessage.setTextAppearance(R.style.TextAppearance_Graphy_Body1);
        spaceMessage.setText(R.string.tool_more_space);
        spaceMessage.setGravity(Gravity.CENTER);
        int padding = getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        spaceMessage.setPadding(padding, padding, padding, padding);
        spaceMessage.setVisibility(GONE);
        spaceMessage.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        addView(spaceMessage, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = Math.max(0, MeasureSpec.getSize(heightMeasureSpec)
                - getPaddingTop() - getPaddingBottom());
        content.setViewportHeight(height);
        // INVISIBLE preserves state and remains measurable when a window grows again.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        boolean fits = content.fitsViewport();
        content.setVisibility(fits ? VISIBLE : INVISIBLE);
        spaceMessage.setVisibility(fits ? GONE : VISIBLE);
        if (!fits) {
            spaceMessage.measure(MeasureSpec.makeMeasureSpec(Math.max(0,
                            getMeasuredWidth() - getPaddingLeft() - getPaddingRight()), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }
}
