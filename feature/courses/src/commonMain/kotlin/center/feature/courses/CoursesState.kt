package center.feature.courses

enum class CourseStage { Primary, Prep, Secondary }

enum class CourseStageFilter { All, Primary, Prep, Secondary }

enum class CourseSystem { Arabic, Languages, Mixed }

enum class CoursesDestination { Home, Students, Courses, Schedule, Financials, Reports, Settings }

enum class ScheduleDay { Saturday, Sunday, Monday, Tuesday, Wednesday, Thursday, Friday }

enum class GroupDropdown { Teacher, Grade, Room }

data class CourseTeacher(
    val id: String,
    val name: String,
)

data class CourseGrade(
    val id: String,
    val label: String,
    val stage: CourseStage,
)

data class CourseRoom(
    val id: String,
    val label: String,
)

data class CourseSchedule(
    val id: String,
    val day: ScheduleDay,
    val from: String,
    val to: String,
)

data class CourseGroup(
    val id: String,
    val subject: String,
    val teacherId: String,
    val gradeId: String,
    val system: CourseSystem,
    val monthlyPrice: String,
    val teacherCommission: String,
    val roomId: String,
    val enrolled: Int,
    val maxCapacity: Int,
    val schedule: List<CourseSchedule>,
    val isActive: Boolean = true,
) {
    val capacityProgress: Float get() = if (maxCapacity > 0) (enrolled.toFloat() / maxCapacity).coerceIn(0f, 1f) else 0f
}

data class CoursesSummary(
    val totalGroups: String,
    val activeGroups: String,
    val totalTeachers: String,
)

data class CoursesState(
    val summary: CoursesSummary,
    val groups: List<CourseGroup>,
    val teachers: List<CourseTeacher>,
    val grades: List<CourseGrade>,
    val rooms: List<CourseRoom>,
    val userName: String,
    val dateLabel: String,
    val stageFilter: CourseStageFilter = CourseStageFilter.All,
    val searchQuery: String = "",
    val expandedGroupMenuId: String? = null,
    val modal: CoursesModal = CoursesModal.Closed,
) {
    val visibleGroups: List<CourseGroup>
        get() {
            val query = searchQuery.trim()
            val teachersById = teachers.associateBy { it.id }
            val gradesById = grades.associateBy { it.id }
            return groups.filter { group ->
                val stage = gradesById[group.gradeId]?.stage
                val matchesStage =
                    when (stageFilter) {
                        CourseStageFilter.All -> true
                        CourseStageFilter.Primary -> stage == CourseStage.Primary
                        CourseStageFilter.Prep -> stage == CourseStage.Prep
                        CourseStageFilter.Secondary -> stage == CourseStage.Secondary
                    }
                matchesStage &&
                    (
                        query.isEmpty() ||
                            group.subject.contains(query, true) ||
                            teachersById[group.teacherId]?.name?.contains(query, true) == true
                    )
            }
        }
}

sealed interface GroupFormMode {
    data object Add : GroupFormMode

    data class Edit(
        val groupId: String,
    ) : GroupFormMode
}

data class GroupTimeSlotState(
    val id: String,
    val day: ScheduleDay? = null,
    val from: String = "",
    val to: String = "",
    val error: String? = null,
)

data class GroupFormState(
    val mode: GroupFormMode = GroupFormMode.Add,
    val subject: String = "",
    val teachers: List<CourseTeacher> = emptyList(),
    val grades: List<CourseGrade> = emptyList(),
    val rooms: List<CourseRoom> = emptyList(),
    val selectedTeacherId: String? = null,
    val selectedGradeId: String? = null,
    val system: CourseSystem = CourseSystem.Arabic,
    val monthlyPrice: String = "",
    val teacherCommission: String = "70",
    val maxCapacity: String = "",
    val selectedRoomId: String? = null,
    val expandedDropdown: GroupDropdown? = null,
    val expandedDaySlotId: String? = null,
    val timeSlots: List<GroupTimeSlotState> = emptyList(),
    val fieldErrors: Map<GroupField, String> = emptyMap(),
    val message: String? = null,
    val canSubmit: Boolean = false,
    val isSubmitting: Boolean = false,
)

enum class GroupField { Subject, Teacher, Grade, MonthlyPrice, Commission, MaxCapacity, Room }

data class DeleteGroupState(
    val groupId: String,
    val groupTitle: String,
    val isDeleting: Boolean = false,
    val message: String? = null,
)

sealed interface CoursesModal {
    data object Closed : CoursesModal

    data class Form(
        val state: GroupFormState,
    ) : CoursesModal

    data class Delete(
        val state: DeleteGroupState,
    ) : CoursesModal
}
