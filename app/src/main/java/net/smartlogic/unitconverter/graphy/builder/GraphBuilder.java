package net.smartlogic.unitconverter.graphy.builder;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.utils.EvalTrace;

/**
 * Builds renderer-neutral Graphy output from a calculation snapshot and trace.
 */
public interface GraphBuilder {

    @NonNull
    GraphyOutput build(@NonNull CalculationSnapshot snapshot, @NonNull EvalTrace trace);
}
