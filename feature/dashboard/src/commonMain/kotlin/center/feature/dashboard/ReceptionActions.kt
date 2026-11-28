package center.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import center.domain.usecase.LessonAttendanceOption
import center.ui.components.CenterOneAccentIcon
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneButtonTone
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.localization.AppLanguage
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.text.isolateLtr
import center.ui.theme.Gold
import center.ui.theme.Indigo
import center.ui.theme.Rose

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
                    enabled = state.query.isNotBlank(),
                ) {
                    Text(dashboardString(DashboardText.SEARCH))
                }
            }
            if (state.candidates.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    items(state.candidates, key = { it.code }) { candidate ->
                        CenterOneTextButton(onClick = { onEvent(DashboardEvent.SelectStudent(candidate.code)) }) {
                            Text("${candidate.name} · ${candidate.label}")
                        }
                    }
                }
            } else if (state.searchSubmitted && state.student == null) {
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
    attendanceGroupId: String? = null,
    groupMenuExpanded: Boolean = false,
    checkingIn: Boolean = false,
    lessonAttendanceOptions: List<LessonAttendanceOption> = emptyList(),
) {
    val name = student.name.resolve(language)
    val groups = student.attendanceGroups
    val groupId = attendanceGroupId?.takeIf { id -> groups?.any { it.id == id } == true } ?: groups?.singleOrNull()?.id
    val session = student.attendance?.firstOrNull { it.groupId == groupId.orEmpty() }
    val canSelectGroup = groups == null || groups.isEmpty() || groupId != null
    val (label, tone) =
        when (student.paymentStatus) {
            PaymentStatus.Paid -> DashboardText.PAID to StatusTone.Success
            PaymentStatus.Overdue -> DashboardText.OVERDUE to StatusTone.Overdue
            PaymentStatus.Due -> DashboardText.DUE to StatusTone.Pending
            PaymentStatus.NoSubscription -> DashboardText.NO_SUBSCRIPTION to StatusTone.Pending
        }
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
                if (student.isStudent) StatusBadge(dashboardString(label), tone)
            }
            if (groups != null && groups.size > 1) {
                CenterOneDropdown(
                    value = groups.firstOrNull { it.id == groupId }?.name.orEmpty(),
                    label = dashboardString(DashboardText.ATTENDANCE_GROUP),
                    options = groups.map { CenterOneDropdownOption(it.id, it.name) },
                    expanded = groupMenuExpanded,
                    onExpandedChange = { onEvent(DashboardEvent.AttendanceGroupMenuChanged(it)) },
                    onOptionSelected = { onEvent(DashboardEvent.SelectAttendanceGroup(it)) },
                    enabled = !checkingIn,
                )
            }
            if (session != null) {
                val enter = session.checkIn.isolateLtr()
                val exit = session.checkOut?.isolateLtr() ?: "—"
                Text(LocalizedText("حضور $enter · انصراف $exit", "Check-in $enter · Check-out $exit").resolve(language))
            }
            if (session == null && lessonAttendanceOptions.isNotEmpty()) {
                Text(LocalizedText("دروس اليوم", "Today's lessons").resolve(language), style = MaterialTheme.typography.titleSmall)
                lessonAttendanceOptions.forEach { lesson ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (student.isStudent) {
                            Checkbox(
                                checked = lesson.attended,
                                onCheckedChange = { onEvent(DashboardEvent.ToggleLessonAttendance(lesson.key)) },
                                enabled = !checkingIn,
                            )
                        }
                        Text(
                            "${lesson.lessonName} · ${lesson.startMinute.toClockLabel(
                                language,
                            )}–${lesson.endMinute.toClockLabel(language)}",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (lesson.isMakeup) {
                            Text(AppStrings.get(AppStringKey.MakeupSession, language), color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CenterOnePrimaryButton(
                    onClick = {
                        onEvent(
                            DashboardEvent.RegisterAttendance(
                                student.code,
                                groupId,
                                lessonAttendanceOptions.filter { it.attended }.map { it.key }.toSet(),
                            ),
                        )
                    },
                    enabled = !checkingIn && canSelectGroup && session == null,
                    tone = CenterOneButtonTone.Success,
                ) {
                    Text(dashboardString(DashboardText.CHECK_IN))
                }
                if (student.attendance != null) {
                    CenterOnePrimaryButton(
                        onClick = { onEvent(DashboardEvent.CheckOut(student.code, groupId)) },
                        enabled = !checkingIn && session != null && session.checkOut == null,
                    ) {
                        Text(LocalizedText("تسجيل انصراف", "Check-out").resolve(language))
                    }
                }
                if (student.isStudent) {
                    CenterOnePrimaryButton(onClick = { onEvent(DashboardEvent.PaySubscription(student.code)) }) {
                        Text(dashboardString(DashboardText.PAY_SUBSCRIPTION))
                    }
                }
            }
        }
    }
}

private fun Int.toClockLabel(language: AppLanguage): String {
    val hour = this / 60
    val minute = this % 60
    val twelveHour = (hour % 12).let { if (it == 0) 12 else it }
    val suffix =
        when (language) {
            AppLanguage.Arabic -> if (hour >= 12) "م" else "ص"
            AppLanguage.English -> if (hour >= 12) "PM" else "AM"
        }
    return "${twelveHour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} $suffix"
}

@Composable
internal fun ReceptionShortcuts(onEvent: (DashboardEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 620.dp) 3 else 1
        val tileWidth = (maxWidth - 12.dp * (columns - 1)) / columns
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val tile = Modifier.widthIn(max = tileWidth).fillMaxWidth()
            Shortcut(DashboardText.ADD_STUDENT, Icons.Outlined.Add, Indigo, tile) { onEvent(DashboardEvent.AddStudent) }
            Shortcut(DashboardText.COLLECT_SUBSCRIPTION, Icons.Outlined.Add, Gold, tile) { onEvent(DashboardEvent.CollectSubscription) }
            Shortcut(DashboardText.RECORD_EXPENSE, Icons.Outlined.Remove, Rose, tile) { onEvent(DashboardEvent.RecordExpense) }
            CenterOneCard(tile.heightIn(min = 72.dp), onClick = { onEvent(DashboardEvent.CloseOpenCheckIns) }) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        AppStrings.get(AppStringKey.CloseOpenCheckIns, LocalAppLanguage.current),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
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
    CenterOneCard(modifier.heightIn(min = 72.dp), onClick = onClick) {
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
private fun ReceptionIcon(
    icon: ImageVector,
    accent: Color,
) {
    CenterOneAccentIcon(accent) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}
