package net.smartlogic.unitconverter.graphy.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import net.smartlogic.unitconverter.R
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.graphy.model.GraphyNode
import net.smartlogic.unitconverter.graphy.model.GraphyNodeType
import net.smartlogic.unitconverter.graphy.model.GraphyOutput
import net.smartlogic.unitconverter.graphy.renderer.FlowchartGraphView
import net.smartlogic.unitconverter.graphy.theme.GraphyViewTheme
import net.smartlogic.unitconverter.graphy.theme.GraphySemanticStyle

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

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(semanticColors.background)
            .padding(spacing.sm),
    ) {
        val graphWidth = constraints.maxWidth
        val download = rememberGraphyDownload(output, (graphWidth / LocalDensity.current.density).toInt())
        // The user permits both scroll axes for Graphy only. New output resets exploration.
        key(output?.calculatorId, output?.expression, output?.result) {
            var collapsedStages by rememberSaveable(output?.calculatorId, output?.expression, output?.result) {
                mutableStateOf<List<Int>>(emptyList())
            }
            val horizontalScroll = rememberScrollState()
            val verticalScroll = rememberScrollState()
            Column(Modifier.fillMaxWidth().verticalScroll(verticalScroll).heightIn(min = maxHeight)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = output?.metadata?.get(GraphyOutput.METADATA_CONTEXT) as? String ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = semanticColors.primaryText, modifier = Modifier.weight(1f))
                    GraphyHeader(onInfoClick = { showLegend = true })
                    GraphyDownloadButton(download)
                }
                if (output == null || output.isEmpty) {
                    Text(stringResource(R.string.graphy_unavailable), color = semanticColors.secondaryText)
                } else {
                    (output.metadata[GraphyOutput.METADATA_NOTICE] as? String)?.let { notice ->
                        Text(notice, style = MaterialTheme.typography.bodySmall,
                            color = semanticColors.secondaryText, modifier = Modifier.padding(bottom = spacing.sm))
                    }
                    // Only empty space is weighted: short graphs center, long graphs keep
                    // their intrinsic height and every node remains vertically reachable.
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth().horizontalScroll(horizontalScroll)) {
                        GraphyFlowchartHost(output, viewTheme, graphWidth, { selectedNode = it },
                            collapsedStages.toSet(), { collapsedStages = it.sorted() }, { horizontalScroll.value },
                            { dx, dy -> horizontalScroll.dispatchRawDelta(dx); verticalScroll.dispatchRawDelta(dy) },
                            modifier = Modifier.wrapContentWidth(unbounded = true))
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (showLegend) {
        GraphyLegendDialog(output, viewTheme, onDismiss = { showLegend = false })
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
private fun GraphyLegendDialog(output: GraphyOutput?, viewTheme: GraphyViewTheme, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.graphy_legend_title),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GraphyThemeTokens.spacing.sm)) {
                output?.explanationTemplate?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(stringResource(R.string.graphy_node_hint), style = MaterialTheme.typography.bodyMedium)
                LegendRow(GraphyNodeType.INPUT, viewTheme, stringResource(R.string.graphy_legend_input))
                LegendRow(GraphyNodeType.CONSTANT, viewTheme, stringResource(R.string.graphy_legend_constant))
                LegendRow(GraphyNodeType.OPERATION, viewTheme, stringResource(R.string.graphy_legend_operator))
                LegendRow(GraphyNodeType.DERIVED, viewTheme, stringResource(R.string.graphy_legend_result))
                LegendRow(GraphyNodeType.RESULT, viewTheme, stringResource(R.string.graphy_legend_final))
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
    type: GraphyNodeType,
    viewTheme: GraphyViewTheme,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GraphyThemeTokens.spacing.sm),
    ) {
        // The guide uses the same semantic surfaces and dash tokens as the renderer.
        val fill = Color(viewTheme.nodeFill(type))
        val border = Color(viewTheme.nodeBorder(type))
        val stroke = viewTheme.getNodeBorderStroke()
        val dash = viewTheme.getNodeDashLength()
        val gap = viewTheme.getNodeDashGap()
        Canvas(Modifier.size(GraphyThemeTokens.spacing.lg, GraphyThemeTokens.spacing.md)) {
            val radius = CornerRadius(if (type == GraphyNodeType.OPERATION) size.height / 2
                else viewTheme.getNodeCornerRadius())
            drawRoundRect(fill, cornerRadius = radius)
            if (type != GraphyNodeType.RESULT) {
                drawRoundRect(border, cornerRadius = radius, style = Stroke(stroke,
                    pathEffect = if (GraphySemanticStyle.hasDashedBorder(type))
                        PathEffect.dashPathEffect(floatArrayOf(dash, gap)) else null))
            }
        }
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
    collapsedStages: Set<Int>,
    onStagesChanged: (Set<Int>) -> Unit,
    horizontalOffset: () -> Int,
    onPan: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GraphyThemeTokens.semanticColors
    val configuration = LocalConfiguration.current
    AndroidView(
        modifier = modifier,
        factory = { context -> FlowchartGraphView(context) },
        onReset = null,
        onRelease = { it.setOnNodeClickListener(null); it.setOnStageToggleListener(null); it.setOnPanListener(null) },
        update = { graphView ->
            graphView.setOnNodeClickListener { onNodeSelected(it) }
            graphView.setOnStageToggleListener { onStagesChanged(it) }
            graphView.setOnPanListener { dx, dy -> onPan(dx, dy) }
            graphView.setMaxWidth(maxWidth)
            graphView.setBackgroundColor(colors.background.toArgb())
            graphView.setGraph(output, viewTheme, configuration)
            graphView.setCollapsedStages(collapsedStages)
            graphView.setHorizontalViewportOffset(horizontalOffset())
        },
    )
}
