package net.smartlogic.unitconverter.age;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Calendar arithmetic; never uses the duration converter's fixed-length years/months. */
public final class AgeCalculator {
    public static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    public static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);

    private AgeCalculator() { }

    public record Breakdown(int years, int months, int days, LocalDate anniversary,
                            LocalDate monthAnchor) { }

    public record Result(LocalDate birth, LocalDate asOf, Breakdown age,
                         LocalDate nextBirthday, Breakdown countdown, long totalDays) {
        public long totalMonths() { return age.years() * 12L + age.months(); }
        public long totalWeeks() { return totalDays / 7; }
        public long totalHours() { return totalDays * 24; }
        public long totalMinutes() { return totalDays * 1440; }
    }

    public static Result calculate(LocalDate birth, LocalDate asOf) {
        if (birth == null || asOf == null || birth.isBefore(MIN_DATE) || birth.isAfter(MAX_DATE)
                || asOf.isBefore(MIN_DATE) || asOf.isAfter(MAX_DATE) || birth.isAfter(asOf)) {
            throw new IllegalArgumentException("Dates must be in range and birth must precede as-of");
        }
        LocalDate next = nextBirthday(birth, asOf);
        return new Result(birth, asOf, between(birth, asOf), next,
                between(asOf, next), ChronoUnit.DAYS.between(birth, asOf));
    }

    /** plusYears clamps Feb 29 to Feb 28; recompute from the original date each year. */
    public static LocalDate nextBirthday(LocalDate birth, LocalDate from) {
        LocalDate candidate = birth.withYear(Math.max(birth.getYear(), from.getYear()));
        return candidate.isBefore(from) ? birth.withYear(candidate.getYear() + 1) : candidate;
    }

    static Breakdown between(LocalDate start, LocalDate end) {
        int years = end.getYear() - start.getYear();
        if (start.plusYears(years).isAfter(end)) years--;
        LocalDate anniversary = start.plusYears(years);
        int months = (end.getYear() - anniversary.getYear()) * 12
                + end.getMonthValue() - anniversary.getMonthValue();
        months = Math.min(11, months);
        if (anniversary.plusMonths(months).isAfter(end)) months--;
        LocalDate anchor = anniversary.plusMonths(months);
        return new Breakdown(years, months, (int) ChronoUnit.DAYS.between(anchor, end), anniversary, anchor);
    }
}
