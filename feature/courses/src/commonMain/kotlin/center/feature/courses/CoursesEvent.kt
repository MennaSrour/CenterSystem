package center.feature.courses

sealed interface CoursesEvent {
    data class TeacherFilterSelected(
        val id: String?,
    ) : CoursesEvent

    data class GradeFilterSelected(
        val id: String?,
    ) : CoursesEvent

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

    data class AddMakeupSession(
        val id: String,
    ) : CoursesEvent

    data class ExportLessonRoster(
        val id: String,
    ) : CoursesEvent

    data class PrintLessonRoster(
        val id: String,
    ) : CoursesEvent

    data class ExportTimetable(
        val gradeId: String?,
        val track: TimetableTrack,
        val language: center.ui.localization.AppLanguage,
    ) : CoursesEvent

    data class PrintTimetable(
        val gradeId: String?,
        val track: TimetableTrack,
        val language: center.ui.localization.AppLanguage,
    ) : CoursesEvent

    data class MakeupDateChanged(
        val value: String,
    ) : CoursesEvent

    data class MakeupStartTimeChanged(
        val value: String,
    ) : CoursesEvent

    data class MakeupEndTimeChanged(
        val value: String,
    ) : CoursesEvent

    data class OriginalCanceledDateChanged(
        val value: String,
    ) : CoursesEvent

    data class MakeupNotesChanged(
        val value: String,
    ) : CoursesEvent

    data object SubmitMakeupSession : CoursesEvent

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

    data class LessonTypeSelected(
        val lessonType: CourseLessonType,
    ) : CoursesEvent

    data class MonthlyPriceChanged(
        val value: String,
    ) : CoursesEvent

    data class LanguagesPriceChanged(
        val value: String,
    ) : CoursesEvent

    data class StudentSearchChanged(
        val value: String,
    ) : CoursesEvent

    data class ShowOtherGradesChanged(
        val show: Boolean,
    ) : CoursesEvent

    data class StudentEnrollmentToggled(
        val studentId: String,
        val selected: Boolean,
    ) : CoursesEvent

    data class StudentTrackSelected(
        val studentId: String,
        val track: center.domain.model.StudentTrack,
    ) : CoursesEvent

    data class StudentPriceChanged(
        val studentId: String,
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

    data class ToggleScheduleDay(
        val day: ScheduleDay,
    ) : CoursesEvent

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

    data class LanguageSelected(
        val language: center.ui.localization.AppLanguage,
    ) : CoursesEvent

    data object LogOut : CoursesEvent
}
