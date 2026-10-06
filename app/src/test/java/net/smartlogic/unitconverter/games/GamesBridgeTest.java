package net.smartlogic.unitconverter.games;

import android.app.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class GamesBridgeTest {
    private final GamesBridge bridge = new GamesBridge();
    private static final String DAY = "2026-10-06";
    private static final String START = "{\"type\":\"onGameStarted\",\"bridgeVersion\":1,\"gameId\":\"future_game\",\"date\":\"2026-10-06\"}";
    private static final String FINISH = "{\"type\":\"onGameCompleted\",\"bridgeVersion\":1,\"gameId\":\"future_game\",\"date\":\"2026-10-06\",\"result\":{\"score\":1}}";

    @Before public void configure() { bridge.configure(DAY); }

    @Test public void readyDoesNotNeedAHostDate() {
        assertNotNull(bridge.accept("{\"type\":\"onReady\",\"bridgeVersion\":1}"));
    }

    @Test public void finishRequiresStartAndOnlyCountsOnce() {
        assertNull(bridge.accept(FINISH));
        assertNotNull(bridge.accept(START));
        assertNotNull(bridge.accept(FINISH));
        assertNull(bridge.accept(FINISH));
        assertNotNull(bridge.accept(START));
        assertNull(bridge.accept(FINISH));
    }

    @Test public void exitedAndStaleGamesCannotFinish() {
        bridge.accept(START);
        assertNotNull(bridge.accept(START.replace("onGameStarted", "onGameExited")));
        assertNull(bridge.accept(FINISH));
        bridge.accept(START);
        bridge.configure("2026-10-07");
        assertNull(bridge.accept(FINISH));
        bridge.reset();
        assertNull(bridge.accept(START));
    }

    @Test public void invalidEnvelopeVersionsIdsAndResultsAreRejected() {
        bridge.accept(START);
        for (String invalid : new String[]{"broken", "null", FINISH.replace("\"bridgeVersion\":1", "\"bridgeVersion\":2"),
                FINISH.replace("\"bridgeVersion\":1", "\"bridgeVersion\":\"1\""),
                FINISH.replace("future_game", "../game"), FINISH.replace("future_game", "other_game"),
                FINISH.replace("2026-10-06", "2026-02-29"), FINISH.replace("{\"score\":1}", "\"done\""),
                "a".repeat(GamesConfig.MAX_MESSAGE_LENGTH + 1)}) assertNull(invalid, bridge.accept(invalid));
        assertNull(bridge.accept(null));
    }

    @Test public void catalogErrorsUseGenericErrorEvents() {
        GamesBridge.Event event = bridge.accept("{\"type\":\"onError\",\"bridgeVersion\":1,\"errorCode\":\"CATALOG_LOAD_FAILED\"}");
        assertNotNull(event);
        assertNull(event.gameId());
        assertEquals("CATALOG_LOAD_FAILED", event.errorCode());
    }
}
