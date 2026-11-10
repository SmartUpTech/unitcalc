package net.smartlogic.unitconverter;

import android.app.Application;
import java.math.BigDecimal;

import net.smartlogic.unitconverter.graphy.model.*;
import net.smartlogic.unitconverter.model.*;
import net.smartlogic.unitconverter.utils.Conversions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 34)
public class ToolGraphReviewTest {
    private GraphyNode node(GraphyOutput graph, String id) {
        return graph.getNodes().stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
    }
    private boolean edge(GraphyOutput graph, String from, String to) {
        return graph.getConnections().stream().anyMatch(e -> e.fromNodeId().equals(from) && e.toNodeId().equals(to));
    }
    @Test public void adjustmentsConsumeBothBaseAndPercentageAmount() {
        for (var mode : new PercentageCalculator.Mode[]{PercentageCalculator.Mode.INCREASE, PercentageCalculator.Mode.DECREASE}) {
            var result = PercentageCalculator.calculate(new PercentageCalculator.Inputs(mode, new BigDecimal("250"), new BigDecimal("12")));
            var graph = PercentageGraphBuilder.build(RuntimeEnvironment.getApplication(), result, result.result().toPlainString());
            assertEquals("30", node(graph, "derived").displayValue());
            assertTrue(edge(graph, "first", "adjust"));
            assertTrue(edge(graph, "derived", "adjust"));
            assertTrue(edge(graph, "adjust", "result"));
            assertFalse(edge(graph, "derived", "result"));
        }
    }
    @Test public void ratioRuleExplainsDivision() {
        var result = PercentageCalculator.calculate(new PercentageCalculator.Inputs(PercentageCalculator.Mode.WHAT_PERCENT,
                new BigDecimal("30"), new BigDecimal("250")));
        var graph = PercentageGraphBuilder.build(RuntimeEnvironment.getApplication(), result, "12%");
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.percentage_graphy_step_what), graph.getExplanationTemplate());
    }
    @Test public void fuelGraphUsesNormalizedValuesAndPassengerShare() {
        var conversions = Conversions.getInstance();
        var km = conversions.getById(Conversion.LENGTH).getUnits().stream().filter(u -> u.getId() == Unit.KILOMETRE).findFirst().orElseThrow();
        for (var unit : conversions.getById(Conversion.FUEL).getUnits()) {
            var result = FuelEfficiencyCalculator.calculate(new FuelEfficiencyCalculator.Inputs(new BigDecimal("100"), km,
                    new BigDecimal("10"), unit, new BigDecimal("2"), "USD", 4, true), conversions);
            var graph = FuelEfficiencyGraphBuilder.build(RuntimeEnvironment.getApplication(), result);
            assertEquals("200 km", node(graph, "distance").displayValue());
            assertTrue(node(graph, "efficiency").displayValue().endsWith(" L/100 km"));
            assertEquals("4", node(graph, "passengers").displayValue());
            assertTrue(edge(graph, "passengers", "share_op"));
            assertTrue(edge(graph, "share_op", "result"));
            assertEquals(new net.smartlogic.unitconverter.emi.EmiFormatter(RuntimeEnvironment.getApplication()).money(result.costPerPassenger(), "USD"), node(graph, "result").displayValue());
        }
    }
    @Test public void historyReadsOldVersionAndRetainsCurrencyInNewPayloadAndDraft() {
        var state = new FuelHistoryState("100", "10", "2", "4", Unit.KILOMETRE, Unit.L_100K, false, "USD");
        assertEquals("USD", FuelHistoryState.fromPayload(state.toPayload()).currencyCode);
        assertEquals("USD", FuelHistoryState.fromDraft(state.toPayload()).currencyCode);
        assertNull(FuelHistoryState.fromPayload("{\"version\":1,\"price\":\"2\"}").currencyCode);
        assertNull(FuelHistoryState.fromPayload("{\"version\":999}"));
    }
}
