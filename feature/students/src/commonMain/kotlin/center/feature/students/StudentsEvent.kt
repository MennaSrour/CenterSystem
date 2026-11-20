package center.feature.students

import center.ui.localization.AppLanguage

sealed interface StudentsEvent {
    data class OpenDetails(
        val id: String,
    ) : StudentsEvent

    data object CloseDetails : StudentsEvent

    data class StudentPhoneChanged(
        val value: String,
    ) : StudentsEvent

    data class DiscountChanged(
        val groupId: String,
        val value: String,
    ) : StudentsEvent

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

    data object DismissDelete : StudentsEvent

    data object ConfirmDelete : StudentsEvent

    data class ToggleGroup(
        val id: String,
    ) : StudentsEvent

    data class Navigate(
        val destination: StudentsDestination,
    ) : StudentsEvent

    data object OpenNotifications : StudentsEvent

    data class LanguageSelected(
        val language: AppLanguage,
    ) : StudentsEvent

    data class SendReminder(
        val studentId: String,
    ) : StudentsEvent

    data class RemindAll(
        val studentIds: List<String>,
    ) : StudentsEvent

    data class PayNow(
        val paymentId: String,
    ) : StudentsEvent

    data object LogOut : StudentsEvent
}
