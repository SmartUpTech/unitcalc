package net.smartlogic.unitconverter.age;

import android.content.Context;
import net.smartlogic.unitconverter.R;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** All consumers format the authoritative date result, never parse display text. */
public final class AgeFormatter {
    private final Context context;
    private final Locale locale;

    public AgeFormatter(Context context) {
        this.context = context;
        locale = context.getResources().getConfiguration().getLocales().get(0);
    }

    public String date(LocalDate date) {
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
    }

    public String number(long value) { return NumberFormat.getIntegerInstance(locale).format(value); }

    public String years(int years) { return quantity(R.plurals.age_years, years); }
    public String months(int months) { return quantity(R.plurals.age_months, months); }
    public String days(int days) { return quantity(R.plurals.age_days, days); }
    private String quantity(int id, int value) {
        return context.getResources().getQuantityString(id, value, number(value));
    }

    public String breakdown(AgeCalculator.Breakdown value) {
        return context.getString(R.string.age_breakdown, years(value.years()),
                months(value.months()), days(value.days()));
    }

    public String countdown(AgeCalculator.Result result) {
        return result.nextBirthday().equals(result.asOf()) ? days(0)
                : context.getString(R.string.age_remaining,
                        months(result.countdown().years() * 12 + result.countdown().months()), days(result.countdown().days()));
    }

    public String weekday(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("EEEE", locale));
    }

    public String share(AgeCalculator.Result r) {
        return context.getString(R.string.age_share_body, date(r.birth()), date(r.asOf()),
                breakdown(r.age()), date(r.nextBirthday()), weekday(r.nextBirthday()), countdown(r),
                number(r.age().years()), number(r.totalMonths()), number(r.totalWeeks()),
                number(r.totalDays()), number(r.totalHours()), number(r.totalMinutes()));
    }
}
