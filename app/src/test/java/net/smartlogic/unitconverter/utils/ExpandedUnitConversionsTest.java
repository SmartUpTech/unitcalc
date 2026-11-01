package net.smartlogic.unitconverter.utils;

import java.math.BigDecimal;
import java.util.HashSet;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExpandedUnitConversionsTest {
    private final Conversions conversions = Conversions.getInstance();
    private Unit unit(int category, int id) {
        return conversions.getById(category).getUnits().stream().filter(u -> u.getId() == id).findFirst().get();
    }
    private void exact(int category, int from, int to, String input, String expected) {
        BigDecimal result = conversions.convert(new BigDecimal(input), unit(category, from), unit(category, to));
        assertEquals(from + " → " + to, 0, new BigDecimal(expected).compareTo(result));
    }

    @Test public void angleSupportsExactTurnsGradiansAndSignedUnboundedDegrees() {
        exact(Conversion.ANGLE, Unit.REVOLUTION, Unit.DEGREE, "1", "360");
        exact(Conversion.ANGLE, Unit.GRADIAN, Unit.DEGREE, "400", "360");
        exact(Conversion.ANGLE, Unit.DEGREE, Unit.GRADIAN, "90", "100");
        exact(Conversion.ANGLE, Unit.DEGREE, Unit.REVOLUTION, "-180", "-0.5");
        exact(Conversion.ANGLE, Unit.REVOLUTION, Unit.DEGREE, "2", "720");
        BigDecimal radians = conversions.convert(new BigDecimal("180"), unit(Conversion.ANGLE, Unit.DEGREE),
                unit(Conversion.ANGLE, Unit.RADIAN));
        BigDecimal pi = new BigDecimal("3.141592653589793238462643383279503");
        assertTrue(radians.subtract(pi).abs().compareTo(new BigDecimal("1e-33")) <= 0);
        BigDecimal degrees = conversions.convert(pi, unit(Conversion.ANGLE, Unit.RADIAN), unit(Conversion.ANGLE, Unit.DEGREE));
        assertTrue(degrees.subtract(new BigDecimal("180")).abs().compareTo(new BigDecimal("1e-31")) <= 0);
    }

    @Test public void storageDistinguishesAllDecimalAndBinaryPrefixes() {
        int[] decimalBytes = {Unit.DECIMAL_KILOBYTE, Unit.DECIMAL_MEGABYTE, Unit.DECIMAL_GIGABYTE,
                Unit.DECIMAL_TERABYTE, Unit.DECIMAL_PETABYTE, Unit.DECIMAL_EXABYTE};
        int[] decimalBits = {Unit.DECIMAL_KILOBIT, Unit.DECIMAL_MEGABIT, Unit.DECIMAL_GIGABIT,
                Unit.DECIMAL_TERABIT, Unit.DECIMAL_PETABIT, Unit.DECIMAL_EXABIT};
        int[] binaryBytes = {Unit.KILOBYTE, Unit.MEGABYTE, Unit.GIGABYTE, Unit.TERABYTE, Unit.PETABYTE, Unit.EXABYTE};
        int[] binaryBits = {Unit.KILOBIT, Unit.MEGABIT, Unit.GIGABIT, Unit.TERABIT, Unit.PETABIT, Unit.EXABIT};
        for (int i = 0; i < 6; i++) {
            String decimal = java.math.BigInteger.TEN.pow(3 * (i + 1)).toString();
            String binary = java.math.BigInteger.ONE.shiftLeft(10 * (i + 1)).toString();
            exact(Conversion.STORAGE, decimalBytes[i], Unit.BYTE, "1", decimal);
            exact(Conversion.STORAGE, decimalBits[i], Unit.BIT, "1", decimal);
            exact(Conversion.STORAGE, binaryBytes[i], Unit.BYTE, "1", binary);
            exact(Conversion.STORAGE, binaryBits[i], Unit.BIT, "1", binary);
        }
        assertEquals("KiB", unit(Conversion.STORAGE, Unit.KILOBYTE).getSymbol());
        assertEquals(R.string.kibibyte, unit(Conversion.STORAGE, Unit.KILOBYTE).getLabelResource());
        assertEquals("kB", unit(Conversion.STORAGE, Unit.DECIMAL_KILOBYTE).getSymbol());
        exact(Conversion.STORAGE, Unit.MEGABYTE, Unit.DECIMAL_MEGABYTE, "1", "1.048576");
    }

    @Test public void traditionalMassUnitsUseExplicitDefinitions() {
        exact(Conversion.MASS, Unit.QUINTAL, Unit.KILOGRAM, "1", "100");
        exact(Conversion.MASS, Unit.TOLA, Unit.GRAM, "1", "11.6638038");
        exact(Conversion.MASS, Unit.TROY_OUNCE, Unit.GRAM, "1", "31.1034768");
        exact(Conversion.MASS, Unit.TROY_OUNCE, Unit.TOLA, "3", "8");
    }

    @Test public void regionalAreaDefinitionsNeverUseAGenericBigha() {
        exact(Conversion.AREA, Unit.SQ_MILLIMETRE, Unit.SQ_CENTIMETRES, "100", "1");
        exact(Conversion.AREA, Unit.GUNTHA, Unit.SQ_FOOT, "1", "1089");
        exact(Conversion.AREA, Unit.CENT, Unit.SQ_FOOT, "1", "435.6");
        exact(Conversion.AREA, Unit.MARLA, Unit.SQ_FOOT, "1", "272.25");
        assertFalse(conversions.getById(Conversion.AREA).getUnits().stream().anyMatch(u ->
                u.getId() == Unit.BIGHA_ASSAM || u.getId() == Unit.BIGHA_RAJASTHAN_PUCCA
                        || u.getId() == Unit.BIGHA_RAJASTHAN_KACCHA));
        exact(Conversion.AREA, Unit.GUNTHA, Unit.ACRE, "40", "1");
        exact(Conversion.AREA, Unit.CENT, Unit.ACRE, "100", "1");
    }

    @Test public void volumeSeparatesOilAndBeerBarrels() {
        exact(Conversion.VOLUME, Unit.MICROLITRE, Unit.MILLILITRE, "1000", "1");
        exact(Conversion.VOLUME, Unit.CENTILITRE, Unit.MILLILITRE, "1", "10");
        exact(Conversion.VOLUME, Unit.DECILITRE, Unit.MILLILITRE, "1", "100");
        exact(Conversion.VOLUME, Unit.OIL_BARREL_US, Unit.GALLON, "1", "42");
        exact(Conversion.VOLUME, Unit.BARREL, Unit.GALLON, "1", "31.5");
        exact(Conversion.VOLUME, Unit.OIL_BARREL_US, Unit.LITRE, "1", "158.987294928");
    }

    @Test public void cookingVariantsUseOneDefinitionAcrossCookingAndVolume() {
        exact(Conversion.COOKING, Unit.METRIC_CUP, Unit.MILLILITRE, "1", "250");
        exact(Conversion.COOKING, Unit.TABLESPOON_AU, Unit.MILLILITRE, "1", "20");
        exact(Conversion.COOKING, Unit.TEASPOON_METRIC, Unit.MILLILITRE, "1", "5");
        exact(Conversion.COOKING, Unit.TABLESPOON_METRIC, Unit.MILLILITRE, "1", "15");
        exact(Conversion.COOKING, Unit.TEASPOON_IMPERIAL, Unit.GALLON_UK, "768", "1");
        exact(Conversion.COOKING, Unit.TABLESPOON_IMPERIAL, Unit.GALLON_UK, "256", "1");
        exact(Conversion.COOKING, Unit.TEASPOON_IMPERIAL, Unit.TABLESPOON_IMPERIAL, "3", "1");
        for (int id : new int[]{Unit.METRIC_CUP, Unit.TABLESPOON_AU, Unit.TEASPOON_IMPERIAL,
                Unit.TABLESPOON_IMPERIAL, Unit.TEASPOON_METRIC, Unit.TABLESPOON_METRIC}) {
            assertSame(unit(Conversion.COOKING, id), unit(Conversion.VOLUME, id));
        }
    }

    @Test public void pressureAndTorqueReferencesAgree() {
        exact(Conversion.PRESSURE, Unit.MILLIBAR, Unit.PASCAL, "1", "100");
        exact(Conversion.PRESSURE, Unit.INCH_MERCURY, Unit.PASCAL, "1", "3386.389");
        exact(Conversion.PRESSURE, Unit.CM_WATER, Unit.PASCAL, "1", "98.0665");
        exact(Conversion.PRESSURE, Unit.INCH_WATER, Unit.PASCAL, "1", "249.08891");
        exact(Conversion.PRESSURE, Unit.INCH_WATER, Unit.CM_WATER, "1", "2.54");
        exact(Conversion.TORQUE, Unit.KGF_M, Unit.N_M, "1", "9.80665");
        exact(Conversion.TORQUE, Unit.N_CM, Unit.N_M, "100", "1");
    }

    @Test public void registryExposesAngleAndPreservesLegacySelections() {
        assertEquals(14, Conversion.CURRENCY);
        assertEquals(15, Conversion.ANGLE);
        assertTrue(conversions.getCategoryIds().contains(Conversion.ANGLE));
        assertFalse(conversions.getCategoryIds().contains(Conversion.CURRENCY));
        int[] legacyStorage = {Unit.BIT, Unit.BYTE, Unit.KILOBIT, Unit.KILOBYTE, Unit.MEGABIT, Unit.MEGABYTE,
                Unit.GIGABIT, Unit.GIGABYTE, Unit.TERABIT, Unit.TERABYTE, Unit.PETABIT, Unit.PETABYTE, Unit.EXABIT, Unit.EXABYTE};
        for (int i = 0; i < legacyStorage.length; i++) {
            assertEquals(legacyStorage[i], conversions.getById(Conversion.STORAGE).getUnits().get(i).getId());
        }
        for (int category : conversions.getCategoryIds()) {
            var ids = new HashSet<Integer>();
            for (Unit unit : conversions.getById(category).getUnits()) assertTrue(ids.add(unit.getId()));
        }
    }
}
