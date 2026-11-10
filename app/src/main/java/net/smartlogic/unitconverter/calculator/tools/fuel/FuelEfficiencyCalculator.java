package net.smartlogic.unitconverter.calculator.tools.fuel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.utils.Conversions;
import net.smartlogic.unitconverter.utils.DecimalMath;

/** Trip fuel volume and cost from distance, consumption, price, passengers and optional round trip. */
public final class FuelEfficiencyCalculator {
    public static final int MIN_PASSENGERS = 1;
    public static final int MAX_PASSENGERS = 99;
    public static final BigDecimal MAX_DISTANCE = new BigDecimal("1000000");
    public static final BigDecimal MAX_EFFICIENCY = new BigDecimal("1000");
    public static final BigDecimal MAX_PRICE = new BigDecimal("100000");

    private FuelEfficiencyCalculator() {
    }

    public record Inputs(
            BigDecimal distance,
            Unit distanceUnit,
            BigDecimal efficiency,
            Unit efficiencyUnit,
            BigDecimal pricePerLitre,
            String currencyCode,
            int passengers,
            boolean roundTrip) {
    }

    public record Result(
            BigDecimal fuelLitres,
            BigDecimal tripCost,
            BigDecimal costPerPassenger,
            Inputs inputs,
            BigDecimal distanceKm,
            BigDecimal litresPer100Km) {
    }

    public static Result calculate(Inputs inputs, Conversions conversions) {
        if (inputs == null || conversions == null) throw new IllegalArgumentException("inputs");
        if (inputs.distance() == null || inputs.efficiency() == null || inputs.pricePerLitre() == null) {
            throw new IllegalArgumentException("values");
        }
        if (inputs.distance().signum() <= 0 || inputs.efficiency().signum() <= 0) {
            throw new IllegalArgumentException("positive");
        }
        if (inputs.pricePerLitre().signum() < 0) throw new IllegalArgumentException("price");
        if (inputs.passengers() < MIN_PASSENGERS || inputs.passengers() > MAX_PASSENGERS) {
            throw new IllegalArgumentException("passengers");
        }
        if (inputs.distance().compareTo(MAX_DISTANCE) > 0
                || inputs.efficiency().compareTo(MAX_EFFICIENCY) > 0
                || inputs.pricePerLitre().compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("range");
        }
        Unit km = unit(conversions, Conversion.LENGTH, Unit.KILOMETRE);
        Unit litrePer100 = unit(conversions, Conversion.FUEL, Unit.L_100K);
        BigDecimal distanceKm = conversions.convert(inputs.distance(), inputs.distanceUnit(), km);
        if (inputs.roundTrip()) distanceKm = DecimalMath.checked(distanceKm.multiply(new BigDecimal("2")));
        BigDecimal lPer100 = conversions.convertFuelValue(inputs.efficiency(), inputs.efficiencyUnit(), litrePer100);
        BigDecimal fuelLitres = DecimalMath.divide(distanceKm.multiply(lPer100), new BigDecimal("100"));
        BigDecimal tripCost = moneyScale(fuelLitres.multiply(inputs.pricePerLitre()), inputs.currencyCode());
        BigDecimal perPassenger = moneyScale(
                DecimalMath.divide(tripCost, BigDecimal.valueOf(inputs.passengers())), inputs.currencyCode());
        return new Result(fuelLitres, tripCost, perPassenger, inputs, distanceKm, lPer100);
    }

    private static Unit unit(Conversions conversions, int category, int id) {
        return conversions.getById(category).getUnits().stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unit"));
    }

    private static BigDecimal moneyScale(BigDecimal amount, String currencyCode) {
        int scale = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        if (scale < 0) scale = 2;
        return amount.setScale(scale, RoundingMode.HALF_EVEN);
    }
}
