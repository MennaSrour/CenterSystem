package center.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.theme.Pending
import center.ui.theme.PendingContainer
import center.ui.theme.Success
import center.ui.theme.SuccessContainer

enum class StatusTone { Success, Pending }

@Composable
fun StatusBadge(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val foreground =
        when (tone) {
            StatusTone.Success -> Success
            StatusTone.Pending -> Pending
        }
    val background =
        when (tone) {
            StatusTone.Success -> SuccessContainer
            StatusTone.Pending -> PendingContainer
        }
    Surface(modifier = modifier, color = background, contentColor = foreground, shape = CircleShape) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).background(foreground, CircleShape))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
