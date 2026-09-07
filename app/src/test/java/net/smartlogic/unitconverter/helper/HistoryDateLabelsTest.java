package net.smartlogic.unitconverter.helper;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HistoryDateLabelsTest {

    @Test
    public void formatDayMonth_usesEnglishDayMonth() {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.clear();
        calendar.set(2024, Calendar.SEPTEMBER, 2, 12, 0, 0);
        assertEquals("2 Sep", HistoryDateLabels.formatDayMonth(calendar.getTimeInMillis()));
    }

    @Test
    public void formatTime_usesShortLocaleTime() {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.clear();
        calendar.set(2024, Calendar.SEPTEMBER, 2, 14, 34, 0);
        String formatted = HistoryDateLabels.formatTime(calendar.getTimeInMillis(), Locale.US);
        assertTrue(formatted.contains("2:34"));
    }

    @Test
    public void formatTime_emptyForMissingTimestamp() {
        assertEquals("", HistoryDateLabels.formatTime(0L, Locale.US));
    }
}
