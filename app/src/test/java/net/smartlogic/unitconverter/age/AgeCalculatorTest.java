package net.smartlogic.unitconverter.age;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class AgeCalculatorTest {
    private AgeCalculator.Result result(String birth, String asof) {
        return AgeCalculator.calculate(LocalDate.parse(birth), LocalDate.parse(asof));
    }
    private void age(String birth, String asof, int y, int m, int d) {
        var r = result(birth, asof);
        assertEquals(y, r.age().years()); assertEquals(m, r.age().months()); assertEquals(d, r.age().days());
        assertEquals(r.asOf(), r.age().anniversary().plusMonths(m).plusDays(d));
    }
    @Test public void referenceExampleHasIndependentTotals() {
        age("2010-08-16", "2020-01-21", 9, 5, 5);
        var r = result("2010-08-16", "2020-01-21");
        assertEquals(113, r.totalMonths()); assertEquals(3445, r.totalDays());
        assertEquals(492, r.totalWeeks()); assertEquals(82680, r.totalHours()); assertEquals(4960800, r.totalMinutes());
        assertEquals(LocalDate.of(2020, 8, 16), r.nextBirthday());
        assertEquals(6, r.countdown().months()); assertEquals(26, r.countdown().days());
    }
    @Test public void leapBirthdayUsesFebruary28ButReturnsTo29InLeapYears() {
        age("2020-02-29", "2021-02-27", 0, 11, 29);
        age("2020-02-29", "2021-02-28", 1, 0, 0);
        age("2020-02-29", "2024-02-28", 3, 11, 31);
        age("2020-02-29", "2024-02-29", 4, 0, 0);
        assertEquals(LocalDate.of(2024, 2, 29), result("2020-02-29", "2023-03-01").nextBirthday());
    }
    @Test public void monthEndsAreClampedFromTheYearAnchor() {
        age("2023-01-31", "2023-02-28", 0, 1, 0);
        age("2023-01-31", "2023-03-30", 0, 1, 30);
        age("2023-01-31", "2023-03-31", 0, 2, 0);
        age("2023-08-31", "2024-02-29", 0, 6, 0);
    }
    @Test public void birthdayTodayAndSameDayAreZeroCountdown() {
        age("2000-01-01", "2000-01-01", 0, 0, 0);
        var r = result("2000-01-01", "2025-01-01");
        assertEquals(r.asOf(), r.nextBirthday()); assertEquals(0, r.countdown().days());
        assertEquals(0, r.countdown().months()); assertEquals(0, r.countdown().years());
    }
    @Test public void futureAndCenturyBoundariesAreCalendarBased() {
        age("2000-02-29", "2100-02-28", 100, 0, 0);
        assertEquals(365, result("2099-03-01", "2100-03-01").totalDays());
        var r = result("1900-01-01", "2100-12-31");
        assertEquals(73413, r.totalDays());
        assertEquals(105714720, r.totalMinutes());
        assertEquals(LocalDate.of(2101, 1, 1), r.nextBirthday());
    }
    @Test public void unsupportedDatesAndReversedDatesAreRejected() {
        for (String[] pair : new String[][]{{"2020-01-02", "2020-01-01"}, {"1899-12-31", "2020-01-01"}, {"2000-01-01", "2101-01-01"}}) {
            assertThrows(IllegalArgumentException.class, () -> result(pair[0], pair[1]));
        }
    }
    @Test public void decompositionAlwaysReconstructsDateAcrossLeapAndMonthEndBoundaries() {
        for (LocalDate birth : new LocalDate[]{LocalDate.of(1996, 2, 29), LocalDate.of(2000, 1, 31), LocalDate.of(2020, 8, 31)}) {
            for (int days = 0; days < 4000; days++) {
                var r = AgeCalculator.calculate(birth, birth.plusDays(days));
                assertTrue(r.age().months() >= 0 && r.age().months() < 12);
                assertTrue(r.age().days() >= 0);
                assertEquals(r.asOf(), birth.plusYears(r.age().years()).plusMonths(r.age().months()).plusDays(r.age().days()));
                assertFalse(r.nextBirthday().isBefore(r.asOf()));
            }
        }
    }
}
