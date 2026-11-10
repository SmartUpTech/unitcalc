package net.smartlogic.unitconverter.calculator.finance.emi;

import android.content.Context;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.PreferredCurrency;
import net.smartlogic.unitconverter.utils.NumberUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;

public final class EmiFormatter {
    private final Context context;
    private final String decimal, group;
    private final java.util.Map<String, String> symbols = new java.util.HashMap<>();
    public EmiFormatter(Context context) {
        this.context = context;
        Preferences p = Preferences.getInstance(context); decimal = p.getDecimalSeparator(); group = p.getGroupSeparator();
        for (var currency : PreferredCurrency.choices(context)) symbols.put(currency.getCurrencyISOCode(), currency.getCurrencySymbol());
    }
    public String symbol(String code) {
        String symbol = symbols.get(code);
        if (symbol == null) throw new IllegalArgumentException("currency");
        return symbol;
    }
    public String money(BigDecimal amount, String code) {
        int scale = Currency.getInstance(code).getDefaultFractionDigits();
        char dec = decimal.isEmpty() ? '.' : decimal.charAt(0);
        char sep = group.isEmpty() ? ',' : group.charAt(0);
        boolean grouping = !group.isEmpty() && !"None".equalsIgnoreCase(group)
                && !group.equals(context.getString(R.string.group_separator_none)) && dec != sep;
        String number = NumberUtils.formatCalculator(amount, scale, dec, sep, grouping);
        // Money retains the currency's full minor-unit scale, unlike general calculator output.
        if (scale > 0) {
            int point = number.lastIndexOf(dec);
            if (point < 0) { number += dec; point = number.length() - 1; }
            while (number.length() - point - 1 < scale) number += "0";
        }
        return context.getString(R.string.emi_money, symbol(code), number);
    }
    public String money(BigDecimal amount, EmiResult r) { return money(amount, r.inputs().currencyCode()); }
    public String date(LocalDate date) { return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())); }
    public String period(LocalDate date) { return date.format(DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault())); }
    public String summary(EmiResult r) {
        return context.getString(R.string.emi_share_summary, money(r.inputs().principal(), r), r.inputs().annualRate().toPlainString(),
                r.installments().size(), money(r.regularEmi(), r), money(r.totalInterest(), r), money(r.totalPayment(), r),
                date(r.payoffDate()), money(r.interestSaved(), r), r.monthsSaved(), money(r.finalPayment(), r))
                + (r.inputs().method() == EmiInputs.Method.REDUCE_EMI && !r.inputs().prepayments().isEmpty()
                    ? "\n" + context.getString(R.string.emi_changing_payments) : "")
                + "\n" + context.getString(R.string.emi_assumptions);
    }
}
