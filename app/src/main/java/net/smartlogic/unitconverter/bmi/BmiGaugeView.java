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
    private static final double SCALE_MAX = 40d;
    /** One boundary per category plus an upper cap on the 0–40 visual scale. */
    private static final double[] ZONE_STARTS = {0d, 18.5, 25d, 30d, 35d, 40d, SCALE_MAX};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private BmiCalculator.Result result;
    private BmiAgeAssessment.Assessment assessment;
    public BmiGaugeView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setResult(BmiCalculator.Result value) { assessment = null; result = value; invalidate(); }
    public void setAssessment(BmiAgeAssessment.Assessment value) { assessment = value; result = value == null ? null : value.result(); invalidate(); }
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
        paint.setStrokeCap(Paint.Cap.BUTT);
        double[] zones = ZONE_STARTS;
        BmiCalculator.Category[] tones = BmiCalculator.Category.values();
        if (assessment != null && assessment.group() == BmiAgeAssessment.Group.CHILD) {
            var r = assessment.reference();
            zones = new double[]{0, r.p5().doubleValue(), r.p85().doubleValue(), r.p95().doubleValue(), r.severe().doubleValue(), SCALE_MAX};
            tones = new BmiCalculator.Category[]{BmiCalculator.Category.UNDERWEIGHT, BmiCalculator.Category.HEALTHY,
                    BmiCalculator.Category.OVERWEIGHT, BmiCalculator.Category.OBESITY_I, BmiCalculator.Category.OBESITY_III};
        }
        boolean neutral = result == null && (assessment == null || assessment.group() == BmiAgeAssessment.Group.INFANT);
        for (int i = 0; i + 1 < zones.length; i++) {
            paint.setColor(ColorUtils.setAlphaComponent(neutral ? ThemeManager.get().secondaryContentColor() : color(tones[i]), 38));
            float start = angle(zones[i]);
            float sweep = angle(zones[i + 1]) - start;
            if (sweep <= 0f) continue;
            canvas.drawArc(arc, start, sweep, false, paint);
        }
        if (result != null) {
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(color(result.category()));
            float fraction = (float) Math.max(0, Math.min(1, result.bmi().doubleValue() / SCALE_MAX));
            canvas.drawArc(arc, 180, 180 * fraction, false, paint);
        }
    }
    private static float angle(double bmi) { return 180f + (float) (180d * Math.max(0, Math.min(1, bmi / SCALE_MAX))); }
}
