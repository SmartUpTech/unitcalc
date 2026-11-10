package net.smartlogic.unitconverter.calculator.finance.investment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

/** Offline month-based estimates. No bank-specific daily accrual or tax assumptions. */
public final class InvestmentCalculator {
    public static final int MAX_MONTHS = 600;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");
    public static final BigDecimal MAX_TOTAL = new BigDecimal("1000000000000000000");
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private InvestmentCalculator() { }

    public static void validate(InvestmentInputs i) {
        if (i == null || i.kind() == null) fail("input");
        if (i.currency() == null) fail("currency");
        int scale;
        try { scale = i.scale(); } catch (RuntimeException error) { throw new IllegalArgumentException("currency"); }
        if (scale < 0 || scale > 4) fail("currency");
        amount(i.amount(), scale, false, "amount");
        if (i.annualRate() == null || i.annualRate().stripTrailingZeros().scale() > 4
                || i.annualRate().compareTo(HUNDRED) > 0
                || i.annualRate().compareTo(i.kind() == InvestmentInputs.Kind.SIP ? new BigDecimal("-99.99") : BigDecimal.ZERO) < 0) fail("rate");
        if (i.months() < 1 || i.months() > MAX_MONTHS) fail("tenure");
        if (i.start() == null || i.start().getYear() < 1900 || i.start().getYear() > 2100) fail("date");
        if (!java.util.Arrays.asList(1, 3, 6, 12).contains(i.frequency())) fail("frequency");
        if (!java.util.Arrays.asList(3, 6, 12).contains(i.stepMonths())) fail("step");
        if (i.step() == null || i.step().signum() < 0) fail("step");
        if (i.stepPercent()) {
            if (i.step().compareTo(HUNDRED) > 0 || i.step().stripTrailingZeros().scale() > 4) fail("step");
        } else amount(i.step(), scale, true, "step");
        if (i.kind() != InvestmentInputs.Kind.FD && i.periodic()) fail("frequency");
        if (i.kind() != InvestmentInputs.Kind.SIP && i.stepEnabled()) fail("step");
    }

    public static InvestmentResult calculate(InvestmentInputs i) {
        validate(i);
        return i.kind() == InvestmentInputs.Kind.FD ? fixed(i) : contributions(i);
    }

    private static InvestmentResult contributions(InvestmentInputs i) {
        BigDecimal factor = i.kind() == InvestmentInputs.Kind.SIP
                ? BigDecimal.ONE.add(i.annualRate().divide(new BigDecimal("1200"), MC), MC)
                : cubeRoot(BigDecimal.ONE.add(i.annualRate().divide(new BigDecimal("400"), MC), MC));
        BigDecimal balance = BigDecimal.ZERO, invested = money(BigDecimal.ZERO, i), contribution = money(i.amount(), i);
        BigDecimal previous = money(BigDecimal.ZERO, i);
        List<InvestmentPeriod> rows = new ArrayList<>();
        for (int month = 1; month <= i.months(); month++) {
            cancelled();
            if (i.stepEnabled() && month > 1 && (month - 1) % i.stepMonths() == 0) {
                contribution = money(i.stepPercent() ? contribution.multiply(BigDecimal.ONE.add(i.step().divide(HUNDRED, MC)), MC)
                        : contribution.add(i.step()), i);
                amount(contribution, i.scale(), false, "step");
            }
            invested = invested.add(contribution);
            balance = balance.add(contribution, MC).multiply(factor, MC);
            bounded(balance); bounded(invested);
            BigDecimal posted = money(balance, i);
            rows.add(new InvestmentPeriod(month, i.start().plusMonths(month - 1L), contribution, invested,
                    posted.subtract(previous).subtract(contribution), money(BigDecimal.ZERO, i), posted));
            previous = posted;
        }
        return new InvestmentResult(i, invested, previous.subtract(invested), previous, previous,
                factor.pow(12, MC).subtract(BigDecimal.ONE).multiply(HUNDRED, MC), rows);
    }

