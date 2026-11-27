package center.domain.model

import kotlinx.datetime.LocalDate

data class StaffMember(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val workSchedule: List<GroupScheduleSlot> = emptyList(),
    val isActive: Boolean = true,
    val monthlySalaryMinor: Long? = null,
)

data class LessonAttendance(
    val studentId: String,
    val lessonId: String,
    val date: LocalDate,
    val attended: Boolean,
    val recordedAt: Long,
    val checkOutAt: Long? = null,
)

enum class PersonType { STUDENT, TEACHER, STAFF }

data class AttendancePerson(
    val type: PersonType,
    val id: String,
    val groupId: String = "",
) {
    init {
        require(id.isNotBlank())
        require(type == PersonType.STUDENT || groupId.isEmpty())
    }
}

data class AttendanceSession(
    val person: AttendancePerson,
    val date: LocalDate,
    val checkInTime: Long,
    val checkOutTime: Long?,
)

fun Student.activeSchedule(groups: List<ClassGroup>): List<ClassGroup> =
    groups.filter { it.status == GroupStatus.ACTIVE && it.id in enrolledGroupIds }

fun Teacher.activeSchedule(groups: List<ClassGroup>): List<ClassGroup> =
    groups.filter { it.status == GroupStatus.ACTIVE && it.teacherId == id }

fun ClassGroup.effectiveMonthlyPriceMinor(student: Student): Long {
    student.groupCustomPricesMinor[id]?.let { return it }
    val track =
        student.groupTracks[id] ?: when (lessonType) {
            LessonType.ARABIC, LessonType.MIXED -> StudentTrack.ARABIC
            LessonType.LANGUAGES -> StudentTrack.LANGUAGES
            LessonType.PRIVATE -> StudentTrack.PRIVATE
        }
    val base = if (lessonType == LessonType.MIXED && track == StudentTrack.LANGUAGES) languagesPriceMinor else monthlyPriceMinor
    return ((base ?: 0L) - ((student.groupDiscounts[id] ?: 0.0) * 100).toLong()).coerceAtLeast(0)
}

fun Student.totalMonthlyDueMinor(groups: List<ClassGroup>): Long = activeSchedule(groups).sumOf { it.effectiveMonthlyPriceMinor(this) }

data class TeacherLessonAnalytics(
    val assignedLessonCount: Int,
    val arabicStudentCount: Int,
    val languagesStudentCount: Int,
    val privateStudentCount: Int,
    val expectedSchedule: List<GroupScheduleSlot>,
)

fun Teacher.lessonAnalytics(
    groups: List<ClassGroup>,
    students: List<Student>,
): TeacherLessonAnalytics {
    val assigned = activeSchedule(groups)
    val studentTracks =
        students
            .flatMap { student ->
                assigned.filter { it.id in student.enrolledGroupIds }.map { group ->
                    student.id to (
                        student.groupTracks[group.id] ?: when (group.lessonType) {
                            LessonType.LANGUAGES -> StudentTrack.LANGUAGES
                            LessonType.PRIVATE -> StudentTrack.PRIVATE
                            LessonType.ARABIC, LessonType.MIXED -> StudentTrack.ARABIC
                        }
                    )
                }
            }.distinct()
    return TeacherLessonAnalytics(
        assigned.size,
        studentTracks
            .filter { it.second == StudentTrack.ARABIC }
            .map { it.first }
            .distinct()
            .size,
        studentTracks
            .filter { it.second == StudentTrack.LANGUAGES }
            .map { it.first }
            .distinct()
            .size,
        studentTracks
            .filter { it.second == StudentTrack.PRIVATE }
            .map { it.first }
            .distinct()
            .size,
        assigned.flatMap { it.schedule }.distinct().sortedWith(compareBy({ it.day.ordinal }, { it.from })),
    )
}
