package net.smartlogic.unitconverter.emi;

import java.math.BigDecimal;

/** Extra principal posted after an installment; interval 0 means one time. */
public record PrepaymentRule(BigDecimal amount, int firstMonth, int lastMonth, int interval) {
    public boolean applies(int month) {
        return month >= firstMonth && month <= lastMonth
                && (interval == 0 ? month == firstMonth : (month - firstMonth) % interval == 0);
    }
}
