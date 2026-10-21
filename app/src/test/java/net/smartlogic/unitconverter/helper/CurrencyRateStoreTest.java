package net.smartlogic.unitconverter.helper;

import android.app.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.IOException;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class CurrencyRateStoreTest {
    private static final long NOW = 1800000000000L;
    private static final String RESPONSE = "[{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"EUR\",\"rate\":0.8},"
            + "{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"INR\",\"rate\":80}]";
    private Preferences prefs;

    @Before public void setup() {
        prefs = Preferences.getInstance(RuntimeEnvironment.getApplication());
        prefs.getPreferences().edit().clear().commit();
    }

    @Test public void parsesDatesBaseAndKnownCrossRate() throws Exception {
        CurrencyRateStore.Snapshot snapshot = CurrencyRateStore.parseResponse(RESPONSE, NOW);
        assertEquals(1f, snapshot.rates.get("USD"), 0);
        assertEquals(100f, snapshot.rates.get("INR") / snapshot.rates.get("EUR"), 0.0001f);
        assertEquals("2026-10-20", snapshot.dates.get("INR"));
        assertFalse(snapshot.rates.containsKey("HRK"));
        assertEquals(snapshot.rates, CurrencyRateStore.readCache(snapshot.encode(), 0).rates);
    }

    @Test public void rejectsMissingMalformedAndInvalidRates() {
        String[] invalid = {null, "", "[]", "{}", "not json", RESPONSE.replace("80}", "0}"),
                RESPONSE.replace("80}", "-1}"), RESPONSE.replace("80}", "1e100}"),
                RESPONSE.replace("2026-10-20", "bad"), RESPONSE.replace("USD", "EUR"),
                RESPONSE.replace("INR", "EUR")};
        for (String body : invalid) {
            try { CurrencyRateStore.parseResponse(body, NOW); fail("Accepted invalid snapshot: " + body); }
            catch (org.json.JSONException expected) { }
        }
    }

    @Test public void twelveHoursAndClockRollbackAreHandled() throws Exception {
        CurrencyRateStore.Snapshot snapshot = CurrencyRateStore.parseResponse(RESPONSE, NOW);
        assertTrue(snapshot.isFresh(NOW + 13 * 60 * 1000L));
        assertTrue(snapshot.isFresh(NOW + CurrencyRateStore.REFRESH_INTERVAL - 1));
        assertFalse(snapshot.isFresh(NOW + CurrencyRateStore.REFRESH_INTERVAL));
        assertFalse(snapshot.isFresh(NOW - 1));
        assertFalse(snapshot.isFresh(NOW + 100L * 24 * 60 * 60 * 1000));
    }

    @Test public void freshCacheSkipsFetchAndForceReplacesSnapshotAtomically() throws Exception {
        CurrencyRateStore.refresh(prefs, false, NOW, () -> RESPONSE);
        CurrencyRateStore.refresh(prefs, false, NOW + 1, () -> { throw new AssertionError("Unexpected fetch"); });
        CurrencyRateStore.refresh(prefs, true, NOW + 2, () -> RESPONSE.replace("INR", "GBP"));
        CurrencyRateStore.Snapshot result = CurrencyRateStore.readCache(prefs.getCurrencyResponse(), 0);
        assertFalse(result.rates.containsKey("INR"));
        assertEquals(NOW + 2, prefs.getCurrencyLastUpdateDate());
    }

    @Test public void failedOrInvalidRefreshPreservesCacheAndTimestamp() throws Exception {
        CurrencyRateStore.refresh(prefs, false, NOW, () -> RESPONSE);
        String original = prefs.getCurrencyResponse();
        CurrencyRateStore.Fetcher[] failures = {() -> null, () -> "{}", () -> { throw new IOException("Timeout / HTTP failure"); }};
        for (CurrencyRateStore.Fetcher fetcher : failures) {
            try { CurrencyRateStore.refresh(prefs, true, NOW + 1, fetcher); fail(); }
            catch (Exception expected) { }
            assertEquals(original, prefs.getCurrencyResponse());
            assertEquals(NOW, prefs.getCurrencyLastUpdateDate());
        }
    }

    @Test public void legacyCacheSurvivesOfflineButMigratesOnSuccess() throws Exception {
        String legacy = "{\"data\":{\"USD\":1,\"INR\":80}}";
        prefs.setCurrencySnapshot(legacy, NOW);
        assertEquals("Freecurrencyapi", CurrencyRateStore.readCache(legacy, NOW).provider);
        assertNull(CurrencyRateStore.readCache("{broken", NOW));
        assertNull(CurrencyRateStore.readCache(null, NOW));
        CurrencyRateStore.refresh(prefs, false, NOW + 1, () -> RESPONSE);
        assertEquals("Frankfurter", CurrencyRateStore.readCache(prefs.getCurrencyResponse(), 0).provider);
    }
}
