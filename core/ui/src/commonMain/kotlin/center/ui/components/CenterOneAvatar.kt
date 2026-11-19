package center.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.theme.Indigo
import center.ui.theme.accentContentColor

@Composable
fun CenterOneAvatar(
    initial: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier.size(40.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = Indigo.accentContentColor(),
    ) {
        Box(contentAlignment = Alignment.Center) { Text(initial, style = MaterialTheme.typography.titleSmall) }
    }
}
