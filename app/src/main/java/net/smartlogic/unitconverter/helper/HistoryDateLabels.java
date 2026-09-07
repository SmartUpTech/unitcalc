package net.smartlogic.unitconverter.helper;

import android.content.Context;

import net.smartlogic.unitconverter.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class HistoryDateLabels {

    private HistoryDateLabels() {
    }

    public static String sectionLabel(Context context, long epochMillis) {
        if (epochMillis <= 0L) {
            return context.getString(R.string.history_today);
        }
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(epochMillis);
        Calendar today = Calendar.getInstance();
        if (isSameDay(then, today)) {
            return context.getString(R.string.history_today);
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(then, yesterday)) {
            return context.getString(R.string.history_yesterday);
        }
        return formatDayMonth(then.getTimeInMillis());
    }

    public static String formatTime(long epochMillis) {
        return formatTime(epochMillis, Locale.getDefault());
    }

    static String formatTime(long epochMillis, Locale locale) {
        if (epochMillis <= 0L) {
            return "";
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(new Date(epochMillis));
    }

    static String formatDayMonth(long epochMillis) {
        return new SimpleDateFormat("d MMM", Locale.ENGLISH).format(new java.util.Date(epochMillis));
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
