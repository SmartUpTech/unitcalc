package net.smartlogic.unitconverter.graphy.renderer;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.graphy.theme.GraphyTheme;

/**
 * Default Graphy renderer that draws expression graphs as left-to-right flowcharts.
 */
public final class FlowchartRenderer implements GraphyRenderer {

    @Override
    public boolean supports(@NonNull GraphyOutput output) {
        return !output.isEmpty();
    }

    @NonNull
    @Override
    public View render(@NonNull Context context,
                       @NonNull GraphyOutput output,
                       @NonNull GraphyTheme theme) {
        FlowchartGraphView graphView = new FlowchartGraphView(context);
        graphView.setGraph(output, theme);

        FrameLayout container = new FrameLayout(context);
        container.addView(graphView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        return container;
    }
}
