package center.feature.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOneTable
import center.ui.components.CenterOneTableColumn
import center.ui.components.CenterOneTag
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.theme.Indigo
import center.ui.theme.Pending

@Composable
fun StudentsDataGrid(
    students: List<StudentRow>,
    grades: List<StudentGrade>,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
    canDelete: Boolean = true,
) {
    val details = LocalizedText("عرض التفاصيل", "View details").resolve(LocalAppLanguage.current)
    val gradeById = grades.associateBy { it.id }
    val columns =
        listOf(
            CenterOneTableColumn(appString("id"), 1f),
            CenterOneTableColumn(appString("name"), 1.8f),
            CenterOneTableColumn(appString("grade"), 1.3f),
            CenterOneTableColumn(appString("system"), 0.8f),
            CenterOneTableColumn(appString("parent_phone"), 1.5f),
            CenterOneTableColumn(appString("monthly_due"), 1.2f),
            CenterOneTableColumn(appString("actions"), 1.5f),
        )
    CenterOneTable(columns, students, rowKey = {
        it.id
    }, emptyLabel = appString("no_students"), modifier = modifier) { student, column ->
        when (column) {
            0 -> Text(student.code, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr))
            1 ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CenterOneAvatar(student.name.take(1))
                    Column {
                        Text(student.name, style = MaterialTheme.typography.titleSmall)
                        if (student.englishName.isNotBlank() && student.englishName != student.name) {
                            Text(
                                student.englishName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            2 ->
                Text(
                    gradeById[student.gradeId]?.label ?: student.gradeLabel ?: appString("unknown_grade"),
                    style = MaterialTheme.typography.bodySmall,
                )
            3 ->
                when (student.system) {
                    StudentSystem.Arabic -> CenterOneTag(appString("arabic_system"), color = Pending)
                    StudentSystem.Languages -> CenterOneTag(appString("languages_system"), color = Indigo)
                    null -> Text("—", style = MaterialTheme.typography.bodySmall)
                }
            4 -> Text(student.parentPhone, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr))
            5 -> Text(student.monthlyDueMinor.asEgp(LocalAppLanguage.current), style = MaterialTheme.typography.bodySmall)
            6 ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CenterOneIconButton(Icons.Outlined.Info, details, { onEvent(StudentsEvent.OpenDetails(student.id)) })
                    CenterOneIconButton(Icons.Outlined.Edit, appString("edit_student", student.name), {
                        onEvent(StudentsEvent.EditStudent(student.id))
                    })
                    if (canDelete) {
                        CenterOneIconButton(Icons.Outlined.DeleteOutline, appString("delete_student", student.name), {
                            onEvent(StudentsEvent.DeleteStudent(student.id))
                        }, color = MaterialTheme.colorScheme.error)
                    }
                }
        }
    }
}

private fun Long.asEgp(language: AppLanguage): String {
    val amount = if (this % 100L == 0L) (this / 100).toString() else "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
    val currency = if (language == AppLanguage.Arabic) "ج.م" else "EGP"
    return "$amount $currency"
}
