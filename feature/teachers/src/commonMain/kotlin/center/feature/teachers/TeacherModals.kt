package center.feature.teachers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneGradientCard
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.localization.LocalAppLanguage
import center.ui.theme.CenterOneBrushes
import center.ui.theme.GoldLight
import center.ui.theme.Navy
import center.ui.theme.PendingContainer
import center.ui.theme.PendingOutline

@Composable
internal fun TeacherDetails(
    teacher: Teacher,
    onEvent: (TeachersEvent) -> Unit,
    paying: Boolean = false,
) {
    val language = LocalAppLanguage.current
    CenterOneModalContent(
        title = teacher.name.resolve(language),
        subtitle = teacherString(TeacherText.Details),
        closeLabel = teacherString(TeacherText.Close),
        onClose = { onEvent(TeachersEvent.CloseModal) },
        modifier = Modifier.padding(16.dp),
        maxWidth = 640.dp,
    ) {
        Text(teacher.subject.resolve(language), style = MaterialTheme.typography.titleMedium)
        Column {
            Text(teacherString(TeacherText.Phone), style = MaterialTheme.typography.labelMedium)
            Text(teacher.phone, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr))
        }
        if (teacher.notes.isNotBlank()) Text(teacher.notes, style = MaterialTheme.typography.bodyMedium)
        CenterOnePrimaryButton(onClick = {
            onEvent(TeachersEvent.EditTeacher(teacher.id))
        }, enabled = !paying) { Text(teacherString(TeacherText.Edit)) }
        SalaryBox(teacher, paying, onPay = { onEvent(TeachersEvent.PaySalary(teacher.id)) })
        if (teacher.unpaidStudents.isNotEmpty()) CollectionDeficit(teacher.unpaidStudents)
        Text(teacherString(TeacherText.Groups), style = MaterialTheme.typography.titleMedium)
        if (teacher.groups.isEmpty()) {
            Text(teacherString(TeacherText.NoGroups), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        teacher.groups.forEach { group ->
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(group.name.resolve(language), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge)
                Text(group.schedule.resolve(language), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SalaryBox(
    teacher: Teacher,
    paying: Boolean,
    onPay: () -> Unit,
) {
    CenterOneGradientCard(CenterOneBrushes.Treasury) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                teacherString(if (teacher.salaryPaid) TeacherText.MonthlySalary else TeacherText.SalaryDue),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                teacher.monthlySalaryMinor?.let { formatEgp(it) } ?: teacherString(TeacherText.NoSalary),
                color = GoldLight,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(teacherString(TeacherText.SalaryPolicy), style = MaterialTheme.typography.bodySmall)
            if (teacher.salaryPaid) {
                StatusBadge(teacherString(TeacherText.Paid), StatusTone.Success)
            } else {
                CenterOnePrimaryButton(onClick = onPay, enabled = !paying && (teacher.monthlySalaryMinor ?: 0) > 0) {
                    Text(teacherString(TeacherText.PaySalary))
                }
            }
        }
    }
}

@Composable
private fun CollectionDeficit(students: List<UnpaidStudent>) {
    val language = LocalAppLanguage.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = PendingContainer,
        contentColor = Navy,
        border = BorderStroke(1.dp, PendingOutline),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(teacherString(TeacherText.DeficitTitle), style = MaterialTheme.typography.titleMedium)
            Text(
                teacherString(
                    TeacherText.DeficitDescription,
                    students
                        .distinctBy {
                            it.studentId
                        }.size
                        .toString(),
                    formatEgp(students.sumOf { it.subscriptionMinor }),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            students.forEach { student ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(student.name.resolve(language), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(formatEgp(student.subscriptionMinor), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
internal fun AddTeacherForm(
    draft: TeacherDraft,
    onEvent: (TeachersEvent) -> Unit,
    editing: Boolean = false,
    saving: Boolean = false,
    message: String? = null,
) {
    CenterOneModalContent(
        title = teacherString(if (editing) TeacherText.Edit else TeacherText.Add),
        subtitle = teacherString(TeacherText.AddHint),
        closeLabel = teacherString(TeacherText.Close),
        onClose = { onEvent(TeachersEvent.CloseModal) },
        modifier = Modifier.padding(16.dp),
        enabled = !saving,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val mainFields: @Composable (Modifier) -> Unit = { fieldModifier ->
                Column(fieldModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CenterOneTextField(
                        draft.name,
                        { onEvent(TeachersEvent.NameChanged(it)) },
                        Modifier.fillMaxWidth(),
                        enabled = !saving,
                        label = { Text(teacherString(TeacherText.Name)) },
                    )
                    CenterOneTextField(
                        draft.subject,
                        { onEvent(TeachersEvent.SubjectChanged(it)) },
                        Modifier.fillMaxWidth(),
                        enabled = !saving,
                        label = { Text(teacherString(TeacherText.Subject)) },
                    )
                }
            }
            val contactFields: @Composable (Modifier) -> Unit = { fieldModifier ->
                Column(fieldModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CenterOneTextField(
                        draft.phone,
                        { onEvent(TeachersEvent.PhoneChanged(it)) },
                        Modifier.fillMaxWidth(),
                        enabled = !saving,
                        label = { Text(teacherString(TeacherText.Phone)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    )
                    CenterOneTextField(
                        draft.notes,
                        { onEvent(TeachersEvent.NotesChanged(it)) },
                        Modifier.fillMaxWidth(),
                        enabled = !saving,
                        label = { Text(teacherString(TeacherText.Notes)) },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                    )
                }
            }
            if (maxWidth >= 420.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    mainFields(Modifier.weight(1f))
                    contactFields(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    mainFields(Modifier.fillMaxWidth())
                    contactFields(Modifier.fillMaxWidth())
                }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        CenterOnePrimaryButton(
            onClick = { onEvent(TeachersEvent.SaveTeacher) },
            modifier = Modifier.align(Alignment.End),
            enabled = draft.canSave && !saving,
        ) {
            Text(teacherString(TeacherText.Save))
        }
    }
}
