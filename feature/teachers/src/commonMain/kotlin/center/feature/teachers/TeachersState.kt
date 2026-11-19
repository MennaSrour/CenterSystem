package center.feature.teachers

import center.ui.localization.LocalizedText

data class Teacher(
    val id: Int,
    val name: LocalizedText,
    val phone: String,
    val subject: LocalizedText,
    val monthlySalaryEgp: Long?,
    val groups: List<TeacherGroup> = emptyList(),
    val unpaidStudents: List<UnpaidStudent> = emptyList(),
    val notes: String = "",
    val salaryPaidInDemo: Boolean = false,
)

data class TeacherGroup(
    val name: LocalizedText,
    val schedule: LocalizedText,
)

data class UnpaidStudent(
    val name: LocalizedText,
    val subscriptionEgp: Long,
)

data class TeacherDraft(
    val name: String = "",
    val phone: String = "",
    val subject: String = "",
    val notes: String = "",
) {
    val canSave: Boolean get() = name.isNotBlank() && phone.isNotBlank() && subject.isNotBlank()
}

sealed interface TeachersModal {
    data object Closed : TeachersModal

    data class Details(
        val teacherId: Int,
    ) : TeachersModal

    data class Add(
        val draft: TeacherDraft = TeacherDraft(),
    ) : TeachersModal
}

data class TeachersState(
    val teachers: List<Teacher>,
    val query: String = "",
    val modal: TeachersModal = TeachersModal.Closed,
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
