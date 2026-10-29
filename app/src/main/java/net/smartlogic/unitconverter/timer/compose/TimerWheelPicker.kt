package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens
import net.smartlogic.unitconverter.R

@Composable
fun TimerWheelPicker(
    value: Int,
    range: IntRange,
    unitLabel: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = GraphyThemeTokens.semanticColors
    val values = range.toList()
    val itemHeight = maxOf(dimensionResource(R.dimen.timer_wheel_row_height),
        with(LocalDensity.current) { dimensionResource(R.dimen.timer_picker_size).toSp().toDp() })
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = values.indexOf(value).coerceAtLeast(0))
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            if (itemSize <= 0) 0 else {
                val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                layoutInfo.visibleItemsInfo.minByOrNull { Math.abs((it.offset + it.size / 2) - center) }?.index ?: 0
            }
        }
    }

    LaunchedEffect(listState) {
        var wasScrolling = false
        snapshotFlow {
            listState.isScrollInProgress
        }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (wasScrolling && !scrolling) {
                    val clamped = centerIndex.coerceIn(0, values.lastIndex)
                    if (values[clamped] != currentValue) currentOnValueChange(values[clamped])
                }
                wasScrolling = scrolling
            }
    }

    LaunchedEffect(value) {
        val target = values.indexOf(value).coerceAtLeast(0)
        if (!listState.isScrollInProgress) {
            listState.scrollToItem(target)
        }
    }

    Column(modifier = modifier.fillMaxWidth().semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat(), range.count() - 2)
        setProgress { target -> onValueChange(target.toInt().coerceIn(range)); true }
    }, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(unitLabel, color = semantic.secondaryText, style = MaterialTheme.typography.bodySmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        BoxWithConstraints(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val visibleCount = when {
                maxHeight >= itemHeight * 5 -> 5
                maxHeight >= itemHeight * 3 -> 3
                else -> 1
            }
            Box(Modifier.height(itemHeight * visibleCount), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(itemHeight)
                    .background(
                        color = semantic.connector.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.timer_corner_radius)),
                    ),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                flingBehavior = rememberSnapFlingBehavior(listState),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2))
            ) {
                items(values.size) { index ->
                    val itemValue = values[index]
                    val isSelected = index == centerIndex
                    Text(
                        text = itemValue.toString(),
                        modifier = Modifier
                            .height(itemHeight)
                            .semantics { contentDescription = "$itemValue $unitLabel" }
                            .alpha(if (isSelected) 1f else 0.35f)
                            .padding(horizontal = dimensionResource(R.dimen.converter_small_spacing)),
                        color = semantic.primaryText,
                        style = if (isSelected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
            }
        }
    }
}
