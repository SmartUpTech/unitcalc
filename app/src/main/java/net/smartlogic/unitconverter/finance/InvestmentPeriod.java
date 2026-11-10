package net.smartlogic.unitconverter.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Posted currency amounts. Interest is earned during this period; payout leaves the account. */
public record InvestmentPeriod(int month, LocalDate date, BigDecimal contribution,
        BigDecimal invested, BigDecimal interest, BigDecimal payout, BigDecimal balance) { }
