package net.smartlogic.unitconverter.calculator.finance.emi;

import org.junit.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class EmiCalculatorTest {
    private EmiInputs input(String p, String r, int n, String currency, EmiInputs.Method method, PrepaymentRule... rules) {
        return new EmiInputs(new BigDecimal(p), new BigDecimal(r), n, LocalDate.of(2024, 1, 31), currency, method, List.of(rules));
    }
    private void equal(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
    @Test public void referenceAndPostedRounding() {
        EmiResult r = EmiCalculator.calculate(input("1000000", "8.5", 240, "INR", EmiInputs.Method.REDUCE_TENURE));
        equal("8678.23", r.regularEmi()); equal("1082776.63", r.totalInterest()); equal("2082776.63", r.totalPayment());
        equal("1594.90", r.installments().get(0).principal()); equal("7083.33", r.installments().get(0).interest());
        equal("998405.10", r.installments().get(0).closing()); reconcile(r);
    }
    @Test public void bothPrepaymentMethodsAtZeroInterest() {
        PrepaymentRule extra = new PrepaymentRule(new BigDecimal("100"), 1, 1, 0);
        EmiResult shorter = EmiCalculator.calculate(input("1200", "0", 12, "INR", EmiInputs.Method.REDUCE_TENURE, extra));
        assertEquals(11, shorter.installments().size()); equal("100", shorter.regularEmi()); reconcile(shorter);
        EmiResult smaller = EmiCalculator.calculate(input("1200", "0", 12, "INR", EmiInputs.Method.REDUCE_EMI, extra));
        assertEquals(12, smaller.installments().size()); equal("90.91", smaller.installments().get(1).payment());
        equal("90.90", smaller.finalPayment()); reconcile(smaller);
    }
    @Test public void overlappingAndRecurringRulesCapAtBalance() {
        EmiResult r = EmiCalculator.calculate(input("1200", "0", 12, "INR", EmiInputs.Method.REDUCE_TENURE,
                new PrepaymentRule(new BigDecimal("50"), 1, 12, 1), new PrepaymentRule(new BigDecimal("2000"), 1, 1, 0)));
        assertEquals(1, r.installments().size()); equal("1100", r.totalPrepayment()); equal("950", r.unusedPrepayment()); reconcile(r);
    }
    @Test public void currencyScalesMonthAnchorsAndOnePayment() {
        for (String currency : List.of("JPY", "INR", "KWD")) {
            EmiResult r = EmiCalculator.calculate(input("1000", "0", 3, currency, EmiInputs.Method.REDUCE_TENURE));
            assertEquals(LocalDate.of(2024, 2, 29), r.installments().get(1).date());
            assertEquals(LocalDate.of(2024, 3, 31), r.payoffDate()); reconcile(r);
        }
        EmiResult one = EmiCalculator.calculate(input("1200", "12", 1, "INR", EmiInputs.Method.REDUCE_TENURE));
        equal("1212", one.finalPayment()); equal("12", one.totalInterest());
    }
    @Test public void interestAndTermSavingsAreFromBaseline() {
        EmiResult r = EmiCalculator.calculate(input("100000", "12", 120, "INR", EmiInputs.Method.REDUCE_TENURE,
                new PrepaymentRule(new BigDecimal("1000"), 12, 120, 12)));
        assertTrue(r.interestSaved().signum() > 0); assertTrue(r.monthsSaved() > 0); reconcile(r);
    }
    @Test public void boundsAndTinyRate() {
        reconcile(EmiCalculator.calculate(input("999999999999.99", "12", 600, "INR", EmiInputs.Method.REDUCE_TENURE)));
        reconcile(EmiCalculator.calculate(input("1000", "0.0001", 600, "KWD", EmiInputs.Method.REDUCE_TENURE)));
        for (EmiInputs invalid : List.of(input("0", "1", 12, "INR", EmiInputs.Method.REDUCE_TENURE),
                input("1.001", "1", 12, "INR", EmiInputs.Method.REDUCE_TENURE), input("1", "101", 12, "INR", EmiInputs.Method.REDUCE_TENURE),
                input("100", "1", 601, "INR", EmiInputs.Method.REDUCE_TENURE), input("0.01", "0", 600, "INR", EmiInputs.Method.REDUCE_TENURE), input("999999999999.99", "100", 600, "INR", EmiInputs.Method.REDUCE_TENURE))) {
            assertThrows(IllegalArgumentException.class, () -> EmiCalculator.calculate(invalid));
        }
    }
    private void reconcile(EmiResult r) {
        BigDecimal principal = BigDecimal.ZERO, paid = BigDecimal.ZERO;
        for (EmiInstallment row : r.installments()) {
            equal(row.payment().toPlainString(), row.principal().add(row.interest()));
            equal(row.closing().toPlainString(), row.opening().subtract(row.principal()).subtract(row.prepayment()));
            assertTrue(row.closing().signum() >= 0);
            principal = principal.add(row.principal()).add(row.prepayment()); paid = paid.add(row.payment()).add(row.prepayment());
        }
        equal(r.inputs().principal().toPlainString(), principal); equal(r.totalPayment().toPlainString(), paid);
        equal("0", r.installments().get(r.installments().size() - 1).closing());
    }
}
