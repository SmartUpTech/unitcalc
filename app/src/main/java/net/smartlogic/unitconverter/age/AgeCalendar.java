package net.smartlogic.unitconverter.age;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import net.smartlogic.unitconverter.R;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** User-reviewed calendar handoff; no Calendar Provider reads/writes or permissions. */
public final class AgeCalendar {
    private AgeCalendar() { }

    public static String recurrence(LocalDate birth) {
        return birth.getMonthValue() == 2 && birth.getDayOfMonth() == 29
                ? "FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=28,29;BYSETPOS=-1"
                : "FREQ=YEARLY;BYMONTH=" + birth.getMonthValue() + ";BYMONTHDAY=" + birth.getDayOfMonth();
    }

    public static Intent createIntent(Context context, LocalDate birth, LocalDate today, boolean yearly) {
        LocalDate event = AgeCalculator.nextBirthday(birth, today);
        Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, context.getString(R.string.age_birthday))
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
        // Let the insert editor infer one all-day date. Supplying next midnight as
        // an end extra makes Google Calendar include that following date in its draft.
        if (yearly) intent.putExtra(CalendarContract.Events.RRULE, recurrence(birth));
        return intent;
    }
}
