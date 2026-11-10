package net.smartlogic.unitconverter.fuel;

import net.smartlogic.unitconverter.model.Unit;
import org.junit.Test;
import static org.junit.Assert.*;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(application = android.app.Application.class, sdk = 34)
public class FuelHistoryStateTest {
    @Test public void payloadRoundTrip() {
        FuelHistoryState original = new FuelHistoryState(
                "420",
                "15",
                "102.50",
                "4",
                Unit.KILOMETRE,
                Unit.L_100K,
                true);
        FuelHistoryState restored = FuelHistoryState.fromPayload(original.toPayload());
        assertNotNull(restored);
        assertEquals("420", restored.distance);
        assertEquals("15", restored.efficiency);
        assertEquals("102.50", restored.price);
        assertEquals("4", restored.passengers);
        assertEquals(Unit.KILOMETRE, restored.distanceUnitId);
        assertEquals(Unit.L_100K, restored.efficiencyUnitId);
        assertTrue(restored.roundTrip);
    }

    @Test public void legacyExpressionParsesCoreFields() {
        FuelHistoryState legacy = FuelHistoryState.fromLegacyExpression(
                "420 · 15 L/100km · ₹102.50/L · 4 pax · RT");
        assertNotNull(legacy);
        assertEquals("420", legacy.distance);
        assertEquals("15", legacy.efficiency);
        assertEquals("102.50", legacy.price);
        assertEquals("4", legacy.passengers);
        assertTrue(legacy.roundTrip);
    }
}
