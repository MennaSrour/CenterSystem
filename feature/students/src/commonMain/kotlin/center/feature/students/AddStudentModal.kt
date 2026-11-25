package center.feature.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneChoiceGroup
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
    val language = LocalAppLanguage.current
    CenterOneModalContent(
        title =
            if (state.editingId ==
                null
            ) {
                appString("add_title")
            } else {
                LocalizedText("تعديل الطالب", "Edit student").resolve(language)
            },
        subtitle = appString("add_subtitle"),
        closeLabel = appString("close"),
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
                        label = { Text(appString("student_name")) },
                        enabled = !state.isSubmitting,
                        isError = state.nameError != null,
                        supportingText = state.nameError?.let { error -> ({ Text(error) }) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CenterOneTextField(
                        value = state.code,
                        onValueChange = { onEvent(StudentsEvent.CodeChanged(it)) },
                        label = { Text(appString("student_code")) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                        enabled = !state.isSubmitting,
                        readOnly = state.editingId == null,
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
                        label = appString("select_grade"),
                        options = state.grades.map { CenterOneDropdownOption(it.id, it.label) },
                        expanded = state.gradeMenuExpanded,
                        onExpandedChange = { onEvent(StudentsEvent.AddGradeMenuChanged(it)) },
                        onOptionSelected = { onEvent(StudentsEvent.AddGradeSelected(it)) },
                        enabled = !state.isSubmitting && state.grades.isNotEmpty(),
                        error = state.gradeError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(appString("select_system"), style = MaterialTheme.typography.labelSmall)
                        CenterOneChoiceGroup(
                            labels = listOf(appString("arabic_system"), appString("languages_system")),
                            selectedIndex = StudentSystem.entries.indexOf(state.system),
                            onSelected = { onEvent(StudentsEvent.SystemSelected(StudentSystem.entries[it])) },
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    CenterOneTextField(
                        value = state.studentPhone,
                        onValueChange = { onEvent(StudentsEvent.StudentPhoneChanged(it)) },
                        label = { Text(LocalizedText("هاتف الطالب (اختياري)", "Student phone (optional)").resolve(language)) },
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CenterOneTextField(
                        value = state.parentPhone,
                        onValueChange = { onEvent(StudentsEvent.ParentPhoneChanged(it)) },
                        label = { Text(appString("phone_label")) },
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
        if (state.groupOptions.isNotEmpty()) {
            Text(LocalizedText("الدروس المسجل بها", "Enrolled lessons").resolve(language), style = MaterialTheme.typography.titleSmall)
            state.groupOptions.forEach { group ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = group.id in state.selectedGroupIds,
                        onCheckedChange = { onEvent(StudentsEvent.ToggleGroup(group.id)) },
                        enabled = !state.isSubmitting,
                    )
                    Text(group.label)
                }
                if (group.id in state.selectedGroupIds) {
                    CenterOneTextField(
                        value = state.customPrices[group.id].orEmpty(),
                        onValueChange = { onEvent(StudentsEvent.CustomPriceChanged(group.id, it)) },
                        label = { Text(LocalizedText("السعر الشهري للطالب (ج.م)", "Student monthly price (EGP)").resolve(language)) },
                        enabled = !state.isSubmitting,
                        isError = group.id in state.invalidCustomPrices,
                        supportingText =
                            if (group.id in state.invalidCustomPrices) {
                                { Text(LocalizedText("أدخل سعرًا صالحًا", "Enter a valid price").resolve(language)) }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CenterOneTextField(
                        value = state.discounts[group.id].orEmpty(),
                        onValueChange = { onEvent(StudentsEvent.DiscountChanged(group.id, it)) },
                        label = { Text(LocalizedText("الخصم الشهري (ج.م)", "Monthly discount (EGP)").resolve(language)) },
                        enabled = !state.isSubmitting,
                        isError = group.id in state.invalidDiscounts,
                        supportingText =
                            if (group.id in state.invalidDiscounts) {
                                {
                                    Text(
                                        LocalizedText(
                                            "أدخل مبلغًا صالحًا بمنزلتين عشريتين كحد أقصى",
                                            "Enter an amount with up to two decimal places",
                                        ).resolve(language),
                                    )
                                }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOnePrimaryButton(onClick = {
                onEvent(StudentsEvent.SubmitAddStudent)
            }, enabled = state.canSubmit && !state.isSubmitting) { Text(appString("save_student")) }
            CenterOneTextButton(onClick = {
                onEvent(StudentsEvent.DismissAddStudent)
            }, enabled = !state.isSubmitting) { Text(appString("cancel")) }
        }
    }
}
