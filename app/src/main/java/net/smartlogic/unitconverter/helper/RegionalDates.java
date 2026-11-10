package net.smartlogic.unitconverter.helper;

import android.content.Context;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Locale;

/** Region owns ordering; the resource locale supplies month/day words. */
public final class RegionalDates {
    private RegionalDates() {}
    private static Locale region(Context context) {
        return Locale.forLanguageTag(Preferences.getInstance(context).getRegionalProfile().region.formattingTag);
    }
    private static Locale words(Context context) {
        return AppLanguage.resourceContext(context).getResources().getConfiguration().getLocales().get(0);
    }
    public static String date(Context context, LocalDate date) {
        String pattern = android.text.format.DateFormat.getBestDateTimePattern(region(context), "yMMMd");
        return date.format(DateTimeFormatter.ofPattern(pattern, words(context)).withDecimalStyle(DecimalStyle.STANDARD));
    }
    public static String instant(Context context, long epochMillis, String skeleton) {
        String pattern = android.text.format.DateFormat.getBestDateTimePattern(region(context), skeleton);
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern, words(context));
        java.text.DecimalFormat number = new java.text.DecimalFormat("0",
                java.text.DecimalFormatSymbols.getInstance(Locale.US));
        number.setGroupingUsed(false);
        format.setNumberFormat(number);
        return format.format(new java.util.Date(epochMillis));
    }
}
