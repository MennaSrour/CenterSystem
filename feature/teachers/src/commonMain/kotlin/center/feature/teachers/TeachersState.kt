package center.feature.teachers

import center.ui.localization.LocalizedText

data class Teacher(
    val id: String,
    val name: LocalizedText,
    val phone: String,
    val subject: LocalizedText,
    val monthlySalaryMinor: Long?,
    val groups: List<TeacherGroup> = emptyList(),
    val unpaidStudents: List<UnpaidStudent> = emptyList(),
    val notes: String = "",
    val salaryPaid: Boolean = false,
    val assignedLessonCount: Int = groups.size,
    val arabicStudentCount: Int = 0,
    val languagesStudentCount: Int = 0,
    val privateStudentCount: Int = 0,
    val expectedSchedule: LocalizedText = LocalizedText("", ""),
    val payoutHistory: List<LocalizedText> = emptyList(),
)

data class TeacherGroup(
    val name: LocalizedText,
    val schedule: LocalizedText,
)

data class UnpaidStudent(
    val name: LocalizedText,
    val subscriptionMinor: Long,
    val studentId: String = name.english,
)

data class TeacherDraft(
    val name: String = "",
    val phone: String = "",
    val subject: String = "",
    val notes: String = "",
    val code: String = "",
) {
    val canSave: Boolean get() = name.isNotBlank() && phone.isNotBlank() && subject.isNotBlank()
}

sealed interface TeachersModal {
    data class Delete(
        val id: String,
        val name: String,
        val deleting: Boolean = false,
        val message: String? = null,
    ) : TeachersModal

    data class StaffDetails(
        val id: String,
    ) : TeachersModal

    data class DeleteStaff(
        val id: String,
        val name: String,
        val deleting: Boolean = false,
        val message: String? = null,
    ) : TeachersModal

    data class StaffForm(
        val draft: StaffDraft = StaffDraft(),
        val id: String? = null,
        val saving: Boolean = false,
        val message: String? = null,
    ) : TeachersModal

    data object Closed : TeachersModal

    data class Details(
        val teacherId: String,
    ) : TeachersModal

    data class Add(
        val draft: TeacherDraft = TeacherDraft(),
        val teacherId: String? = null,
        val saving: Boolean = false,
        val message: String? = null,
    ) : TeachersModal
}

data class TeachersState(
    val teachers: List<Teacher>,
    val query: String = "",
    val modal: TeachersModal = TeachersModal.Closed,
    val payingTeacherId: String? = null,
    val showStaff: Boolean = false,
    val staff: List<StaffCardModel> = emptyList(),
    val history: List<LocalizedText> = emptyList(),
    val canDelete: Boolean = true,
) {
    val visibleTeachers: List<Teacher>
        get() {
            val term = query.trim()
            return teachers.filter { teacher ->
                teacher.name.arabic.contains(term, ignoreCase = true) ||
                    teacher.name.english.contains(term, ignoreCase = true) ||
                    teacher.subject.arabic.contains(term, ignoreCase = true) ||
                    teacher.subject.english.contains(term, ignoreCase = true) ||
                    teacher.phone.contains(term)
            }
        }
}
