package net.smartlogic.unitconverter.helper;

import org.junit.Test;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class InAppReviewTest {
    private final long day = TimeUnit.DAYS.toMillis(1);
    @Test public void requiresEnoughUseAndTime() {
        assertFalse(InAppReview.eligible(20 * day, 0, 100, 0));
        assertFalse(InAppReview.eligible(7 * day, day, 10, 0));
        assertFalse(InAppReview.eligible(8 * day, day, 9, 0));
        assertTrue(InAppReview.eligible(8 * day, day, 10, 0));
    }
    @Test public void cooldownAndClockRollbackPreventPrompts() {
        assertFalse(InAppReview.eligible(100 * day, day, 100, 20 * day));
        assertTrue(InAppReview.eligible(110 * day, day, 100, 20 * day));
        assertFalse(InAppReview.eligible(8 * day, 9 * day, 100, 0));
        assertFalse(InAppReview.eligible(100 * day, day, 100, 101 * day));
    }
}
