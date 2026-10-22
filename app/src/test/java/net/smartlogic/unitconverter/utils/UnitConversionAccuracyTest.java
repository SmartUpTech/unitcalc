package net.smartlogic.unitconverter.utils;

import java.math.BigDecimal;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import org.junit.Test;
import static org.junit.Assert.*;

public class UnitConversionAccuracyTest {
    private final Conversions conversions = Conversions.getInstance();
    private Unit unit(int category, int id) {
        return conversions.getById(category).getUnits().stream().filter(u -> u.getId() == id).findFirst().get();
    }
    private void conversion(int category, int from, int to, String input, String expected) {
        BigDecimal result = conversions.convert(new BigDecimal(input), unit(category, from), unit(category, to));
        assertEquals(from + " → " + to, 0, new BigDecimal(expected).compareTo(result));
    }

    @Test public void knownReferenceRelationshipsAreExact() {
        conversion(Conversion.TIME, Unit.YEAR, Unit.MONTH, "99", "1188");
        conversion(Conversion.TIME, Unit.MONTH, Unit.YEAR, "1188", "99");
        conversion(Conversion.TIME, Unit.YEAR, Unit.MONTH, "0.1", "1.2");
        conversion(Conversion.TIME, Unit.SECOND, Unit.MINUTE, "60", "1");
        conversion(Conversion.TIME, Unit.YEAR, Unit.DAY, "1", "365");
        conversion(Conversion.LENGTH, Unit.METRE, Unit.CENTIMETRE, "1", "100");
        conversion(Conversion.LENGTH, Unit.MILE, Unit.FEET, "1", "5280");
        conversion(Conversion.LENGTH, Unit.MILE, Unit.FURLONG, "99000000", "792000000");
        conversion(Conversion.VOLUME, Unit.CUBIC_YARD, Unit.CUBIC_FOOT, "99", "2673");
        conversion(Conversion.MASS, Unit.POUND, Unit.OUNCE, "1", "16");
        conversion(Conversion.VOLUME, Unit.GALLON, Unit.LITRE, "1", "3.785411784");
        conversion(Conversion.COOKING, Unit.TEASPOON, Unit.TABLESPOON, "3", "1");
        conversion(Conversion.COOKING, Unit.GALLON, Unit.TEASPOON, "1", "768");
        conversion(Conversion.STORAGE, Unit.BIT, Unit.BYTE, "8000000000000", "1000000000000");
        conversion(Conversion.STORAGE, Unit.KILOBYTE, Unit.BYTE, "1", "1024");
        conversion(Conversion.STORAGE, Unit.BYTE, Unit.BIT, "9007199254740993", "72057594037927944");
        conversion(Conversion.ENERGY, Unit.FT_LBF, Unit.IN_LBF, "99000000", "1188000000");
        conversion(Conversion.TORQUE, Unit.FT_LBF, Unit.IN_LBF, "1", "12");
        conversion(Conversion.SPEED, Unit.M_MIN, Unit.KM_HR, "1000", "60");
    }

    @Test public void everyLinearPairHasConsistentInverseAcrossMagnitudes() {
        for (int category : conversions.getCategoryIds()) {
            if (category == Conversion.TEMPERATURE || category == Conversion.FUEL) continue;
            for (Unit from : conversions.getById(category).getUnits()) {
                for (Unit to : conversions.getById(category).getUnits()) {
                    for (String text : new String[]{"0", "1", "-99", "0.125", "1000000000", "0.000000000001"}) {
                        BigDecimal input = new BigDecimal(text);
                        BigDecimal result = conversions.convert(input, from, to);
                        BigDecimal restored = conversions.convert(result, to, from);
                        BigDecimal tolerance = input.abs().max(BigDecimal.ONE).scaleByPowerOfTen(-31);
                        assertTrue(from.getSymbol() + " → " + to.getSymbol(),
                                restored.subtract(input).abs().compareTo(tolerance) <= 0);
                    }
                }
            }
        }
    }

    @Test public void temperatureAndFuelKeepTheirSeparateFormulas() {
        Unit c = unit(Conversion.TEMPERATURE, Unit.CELSIUS), f = unit(Conversion.TEMPERATURE, Unit.FAHRENHEIT);
        assertEquals(32, conversions.convertTemperatureValue(0, c, f), 1e-12);
        assertEquals(-40, conversions.convertTemperatureValue(-40, c, f), 1e-12);
        assertEquals(0, conversions.convertTemperatureValue(273.15,
                unit(Conversion.TEMPERATURE, Unit.KELVIN), c), 1e-12);
        Unit litre = unit(Conversion.FUEL, Unit.L_100K), mpg = unit(Conversion.FUEL, Unit.MPG_US);
        assertEquals(23.5214582, conversions.convertFuelValue(10, litre, mpg), 1e-10);
        assertEquals(10, conversions.convertFuelValue(23.5214582, mpg, litre), 1e-10);
        assertEquals(0, conversions.convertFuelValue(0, litre, mpg), 0);
    }
}
