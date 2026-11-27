package center.feature.courses

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneConfirmationContent
import center.ui.components.CenterOneModal

@Composable
fun DeleteConfirmationModal(
    state: DeleteGroupState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModal(onDismissRequest = { if (!state.isDeleting) onEvent(CoursesEvent.DismissModal) }) {
        DeleteConfirmationModalContent(state, onEvent, modifier)
    }
}

@Composable
internal fun DeleteConfirmationModalContent(
    state: DeleteGroupState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneConfirmationContent(
        title = appString("delete_title"),
        message = appString("delete_message", state.groupTitle),
        confirmLabel = appString("confirm_delete"),
        cancelLabel = appString("cancel"),
        onConfirm = { onEvent(CoursesEvent.ConfirmDeleteGroup) },
        onCancel = { onEvent(CoursesEvent.DismissModal) },
        icon = {
            Icon(
                Icons.Outlined.WarningAmber,
                appString("warning"),
                Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        },
        modifier = modifier,
        enabled = !state.isDeleting,
        error = state.message,
    )
}
