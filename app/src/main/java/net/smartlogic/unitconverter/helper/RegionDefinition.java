package net.smartlogic.unitconverter.helper;

import android.icu.text.DecimalFormat;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

/** Stable territory identity; translation locales never select this preset. */
public final class RegionDefinition {
    public final String code;
    public final int nameResource;
    public final String englishName;
    public final String formattingTag;
    public final String currency;
    private final String[] commonLanguages;

    RegionDefinition(String code, int nameResource, String englishName, String formattingTag,
                     String currency, String languages) {
        this.code = code;
        this.nameResource = nameResource;
        this.englishName = englishName;
        this.formattingTag = formattingTag;
        this.currency = currency;
        commonLanguages = languages.split(" ");
    }

    public boolean commonlyUses(String language) {
        return Arrays.asList(commonLanguages).contains(language);
    }

    public NumberFormatSettings defaults() {
        Locale locale = Locale.forLanguageTag(formattingTag);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(locale);
        return new NumberFormatSettings(symbols.getDecimalSeparator(),
                format.isGroupingUsed() ? String.valueOf(symbols.getGroupingSeparator()) : "",
                format.getSecondaryGroupingSize() == 2 ? NumberFormatSettings.INDIAN
                        : NumberFormatSettings.INTERNATIONAL, 4);
    }
}
