package net.smartlogic.unitconverter.calculator.finance.investment;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Posted currency amounts. Interest is earned during this period; payout leaves the account. */
public record InvestmentPeriod(int month, LocalDate date, BigDecimal contribution,
        BigDecimal invested, BigDecimal interest, BigDecimal payout, BigDecimal balance) { }
