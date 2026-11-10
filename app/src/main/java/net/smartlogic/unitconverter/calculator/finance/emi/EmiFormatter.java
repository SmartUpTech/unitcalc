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
    private final java.util.Map<String, String> symbols = new java.util.HashMap<>();
    public EmiFormatter(Context context) {
        this.context = context;
        for (var currency : PreferredCurrency.choices(context)) symbols.put(currency.getCurrencyISOCode(), currency.getCurrencySymbol());
    }
    public String symbol(String code) {
        String symbol = symbols.get(code);
        if (symbol == null) throw new IllegalArgumentException("currency");
        return symbol;
    }
    public String money(BigDecimal amount, String code) {
        int scale = Currency.getInstance(code).getDefaultFractionDigits();
        String number = Preferences.getInstance(context).getRegionalProfile().numbers.format(amount, scale, true);
        return context.getString(R.string.emi_money, symbol(code), number);
    }
    public String money(BigDecimal amount, EmiResult r) { return money(amount, r.inputs().currencyCode()); }
    public String date(LocalDate date) { return net.smartlogic.unitconverter.helper.RegionalDates.date(context, date); }
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
