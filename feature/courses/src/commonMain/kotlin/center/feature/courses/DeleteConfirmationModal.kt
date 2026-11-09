package center.feature.courses

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.feature.courses.resources.Res
import center.feature.courses.resources.cancel
import center.feature.courses.resources.confirm_delete
import center.feature.courses.resources.delete_message
import center.feature.courses.resources.delete_title
import center.feature.courses.resources.warning
import center.ui.components.CenterOneConfirmationContent
import center.ui.components.CenterOneModal
import org.jetbrains.compose.resources.stringResource

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
        title = stringResource(Res.string.delete_title),
        message = stringResource(Res.string.delete_message, state.groupTitle),
        confirmLabel = stringResource(Res.string.confirm_delete),
        cancelLabel = stringResource(Res.string.cancel),
        onConfirm = { onEvent(CoursesEvent.ConfirmDeleteGroup) },
        onCancel = { onEvent(CoursesEvent.DismissModal) },
        icon = {
            Icon(
                Icons.Outlined.WarningAmber,
                stringResource(Res.string.warning),
                Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        },
        modifier = modifier,
        enabled = !state.isDeleting,
        error = state.message,
    )
}
