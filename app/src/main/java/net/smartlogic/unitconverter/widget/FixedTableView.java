package net.smartlogic.unitconverter.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import java.util.List;
import java.util.function.IntConsumer;

/** Measured pagination, never a scrolling or clipped table. Full values may wrap. */
public final class FixedTableView extends LinearLayout implements ViewportContent {
    private List<String> headers = java.util.Collections.emptyList();
    private List<List<String>> rows = java.util.Collections.emptyList();
    private int start, end, previousWidth = -1, previousHeight = -1;
    private boolean fits;
    private IntConsumer selected = i -> { };
    private Runnable changed = () -> { };
    public FixedTableView(Context c) { super(c); setOrientation(VERTICAL); }
    public void setData(List<String> headers, List<List<String>> rows, int start, IntConsumer selected, Runnable changed) {
        this.headers = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(headers)); this.rows = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(rows)); this.start = Math.max(0, Math.min(start, rows.size() - 1));
        this.selected = selected; this.changed = changed; previousWidth = -1; requestLayout();
    }
    public int start() { return start; }
    public int end() { return end; }
    public int count() { return rows.size(); }
    public void jump(int index) { start = Math.max(0, Math.min(index, rows.size() - 1)); previousWidth = -1; requestLayout(); }
    public void next() { if (end < rows.size()) jump(end); }
    public void previous() { jump(Math.max(0, start - Math.max(1, end - start))); }
    @Override public void setViewportHeight(int h) { }
    @Override public boolean fitsViewport() { return fits; }
    @Override protected void onMeasure(int ws, int hs) {
        int width = MeasureSpec.getSize(ws), height = MeasureSpec.getSize(hs);
        if (width != previousWidth || height != previousHeight) {
            previousWidth = width; previousHeight = height;
            removeAllViewsInLayout();
            int used = 0;
            if (!headers.isEmpty()) { View header = row(headers, -1); natural(header, width); used = header.getMeasuredHeight(); addViewInLayout(header, -1, new LayoutParams(-1, -2), true); }
            end = start;
            while (end < rows.size()) {
                View row = row(rows.get(end), end); natural(row, width);
                if (used + row.getMeasuredHeight() > height) break;
                used += row.getMeasuredHeight(); addViewInLayout(row, -1, new LayoutParams(-1, -2), true); end++;
            }
            fits = rows.isEmpty() || end > start;
            ThemeApplier.apply(this);
            post(changed);
        }
        super.onMeasure(ws, hs);
    }
    private void natural(View v, int width) { v.measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)); }
    private View row(List<String> cells, int index) {
        LinearLayout row = new LinearLayout(getContext()); row.setGravity(Gravity.CENTER_VERTICAL); row.setBaselineAligned(false);
        row.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.converter_touch_target));
        int gap = getResources().getDimensionPixelSize(R.dimen.converter_small_spacing);
        for (int i = 0; i < cells.size(); i++) {
            TextView cell = (TextView) android.view.LayoutInflater.from(getContext()).inflate(R.layout.emi_label, row, false);
            cell.setText(cells.get(i)); cell.setGravity(i == 0 ? Gravity.START | Gravity.CENTER_VERTICAL : Gravity.END | Gravity.CENTER_VERTICAL);
            cell.setPadding(gap, gap, gap, gap); cell.setTag(index < 0 ? "content_secondary" : "content_primary");
            row.addView(cell, new LayoutParams(0, -2, 1));
        }
        if (index >= 0) {
            row.setFocusable(true); row.setContentDescription(String.join("\n", cells));
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            row.setOnClickListener(v -> selected.accept(index));
        }
        return row;
    }
}
