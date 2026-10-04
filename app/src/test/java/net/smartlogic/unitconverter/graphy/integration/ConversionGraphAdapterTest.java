package net.smartlogic.unitconverter.graphy.integration;

import android.app.Application;
import android.content.Context;
import net.smartlogic.unitconverter.graphy.model.*;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.utils.Conversions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class ConversionGraphAdapterTest {
    private final Context context = RuntimeEnvironment.getApplication();
    private final Conversions conversions = Conversions.getInstance();
    private Unit unit(int category, int id) {
        return conversions.getById(category).getUnits().stream().filter(u -> u.getId() == id).findFirst().orElseThrow();
    }
    @Test public void everyAffineTemperaturePairAgreesWithExistingConverter() {
        for (Unit from : conversions.getById(Conversion.TEMPERATURE).getUnits()) {
            for (Unit to : conversions.getById(Conversion.TEMPERATURE).getUnits()) {
                var transformation = ConversionGraphAdapter.transformation(conversions, Conversion.TEMPERATURE, from, to);
                if (from.getId() == Unit.GAS_MARK || to.getId() == Unit.GAS_MARK) continue;
                for (double input : new double[]{-40, 0, 100, 273.15, 1e6}) {
                    double result = conversions.convertTemperatureValue(input, from, to);
                    assertEquals(from.getSymbol() + " → " + to.getSymbol(), result, transformation.apply(input),
                            Math.max(1e-9, Math.abs(result) * 1e-12));
                }
            }
        }
    }
    @Test public void knownTemperatureReferencesAndReverseAreExplained() {
        Unit c = unit(Conversion.TEMPERATURE, Unit.CELSIUS), f = unit(Conversion.TEMPERATURE, Unit.FAHRENHEIT);
        var forward = ConversionGraphAdapter.unit(context, conversions, Conversion.TEMPERATURE,
                c, f, 100, 212, "100 °C", "212 °F", "°C", "°F");
        assertTrue(forward.getNodes().get(2).displayValue().contains("100 × 1.8"));
        assertTrue(forward.getNodes().get(2).displayValue().contains("+ 32"));
        var reverse = ConversionGraphAdapter.unit(context, conversions, Conversion.TEMPERATURE,
                f, c, 212, 100, "212 °F", "100 °C", "°F", "°C");
        assertTrue(reverse.getNodes().get(2).displayValue().contains("−"));
        assertEquals("100 °C", reverse.getResult());
    }
    @Test public void gasMarkAndFuelHaveHonestSemanticsIncludingZero() {
        Unit c = unit(Conversion.TEMPERATURE, Unit.CELSIUS), gas = unit(Conversion.TEMPERATURE, Unit.GAS_MARK);
        assertEquals(ConversionTransformation.Kind.DIRECT,
                ConversionGraphAdapter.transformation(conversions, Conversion.TEMPERATURE, c, gas).kind());
        Unit litre = unit(Conversion.FUEL, Unit.L_100K);
        Unit other = conversions.getById(Conversion.FUEL).getUnits().stream()
                .filter(u -> u.getId() != Unit.L_100K).findFirst().orElseThrow();
        var output = ConversionGraphAdapter.unit(context, conversions, Conversion.FUEL,
                litre, other, 5, conversions.convertFuelValue(5, litre, other), "5 L/100km", "exact display", "L/100km", other.getSymbol());
        assertTrue(output.getNodes().get(2).displayValue().contains("÷"));
        assertEquals("exact display", output.getResult());
        var zero = ConversionGraphAdapter.unit(context, conversions, Conversion.FUEL,
                litre, other, 0, 0, "0 L/100km", "0", "L/100km", other.getSymbol());
        assertEquals(GraphyNodeType.EXPLANATION, zero.getNodes().get(1).type());
        assertTrue(zero.getExplanationTemplate().contains("undefined at zero"));
    }
    @Test public void everyLinearCategoryUsesItsExistingFactorsIncludingReverse() {
        int[] categories = {Conversion.LENGTH, Conversion.MASS, Conversion.AREA, Conversion.VOLUME,
                Conversion.SPEED, Conversion.TIME, Conversion.STORAGE, Conversion.COOKING,
                Conversion.POWER, Conversion.PRESSURE, Conversion.ENERGY, Conversion.TORQUE};
        for (int category : categories) {
            var units = conversions.getById(category).getUnits();
            Unit first = units.get(0), last = units.get(units.size() - 1);
            for (double input : new double[]{0, 5, 1e12, 0.000000123}) {
                var forward = ConversionGraphAdapter.transformation(conversions, category, first, last);
                var reverse = ConversionGraphAdapter.transformation(conversions, category, last, first);
                assertEquals(conversions.convert(input, first, last), forward.apply(input),
                        Math.max(1e-20, Math.abs(forward.apply(input)) * 1e-12));
                assertEquals(conversions.convert(input, last, first), reverse.apply(input),
                        Math.max(1e-20, Math.abs(reverse.apply(input)) * 1e-12));
            }
        }
    }
    @Test public void currencyIncludesCachedTimeAndZeroWithoutClaimingPermanentRate() {
        var output = ConversionGraphAdapter.currency(context, 0, 0, 83.5, "0 USD", "0 INR", "USD", "INR", 1234);
        assertEquals(GraphyNodeType.DERIVED, output.getNodes().get(1).type());
        assertTrue(output.getExplanationTemplate().contains("Cached exchange rate"));
        assertEquals(1234L, output.getMetadata().get("rateTimestamp"));
        assertTrue(ConversionGraphAdapter.currency(context, 1, Double.POSITIVE_INFINITY, Double.NaN,
                "1 USD", "invalid", "USD", "INR", 0).isEmpty());
    }
    @Test public void unitFactorsFollowConfiguredDecimalSeparator() {
        var preferences = net.smartlogic.unitconverter.helper.Preferences.getInstance(context);
        var store = preferences.getPreferences();
        String key = net.smartlogic.unitconverter.helper.Preferences.PREFS_DECIMAL_SEPARATOR;
        String before = preferences.getDecimalSeparator();
        store.edit().putString(key, ",").commit();
        try {
            Unit c = unit(Conversion.TEMPERATURE, Unit.CELSIUS), f = unit(Conversion.TEMPERATURE, Unit.FAHRENHEIT);
            var graph = ConversionGraphAdapter.unit(context, conversions, Conversion.TEMPERATURE,
                    c, f, 100, 212, "100 °C", "212 °F", "°C", "°F");
            assertTrue(graph.getNodes().get(2).displayValue().contains("1,8"));
        } finally { store.edit().putString(key, before).commit(); }
    }
    @Test public void formulaUsesEnteredValueRatherThanFloatArtifacts() {
        var output = ConversionGraphAdapter.currency(context, (double) 1.2f, 100.2, 83.5,
                "1.2 USD", "100.20 INR", "USD", "INR", 0);
        assertEquals("1.2 × 83.5", output.getNodes().get(2).displayValue());
        assertEquals("100.20 INR", output.getResult());
    }
    @Test public void tinyFactorsDoNotRoundToZero() {
        var output = ConversionGraphAdapter.currency(context, 1, 1e-12, 1e-12, "1 X", "0.000000000001 Y", "X", "Y", 0);
        assertTrue(output.getNodes().get(1).displayValue().contains("E"));
        assertFalse(output.getNodes().get(1).displayValue().contains("= 0 Y"));
    }
}
