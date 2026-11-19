package center.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneCard
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.localization.AppLanguage
import center.ui.theme.Gold
import center.ui.theme.Navy
import center.ui.theme.Rose
import center.ui.theme.Success
import center.ui.theme.White

@Composable
internal fun StudentSearch(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
) {
    CenterOneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ReceptionIcon(Icons.Outlined.QrCodeScanner, Gold)
                Text(dashboardString(DashboardText.SEARCH_HEADING), style = MaterialTheme.typography.headlineSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CenterOneTextField(
                    value = state.query,
                    onValueChange = { onEvent(DashboardEvent.QueryChanged(it)) },
                    modifier =
                        Modifier.weight(1f).onPreviewKeyEvent {
                            if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                if (it.type == KeyEventType.KeyUp) onEvent(DashboardEvent.Search)
                                true
                            } else {
                                false
                            }
                        },
                    placeholder = { Text(dashboardString(DashboardText.SEARCH_HINT)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onEvent(DashboardEvent.Search) }),
                )
                CenterOnePrimaryButton(
                    onClick = { onEvent(DashboardEvent.Search) },
                    modifier = Modifier.heightIn(min = 56.dp),
                    enabled = state.query.isNotBlank(),
                ) {
                    Text(dashboardString(DashboardText.SEARCH))
                }
            }
            if (state.searchSubmitted && state.student == null) {
                Text(dashboardString(DashboardText.NO_STUDENT), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun StudentActionCard(
    student: QuickStudent,
    language: AppLanguage,
    onEvent: (DashboardEvent) -> Unit,
) {
    val name = student.name.resolve(language)
    val paid = student.paymentStatus == PaymentStatus.Paid
    CenterOneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CenterOneAvatar(name.take(1))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    Text(student.grade.resolve(language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(
                    dashboardString(if (paid) DashboardText.PAID else DashboardText.OVERDUE),
                    if (paid) StatusTone.Success else StatusTone.Overdue,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onEvent(DashboardEvent.RegisterAttendance(student.code)) },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Navy),
                ) {
                    Text(dashboardString(DashboardText.CHECK_IN))
                }
                CenterOnePrimaryButton(onClick = { onEvent(DashboardEvent.PaySubscription(student.code)) }) {
                    Text(dashboardString(DashboardText.PAY_SUBSCRIPTION))
                }
            }
        }
    }
}

@Composable
internal fun ReceptionShortcuts(onEvent: (DashboardEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 620.dp) 3 else 1
        val tileWidth = (maxWidth - 12.dp * (columns - 1)) / columns
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val tile = Modifier.widthIn(max = tileWidth).fillMaxWidth()
            Shortcut(DashboardText.ADD_STUDENT, Icons.Outlined.Add, Success, tile) { onEvent(DashboardEvent.AddStudent) }
            Shortcut(DashboardText.COLLECT_SUBSCRIPTION, Icons.Outlined.Add, Gold, tile) { onEvent(DashboardEvent.CollectSubscription) }
            Shortcut(DashboardText.RECORD_EXPENSE, Icons.Outlined.Remove, Rose, tile) { onEvent(DashboardEvent.RecordExpense) }
        }
    }
}

@Composable
private fun Shortcut(
    title: DashboardText,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 72.dp),
        shape = MaterialTheme.shapes.medium,
        color = White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReceptionIcon(icon, accent)
            Text(dashboardString(title), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
internal fun ReceptionIcon(
    icon: ImageVector,
    accent: Color,
) {
    Box(
        Modifier.size(40.dp).background(accent.copy(alpha = 0.12f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
    }
}
