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
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.localization.LocalAppLanguage
import center.ui.theme.Gold
import center.ui.theme.Navy
import center.ui.theme.Pending
import center.ui.theme.PendingContainer
import center.ui.theme.White

@Composable
internal fun TeacherDetails(
    teacher: Teacher,
    onEvent: (TeachersEvent) -> Unit,
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
        SalaryBox(teacher, onPay = { onEvent(TeachersEvent.PaySalary(teacher.id)) })
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
    onPay: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.medium, color = Navy, contentColor = White) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                teacherString(if (teacher.salaryPaidInDemo) TeacherText.MonthlySalary else TeacherText.SalaryDue),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                teacher.monthlySalaryEgp?.let { formatEgp(it) } ?: teacherString(TeacherText.NoSalary),
                color = Gold,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(teacherString(TeacherText.SalaryPolicy), style = MaterialTheme.typography.bodySmall)
            if (teacher.salaryPaidInDemo) {
                StatusBadge(teacherString(TeacherText.DemoPaid), StatusTone.Success)
            } else {
                CenterOnePrimaryButton(onClick = onPay, enabled = (teacher.monthlySalaryEgp ?: 0) > 0) {
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
        shape = MaterialTheme.shapes.medium,
        color = PendingContainer,
        contentColor = Navy,
        border = BorderStroke(1.dp, Pending.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(teacherString(TeacherText.DeficitTitle), style = MaterialTheme.typography.titleMedium)
            Text(
                teacherString(TeacherText.DeficitDescription, students.size.toString(), formatEgp(students.sumOf { it.subscriptionEgp })),
                style = MaterialTheme.typography.bodySmall,
            )
            students.forEach { student ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(student.name.resolve(language), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(formatEgp(student.subscriptionEgp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
internal fun AddTeacherForm(
    draft: TeacherDraft,
    onEvent: (TeachersEvent) -> Unit,
) {
    CenterOneModalContent(
        title = teacherString(TeacherText.Add),
        subtitle = teacherString(TeacherText.AddHint),
        closeLabel = teacherString(TeacherText.Close),
        onClose = { onEvent(TeachersEvent.CloseModal) },
        modifier = Modifier.padding(16.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val mainFields: @Composable (Modifier) -> Unit = { fieldModifier ->
                Column(fieldModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CenterOneTextField(
                        draft.name,
                        { onEvent(TeachersEvent.NameChanged(it)) },
                        Modifier.fillMaxWidth(),
                        label = { Text(teacherString(TeacherText.Name)) },
                    )
                    CenterOneTextField(
                        draft.subject,
                        { onEvent(TeachersEvent.SubjectChanged(it)) },
                        Modifier.fillMaxWidth(),
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
                        label = { Text(teacherString(TeacherText.Phone)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    )
                    CenterOneTextField(
                        draft.notes,
                        { onEvent(TeachersEvent.NotesChanged(it)) },
                        Modifier.fillMaxWidth(),
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
        CenterOnePrimaryButton(
            onClick = { onEvent(TeachersEvent.SaveTeacher) },
            modifier = Modifier.align(Alignment.End),
            enabled = draft.canSave,
        ) {
            Text(teacherString(TeacherText.Save))
        }
    }
}
