package net.smartlogic.unitconverter.emi;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmiInstallment(int number, LocalDate date, BigDecimal opening,
                             BigDecimal payment, BigDecimal principal, BigDecimal interest,
                             BigDecimal prepayment, BigDecimal unusedPrepayment, BigDecimal closing) { }
