package net.smartlogic.unitconverter.graphy.integration;

import net.smartlogic.unitconverter.graphy.model.GraphyOutput;

/**
 * Host contract for embedding an optional collapsible Graphy panel in calculator screens.
 */
public interface GraphyPanelHost {

    void showGraphy(GraphyOutput output);

    void hideGraphy();

    boolean isGraphyVisible();
}
