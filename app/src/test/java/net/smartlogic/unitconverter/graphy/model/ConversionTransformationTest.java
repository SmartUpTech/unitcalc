package net.smartlogic.unitconverter.graphy.model;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversionTransformationTest {
    @Test public void linearAndReverseKnownReference() {
        var milesToKm = ConversionTransformation.linear(1.609344);
        assertEquals(8.04672, milesToKm.apply(5), 1e-10);
        assertEquals(5, milesToKm.reversed().apply(8.04672), 1e-10);
    }
    @Test public void affineAndReverseKnownReference() {
        var celsiusToFahrenheit = ConversionTransformation.affine(1.8, 32);
        assertEquals(212, celsiusToFahrenheit.apply(100), 1e-10);
        assertEquals(0, celsiusToFahrenheit.reversed().apply(32), 1e-10);
        assertEquals(-40, celsiusToFahrenheit.apply(-40), 1e-10);
        assertEquals(100, celsiusToFahrenheit.reversed().apply(212), 1e-10);
    }
    @Test public void zeroLargeAndSmallValues() {
        var scale = ConversionTransformation.linear(0.001);
        assertEquals(0, scale.apply(0), 0);
        assertEquals(1e12, scale.apply(1e15), 1e-2);
        assertEquals(1.23e-10, scale.apply(1.23e-7), 1e-22);
        assertEquals(-1.5, scale.apply(-1500), 1e-12);
    }
    @Test public void dynamicRateAndReciprocal() {
        assertEquals(8350, ConversionTransformation.rate(83.5).apply(100), 1e-10);
        assertEquals(47.043, ConversionTransformation.reciprocal(235.215).apply(5), 1e-10);
        assertFalse(ConversionTransformation.rate(0).isValid());
        assertFalse(ConversionTransformation.linear(0).reversed().isValid());
    }
    @Test public void compositePreservesStepOrderAndCanReverse() {
        var composite = new ConversionTransformation(ConversionTransformation.Kind.COMPOSITE, List.of(
                new ConversionTransformation.Step(ConversionTransformation.Operator.OFFSET, -32),
                new ConversionTransformation.Step(ConversionTransformation.Operator.SCALE, 5.0 / 9)));
        assertEquals(100, composite.apply(212), 1e-10);
        assertEquals(212, composite.reversed().apply(100), 1e-10);
    }
    @Test public void directMappingDoesNotInventAnEquation() {
        assertTrue(ConversionTransformation.direct().isValid());
        assertTrue(Double.isNaN(ConversionTransformation.direct().apply(1)));
    }
}
