package net.smartlogic.unitconverter.games;

import org.junit.Test;
import static org.junit.Assert.*;

public class GamesSessionTest {
    private static final String DAY = "2026-10-06";

    @Test public void onlyThirdAndEighthGenuineCompletionsTriggerAnOpportunity() {
        GamesSession session = new GamesSession(3, 8, 2);
        for (int i = 1; i <= 20; i++) {
            boolean opportunity = session.recordCompletion("game_" + i, DAY);
            assertEquals("completion " + i, i == 3 || i == 8, opportunity);
            if (opportunity) assertTrue(session.reserveAd());
        }
        assertEquals(20, session.completionCount());
        assertEquals(2, session.adCount());
        assertFalse(session.reserveAd());
    }

    @Test public void duplicatesReloadsAndRecreationCannotAdvanceTheSameSession() {
        GamesSession session = new GamesSession(3, 8, 2);
        assertFalse(session.recordCompletion("future_game", DAY));
        for (int i = 0; i < 100; i++) assertFalse(session.recordCompletion("future_game", DAY));
        assertEquals(1, session.completionCount());
        assertFalse(session.recordCompletion("other_game", DAY));
        assertTrue(session.recordCompletion("third_game", DAY));
    }

    @Test public void missedThresholdIsNeverRetriedLate() {
        GamesSession session = new GamesSession(3, 8, 2);
        for (int i = 1; i <= 9; i++) {
            assertEquals(i == 3 || i == 8, session.recordCompletion("game_" + i, DAY));
        }
        assertEquals(0, session.adCount());
    }

    @Test public void sessionPersistsAcrossDatesButNewProcessStartsFresh() {
        GamesSession session = new GamesSession(3, 8, 2);
        assertFalse(session.recordCompletion("future_game", DAY));
        assertFalse(session.recordCompletion("future_game", "2026-10-07"));
        assertTrue(session.recordCompletion("future_game", "2026-10-08"));
        assertEquals(0, new GamesSession(3, 8, 2).completionCount());
    }

    @Test public void invalidCallbacksNeverCount() {
        GamesSession session = new GamesSession(3, 8, 2);
        assertFalse(session.recordCompletion("../game", DAY));
        assertFalse(session.recordCompletion("valid_game", "2026-02-29"));
        assertEquals(0, session.completionCount());
    }
}
