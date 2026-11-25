package center.feature.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.components.AttendanceHistory
import center.ui.components.CenterOneDestructiveButton
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOneTextButton
import center.ui.components.LocalStudentPortalDetails
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.text.isolateLtr

data class StudentDetailsState(
    val id: String,
    val name: String,
    val code: String,
    val studentPhone: String?,
    val parentPhone: String,
    val groups: List<LocalizedText>,
    val history: List<LocalizedText> = emptyList(),
    val grade: String = "",
    val enrolledLessons: List<StudentEnrolledLesson> = emptyList(),
    val financialSummary: List<LocalizedText> = emptyList(),
    val paymentHistory: List<LocalizedText> = emptyList(),
    val lessonAttendance: List<LocalizedText> = emptyList(),
)

data class StudentEnrolledLesson(
    val id: String,
    val description: LocalizedText,
)

@Composable
internal fun StudentDetails(
    state: StudentDetailsState,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onEditEnrollment: (String) -> Unit,
    onRemoveEnrollment: (String) -> Unit,
) {
    val language = LocalAppLanguage.current
    CenterOneModal(onClose) {
        CenterOneModalContent(
            title = state.name,
            subtitle = state.code.isolateLtr(),
            closeLabel = LocalizedText("إغلاق", "Close").resolve(language),
            onClose = onClose,
        ) {
            CenterOneTextButton(onClick = onEdit) {
                Text(LocalizedText("تعديل بيانات الطالب", "Edit student").resolve(language))
            }
            state.studentPhone?.let {
                Text(LocalizedText("هاتف الطالب", "Student phone").resolve(language) + " · " + it.isolateLtr())
            }
            Text(LocalizedText("هاتف ولي الأمر", "Parent phone").resolve(language) + " · " + state.parentPhone.isolateLtr())
            if (state.grade.isNotBlank()) Text(LocalizedText("الصف الدراسي", "Grade").resolve(language) + " · " + state.grade)
            Text(
                LocalizedText("جدول الدروس النشطة", "Active lesson schedule").resolve(language),
                style = MaterialTheme.typography.titleSmall,
            )
            if (state.groups.isEmpty()) Text(LocalizedText("لا توجد دروس نشطة", "No active lessons").resolve(language))
            state.groups.forEach { Text(it.resolve(language)) }
            Text(LocalizedText("الدروس المسجل بها", "Enrolled lessons").resolve(language), style = MaterialTheme.typography.titleSmall)
            if (state.enrolledLessons.isEmpty()) Text(LocalizedText("لا توجد دروس مسجلة", "No enrolled lessons").resolve(language))
            state.enrolledLessons.forEach { lesson ->
                Text(lesson.description.resolve(language))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CenterOneTextButton(onClick = { onEditEnrollment(lesson.id) }) {
                        Text(LocalizedText("تعديل السعر", "Edit price").resolve(language))
                    }
                    CenterOneDestructiveButton(onClick = { onRemoveEnrollment(lesson.id) }) {
                        Text(LocalizedText("إزالة من الدرس", "Remove from lesson").resolve(language))
                    }
                }
            }
            Text(LocalizedText("الملخص المالي", "Financial summary").resolve(language), style = MaterialTheme.typography.titleSmall)
            state.financialSummary.forEach { Text(it.resolve(language)) }
            if (state.paymentHistory.isNotEmpty()) {
                Text(LocalizedText("سجل المدفوعات", "Payment history").resolve(language), style = MaterialTheme.typography.titleSmall)
                state.paymentHistory.forEach { Text(it.resolve(language)) }
            }
            if (state.lessonAttendance.isNotEmpty()) {
                Text(
                    LocalizedText("حضور الدروس وغيابها", "Lesson attendance").resolve(language),
                    style = MaterialTheme.typography.titleSmall,
                )
                state.lessonAttendance.forEach { Text(it.resolve(language)) }
            }
            LocalStudentPortalDetails.current(state.id, state.parentPhone)
            AttendanceHistory(state.history)
        }
    }
}
