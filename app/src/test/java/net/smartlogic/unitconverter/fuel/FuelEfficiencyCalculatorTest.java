package net.smartlogic.unitconverter.fuel;

import java.math.BigDecimal;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.utils.Conversions;
import org.junit.Test;
import static org.junit.Assert.*;

public class FuelEfficiencyCalculatorTest {
    private final Conversions conversions = Conversions.getInstance();

    private Unit length(int id) {
        return conversions.getById(Conversion.LENGTH).getUnits().stream()
                .filter(u -> u.getId() == id).findFirst().get();
    }

    private Unit fuel(int id) {
        return conversions.getById(Conversion.FUEL).getUnits().stream()
                .filter(u -> u.getId() == id).findFirst().get();
    }

    @Test public void goldenRoundTripAndPassengers() {
        FuelEfficiencyCalculator.Result result = FuelEfficiencyCalculator.calculate(
                new FuelEfficiencyCalculator.Inputs(
                        new BigDecimal("420"),
                        length(Unit.KILOMETRE),
                        new BigDecimal("15"),
                        fuel(Unit.L_100K),
                        new BigDecimal("102.50"),
                        "INR",
                        4,
                        true),
                conversions);
        assertEquals(0, new BigDecimal("126.0").compareTo(result.fuelLitres().setScale(1, java.math.RoundingMode.HALF_EVEN)));
        assertEquals(0, new BigDecimal("12915.00").compareTo(result.tripCost()));
        assertEquals(0, new BigDecimal("3228.75").compareTo(result.costPerPassenger()));
    }

    @Test public void oneWaySinglePassenger() {
        FuelEfficiencyCalculator.Result result = FuelEfficiencyCalculator.calculate(
                new FuelEfficiencyCalculator.Inputs(
                        new BigDecimal("420"),
                        length(Unit.KILOMETRE),
                        new BigDecimal("15"),
                        fuel(Unit.L_100K),
                        new BigDecimal("102.50"),
                        "INR",
                        1,
                        false),
                conversions);
        assertEquals(0, new BigDecimal("63.0").compareTo(result.fuelLitres().setScale(1, java.math.RoundingMode.HALF_EVEN)));
        assertEquals(0, new BigDecimal("6457.50").compareTo(result.tripCost()));
    }

    @Test public void rejectsNonPositiveDistance() {
        assertThrows(IllegalArgumentException.class, () -> FuelEfficiencyCalculator.calculate(
                new FuelEfficiencyCalculator.Inputs(
                        BigDecimal.ZERO,
                        length(Unit.KILOMETRE),
                        new BigDecimal("15"),
                        fuel(Unit.L_100K),
                        new BigDecimal("1"),
                        "INR",
                        1,
                        false),
                conversions));
    }
}
