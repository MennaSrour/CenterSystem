package center.feature.students

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
import center.feature.students.resources.Res
import center.feature.students.resources.add_subtitle
import center.feature.students.resources.add_title
import center.feature.students.resources.arabic_system
import center.feature.students.resources.cancel
import center.feature.students.resources.close
import center.feature.students.resources.languages_system
import center.feature.students.resources.phone_label
import center.feature.students.resources.save_student
import center.feature.students.resources.select_grade
import center.feature.students.resources.select_system
import center.feature.students.resources.student_code
import center.feature.students.resources.student_name
import center.ui.components.CenterOneChoiceGroup
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddStudentModal(
    state: AddStudentState,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModal(onDismissRequest = { if (!state.isSubmitting) onEvent(StudentsEvent.DismissAddStudent) }) {
        AddStudentModalContent(state, onEvent, modifier)
    }
}

@Composable
internal fun AddStudentModalContent(
    state: AddStudentState,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModalContent(
        title = stringResource(Res.string.add_title),
        subtitle = stringResource(Res.string.add_subtitle),
        closeLabel = stringResource(Res.string.close),
        onClose = { onEvent(StudentsEvent.DismissAddStudent) },
        modifier = modifier,
        enabled = !state.isSubmitting,
        maxWidth = 780.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val twoColumns = maxWidth >= 560.dp
            val identityFields: @Composable (Modifier) -> Unit = { columnModifier ->
                Column(columnModifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    CenterOneTextField(
                        value = state.name,
                        onValueChange = { onEvent(StudentsEvent.NameChanged(it)) },
                        label = { Text(stringResource(Res.string.student_name)) },
                        enabled = !state.isSubmitting,
                        isError = state.nameError != null,
                        supportingText = state.nameError?.let { error -> ({ Text(error) }) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CenterOneTextField(
                        value = state.code,
                        onValueChange = { onEvent(StudentsEvent.CodeChanged(it)) },
                        label = { Text(stringResource(Res.string.student_code)) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                        enabled = !state.isSubmitting,
                        isError = state.codeError != null,
                        supportingText = state.codeError?.let { error -> ({ Text(error) }) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            val enrollmentFields: @Composable (Modifier) -> Unit = { columnModifier ->
                Column(columnModifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    CenterOneDropdown(
                        value =
                            state.grades
                                .firstOrNull { it.id == state.selectedGradeId }
                                ?.label
                                .orEmpty(),
                        label = stringResource(Res.string.select_grade),
                        options = state.grades.map { CenterOneDropdownOption(it.id, it.label) },
                        expanded = state.gradeMenuExpanded,
                        onExpandedChange = { onEvent(StudentsEvent.AddGradeMenuChanged(it)) },
                        onOptionSelected = { onEvent(StudentsEvent.AddGradeSelected(it)) },
                        enabled = !state.isSubmitting && state.grades.isNotEmpty(),
                        error = state.gradeError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.select_system), style = MaterialTheme.typography.labelSmall)
                        CenterOneChoiceGroup(
                            labels = listOf(stringResource(Res.string.arabic_system), stringResource(Res.string.languages_system)),
                            selectedIndex = StudentSystem.entries.indexOf(state.system),
                            onSelected = { onEvent(StudentsEvent.SystemSelected(StudentSystem.entries[it])) },
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    CenterOneTextField(
                        value = state.parentPhone,
                        onValueChange = { onEvent(StudentsEvent.ParentPhoneChanged(it)) },
                        label = { Text(stringResource(Res.string.phone_label)) },
                        enabled = !state.isSubmitting,
                        isError = state.phoneError != null,
                        supportingText = state.phoneError?.let { error -> ({ Text(error) }) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (twoColumns) {
                // The first child in an RTL row is the right-hand column.
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    identityFields(Modifier.weight(1f))
                    enrollmentFields(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    identityFields(Modifier.fillMaxWidth())
                    enrollmentFields(Modifier.fillMaxWidth())
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOnePrimaryButton(onClick = {
                onEvent(StudentsEvent.SubmitAddStudent)
            }, enabled = state.canSubmit && !state.isSubmitting) { Text(stringResource(Res.string.save_student)) }
            TextButton(onClick = {
                onEvent(StudentsEvent.DismissAddStudent)
            }, enabled = !state.isSubmitting) { Text(stringResource(Res.string.cancel)) }
        }
    }
}
