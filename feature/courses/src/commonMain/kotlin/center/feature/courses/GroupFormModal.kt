package center.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.feature.courses.resources.Res
import center.feature.courses.resources.add_title
import center.feature.courses.resources.arabic_system
import center.feature.courses.resources.cancel
import center.feature.courses.resources.close
import center.feature.courses.resources.commission
import center.feature.courses.resources.edit_title
import center.feature.courses.resources.form_subtitle
import center.feature.courses.resources.grade
import center.feature.courses.resources.languages_system
import center.feature.courses.resources.max_capacity
import center.feature.courses.resources.mixed_system
import center.feature.courses.resources.monthly_price
import center.feature.courses.resources.room
import center.feature.courses.resources.save
import center.feature.courses.resources.subject
import center.feature.courses.resources.system
import center.feature.courses.resources.teacher
import center.ui.components.CenterOneChoiceGroup
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun GroupFormModal(
    state: GroupFormState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModal(onDismissRequest = { if (!state.isSubmitting) onEvent(CoursesEvent.DismissModal) }) {
        GroupFormModalContent(state, onEvent, modifier)
    }
}

@Composable
internal fun GroupFormModalContent(
    state: GroupFormState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModalContent(
        title = stringResource(if (state.mode is GroupFormMode.Edit) Res.string.edit_title else Res.string.add_title),
        subtitle = stringResource(Res.string.form_subtitle),
        closeLabel = stringResource(Res.string.close),
        onClose = { onEvent(CoursesEvent.DismissModal) },
        modifier = modifier,
        enabled = !state.isSubmitting,
        maxWidth = 920.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val twoColumns = maxWidth >= 600.dp
            val right: @Composable (Modifier) -> Unit = { columnModifier ->
                Column(columnModifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GroupInput(state.subject, stringResource(Res.string.subject), {
                        onEvent(CoursesEvent.SubjectChanged(it))
                    }, !state.isSubmitting, state.fieldErrors[GroupField.Subject])
                    CenterOneDropdown(
                        value =
                            state.teachers
                                .firstOrNull { it.id == state.selectedTeacherId }
                                ?.name
                                .orEmpty(),
                        label = stringResource(Res.string.teacher),
                        options = state.teachers.map { CenterOneDropdownOption(it.id, it.name) },
                        expanded =
                            state.expandedDropdown == GroupDropdown.Teacher,
                        onExpandedChange = {
                            onEvent(CoursesEvent.DropdownChanged(if (it) GroupDropdown.Teacher else null))
                        },
                        onOptionSelected = { onEvent(CoursesEvent.TeacherSelected(it)) },
                        enabled = !state.isSubmitting && state.teachers.isNotEmpty(),
                        error = state.fieldErrors[GroupField.Teacher],
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CenterOneDropdown(
                        value =
                            state.grades
                                .firstOrNull { it.id == state.selectedGradeId }
                                ?.label
                                .orEmpty(),
                        label = stringResource(Res.string.grade),
                        options = state.grades.map { CenterOneDropdownOption(it.id, it.label) },
                        expanded =
                            state.expandedDropdown == GroupDropdown.Grade,
                        onExpandedChange = {
                            onEvent(CoursesEvent.DropdownChanged(if (it) GroupDropdown.Grade else null))
                        },
                        onOptionSelected = { onEvent(CoursesEvent.GradeSelected(it)) },
                        enabled = !state.isSubmitting && state.grades.isNotEmpty(),
                        error = state.fieldErrors[GroupField.Grade],
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(Res.string.system), style = MaterialTheme.typography.labelSmall)
                    CenterOneChoiceGroup(
                        labels =
                            listOf(
                                stringResource(Res.string.arabic_system),
                                stringResource(Res.string.languages_system),
                                stringResource(Res.string.mixed_system),
                            ),
                        selectedIndex = CourseSystem.entries.indexOf(state.system),
                        onSelected = {
                            onEvent(CoursesEvent.SystemSelected(CourseSystem.entries[it]))
                        },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            val left: @Composable (Modifier) -> Unit = { columnModifier ->
                Column(columnModifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GroupInput(state.monthlyPrice, stringResource(Res.string.monthly_price), {
                        onEvent(CoursesEvent.MonthlyPriceChanged(it))
                    }, !state.isSubmitting, state.fieldErrors[GroupField.MonthlyPrice], KeyboardType.Decimal)
                    GroupInput(state.teacherCommission, stringResource(Res.string.commission), {
                        onEvent(CoursesEvent.CommissionChanged(it))
                    }, !state.isSubmitting, state.fieldErrors[GroupField.Commission], KeyboardType.Decimal)
                    GroupInput(state.maxCapacity, stringResource(Res.string.max_capacity), {
                        onEvent(CoursesEvent.MaxCapacityChanged(it))
                    }, !state.isSubmitting, state.fieldErrors[GroupField.MaxCapacity], KeyboardType.Number)
                    CenterOneDropdown(
                        value =
                            state.rooms
                                .firstOrNull { it.id == state.selectedRoomId }
                                ?.label
                                .orEmpty(),
                        label = stringResource(Res.string.room),
                        options = state.rooms.map { CenterOneDropdownOption(it.id, it.label) },
                        expanded =
                            state.expandedDropdown == GroupDropdown.Room,
                        onExpandedChange = {
                            onEvent(CoursesEvent.DropdownChanged(if (it) GroupDropdown.Room else null))
                        },
                        onOptionSelected = { onEvent(CoursesEvent.RoomSelected(it)) },
                        enabled = !state.isSubmitting && state.rooms.isNotEmpty(),
                        error = state.fieldErrors[GroupField.Room],
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (twoColumns) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    right(Modifier.weight(1f))
                    left(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    right(Modifier.fillMaxWidth())
                    left(Modifier.fillMaxWidth())
                }
            }
        }
        GroupScheduleEditor(state, onEvent, Modifier.fillMaxWidth())
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOnePrimaryButton(onClick = {
                onEvent(CoursesEvent.SubmitGroup)
            }, enabled = state.canSubmit && !state.isSubmitting) { Text(stringResource(Res.string.save)) }
            TextButton(
                onClick = { onEvent(CoursesEvent.DismissModal) },
                enabled = !state.isSubmitting,
            ) { Text(stringResource(Res.string.cancel)) }
        }
    }
}

@Composable
private fun GroupInput(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    CenterOneTextField(
        value,
        onValueChange,
        Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        isError = error != null,
        supportingText =
            error?.let { message ->
                ({ Text(message) })
            },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                textDirection =
                    if (keyboardType ==
                        KeyboardType.Text
                    ) {
                        TextDirection.Content
                    } else {
                        TextDirection.Ltr
                    },
            ),
    )
}
