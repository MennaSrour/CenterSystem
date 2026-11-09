package center.feature.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.feature.students.resources.Res
import center.feature.students.resources.actions
import center.feature.students.resources.arabic_system
import center.feature.students.resources.delete_student
import center.feature.students.resources.edit_student
import center.feature.students.resources.grade
import center.feature.students.resources.id
import center.feature.students.resources.languages_system
import center.feature.students.resources.name
import center.feature.students.resources.no_students
import center.feature.students.resources.parent_phone
import center.feature.students.resources.system
import center.feature.students.resources.unknown_grade
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOneTable
import center.ui.components.CenterOneTableColumn
import center.ui.components.CenterOneTag
import center.ui.theme.Pending
import center.ui.theme.Pink
import org.jetbrains.compose.resources.stringResource

@Composable
fun StudentsDataGrid(
    students: List<StudentRow>,
    grades: List<StudentGrade>,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradeById = grades.associateBy { it.id }
    val columns =
        listOf(
            CenterOneTableColumn(stringResource(Res.string.id), 1f),
            CenterOneTableColumn(stringResource(Res.string.name), 1.8f),
            CenterOneTableColumn(stringResource(Res.string.grade), 1.3f),
            CenterOneTableColumn(stringResource(Res.string.system), 0.8f),
            CenterOneTableColumn(stringResource(Res.string.parent_phone), 1.5f),
            CenterOneTableColumn(stringResource(Res.string.actions), 1.2f),
        )
    CenterOneTable(columns, students, rowKey = {
        it.id
    }, emptyLabel = stringResource(Res.string.no_students), modifier = modifier) { student, column ->
        when (column) {
            0 -> Text(student.code, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr))
            1 ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CenterOneAvatar(student.name.take(1))
                    Column {
                        Text(student.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            student.englishName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            2 ->
                Text(
                    gradeById[student.gradeId]?.label ?: stringResource(Res.string.unknown_grade),
                    style = MaterialTheme.typography.bodySmall,
                )
            3 ->
                CenterOneTag(
                    stringResource(
                        if (student.system ==
                            StudentSystem.Arabic
                        ) {
                            Res.string.arabic_system
                        } else {
                            Res.string.languages_system
                        },
                    ),
                    color =
                        if (student.system ==
                            StudentSystem.Arabic
                        ) {
                            Pending
                        } else {
                            Pink
                        },
                )
            4 -> Text(student.parentPhone, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr))
            5 ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CenterOneIconButton(Icons.Outlined.Edit, stringResource(Res.string.edit_student, student.name), {
                        onEvent(StudentsEvent.EditStudent(student.id))
                    })
                    CenterOneIconButton(Icons.Outlined.DeleteOutline, stringResource(Res.string.delete_student, student.name), {
                        onEvent(StudentsEvent.DeleteStudent(student.id))
                    }, color = MaterialTheme.colorScheme.error)
                }
        }
    }
}
