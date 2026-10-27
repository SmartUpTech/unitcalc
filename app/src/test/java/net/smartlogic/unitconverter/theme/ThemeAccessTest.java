package net.smartlogic.unitconverter.theme;

import org.junit.Test;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class ThemeAccessTest {
    @Test public void exactlyThreeThemesAreFreeWithoutAnExpiry() {
        assertEquals(3, CalculatorThemes.all().stream().filter(t -> ThemeAccess.isAlwaysFree(t.id())).count());
        for (String id : new String[]{"porcelain_white", "oyster_cream", "titanium_gray"}) {
            assertTrue(ThemeAccess.isAvailable(id, false, 0, Long.MAX_VALUE));
        }
        assertFalse(ThemeAccess.isAvailable("glacier", false, 0, 100));
    }
    @Test public void expiryBoundaryIsExclusiveAndProDoesNotAuthorizeUnknownIds() {
        assertTrue(ThemeAccess.isAvailable("glacier", false, 101, 100));
        assertFalse(ThemeAccess.isAvailable("glacier", false, 100, 100));
        assertTrue(ThemeAccess.isAvailable("glacier", true, 0, 100));
        assertFalse(ThemeAccess.isAvailable("removed", true, Long.MAX_VALUE, 100));
        assertFalse(ThemeAccess.isAvailable(null, true, Long.MAX_VALUE, 100));
    }
    @Test public void durationIsSevenContinuousDaysAcrossTimeZones() {
        TimeZone before = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"UTC", "America/New_York", "Asia/Kolkata"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                assertEquals(604800000L, ThemeAccess.REWARD_DURATION_MILLIS);
            }
        } finally { TimeZone.setDefault(before); }
    }
}
