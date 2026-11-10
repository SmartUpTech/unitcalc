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
    @Test public void datesUseSelectedLanguageAndIndependentRegionAndIncludeOlderYear() {
        Application app = RuntimeEnvironment.getApplication();
        Preferences preferences = Preferences.getInstance(app);
        preferences.getPreferences().edit().clear().commit();
        preferences.migrateRegionalPreferences(false);
        android.content.res.Configuration configuration = new android.content.res.Configuration(
                app.getResources().getConfiguration());
        configuration.setLocale(Locale.FRANCE);
        android.content.Context french = app.createConfigurationContext(configuration);
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(2020, Calendar.SEPTEMBER, 2, 14, 34);
        String section = HistoryDateLabels.sectionLabel(french, date.getTimeInMillis());
        assertTrue(section, section.startsWith("sept")); // US order, French month word.
        assertTrue(section, section.contains("2020"));
        preferences.applyRegion("FR");
        String frenchOrder = HistoryDateLabels.sectionLabel(french, date.getTimeInMillis());
        assertTrue(frenchOrder, frenchOrder.startsWith("2"));
        assertTrue(frenchOrder, frenchOrder.contains("sept"));
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
