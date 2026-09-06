package net.smartlogic.unitconverter.graphy.compose.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.graphy.model.GraphyOutput
import net.smartlogic.unitconverter.graphy.renderer.FlowchartGraphView
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme

@Composable
fun GraphyPanel(
    explanation: String,
    output: GraphyOutput?,
    viewTheme: GraphyViewTheme,
    modifier: Modifier = Modifier,
) {
    val spacing = GraphyThemeTokens.spacing
    val elevation = GraphyThemeTokens.elevation
    val shapes = GraphyThemeTokens.shapes
    val textStyles = GraphyThemeTokens.textStyles
    val panelDescription = stringResource(R.string.graphy_panel_content_description)

    Card(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = panelDescription },
        shape = shapes.card,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.card),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.sm),
        ) {
            Text(
                text = explanation.ifEmpty { stringResource(R.string.graphy_explanation_label) },
                style = textStyles.explanation,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = explanation },
            )

            if (output != null) {
                GraphyFlowchartHost(
                    output = output,
                    viewTheme = viewTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = spacing.xs)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun GraphyFlowchartHost(
    output: GraphyOutput,
    viewTheme: GraphyViewTheme,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val flowchartDescription = stringResource(R.string.graphy_flowchart_content_description)

    AndroidView(
        modifier = modifier.semantics { contentDescription = flowchartDescription },
        factory = {
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
        },
        update = { container ->
            container.removeAllViews()
            val graphView = FlowchartGraphView(context)
            graphView.setGraph(output, viewTheme)
            container.addView(
                graphView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        },
    )
}
