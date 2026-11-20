package center.domain.model

import kotlinx.datetime.LocalDate

data class StaffMember(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val workSchedule: List<GroupScheduleSlot> = emptyList(),
    val isActive: Boolean = true,
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
