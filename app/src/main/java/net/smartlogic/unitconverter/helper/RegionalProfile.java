package net.smartlogic.unitconverter.helper;

/** One resolved snapshot for rendering; computation and saved currency identities are independent. */
public final class RegionalProfile {
    public final RegionDefinition region;
    public final NumberFormatSettings numbers;
    public final String currency;
    public final boolean customNumbers;
    public final boolean customCurrency;

    public RegionalProfile(RegionDefinition region, NumberFormatSettings numbers, String currency) {
        this.region = region;
        this.numbers = numbers;
        this.currency = currency;
        customNumbers = !numbers.equals(region.defaults());
        customCurrency = !currency.equals(region.currency);
    }
}
