package center.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import center.ui.theme.CenterOneSpacing

@Composable
fun CenterOneMetricCard(
    title: String,
    value: String,
    caption: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.secondary,
    highlighted: Boolean = false,
) {
    CenterOneCard(modifier, borderColor = if (highlighted) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline) {
        Column(Modifier.fillMaxWidth().padding(CenterOneSpacing.Large), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.background(accent.copy(alpha = 0.1f), MaterialTheme.shapes.small).padding(12.dp)) { icon() }
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(value, style = MaterialTheme.typography.headlineLarge, color = accent)
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
