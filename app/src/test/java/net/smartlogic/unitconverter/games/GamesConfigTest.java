package net.smartlogic.unitconverter.games;

import org.junit.Test;
import static org.junit.Assert.*;

public class GamesConfigTest {
    @Test public void embeddedEntryPreservesCentralRoute() {
        assertEquals("https://smartuptech.in/games/index.html?embedded=1#shape_fit", GamesConfig.embeddedUrl());
        assertTrue(GamesConfig.isTrustedUrl(GamesConfig.embeddedUrl()));
    }

    @Test public void onlyExactHttpsOriginAndGamesPathAreAllowed() {
        assertTrue(GamesConfig.isTrustedUrl("https://smartuptech.in/games/future-game.mjs"));
        assertTrue(GamesConfig.isTrustedUrl("https://smartuptech.in:443/games/index.html"));
        for (String malicious : new String[]{
                "http://smartuptech.in/games/index.html", "https://evil.smartuptech.in/games/",
                "https://smartuptech.in.evil.com/games/", "https://evil.com/games/",
                "https://smartuptech.in:444/games/", "https://user@smartuptech.in/games/",
                "https://smartuptech.in/private/", "https://smartuptech.in/games/../private/",
                "https://smartuptech.in/games/%2e%2e/private/", "https://smartuptech.in/games/%252e%252e/",
                "file:///games/index.html", "javascript:alert(1)", "intent://games/", "not a url"
        }) assertFalse(malicious, GamesConfig.isTrustedUrl(malicious));
        assertFalse(GamesConfig.isTrustedUrl(null));
    }

    @Test public void idsRemainGenericAndDatesMustBeReal() {
        assertTrue(GamesConfig.isGameId("future_game_42"));
        assertFalse(GamesConfig.isGameId("../game"));
        assertFalse(GamesConfig.isGameId("game-name"));
        assertFalse(GamesConfig.isGameId("a".repeat(49)));
        assertTrue(GamesConfig.isDate("2028-02-29"));
        assertFalse(GamesConfig.isDate("2026-02-29"));
        assertFalse(GamesConfig.isDate("2026-10-06Z"));
    }
}
