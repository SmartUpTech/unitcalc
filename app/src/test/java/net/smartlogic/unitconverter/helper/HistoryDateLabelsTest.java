package net.smartlogic.unitconverter.helper;

import android.app.Application;
import net.smartlogic.unitconverter.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Calendar;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
public class HistoryDateLabelsTest {
    @Test public void datesUseLocaleAndIncludeOlderYear() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            Calendar date = Calendar.getInstance();
            date.clear();
            date.set(2020, Calendar.SEPTEMBER, 2, 14, 34);
            String section = HistoryDateLabels.sectionLabel(RuntimeEnvironment.getApplication(), date.getTimeInMillis());
            assertTrue(section, section.contains("sept"));
            assertTrue(section, section.contains("2020"));
        } finally { Locale.setDefault(previous); }
    }
    @Test public void todayAndYesterdayHaveFriendlyLabels() {
        Application app = RuntimeEnvironment.getApplication();
        Calendar day = Calendar.getInstance();
        assertEquals(app.getString(R.string.history_today), HistoryDateLabels.sectionLabel(app, day.getTimeInMillis()));
        day.add(Calendar.DAY_OF_YEAR, -1);
        assertEquals(app.getString(R.string.history_yesterday), HistoryDateLabels.sectionLabel(app, day.getTimeInMillis()));
    }
    @Test public void timeUsesLocaleAndMissingTimeIsEmpty() {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(2024, Calendar.SEPTEMBER, 2, 14, 34);
        assertTrue(HistoryDateLabels.formatTime(date.getTimeInMillis(), Locale.US).contains("2:34"));
        assertEquals("", HistoryDateLabels.formatTime(0, Locale.US));
    }
}
