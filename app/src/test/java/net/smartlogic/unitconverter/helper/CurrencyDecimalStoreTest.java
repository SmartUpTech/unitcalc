package net.smartlogic.unitconverter.helper;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class CurrencyDecimalStoreTest {
    @Test public void decimalSnapshotsAndLegacyCachesRemainReadable() throws Exception {
        String response = "[{\"date\":\"2026-10-20\",\"base\":\"USD\",\"quote\":\"INR\",\"rate\":83.25123456789}]";
        var snapshot = CurrencyRateStore.parseResponse(response, 1000);
        assertEquals(0, new BigDecimal("83.25123456789").compareTo(snapshot.rates.get("INR")));
        var restored = CurrencyRateStore.readCache(snapshot.encode(), 0);
        assertNotNull(restored);
        assertEquals(0, snapshot.rates.get("INR").compareTo(restored.rates.get("INR")));
        assertEquals(1000, restored.fetchedAt);
        assertNotNull(CurrencyRateStore.readCache("{\"data\":{\"USD\":1,\"INR\":83.25}}", 1000));
        assertNull(CurrencyRateStore.readCache("{\"data\":{\"USD\":1,\"INR\":0}}", 1000));
        assertNull(CurrencyRateStore.readCache("{\"data\":{\"USD\":1,\"INR\":\"NaN\"}}", 1000));
    }
}
