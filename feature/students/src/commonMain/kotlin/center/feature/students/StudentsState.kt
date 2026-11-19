package center.feature.students

import center.ui.localization.LocalizedText
import kotlinx.datetime.LocalDate

enum class EducationStage { Primary, Prep, Secondary }

enum class StudentStageFilter { All, Primary, Prep, Secondary, Overdue }

enum class StudentSystem { Arabic, Languages }

enum class StudentsDestination { Home, Students, Teachers, Courses, Schedule, Financials, Reports, Settings }

data class StudentGrade(
    val id: String,
    val label: String,
    val stage: EducationStage,
)

data class StudentRow(
    val id: String,
    val code: String,
    val name: String,
    val englishName: String,
    val gradeId: String,
    val system: StudentSystem,
    val parentPhone: String,
)

data class StudentMonthlyPayment(
    val id: String,
    val studentId: String,
    val groupId: String,
    val group: LocalizedText,
    val teacher: LocalizedText,
    val billingMonth: LocalDate,
    val remainingEgp: Long,
) {
    val dueOn: LocalDate get() = LocalDate(billingMonth.year, billingMonth.month, 5)
}

data class OverdueStudentRow(
    val student: StudentRow,
    val payment: StudentMonthlyPayment,
)

data class StudentsSummary(
    val total: String,
    val secondary: String,
    val prep: String,
    val primary: String,
)

data class StudentsState(
    val summary: StudentsSummary,
    val students: List<StudentRow>,
    val grades: List<StudentGrade>,
    val userName: String,
    val dateLabel: String,
    val stageFilter: StudentStageFilter = StudentStageFilter.All,
    val searchQuery: String = "",
    val selectedGradeId: String? = null,
    val gradeMenuExpanded: Boolean = false,
    val addStudent: AddStudentState? = null,
    val monthlyPayments: List<StudentMonthlyPayment> = emptyList(),
    val asOf: LocalDate? = null,
) {
    val availableGrades: List<StudentGrade> get() = grades.filter { stageFilter.matches(it.stage) }
    val visibleStudents: List<StudentRow>
        get() {
            val query = searchQuery.trim()
            val gradeById = grades.associateBy { it.id }
            return students.filter { student ->
                stageFilter.matches(gradeById[student.gradeId]?.stage) &&
                    (selectedGradeId == null || selectedGradeId == student.gradeId) &&
                    student.matchesSearch(query)
            }
        }

    val overdueRows: List<OverdueStudentRow>
        get() {
            val date = asOf ?: return emptyList()
            val studentById = students.associateBy { it.id }
            return monthlyPayments.mapNotNull { payment ->
                val student = studentById[payment.studentId]
                if (student != null && payment.remainingEgp > 0 && date > payment.dueOn) {
                    OverdueStudentRow(student, payment)
                } else {
                    null
                }
            }
        }

    val visibleOverdueRows: List<OverdueStudentRow>
        get() {
            val query = searchQuery.trim()
            return overdueRows.filter { (student, payment) ->
                student.matchesSearch(query) ||
                    student.parentPhone.contains(query) ||
                    payment.group.arabic.contains(query, ignoreCase = true) ||
                    payment.group.english.contains(query, ignoreCase = true) ||
                    payment.teacher.arabic.contains(query, ignoreCase = true) ||
                    payment.teacher.english.contains(query, ignoreCase = true)
            }
        }
}

private fun StudentRow.matchesSearch(query: String): Boolean =
    name.contains(query, ignoreCase = true) || englishName.contains(query, ignoreCase = true) || code.contains(query, ignoreCase = true)

private fun StudentStageFilter.matches(stage: EducationStage?): Boolean =
    when (this) {
        StudentStageFilter.All, StudentStageFilter.Overdue -> true
        StudentStageFilter.Primary -> stage == EducationStage.Primary
        StudentStageFilter.Prep -> stage == EducationStage.Prep
        StudentStageFilter.Secondary -> stage == EducationStage.Secondary
    }

data class AddStudentState(
    val name: String = "",
    val code: String = "",
    val grades: List<StudentGrade> = emptyList(),
    val selectedGradeId: String? = null,
    val gradeMenuExpanded: Boolean = false,
    val system: StudentSystem = StudentSystem.Arabic,
    val parentPhone: String = "",
    val nameError: String? = null,
    val codeError: String? = null,
    val gradeError: String? = null,
    val phoneError: String? = null,
    val message: String? = null,
    val canSubmit: Boolean = false,
    val isSubmitting: Boolean = false,
)
