package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import net.smartlogic.unitconverter.graphy.compose.theme.GraphyThemeTokens

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
    val itemHeight = 36.dp
    val visibleCount = 5
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = values.indexOf(value).coerceAtLeast(0))

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            if (itemSize <= 0) 0 else {
                val center = layoutInfo.viewportEndOffset / 2
                layoutInfo.visibleItemsInfo.minByOrNull { Math.abs((it.offset + it.size / 2) - center) }?.index ?: 0
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { index ->
                val clamped = index.coerceIn(0, values.lastIndex)
                if (values[clamped] != value) {
                    onValueChange(values[clamped])
                }
            }
    }

    LaunchedEffect(value) {
        val target = values.indexOf(value).coerceAtLeast(0)
        if (centerIndex != target) {
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(itemHeight)
                .background(
                    color = semantic.connector.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                ),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            flingBehavior = rememberSnapFlingBehavior(listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = itemHeight * 2)
        ) {
            items(values.size) { index ->
                val itemValue = values[index]
                val isSelected = index == centerIndex
                Text(
                    text = "$itemValue $unitLabel",
                    modifier = Modifier
                        .height(itemHeight)
                        .alpha(if (isSelected) 1f else 0.35f)
                        .padding(horizontal = 4.dp),
                    color = semantic.primaryText,
                    fontSize = if (isSelected) 18.sp else 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
