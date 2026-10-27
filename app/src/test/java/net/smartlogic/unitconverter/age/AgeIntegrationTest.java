package net.smartlogic.unitconverter.age;

import android.app.Application;
import android.content.Intent;
import android.provider.CalendarContract;
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType;
import net.smartlogic.unitconverter.graphy.model.GraphyTopology;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TimeZone;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
public class AgeIntegrationTest {
    @Test public void graphAndShareUseTheAuthoritativeAgeAndAnchors() {
        var context = RuntimeEnvironment.getApplication();
        var r = AgeCalculator.calculate(LocalDate.of(2010, 8, 16), LocalDate.of(2020, 1, 21));
        var f = new AgeFormatter(context);
        var graph = AgeGraphBuilder.build(context, r);
        assertEquals(CalculatorCatalog.ID_AGE, graph.getCalculatorId());
        assertEquals(f.breakdown(r.age()), graph.getResult());
        assertEquals(1, graph.getNodes().stream().filter(n -> n.type() == GraphyNodeType.RESULT).count());
        assertEquals(graph.getNodes().size(), GraphyTopology.orderedNodes(graph).size());
        assertTrue(graph.getNodes().stream().anyMatch(n -> n.id().equals("years") && n.displayValue().contains(f.date(LocalDate.of(2019, 8, 16)))));
        assertTrue(graph.getNodes().stream().anyMatch(n -> n.id().equals("months") && n.displayValue().contains(f.date(LocalDate.of(2020, 1, 16)))));
        assertTrue(f.share(r).contains(graph.getResult()));
        assertTrue(f.share(r).contains(f.number(4960800)));
    }
    @Test public void calendarUsesRealTodayAndUtcAllDayDatesInEveryTimeZone() {
        TimeZone previous = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"Pacific/Honolulu", "Asia/Kolkata", "Pacific/Kiritimati"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                var intent = AgeCalendar.createIntent(RuntimeEnvironment.getApplication(), LocalDate.of(2000, 8, 16), LocalDate.of(2026, 10, 28), false);
                assertEquals(Intent.ACTION_INSERT, intent.getAction());
                assertEquals(CalendarContract.Events.CONTENT_URI, intent.getData());
                assertTrue(intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false));
                assertEquals(LocalDate.of(2027, 8, 16).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1));
                assertEquals(86400000, intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1) - intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1));
                assertFalse(intent.hasExtra(CalendarContract.Events.RRULE));
            }
        } finally { TimeZone.setDefault(previous); }
    }
    @Test public void yearlyBirthdayIncludesTheRequestedRecurrence() {
        var birth = LocalDate.of(2000, 2, 29);
        var intent = AgeCalendar.createIntent(RuntimeEnvironment.getApplication(), birth, LocalDate.of(2025, 1, 1), true);
        assertEquals("FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=28,29;BYSETPOS=-1", intent.getStringExtra(CalendarContract.Events.RRULE));
        assertEquals("FREQ=YEARLY;BYMONTH=8;BYMONTHDAY=16", AgeCalendar.recurrence(LocalDate.of(2010, 8, 16)));
    }
}
