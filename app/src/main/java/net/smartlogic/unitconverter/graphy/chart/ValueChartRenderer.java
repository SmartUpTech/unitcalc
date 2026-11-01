package net.smartlogic.unitconverter.graphy.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.AppearanceTypography;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import java.math.BigDecimal;

/** Shared plotting/legend styles for native UI and white-paper PDF output. */
public final class ValueChartRenderer {
    private ValueChartRenderer() { }
    public static void draw(Context context, Canvas canvas, RectF bounds, ValueChartData data, CalculatorTheme theme, float textSize) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setTypeface(AppearanceTypography.typeface(context)); p.setTextSize(textSize);
        float gap = textSize, top = bounds.top + gap, bottom = bounds.bottom - gap * 6;
        if (bottom <= top || bounds.width() <= 0) return;
        BigDecimal max = BigDecimal.ZERO;
        for (var s : data.series()) for (var point : s.points()) max = max.max(point.value());
        if (max.signum() <= 0) max = BigDecimal.ONE;
        int index = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (var s : data.series()) if (!s.points().isEmpty()) sum = sum.add(s.points().get(0).value());
        float plotLeft = bounds.left;
        if (data.kind() == ValueChartData.Kind.LINE) {
            plotLeft += Math.max(p.measureText(data.yMax()), p.measureText(data.yZero())) + gap;
            p.setColor(theme.secondaryContentColor());
            canvas.drawText(data.yMax(), bounds.left, top + textSize, p);
            canvas.drawText(data.yZero(), bounds.left, bottom, p);
            canvas.drawText(data.xStart(), plotLeft, bottom + gap * 1.5f, p);
            canvas.drawText(data.xEnd(), Math.max(plotLeft, bounds.right - p.measureText(data.xEnd())), bottom + gap * 1.5f, p);
        }
        float plotWidth = Math.max(1, bounds.right - plotLeft), x = bounds.left;
        for (var s : data.series()) {
            p.setColor(index == 0 ? theme.primaryContentColor() : theme.secondaryContentColor());
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(context.getResources().getDimension(R.dimen.emi_chart_stroke));
            float dash = textSize / 2;
            p.setPathEffect(index == 0 ? null : new DashPathEffect(new float[]{dash, dash}, 0));
            if (data.kind() == ValueChartData.Kind.LINE) {
                Path path = new Path();
                int denominator = Math.max(1, data.series().stream().mapToInt(v -> v.points().size()).max().orElse(1) - 1);
                for (int i = 0; i < s.points().size(); i++) {
                    float px = plotLeft + plotWidth * i / denominator;
                    float py = bottom - (bottom - top) * s.points().get(i).value().divide(max, java.math.MathContext.DECIMAL64).floatValue();
                    if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
                }
                canvas.drawPath(path, p);
            } else if (!s.points().isEmpty() && sum.signum() > 0) {
                float end = x + bounds.width() * s.points().get(0).value().divide(sum, java.math.MathContext.DECIMAL64).floatValue();
                p.setStyle(index == 0 ? Paint.Style.FILL : Paint.Style.STROKE);
                canvas.drawRect(x, top, end, top + gap * 2, p); x = end;
            }
            p.setPathEffect(null); p.setStyle(Paint.Style.FILL);
            String label = s.label() + (data.kind() == ValueChartData.Kind.PROPORTIONS && !s.points().isEmpty() ? ": " + s.points().get(0).label() : "");
            // Legend text wraps through the same measured native text layout as document text.
            android.text.TextPaint tp = new android.text.TextPaint(p);
            var layout = android.text.StaticLayout.Builder.obtain(label, 0, label.length(), tp, Math.max(1, (int) bounds.width())).build();
            canvas.save(); canvas.translate(bounds.left, bottom + gap * (index + 2)); layout.draw(canvas); canvas.restore();
            index++;
        }
        p.setPathEffect(null); p.setStyle(Paint.Style.STROKE); p.setColor(theme.secondaryContentColor());
        if (data.kind() == ValueChartData.Kind.LINE) canvas.drawLine(plotLeft, bottom, bounds.right, bottom, p);
    }
}
