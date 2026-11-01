package net.smartlogic.unitconverter.graphy.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.widget.ViewportContent;

/** Non-scrolling chart with keyboard/TalkBack period selection and a textual data equivalent. */
public final class ValueChartView extends View implements ViewportContent {
    private final ValueChartData data;
    private int selected;
    public ValueChartView(Context c, ValueChartData data) {
        super(c); this.data = data; setFocusable(true); setClickable(true);
        setContentDescription(data.description() + c.getString(R.string.emi_chart_help));
        setOnClickListener(v -> select(selected + 1));
    }
    @Override public void setViewportHeight(int h) { }
    @Override public boolean fitsViewport() {
        return getMeasuredHeight() >= Math.max(getResources().getDimensionPixelSize(R.dimen.converter_touch_target) * 2,
                getResources().getDimension(R.dimen.graphy_label_text_size) * 9);
    }
    @Override protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), resolveSize(getResources().getDimensionPixelSize(R.dimen.emi_chart_height), h));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int gap = getResources().getDimensionPixelSize(R.dimen.converter_spacing);
        ValueChartRenderer.draw(getContext(), canvas, new RectF(gap, gap, getWidth() - gap, getHeight() - gap), data, ThemeManager.get(), getResources().getDimension(R.dimen.graphy_label_text_size));
    }
    private void select(int index) {
        int max = data.series().stream().mapToInt(s -> s.points().size()).max().orElse(0);
        if (max == 0) return;
        selected = Math.floorMod(index, max); StringBuilder text = new StringBuilder();
        for (var s : data.series()) if (selected < s.points().size()) text.append(s.label()).append(' ').append(s.points().get(selected).label()).append('\n');
        setContentDescription(text); announceForAccessibility(text);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            int max = data.series().stream().mapToInt(s -> s.points().size()).max().orElse(0);
            select(Math.round(Math.max(0, Math.min(1, e.getX() / Math.max(1, getWidth()))) * Math.max(0, max - 1)) - 1);
        }
        return super.onTouchEvent(e);
    }
    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) { select(selected + (key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1)); return true; }
        return super.onKeyDown(key, event);
    }
}
