package net.smartlogic.unitconverter.emi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

public record EmiInputs(BigDecimal principal, BigDecimal annualRate, int months,
                        LocalDate firstEmi, String currencyCode, Method method,
                        List<PrepaymentRule> prepayments) {
    public enum Method { REDUCE_TENURE, REDUCE_EMI }
    public EmiInputs { prepayments = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(prepayments)); }
    public int scale() { return Currency.getInstance(currencyCode).getDefaultFractionDigits(); }
    public EmiInputs withoutPrepayments() {
        return new EmiInputs(principal, annualRate, months, firstEmi, currencyCode, method, java.util.Arrays.asList());
    }
}
