package center.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import center.ui.theme.CenterOneSpacing
import center.ui.theme.White
import center.ui.theme.accentContentColor

@Composable
fun CenterOneMetricCard(
    title: String,
    value: String,
    caption: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.secondary,
    gradient: Brush? = null,
) {
    val labelColor = if (gradient == null) MaterialTheme.colorScheme.onSurfaceVariant else White
    val valueColor = if (gradient == null) accent.accentContentColor() else White
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(CenterOneSpacing.Large), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOneAccentIcon(if (gradient == null) accent else White, content = icon)
            Text(title, style = MaterialTheme.typography.bodySmall, color = labelColor)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(value, style = MaterialTheme.typography.headlineLarge, color = valueColor)
                Text(caption, style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
    }
    if (gradient == null) CenterOneCard(modifier) { body() } else CenterOneGradientCard(gradient, modifier, body)
}