    private static InvestmentResult fixed(InvestmentInputs i) {
        BigDecimal principal = money(i.amount(), i), periodRate = i.annualRate()
                .multiply(BigDecimal.valueOf(i.frequency()), MC).divide(new BigDecimal("1200"), MC);
        BigDecimal factor = BigDecimal.ONE.add(periodRate, MC);
        BigDecimal totalInterest = money(BigDecimal.ZERO, i), previous = principal;
        List<InvestmentPeriod> rows = new ArrayList<>();
        BigDecimal maturity = principal;
        for (int month = 1; month <= i.months(); month++) {
            cancelled();
            BigDecimal payout = money(BigDecimal.ZERO, i), earned;
            if (i.periodic()) {
                // Cumulative rounded entitlement prevents repeated small-payout rounding drift.
                BigDecimal entitlement = money(principal.multiply(i.annualRate(), MC)
                        .multiply(BigDecimal.valueOf(month), MC).divide(new BigDecimal("1200"), MC), i);
                earned = entitlement.subtract(totalInterest);
                totalInterest = entitlement;
                if (month % i.frequency() == 0 || month == i.months()) {
                    BigDecimal paid = money(BigDecimal.ZERO, i);
                    for (InvestmentPeriod row : rows) paid = paid.add(row.payout());
                    payout = totalInterest.subtract(paid);
                }
                maturity = principal.add(month == i.months() ? payout : BigDecimal.ZERO);
                rows.add(new InvestmentPeriod(month, i.start().plusMonths(month), month == 1 ? principal : money(BigDecimal.ZERO, i),
                        principal, earned, payout.add(month == i.months() ? principal : BigDecimal.ZERO),
                        month == i.months() ? money(BigDecimal.ZERO, i) : principal));
            } else {
                int complete = month / i.frequency(), remainder = month % i.frequency();
                BigDecimal balance = principal.multiply(factor.pow(complete, MC), MC)
                        .multiply(BigDecimal.ONE.add(i.annualRate().multiply(BigDecimal.valueOf(remainder), MC)
                                .divide(new BigDecimal("1200"), MC)), MC);
                bounded(balance); maturity = money(balance, i); earned = maturity.subtract(previous);
                rows.add(new InvestmentPeriod(month, i.start().plusMonths(month), month == 1 ? principal : money(BigDecimal.ZERO, i),
                        principal, earned, payout, maturity));
                previous = maturity; totalInterest = maturity.subtract(principal);
            }
        }
        BigDecimal receipts = principal.add(totalInterest); bounded(receipts);
        BigDecimal yield = factor.pow(12 / i.frequency(), MC).subtract(BigDecimal.ONE).multiply(HUNDRED, MC);
        return new InvestmentResult(i, principal, totalInterest, maturity, receipts, yield, rows);
    }

    static BigDecimal cubeRoot(BigDecimal value) {
        BigDecimal x = BigDecimal.ONE;
        MathContext guard = new MathContext(42, RoundingMode.HALF_EVEN);
        for (int n = 0; n < 100; n++) {
            cancelled();
            BigDecimal next = x.multiply(new BigDecimal("2"), guard)
                    .add(value.divide(x.multiply(x, guard), guard), guard).divide(new BigDecimal("3"), guard);
            if (next.subtract(x).abs().compareTo(new BigDecimal("1E-38")) < 0) return next.round(MC);
            x = next;
        }
        throw new IllegalArgumentException("calculation");
    }
    private static void amount(BigDecimal value, int scale, boolean zero, String field) {
        if (value == null || value.signum() < (zero ? 0 : 1) || value.compareTo(MAX_AMOUNT) >= 0
                || value.stripTrailingZeros().scale() > scale) fail(field);
    }
    private static BigDecimal money(BigDecimal value, InvestmentInputs i) { return value.setScale(i.scale(), RoundingMode.HALF_EVEN); }
    private static void bounded(BigDecimal value) { if (value.abs().compareTo(MAX_TOTAL) >= 0) fail("overflow"); }
    private static void cancelled() { if (Thread.currentThread().isInterrupted()) throw new CancellationException(); }
    private static void fail(String field) { throw new IllegalArgumentException(field); }
}
