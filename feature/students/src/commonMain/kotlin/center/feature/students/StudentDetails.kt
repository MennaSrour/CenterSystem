package center.feature.students

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import center.ui.components.AttendanceHistory
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
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
)

@Composable
internal fun StudentDetails(
    state: StudentDetailsState,
    onClose: () -> Unit,
) {
    val language = LocalAppLanguage.current
    CenterOneModal(onClose) {
        CenterOneModalContent(
            title = state.name,
            subtitle = state.code.isolateLtr(),
            closeLabel = LocalizedText("إغلاق", "Close").resolve(language),
            onClose = onClose,
        ) {
            state.studentPhone?.let {
                Text(LocalizedText("هاتف الطالب", "Student phone").resolve(language) + " · " + it.isolateLtr())
            }
            Text(LocalizedText("هاتف ولي الأمر", "Parent phone").resolve(language) + " · " + state.parentPhone.isolateLtr())
            Text(
                LocalizedText("جدول المجموعات النشطة", "Active group schedule").resolve(language),
                style = MaterialTheme.typography.titleSmall,
            )
            if (state.groups.isEmpty()) Text(LocalizedText("لا توجد مجموعات نشطة", "No active groups").resolve(language))
            state.groups.forEach { Text(it.resolve(language)) }
            LocalStudentPortalDetails.current(state.id, state.parentPhone)
            AttendanceHistory(state.history)
        }
    }
}
