package net.smartlogic.unitconverter.bmi;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class BmiAgeAssessmentTest {
    private static final BigDecimal HEIGHT = new BigDecimal("100");
    private BmiAgeAssessment.Assessment assess(String bmi, int months, boolean female) {
        // At exactly one metre, kg and BMI have the same numeric value.
        return BmiAgeAssessment.assess(HEIGHT, new BigDecimal(bmi), months, female);
    }
    @Test public void publishedCdcExamplesUseTheCorrectMonthlyReference() {
        var girl = BmiAgeAssessment.reference(114, true);
        assertEquals(22.3979, girl.p95().doubleValue(), 0.00005);
        assertEquals(2, assess("21.2", 114, true).category());
        var boy = BmiAgeAssessment.reference(50, false);
        assertEquals(17.8219, boy.p95().doubleValue(), 0.00005);
        assertEquals(4, assess("22.6", 50, false).category());
        assertEquals(BmiCalculator.Category.HEALTHY, BmiCalculator.category(new BigDecimal("21.2")));
    }
    @Test public void birthdaysSwitchReferencesWithoutApplyingAdultRangesToChildren() {
        assertEquals(BmiAgeAssessment.Group.INFANT, assess("16", 23, false).group());
        assertNull(assess("16", 23, false).result());
        assertEquals(BmiAgeAssessment.Group.CHILD, assess("16", 24, false).group());
        assertEquals(1, assess("26", 239, true).category());
        assertEquals(BmiAgeAssessment.Group.ADULT, assess("26", 240, true).group());
        assertEquals(BmiCalculator.Category.OVERWEIGHT, assess("26", 240, true).result().category());
        assertEquals(BmiAgeAssessment.Group.ADULT, assess("21", 1080, false).group());
    }
    @Test public void everyMonthAndSexHasOrderedCutoffsAndExactBoundaryClassification() {
        BigDecimal epsilon = new BigDecimal("0.000000001");
        for (boolean female : new boolean[]{false, true}) for (int month = 24; month < 240; month++) {
            var r = BmiAgeAssessment.reference(month, female);
            assertTrue(r.p5().compareTo(r.p85()) < 0);
            assertTrue(r.p85().compareTo(r.p95()) < 0);
            assertTrue(r.p95().compareTo(r.severe()) < 0);
            assertEquals(0, BmiAgeAssessment.assess(HEIGHT, r.p5().subtract(epsilon), month, female).category());
            assertEquals(1, BmiAgeAssessment.assess(HEIGHT, r.p5(), month, female).category());
            assertEquals(2, BmiAgeAssessment.assess(HEIGHT, r.p85(), month, female).category());
            assertEquals(3, BmiAgeAssessment.assess(HEIGHT, r.p95(), month, female).category());
            assertEquals(4, BmiAgeAssessment.assess(HEIGHT, r.severe(), month, female).category());
        }
    }
    @Test public void severeObesityHonorsBothSidesOfTheOrRule() {
        assertTrue(BmiAgeAssessment.reference(50, false).severe().compareTo(new BigDecimal("35")) < 0);
        assertEquals(4, assess("22.6", 50, false).category());
        assertTrue(BmiAgeAssessment.reference(239, true).p95().multiply(new BigDecimal("1.2")).compareTo(new BigDecimal("35")) > 0);
        assertEquals(4, assess("35", 239, true).category());
    }
    @Test public void referenceSexAndMonthMatterAndMissingAgeIsRejected() {
        assertNotEquals(BmiAgeAssessment.reference(114, false), BmiAgeAssessment.reference(114, true));
        assertNotEquals(BmiAgeAssessment.reference(114, true), BmiAgeAssessment.reference(115, true));
        assertThrows(IllegalArgumentException.class, () -> assess("21", -1, false));
        assertThrows(IllegalArgumentException.class, () -> BmiAgeAssessment.reference(240, false));
    }
}
