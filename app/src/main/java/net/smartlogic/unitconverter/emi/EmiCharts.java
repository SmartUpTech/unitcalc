package net.smartlogic.unitconverter.emi;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.chart.ValueChartData;

/** Supplies authoritative values and accessible labels, never styling. */
public final class EmiCharts {
    private EmiCharts() { }
    public static ValueChartData breakdown(Context c, EmiResult r) {
        EmiFormatter f = new EmiFormatter(c);
        return new ValueChartData(ValueChartData.Kind.PROPORTIONS, java.util.Arrays.asList(
                new ValueChartData.Series(c.getString(R.string.emi_principal), java.util.Arrays.asList(new ValueChartData.Point(r.inputs().principal(), f.money(r.inputs().principal(), r)))),
                new ValueChartData.Series(c.getString(R.string.emi_interest), java.util.Arrays.asList(new ValueChartData.Point(r.totalInterest(), f.money(r.totalInterest(), r))))));
    }
    public static ValueChartData balance(Context c, EmiResult a, EmiResult b) {
        List<ValueChartData.Series> series = new ArrayList<>();
        series.add(series(c, a, c.getString(b == null ? R.string.emi_balance : R.string.emi_loan_a)));
        if (b != null) series.add(series(c, b, c.getString(R.string.emi_loan_b)));
        EmiFormatter f = new EmiFormatter(c);
        java.math.BigDecimal max = b == null ? a.inputs().principal() : a.inputs().principal().max(b.inputs().principal());
        int months = b == null ? a.installments().size() : Math.max(a.installments().size(), b.installments().size());
        return new ValueChartData(ValueChartData.Kind.LINE, series, "0", c.getString(R.string.emi_term_value, Integer.toString(months), c.getString(R.string.emi_months)),
                f.money(max, a), f.money(java.math.BigDecimal.ZERO, a));
    }
    private static ValueChartData.Series series(Context c, EmiResult r, String label) {
        EmiFormatter f = new EmiFormatter(c);
        List<ValueChartData.Point> points = new ArrayList<>();
        points.add(new ValueChartData.Point(r.inputs().principal(), c.getString(R.string.emi_chart_point, c.getString(R.string.emi_balance_start), f.money(r.inputs().principal(), r))));
        for (EmiInstallment row : r.installments()) points.add(new ValueChartData.Point(row.closing(), c.getString(R.string.emi_chart_point, f.date(row.date()), f.money(row.closing(), r))));
        return new ValueChartData.Series(label, points);
    }
}
