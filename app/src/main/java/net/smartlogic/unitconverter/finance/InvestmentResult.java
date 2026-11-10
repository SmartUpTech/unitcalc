package net.smartlogic.unitconverter.finance;

import java.math.BigDecimal;
import java.util.List;

/** The only source of financial values for UI, history, tables and Graphy. */
public record InvestmentResult(InvestmentInputs inputs, BigDecimal invested, BigDecimal interest,
        BigDecimal maturity, BigDecimal receipts, BigDecimal annualYield, List<InvestmentPeriod> periods) {
    public InvestmentResult { periods = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(periods)); }
}
