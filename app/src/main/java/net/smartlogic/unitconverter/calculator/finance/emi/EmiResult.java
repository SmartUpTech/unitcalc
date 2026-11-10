package net.smartlogic.unitconverter.calculator.finance.emi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One authoritative posted schedule for UI, charts, history, Graphy and documents. */
public record EmiResult(EmiInputs inputs, BigDecimal monthlyRate, BigDecimal regularEmi,
                        List<EmiInstallment> installments, List<EmiInstallment> baseline) {
    public EmiResult { installments = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(installments)); baseline = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(baseline)); }
    public BigDecimal totalInterest() { return interest(installments); }
    public BigDecimal baselineInterest() { return interest(baseline); }
    public BigDecimal interestSaved() { return baselineInterest().subtract(totalInterest()); }
    public BigDecimal totalPrepayment() { return installments.stream().map(EmiInstallment::prepayment).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public BigDecimal unusedPrepayment() { return installments.stream().map(EmiInstallment::unusedPrepayment).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public BigDecimal totalPayment() { return inputs.principal().add(totalInterest()); }
    public int monthsSaved() { return baseline.size() - installments.size(); }
    public LocalDate payoffDate() { return installments.get(installments.size() - 1).date(); }
    public BigDecimal finalPayment() { return installments.get(installments.size() - 1).payment(); }
    private static BigDecimal interest(List<EmiInstallment> rows) { return rows.stream().map(EmiInstallment::interest).reduce(BigDecimal.ZERO, BigDecimal::add); }
}
