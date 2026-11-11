package center.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import center.ui.components.BilingualHeading
import center.ui.components.CenterOneCard
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import center.ui.theme.CenterOneSpacing

@Composable
fun QuickCheckInSection(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneCard(modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(CenterOneSpacing.Large)) {
            val wideLayout = maxWidth >= 1000.dp
            val title: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BilingualHeading(dashboardString(DashboardText.QUICK_CHECK_IN), dashboardString(DashboardText.QUICK_CHECK_IN_EN))
                    Text(
                        dashboardString(DashboardText.SCAN_DESCRIPTION),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val input: @Composable (Modifier) -> Unit = { fieldModifier ->
                CenterOneTextField(
                    value = state.studentCode,
                    onValueChange = { onEvent(DashboardEvent.StudentCodeChanged(it)) },
                    modifier =
                        fieldModifier.onPreviewKeyEvent {
                            if (it.key == Key.Enter) {
                                if (it.type == KeyEventType.KeyUp && state.canCheckIn) onEvent(DashboardEvent.SubmitCheckIn)
                                true
                            } else {
                                false
                            }
                        },
                    enabled = state.checkIn !is CheckInState.Submitting,
                    isError = state.checkIn is CheckInState.Failure,
                    placeholder = { Text(dashboardString(DashboardText.SCAN_PLACEHOLDER)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (state.canCheckIn) onEvent(DashboardEvent.SubmitCheckIn) }),
                )
            }
            val button: @Composable () -> Unit = {
                CenterOnePrimaryButton(onClick = { onEvent(DashboardEvent.SubmitCheckIn) }, enabled = state.canCheckIn) {
                    Text(
                        dashboardString(
                            if (state.checkIn is CheckInState.Submitting) DashboardText.CHECKING_IN else DashboardText.CHECK_IN,
                        ),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
                if (wideLayout) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Large),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        title()
                        input(Modifier.weight(1f))
                        button()
                    }
                } else {
                    title()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        input(Modifier.weight(1f))
                        button()
                    }
                }
                when (val progress = state.checkIn) {
                    is CheckInState.Failure ->
                        Text(
                            progress.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    is CheckInState.Success -> Text(progress.message, style = MaterialTheme.typography.bodySmall)
                    CheckInState.Idle, CheckInState.Submitting -> Unit
                }
            }
        }
    }
}
