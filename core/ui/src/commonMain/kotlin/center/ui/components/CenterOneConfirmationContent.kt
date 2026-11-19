package center.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CenterOneConfirmationContent(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
) {
    CenterOneModalContent(
        title = title,
        subtitle = "",
        closeLabel = null,
        onClose = onCancel,
        modifier = modifier,
        maxWidth = 480.dp,
    ) {
        icon()
        Text(message, style = MaterialTheme.typography.bodyMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOneDestructiveButton(onConfirm, Modifier.weight(1f), enabled) { Text(confirmLabel) }
            CenterOneTextButton(onCancel, Modifier.weight(1f), enabled) { Text(cancelLabel) }
        }
    }
}
