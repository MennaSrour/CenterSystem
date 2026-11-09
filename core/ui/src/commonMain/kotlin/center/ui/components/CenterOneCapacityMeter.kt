package center.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun CenterOneCapacityMeter(
    label: String,
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    val safeProgress = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = {
                safeProgress
            },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            color = color,
            trackColor =
                color.copy(
                    alpha = 0.1f,
                ),
        )
    }
}
