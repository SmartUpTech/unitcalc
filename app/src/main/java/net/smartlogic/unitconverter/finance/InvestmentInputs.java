package net.smartlogic.unitconverter.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

/** Canonical immutable inputs. Frequencies are whole months, never display indices. */
public record InvestmentInputs(Kind kind, BigDecimal amount, BigDecimal annualRate, int months,
        String currency, LocalDate start, int frequency, boolean periodic,
        boolean stepEnabled, boolean stepPercent, BigDecimal step, int stepMonths) {
    public enum Kind { FD, RD, SIP }
    public int scale() { return Currency.getInstance(currency).getDefaultFractionDigits(); }
}
