package center.feature.teachers

import center.ui.components.MainNavigation
import center.ui.localization.AppLanguage

sealed interface TeachersEvent {
    data class StaffTabSelected(
        val selected: Boolean,
    ) : TeachersEvent

    data class OpenStaff(
        val id: String,
    ) : TeachersEvent

    data class EditStaff(
        val id: String,
    ) : TeachersEvent

    data object AddStaff : TeachersEvent

    data class StaffDraftChanged(
        val draft: StaffDraft,
    ) : TeachersEvent

    data object SaveStaff : TeachersEvent

    data class SearchChanged(
        val query: String,
    ) : TeachersEvent

    data class OpenTeacher(
        val id: String,
    ) : TeachersEvent

    data class PaySalary(
        val id: String,
    ) : TeachersEvent

    data class EditTeacher(
        val id: String,
    ) : TeachersEvent

    data object OpenAddTeacher : TeachersEvent

    data object CloseModal : TeachersEvent

    data class NameChanged(
        val value: String,
    ) : TeachersEvent

    data class PhoneChanged(
        val value: String,
    ) : TeachersEvent

    data class SubjectChanged(
        val value: String,
    ) : TeachersEvent

    data class NotesChanged(
        val value: String,
    ) : TeachersEvent

    data object SaveTeacher : TeachersEvent

    data class Navigate(
        val destination: MainNavigation,
    ) : TeachersEvent

    data class LanguageSelected(
        val language: AppLanguage,
    ) : TeachersEvent
}
