package center.feature.dashboard

import center.ui.localization.AppLanguage

sealed interface DashboardEvent {
    data class StudentCodeChanged(
        val value: String,
    ) : DashboardEvent

    data object SubmitCheckIn : DashboardEvent

    data object Retry : DashboardEvent

    data object ViewAllActivity : DashboardEvent

    data object OpenNotifications : DashboardEvent

    data class LanguageSelected(
        val language: AppLanguage,
    ) : DashboardEvent

    data object LogOut : DashboardEvent

    data class Navigate(
        val destination: DashboardDestination,
    ) : DashboardEvent
}
