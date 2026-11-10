package net.smartlogic.unitconverter.calculator.finance.emi;

import android.content.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.smartlogic.unitconverter.R;

public final class EmiTableRows {
    public record Row(LocalDate first, LocalDate last, int count, BigDecimal opening, BigDecimal payment,
                      BigDecimal principal, BigDecimal interest, BigDecimal extra, BigDecimal unused, BigDecimal closing) {
        public String label(Context c, boolean yearly) { return yearly ? c.getString(R.string.emi_year_period, first.getYear(), count) : new EmiFormatter(c).period(first); }
    }
    private EmiTableRows() { }
    public static List<Row> build(EmiResult r, boolean yearly) {
        List<Row> rows = new ArrayList<>();
        for (EmiInstallment i : r.installments()) {
            Row next = new Row(i.date(), i.date(), 1, i.opening(), i.payment(), i.principal(), i.interest(), i.prepayment(), i.unusedPrepayment(), i.closing());
            if (yearly && !rows.isEmpty() && rows.get(rows.size() - 1).first().getYear() == i.date().getYear()) {
                Row old = rows.remove(rows.size() - 1);
                next = new Row(old.first(), i.date(), old.count() + 1, old.opening(), old.payment().add(i.payment()),
                        old.principal().add(i.principal()), old.interest().add(i.interest()), old.extra().add(i.prepayment()), old.unused().add(i.unusedPrepayment()), i.closing());
            }
            rows.add(next);
        }
        return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(rows));
    }
}
