package center.feature.students

enum class EducationStage { Primary, Prep, Secondary }

enum class StudentStageFilter { All, Primary, Prep, Secondary }

enum class StudentSystem { Arabic, Languages }

enum class StudentsDestination { Home, Students, Courses, Schedule, Financials, Reports, Settings }

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
) {
    val availableGrades: List<StudentGrade> get() = grades.filter { stageFilter.matches(it.stage) }
    val visibleStudents: List<StudentRow>
        get() {
            val query = searchQuery.trim()
            val gradeById = grades.associateBy { it.id }
            return students.filter { student ->
                stageFilter.matches(gradeById[student.gradeId]?.stage) &&
                    (selectedGradeId == null || selectedGradeId == student.gradeId) &&
                    (
                        query.isEmpty() ||
                            student.name.contains(query, ignoreCase = true) ||
                            student.englishName.contains(query, ignoreCase = true) ||
                            student.code.contains(query, ignoreCase = true)
                    )
            }
        }
}

private fun StudentStageFilter.matches(stage: EducationStage?): Boolean =
    when (this) {
        StudentStageFilter.All -> true
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
