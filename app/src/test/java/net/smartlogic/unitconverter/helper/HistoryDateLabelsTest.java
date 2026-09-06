package net.smartlogic.unitconverter.helper;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class HistoryDateLabelsTest {

    @Test
    public void formatDayMonth_usesEnglishDayMonth() {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.clear();
        calendar.set(2024, Calendar.SEPTEMBER, 2, 12, 0, 0);
        assertEquals("2 Sep", HistoryDateLabels.formatDayMonth(calendar.getTimeInMillis()));
    }
}
