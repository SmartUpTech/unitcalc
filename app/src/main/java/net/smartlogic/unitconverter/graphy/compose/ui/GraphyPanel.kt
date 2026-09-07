package net.smartlogic.unitconverter.graphy.compose.ui

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.graphy.model.GraphyOutput
import net.smartlogic.unitconverter.graphy.renderer.FlowchartGraphView
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme

@Composable
fun GraphyPanel(
    expression: String,
    output: GraphyOutput?,
    viewTheme: GraphyViewTheme,
    modifier: Modifier = Modifier,
) {
    val spacing = GraphyThemeTokens.spacing
    val semanticColors = GraphyThemeTokens.semanticColors
    val panelDescription = stringResource(R.string.graphy_panel_content_description)
    var showLegend by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(semanticColors.background)
            .semantics { contentDescription = panelDescription },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
        ) {
            GraphyHeader(
                expression = expression,
                onInfoClick = { showLegend = true },
            )

            if (output != null) {
                GraphyFlowchartHost(
                    output = output,
                    viewTheme = viewTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = spacing.md)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }

    if (showLegend) {
        GraphyLegendDialog(onDismiss = { showLegend = false })
    }
}

@Composable
private fun GraphyHeader(
    expression: String,
    onInfoClick: () -> Unit,
) {
    val spacing = GraphyThemeTokens.spacing
    val infoDescription = stringResource(R.string.graphy_info_content_description)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.graphy),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (expression.isNotEmpty()) {
                Text(
                    text = expression,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }
        }
        Text(
            text = "ⓘ",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onInfoClick)
                .padding(start = spacing.sm)
                .semantics { contentDescription = infoDescription },
        )
    }
}

@Composable
private fun GraphyLegendDialog(onDismiss: () -> Unit) {
    val semanticColors = GraphyThemeTokens.semanticColors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.graphy_legend_title),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendRow(color = semanticColors.input, label = stringResource(R.string.graphy_legend_input))
                LegendRow(color = semanticColors.constant, label = stringResource(R.string.graphy_legend_constant))
                LegendRow(color = semanticColors.operation, label = stringResource(R.string.graphy_legend_operator))
                LegendRow(color = semanticColors.derived, label = stringResource(R.string.graphy_legend_result))
                LegendRow(color = semanticColors.resultFill, label = stringResource(R.string.graphy_legend_final))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

@Composable
private fun LegendRow(
    color: androidx.compose.ui.graphics.Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, shape = MaterialTheme.shapes.small),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
                    ViewGroup.LayoutParams.MATCH_PARENT,
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
                    Gravity.CENTER_HORIZONTAL,
                ),
            )
        },
    )
}
