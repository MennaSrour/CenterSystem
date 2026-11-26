package center.feature.dashboard

import center.ui.components.MainNavigation
import center.ui.localization.AppLanguage

sealed interface DashboardEvent {
    data class QueryChanged(
        val value: String,
    ) : DashboardEvent

    data object Search : DashboardEvent

    data class SelectStudent(
        val code: String,
    ) : DashboardEvent

    data class SelectAttendanceGroup(
        val id: String,
    ) : DashboardEvent

    data class AttendanceGroupMenuChanged(
        val expanded: Boolean,
    ) : DashboardEvent

    data class ToggleLessonAttendance(
        val lessonId: String,
    ) : DashboardEvent

    data class RegisterAttendance(
        val studentCode: String,
        val groupId: String? = null,
        val attendedLessonIds: Set<String> = emptySet(),
    ) : DashboardEvent

    data class CheckOut(
        val code: String,
        val groupId: String? = null,
    ) : DashboardEvent

    data class PaySubscription(
        val studentCode: String,
    ) : DashboardEvent

    data object AddStudent : DashboardEvent

    data object CollectSubscription : DashboardEvent

    data object RecordExpense : DashboardEvent

    data class LanguageSelected(
        val language: AppLanguage,
    ) : DashboardEvent

    data class Navigate(
        val destination: MainNavigation,
    ) : DashboardEvent
}
