package net.smartlogic.unitconverter.bmi;

import java.math.BigDecimal;

/** CDC screening reference. Infant growth is deliberately not assigned an adult BMI category. */
public final class BmiAgeAssessment {
    public enum Group { INFANT, CHILD, ADULT }
    public record ChildReference(BigDecimal p5, BigDecimal p85, BigDecimal p95) {
        public BigDecimal severe() { return p95.multiply(new BigDecimal("1.2")).min(new BigDecimal("35")); }
    }
    public record Assessment(Group group, BmiCalculator.Result result, int category,
                             ChildReference reference) { }
    private BmiAgeAssessment() { }

    public static ChildReference reference(int completedMonths, boolean female) {
        if (completedMonths < 24 || completedMonths >= 240) throw new IllegalArgumentException("CDC child reference requires 24–239 months");
        double[] lms = CdcBmiReferenceData.LMS[female ? 1 : 0][completedMonths - 24];
        return new ChildReference(cutoff(lms, -1.6448536269514722), cutoff(lms, 1.0364333894937898),
                cutoff(lms, 1.6448536269514722));
    }
    private static BigDecimal cutoff(double[] lms, double z) {
        // The CDC LMS model uses real-valued Box–Cox powers; retain double precision until display.
        double value = lms[0] == 0 ? lms[1] * Math.exp(lms[2] * z)
                : lms[1] * Math.pow(1 + lms[0] * lms[2] * z, 1 / lms[0]);
        return BigDecimal.valueOf(value);
    }
    public static Assessment assess(BigDecimal heightCm, BigDecimal weightKg, int completedMonths, boolean female) {
        if (completedMonths < 0) throw new IllegalArgumentException("Age is required");
        if (completedMonths < 24) return new Assessment(Group.INFANT, null, -1, null);
        BmiCalculator.Result measured = BmiCalculator.calculate(heightCm, weightKg);
        if (completedMonths >= 240) return new Assessment(Group.ADULT, measured, measured.category().ordinal(), null);
        ChildReference reference = reference(completedMonths, female);
        BigDecimal square = heightCm.movePointLeft(2).pow(2);
        int category = classify(weightKg, square, reference);
        BmiCalculator.Category tone = switch (category) {
            case 0 -> BmiCalculator.Category.UNDERWEIGHT;
            case 1 -> BmiCalculator.Category.HEALTHY;
            case 2 -> BmiCalculator.Category.OVERWEIGHT;
            case 3 -> BmiCalculator.Category.OBESITY_I;
            default -> BmiCalculator.Category.OBESITY_III;
        };
        BmiCalculator.Result result = new BmiCalculator.Result(measured.bmi(), tone,
                reference.p5().multiply(square), reference.p85().multiply(square), BigDecimal.ZERO);
        return new Assessment(Group.CHILD, result, category, reference);
    }
    private static int classify(BigDecimal kg, BigDecimal square, ChildReference reference) {
        if (kg.compareTo(reference.p5().multiply(square)) < 0) return 0;
        if (kg.compareTo(reference.p85().multiply(square)) < 0) return 1;
        if (kg.compareTo(reference.p95().multiply(square)) < 0) return 2;
        return kg.compareTo(reference.severe().multiply(square)) >= 0 ? 4 : 3;
    }
}
