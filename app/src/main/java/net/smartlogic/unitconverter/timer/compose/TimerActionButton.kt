package net.smartlogic.unitconverter.timer.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import net.smartlogic.unitconverter.R

/** Shared app theme and action geometry across Timer phases. */
@Composable
internal fun TimerActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    text: String = label,
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.timer_corner_radius))
    val padding = PaddingValues(horizontal = dimensionResource(R.dimen.timer_compact_gap))
    val bounds = modifier.heightIn(min = dimensionResource(R.dimen.converter_touch_target))
        .semantics { contentDescription = label }
    if (primary) {
        Button(onClick, modifier = bounds, enabled = enabled, shape = shape,
            contentPadding = padding, colors = ButtonDefaults.buttonColors()) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        OutlinedButton(onClick, modifier = bounds, enabled = enabled, shape = shape,
            contentPadding = padding) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
