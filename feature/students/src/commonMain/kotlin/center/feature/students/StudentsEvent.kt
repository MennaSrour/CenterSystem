package center.feature.students

sealed interface StudentsEvent {
    data class StageSelected(
        val filter: StudentStageFilter,
    ) : StudentsEvent

    data class SearchChanged(
        val value: String,
    ) : StudentsEvent

    data class GradeFilterMenuChanged(
        val expanded: Boolean,
    ) : StudentsEvent

    data class GradeFilterSelected(
        val id: String?,
    ) : StudentsEvent

    data object OpenAddStudent : StudentsEvent

    data object DismissAddStudent : StudentsEvent

    data class NameChanged(
        val value: String,
    ) : StudentsEvent

    data class CodeChanged(
        val value: String,
    ) : StudentsEvent

    data class AddGradeMenuChanged(
        val expanded: Boolean,
    ) : StudentsEvent

    data class AddGradeSelected(
        val id: String,
    ) : StudentsEvent

    data class SystemSelected(
        val system: StudentSystem,
    ) : StudentsEvent

    data class ParentPhoneChanged(
        val value: String,
    ) : StudentsEvent

    data object SubmitAddStudent : StudentsEvent

    data class EditStudent(
        val id: String,
    ) : StudentsEvent

    data class DeleteStudent(
        val id: String,
    ) : StudentsEvent

    data class Navigate(
        val destination: StudentsDestination,
    ) : StudentsEvent

    data object OpenNotifications : StudentsEvent

    data object RequestEnglish : StudentsEvent

    data object LogOut : StudentsEvent
}
