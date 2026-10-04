package net.smartlogic.unitconverter.graphy.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.graphy.model.GraphyNode
import net.smartlogic.unitconverter.graphy.model.GraphyOutput
import net.smartlogic.unitconverter.graphy.renderer.FlowchartGraphView
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme

@Composable
fun GraphyPanel(
    output: GraphyOutput?,
    viewTheme: GraphyViewTheme,
    modifier: Modifier = Modifier,
) {
    val spacing = GraphyThemeTokens.spacing
    val semanticColors = GraphyThemeTokens.semanticColors
    var showLegend by remember { mutableStateOf(false) }
    var selectedNode by remember(output) { mutableStateOf<GraphyNode?>(null) }

    Column(
        modifier = modifier.fillMaxSize().background(semanticColors.background)
            .padding(spacing.sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.graphy_explanation_label),
                style = MaterialTheme.typography.titleMedium,
                color = semanticColors.primaryText, modifier = Modifier.weight(1f))
            GraphyHeader(onInfoClick = { showLegend = true })
        }
        Text(stringResource(R.string.graphy_node_hint), style = MaterialTheme.typography.bodySmall,
            color = semanticColors.secondaryText, modifier = Modifier.padding(bottom = spacing.sm))
        if (output == null || output.isEmpty) {
            Text(stringResource(R.string.graphy_unavailable), color = semanticColors.secondaryText)
        } else {
            (output.metadata[GraphyOutput.METADATA_NOTICE] as? String)?.let { notice ->
                Text(notice, style = MaterialTheme.typography.bodySmall,
                    color = semanticColors.secondaryText, modifier = Modifier.padding(bottom = spacing.sm))
            }
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                val graphWidth = constraints.maxWidth
                Column {
                    GraphyFlowchartHost(output, viewTheme, graphWidth, { selectedNode = it },
                        modifier = Modifier.fillMaxWidth())
                    GraphyExplanation(output)
                }
            }
        }
    }

    if (showLegend) {
        GraphyLegendDialog(onDismiss = { showLegend = false })
    }
    selectedNode?.let { node ->
        if (output != null) GraphyNodeDetails(node, output, viewTheme) { selectedNode = null }
    }
}

@Composable
private fun GraphyNodeDetails(node: GraphyNode, output: GraphyOutput,
                             viewTheme: GraphyViewTheme, onDismiss: () -> Unit) {
    val spacing = GraphyThemeTokens.spacing
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(viewTheme.nodeCaption(node)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(node.displayValue, style = MaterialTheme.typography.bodyLarge)
                node.formula?.takeIf { it != node.displayValue }?.let { Text(it) }
                node.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                output.connections.filter { it.toNodeId() == node.id }.forEach { edge ->
                    output.nodes.firstOrNull { it.id == edge.fromNodeId() }?.let { source ->
                        Text(stringResource(R.string.graphy_dependency, source.displayValue))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } })
}

@Composable
private fun GraphyHeader(
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infoDescription = stringResource(R.string.graphy_info_content_description)
    IconButton(onClick = onInfoClick,
        modifier = modifier.semantics { contentDescription = infoDescription }) {
        Text(text = stringResource(R.string.graphy_info_symbol),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GraphyExplanation(output: GraphyOutput) {
    val spacing = GraphyThemeTokens.spacing
    var expanded by remember(output) { mutableStateOf(false) }
    val explanation = output.explanationTemplate ?: return
    Column(modifier = Modifier.padding(vertical = spacing.sm)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.graphy_hide_details else R.string.graphy_show_details))
        }
        if (expanded) Text(text = explanation, style = MaterialTheme.typography.bodyMedium,
            color = GraphyThemeTokens.semanticColors.secondaryText)
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
            Column(verticalArrangement = Arrangement.spacedBy(GraphyThemeTokens.spacing.sm)) {
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
        horizontalArrangement = Arrangement.spacedBy(GraphyThemeTokens.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(GraphyThemeTokens.spacing.md)
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
    maxWidth: Int,
    onNodeSelected: (GraphyNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GraphyThemeTokens.semanticColors
    val configuration = LocalConfiguration.current
    AndroidView(
        modifier = modifier,
        factory = { context -> FlowchartGraphView(context) },
        onReset = null,
        onRelease = { it.setOnNodeClickListener(null) },
        update = { graphView ->
            graphView.setOnNodeClickListener { onNodeSelected(it) }
            graphView.setMaxWidth(maxWidth)
            graphView.setBackgroundColor(colors.background.toArgb())
            graphView.setGraph(output, viewTheme, configuration)
        },
    )
}
