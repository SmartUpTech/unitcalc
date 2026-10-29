package net.smartlogic.unitconverter.bmi;

import android.content.Context;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.utils.NumberUtils;

/** All result, accessibility and history strings use the same authoritative snapshot. */
public final class BmiFormatter {
    public static final int[] CATEGORIES = {R.string.bmi_underweight, R.string.bmi_healthy,
            R.string.bmi_overweight, R.string.bmi_obesity_i, R.string.bmi_obesity_ii, R.string.bmi_obesity_iii};
    public static final int[] RANGES = {R.string.bmi_range_low, R.string.bmi_range_healthy,
            R.string.bmi_range_over, R.string.bmi_range_i, R.string.bmi_range_ii, R.string.bmi_range_iii};
    public static final int[] CHILD_CATEGORIES = {R.string.bmi_underweight, R.string.bmi_healthy,
            R.string.bmi_overweight, R.string.bmi_child_obesity, R.string.bmi_child_severe};
    public static final int[] CHILD_BANDS = {R.string.bmi_child_low_band, R.string.bmi_child_healthy_band,
            R.string.bmi_child_over_band, R.string.bmi_child_obesity_band, R.string.bmi_child_severe_band};
    private final Context context;
    public BmiFormatter(Context context) { this.context = context; }
    public String number(BigDecimal value) {
        Preferences p = Preferences.getInstance(context);
        String formatted = NumberUtils.formatCalculator(value, 2, p.getDecimalSeparator().charAt(0),
                p.getGroupSeparator().charAt(0), false);
        // BMI and weight boundaries consistently show two decimal places.
        char separator = p.getDecimalSeparator().charAt(0);
        int index = formatted.indexOf(separator);
        return index < 0 ? formatted + separator + "00" : formatted.length() - index == 2 ? formatted + "0" : formatted;
    }
    public String category(BmiCalculator.Result result) { return context.getString(CATEGORIES[result.category().ordinal()]); }
    public String childCategory(BmiAgeAssessment.Assessment assessment) { return context.getString(CHILD_CATEGORIES[assessment.category()]); }
    public String childBand(BmiAgeAssessment.Assessment assessment) { return context.getString(CHILD_BANDS[assessment.category()]); }
    public String childRange(BmiAgeAssessment.Assessment assessment) {
        return context.getString(R.string.bmi_child_range, number(assessment.reference().p5()), number(assessment.reference().p85()));
    }
    public String summary(BmiAgeAssessment.Assessment assessment, boolean pounds) {
        return assessment.group() == BmiAgeAssessment.Group.CHILD
                ? context.getString(R.string.bmi_child_history, number(assessment.result().bmi()), childCategory(assessment), childBand(assessment), childRange(assessment))
                : summary(assessment.result(), pounds);
    }
    public String range(BmiCalculator.Result result, boolean pounds) {
        return context.getString(R.string.bmi_weight_range, number(mass(result.minimumKg(), pounds)),
                number(mass(result.upperExclusiveKg(), pounds)), unit(pounds));
    }
    public String difference(BmiCalculator.Result result, boolean pounds) {
        if (result.category() == BmiCalculator.Category.HEALTHY) return context.getString(R.string.bmi_within_range);
        return context.getString(result.category() == BmiCalculator.Category.UNDERWEIGHT
                        ? R.string.bmi_below_range : R.string.bmi_above_range,
                number(mass(result.differenceKg().abs(), pounds)), unit(pounds));
    }
    public String summary(BmiCalculator.Result result, boolean pounds) {
        return context.getString(R.string.bmi_history_result, number(result.bmi()), category(result),
                range(result, pounds), difference(result, pounds));
    }
    public String unit(boolean pounds) { return context.getString(pounds ? R.string.bmi_lb : R.string.bmi_kg); }
    public static BigDecimal mass(BigDecimal kg, boolean pounds) {
        return pounds ? kg.divide(BmiCalculator.KG_PER_POUND, BmiCalculator.PRECISION) : kg;
    }
    public static String input(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString();
    }
}
