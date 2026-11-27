package center.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText

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
@OptIn(ExperimentalLayoutApi::class)
internal fun GroupFormModalContent(
    state: GroupFormState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    CenterOneModalContent(
        title = appString(if (state.mode is GroupFormMode.Edit) "edit_title" else "add_title"),
        subtitle = appString("form_subtitle"),
        closeLabel = appString("close"),
        onClose = { onEvent(CoursesEvent.DismissModal) },
        modifier = modifier,
        enabled = !state.isSubmitting,
        maxWidth = 920.dp,
    ) {
        Text(appString("step_basic"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val twoColumns = maxWidth >= 600.dp
            val right: @Composable (Modifier) -> Unit = { columnModifier ->
                Column(columnModifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GroupInput(state.subject, appString("subject"), {
                        onEvent(CoursesEvent.SubjectChanged(it))
                    }, !state.isSubmitting, state.fieldErrors[GroupField.Subject])
                    CenterOneDropdown(
                        value =
                            state.teachers
                                .firstOrNull { it.id == state.selectedTeacherId }
                                ?.name
                                .orEmpty(),
                        label = appString("teacher"),
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
                        label = appString("grade"),
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
                }
            }
            val left: @Composable (Modifier) -> Unit = { columnModifier ->
                Column(columnModifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GroupInput(state.maxCapacity, appString("max_capacity"), {
                        onEvent(CoursesEvent.MaxCapacityChanged(it))
                    }, !state.isSubmitting, state.fieldErrors[GroupField.MaxCapacity], KeyboardType.Number)
                    CenterOneDropdown(
                        value =
                            state.rooms
                                .firstOrNull { it.id == state.selectedRoomId }
                                ?.label
                                .orEmpty(),
                        label = appString("room"),
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
        Text(
            appString("step_pricing"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val lessonTypes =
                listOf(
                    CourseLessonType.Arabic to appString("arabic_system"),
                    CourseLessonType.Languages to appString("languages_system"),
                    CourseLessonType.Mixed to LocalizedText("مشترك (عربي + لغات)", "Mixed (Arabic + Languages)").resolve(language),
                    CourseLessonType.Private to appString("private_lesson"),
                )
            lessonTypes.forEach { (type, label) ->
                FilterChip(
                    selected = state.lessonType == type,
                    onClick = { onEvent(CoursesEvent.LessonTypeSelected(type)) },
                    label = { Text(label) },
                    enabled = !state.isSubmitting,
                )
            }
        }
        val priceLabel =
            if (state.lessonType == CourseLessonType.Mixed) {
                appString("arabic_track_price")
            } else {
                appString("lesson_base_price")
            }
        GroupInput(state.monthlyPrice, priceLabel, {
            onEvent(CoursesEvent.MonthlyPriceChanged(it))
        }, !state.isSubmitting, state.fieldErrors[GroupField.MonthlyPrice], KeyboardType.Decimal)
        if (state.lessonType == CourseLessonType.Mixed) {
            GroupInput(state.languagesPrice, appString("mixed_languages_price"), {
                onEvent(CoursesEvent.LanguagesPriceChanged(it))
            }, !state.isSubmitting, state.fieldErrors[GroupField.LanguagesPrice], KeyboardType.Decimal)
        }
        GroupInput(state.teacherCommission, appString("commission"), {
            onEvent(CoursesEvent.CommissionChanged(it))
        }, !state.isSubmitting, state.fieldErrors[GroupField.Commission], KeyboardType.Decimal)
        Text(
            appString("student_enrollment"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        CenterOneTextField(
            state.studentSearch,
            { onEvent(CoursesEvent.StudentSearchChanged(it)) },
            Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting,
            label = { Text(appString("student_search")) },
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = state.showOtherGrades,
                onCheckedChange = { onEvent(CoursesEvent.ShowOtherGradesChanged(it)) },
                enabled = !state.isSubmitting,
            )
            Text(appString("show_other_grades"), style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(
                state.students.filter { student ->
                    (state.showOtherGrades || student.gradeId == state.selectedGradeId) &&
                        student.name.contains(state.studentSearch.trim(), ignoreCase = true)
                },
                key = { it.id },
            ) { student ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = student.id in state.enrollments,
                        onCheckedChange = { onEvent(CoursesEvent.StudentEnrollmentToggled(student.id, it)) },
                        enabled = !state.isSubmitting,
                    )
                    Text(student.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        student.gradeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.enrollments[student.id]?.let { enrollment ->
                    if (state.lessonType == CourseLessonType.Mixed) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = enrollment.track == center.domain.model.StudentTrack.ARABIC,
                                onClick = {
                                    onEvent(
                                        CoursesEvent.StudentTrackSelected(student.id, center.domain.model.StudentTrack.ARABIC),
                                    )
                                },
                                label = { Text(appString("arabic_track")) },
                                enabled = !state.isSubmitting,
                            )
                            FilterChip(
                                selected = enrollment.track == center.domain.model.StudentTrack.LANGUAGES,
                                onClick = {
                                    onEvent(
                                        CoursesEvent.StudentTrackSelected(student.id, center.domain.model.StudentTrack.LANGUAGES),
                                    )
                                },
                                label = { Text(appString("languages_track")) },
                                enabled = !state.isSubmitting,
                            )
                        }
                    }
                    GroupInput(
                        enrollment.price,
                        appString("student_price"),
                        { onEvent(CoursesEvent.StudentPriceChanged(student.id, it)) },
                        !state.isSubmitting,
                        null,
                        KeyboardType.Decimal,
                    )
                }
            }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOnePrimaryButton(onClick = {
                onEvent(CoursesEvent.SubmitGroup)
            }, enabled = state.canSubmit && !state.isSubmitting) { Text(appString("save")) }
            CenterOneTextButton(
                onClick = { onEvent(CoursesEvent.DismissModal) },
                enabled = !state.isSubmitting,
            ) { Text(appString("cancel")) }
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
