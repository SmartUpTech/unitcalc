package net.smartlogic.unitconverter.widget;

/** Measurement contract shared by fixed tool pages and their space-constraint host. */
public interface ViewportContent {
    void setViewportHeight(int height);
    boolean fitsViewport();
}
