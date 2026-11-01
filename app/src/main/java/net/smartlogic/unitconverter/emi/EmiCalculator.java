package net.smartlogic.unitconverter.emi;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Monthly reducing balance, DECIMAL128 formula, HALF_EVEN currency postings. No daily accrual. */
public final class EmiCalculator {
    public static final int MAX_MONTHS = 600, MAX_RULES = 20;
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal MAX_PRINCIPAL = new BigDecimal("1000000000000");
    private EmiCalculator() { }

    public static EmiResult calculate(EmiInputs input) {
        validate(input);
        BigDecimal rate = input.annualRate().divide(new BigDecimal("1200"), MC);
        BigDecimal emi = payment(input.principal(), rate, input.months(), input.scale());
        List<EmiInstallment> base = schedule(input.withoutPrepayments(), rate, emi);
        return new EmiResult(input, rate, emi, input.prepayments().isEmpty() ? base : schedule(input, rate, emi), base);
    }

    public static void validate(EmiInputs i) {
        if (i == null || i.principal() == null || i.annualRate() == null || i.firstEmi() == null || i.method() == null || i.currencyCode() == null)
            throw new IllegalArgumentException("input");
        int scale = i.scale();
        if (scale < 0 || scale > 4 || i.principal().signum() <= 0 || i.principal().compareTo(MAX_PRINCIPAL) >= 0
                || i.principal().scale() > scale) throw new IllegalArgumentException("principal");
        if (i.annualRate().signum() < 0 || i.annualRate().compareTo(new BigDecimal("100")) > 0
                || i.annualRate().scale() > 4) throw new IllegalArgumentException("rate");
        if (i.months() < 1 || i.months() > MAX_MONTHS) throw new IllegalArgumentException("months");
        if (i.firstEmi().getYear() < 1900 || i.firstEmi().getYear() > 2100) throw new IllegalArgumentException("date");
        if (i.prepayments().size() > MAX_RULES) throw new IllegalArgumentException("rules");
        for (PrepaymentRule rule : i.prepayments()) {
            if (rule.amount() == null || rule.amount().signum() <= 0 || rule.amount().compareTo(MAX_PRINCIPAL) >= 0
                    || rule.amount().scale() > scale || rule.firstMonth() < 1
                    || rule.lastMonth() < rule.firstMonth() || rule.lastMonth() > i.months()
                    || (rule.interval() == 0 && rule.lastMonth() != rule.firstMonth())
                    || (rule.interval() != 0 && rule.interval() != 1 && rule.interval() != 12))
                throw new IllegalArgumentException("rules");
        }
    }

    private static BigDecimal payment(BigDecimal principal, BigDecimal rate, int months, int scale) {
        BigDecimal value;
        if (rate.signum() == 0) value = principal.divide(BigDecimal.valueOf(months), MC);
        else {
            BigDecimal power = BigDecimal.ONE.add(rate, MC).pow(months, MC);
            value = principal.multiply(rate, MC).multiply(power, MC).divide(power.subtract(BigDecimal.ONE), MC);
        }
        return value.setScale(scale, RoundingMode.HALF_EVEN);
    }

    private static List<EmiInstallment> schedule(EmiInputs i, BigDecimal rate, BigDecimal emi) {
        List<EmiInstallment> rows = new ArrayList<>();
        BigDecimal balance = i.principal().setScale(i.scale()), regular = emi;
        for (int month = 1; month <= i.months() && balance.signum() > 0; month++) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            BigDecimal opening = balance;
            BigDecimal interest = balance.multiply(rate, MC).setScale(i.scale(), RoundingMode.HALF_EVEN);
            if (regular.compareTo(interest) <= 0 && month < i.months()) throw new IllegalArgumentException("rounding");
            BigDecimal pay = month == i.months() ? balance.add(interest) : regular.min(balance.add(interest));
            BigDecimal principal = pay.subtract(interest);
            balance = balance.subtract(principal);
            BigDecimal requested = BigDecimal.ZERO.setScale(i.scale());
            for (PrepaymentRule rule : i.prepayments()) if (rule.applies(month)) requested = requested.add(rule.amount());
            BigDecimal extra = requested.min(balance);
            balance = balance.subtract(extra);
            rows.add(new EmiInstallment(month, i.firstEmi().plusMonths(month - 1L), opening, pay, principal,
                    interest, extra, requested.subtract(extra), balance));
            if (extra.signum() > 0 && balance.signum() > 0 && i.method() == EmiInputs.Method.REDUCE_EMI)
                regular = payment(balance, rate, i.months() - month, i.scale());
        }
        return rows;
    }
}
