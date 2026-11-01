package net.smartlogic.unitconverter.graphy.integration;

import net.smartlogic.unitconverter.graphy.model.ConversionTransformation;

/** Validated numbers plus the exact formatted values already shown by the converter. */
public record ConversionSnapshot(double input, double result, String sourceDisplay,
                                 String targetDisplay, String sourceUnit, String targetUnit,
                                 ConversionTransformation transformation, long rateTimestamp) {
    public boolean isValid() {
        return sourceDisplay != null && targetDisplay != null && transformation != null
                && Double.isFinite(input) && Double.isFinite(result) && !sourceDisplay.trim().isEmpty()
                && !targetDisplay.trim().isEmpty() && transformation.isValid();
    }
}
