package center.feature.courses

sealed interface CoursesEvent {
    data class StageSelected(
        val filter: CourseStageFilter,
    ) : CoursesEvent

    data class SearchChanged(
        val value: String,
    ) : CoursesEvent

    data class GroupMenuChanged(
        val groupId: String?,
    ) : CoursesEvent

    data object OpenAddGroup : CoursesEvent

    data class EditGroup(
        val id: String,
    ) : CoursesEvent

    data class DeactivateGroup(
        val id: String,
    ) : CoursesEvent

    data class RequestDeleteGroup(
        val id: String,
    ) : CoursesEvent

    data object DismissModal : CoursesEvent

    data class SubjectChanged(
        val value: String,
    ) : CoursesEvent

    data class DropdownChanged(
        val dropdown: GroupDropdown?,
    ) : CoursesEvent

    data class TeacherSelected(
        val id: String,
    ) : CoursesEvent

    data class GradeSelected(
        val id: String,
    ) : CoursesEvent

    data class SystemSelected(
        val system: CourseSystem,
    ) : CoursesEvent

    data class MonthlyPriceChanged(
        val value: String,
    ) : CoursesEvent

    data class CommissionChanged(
        val value: String,
    ) : CoursesEvent

    data class MaxCapacityChanged(
        val value: String,
    ) : CoursesEvent

    data class RoomSelected(
        val id: String,
    ) : CoursesEvent

    data object AddTimeSlot : CoursesEvent

    data class RemoveTimeSlot(
        val id: String,
    ) : CoursesEvent

    data class DayMenuChanged(
        val slotId: String?,
    ) : CoursesEvent

    data class DaySelected(
        val slotId: String,
        val day: ScheduleDay,
    ) : CoursesEvent

    data class FromTimeChanged(
        val slotId: String,
        val value: String,
    ) : CoursesEvent

    data class ToTimeChanged(
        val slotId: String,
        val value: String,
    ) : CoursesEvent

    data object SubmitGroup : CoursesEvent

    data object ConfirmDeleteGroup : CoursesEvent

    data class Navigate(
        val destination: CoursesDestination,
    ) : CoursesEvent

    data object OpenNotifications : CoursesEvent

    data object RequestEnglish : CoursesEvent

    data object LogOut : CoursesEvent
}
