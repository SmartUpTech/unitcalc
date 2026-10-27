package net.smartlogic.unitconverter.bmi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.graphics.ColorUtils;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.CalculatorTheme;
import net.smartlogic.unitconverter.theme.ThemeManager;

/** A local BMI indicator, not a Graphy workspace. Text is supplied by native accessible views. */
public final class BmiGaugeView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private BmiCalculator.Result result;
    public BmiGaugeView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setResult(BmiCalculator.Result value) { result = value; invalidate(); }
    public static int color(BmiCalculator.Category category) {
        return ThemeManager.get().statusContentColor(CalculatorTheme.StatusTone.values()[category.ordinal()]);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = getResources().getDimension(R.dimen.bmi_gauge_stroke);
        float radius = Math.max(0, Math.min((getWidth() - stroke) / 2f, getHeight() - stroke));
        float cx = getWidth() / 2f, cy = getHeight() - stroke / 2;
        arc.set(cx - radius, cy - radius, cx + radius, cy + radius);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(ColorUtils.setAlphaComponent(ThemeManager.get().secondaryContentColor(), 45));
        canvas.drawArc(arc, 180, 180, false, paint);
        if (result != null) {
            paint.setColor(color(result.category()));
            // Fixed, documented visual scale 0–40. Values above 40 saturate; numeric BMI stays exact.
            float fraction = (float) Math.max(0, Math.min(1, result.bmi().doubleValue() / 40d));
            canvas.drawArc(arc, 180, 180 * fraction, false, paint);
        }
    }
}
