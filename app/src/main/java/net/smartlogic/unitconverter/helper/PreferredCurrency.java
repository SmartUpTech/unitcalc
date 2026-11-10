package net.smartlogic.unitconverter.helper;

import android.content.Context;
import androidx.fragment.app.FragmentActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.converter.ConverterPickerDialogFragment;
import net.smartlogic.unitconverter.model.Currency;
import net.smartlogic.unitconverter.model.CurrencyCatalog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Formatting currency only: never changes the converter's independently selected currencies. */
public final class PreferredCurrency {
    private PreferredCurrency() { }
    public static List<Currency> choices(Context context) {
        var unique = new LinkedHashMap<String, Currency>();
        for (Currency currency : CurrencyCatalog.create(context)) {
            try {
                int digits = java.util.Currency.getInstance(currency.getCurrencyISOCode()).getDefaultFractionDigits();
                if (digits >= 0 && digits <= 4) unique.putIfAbsent(currency.getCurrencyISOCode(), currency);
            } catch (IllegalArgumentException ignored) { }
        }
        return new ArrayList<>(unique.values());
    }
    public static String regionDefault(Locale locale, List<String> supported) {
        try {
            java.util.Currency regional = java.util.Currency.getInstance(locale);
            if (regional != null && supported.contains(regional.getCurrencyCode())) return regional.getCurrencyCode();
        } catch (IllegalArgumentException ignored) { }
        return "INR";
    }
    public static Currency find(Context context, String code) {
        return choices(context).stream().filter(c -> c.getCurrencyISOCode().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("currency"));
    }
    public static String label(Context context) {
        Currency c = find(context, Preferences.getInstance(context).getPreferredCurrency());
        return context.getString(R.string.emi_currency_choice, c.getCurrencyName(), c.getCurrencySymbol());
    }
    public static void showPicker(FragmentActivity activity) {
        var manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag(ConverterPickerDialogFragment.TAG) != null) return;
        createPicker(activity).showNow(manager, ConverterPickerDialogFragment.TAG);
    }
    /** Rebuild translated copy from the current context after locale recreation. */
    public static ConverterPickerDialogFragment createPicker(Context context) {
        List<Currency> currencies = choices(context);
        ArrayList<String> labels = new ArrayList<>(), details = new ArrayList<>(), keywords = new ArrayList<>();
        int selected = 0;
        int[] flags = new int[currencies.size()];
        String preferred = Preferences.getInstance(context).getPreferredCurrency();
        for (int i = 0; i < currencies.size(); i++) {
            Currency c = currencies.get(i);
            flags[i] = c.getFlagImageResource();
            labels.add(context.getString(R.string.emi_currency_choice, c.getCurrencyName(), c.getCurrencySymbol()));
            details.add(c.getCountry()); keywords.add(c.getCurrencyName() + " " + c.getCountry() + " " + c.getCurrencyISOCode());
            if (c.getCurrencyISOCode().equals(preferred)) selected = i;
        }
        return ConverterPickerDialogFragment.newInstance(context.getString(R.string.emi_preferred_currency), false, 0,
                selected, labels, details, keywords, flags).refreshPreferredCurrencyText();
    }
}
