package net.smartlogic.unitconverter.calculator.tools.percentage;

import org.junit.Test;
import static org.junit.Assert.*;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(application = android.app.Application.class, sdk = 34)
public class PercentageHistoryStateTest {
    @Test public void payloadRoundTrip() {
        PercentageHistoryState original = new PercentageHistoryState(
                PercentageCalculator.Mode.INCREASE, "250", "12", 1);
        PercentageHistoryState restored = PercentageHistoryState.fromPayload(original.toPayload());
        assertNotNull(restored);
        assertEquals(PercentageCalculator.Mode.INCREASE, restored.mode);
        assertEquals("250", restored.first);
        assertEquals("12", restored.second);
        assertEquals(1, restored.focus);
    }

    @Test public void legacyExpression() {
        PercentageHistoryState legacy = PercentageHistoryState.fromLegacyExpression(
                "Increase · 250 · 12%");
        assertNotNull(legacy);
        assertEquals(PercentageCalculator.Mode.INCREASE, legacy.mode);
        assertEquals("250", legacy.first);
        assertEquals("12", legacy.second);
    }
}
