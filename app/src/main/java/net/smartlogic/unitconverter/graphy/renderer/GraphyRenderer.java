package net.smartlogic.unitconverter.graphy.integration;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.theme.GraphyTheme;

/**
 * Contract for Graphy renderers. Implementations ship in later milestones.
 */
public interface GraphyRenderer {

    boolean supports(@NonNull GraphyOutput output);

    @NonNull
    View render(@NonNull Context context,
                @NonNull GraphyOutput output,
                @NonNull GraphyTheme theme);
}
