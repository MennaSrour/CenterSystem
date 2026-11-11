package center.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import center.feature.dashboard.ActivityStatus
import center.feature.dashboard.CheckInState
import center.feature.dashboard.DashboardContent
import center.feature.dashboard.DashboardDestination
import center.feature.dashboard.DashboardEvent
import center.feature.dashboard.DashboardScreen
import center.feature.dashboard.DashboardState
import center.feature.dashboard.DashboardStatistic
import center.feature.dashboard.DashboardText
import center.feature.dashboard.RecentActivity
import center.feature.dashboard.StatisticKind
import center.feature.dashboard.text
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import kotlinx.coroutines.launch

/** Demo composition boundary. Replace the snapshot/event handler with a domain-backed state holder. */
@Composable
internal fun DashboardRoute(
    root: RootComponent,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val userName = DashboardText.DEMO_USER.text(language)
    val dateLabel = DashboardText.DEMO_DATE.text(language)
    val checkInUnavailable = DashboardText.CHECK_IN_UNAVAILABLE.text(language)
    val unavailable = DashboardText.FEATURE_UNAVAILABLE.text(language)
    val allActivity = DashboardText.ALL_ACTIVITY_MESSAGE.text(language)
    var state by remember { mutableStateOf(dashboardDemoState(userName, dateLabel)) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(language) { snackbar.currentSnackbarData?.dismiss() }
    CenterOneLocalization(language) {
        Box(Modifier.fillMaxSize()) {
            val displayedState =
                state.copy(
                    userName = userName,
                    dateLabel = dateLabel,
                    language = language,
                    checkIn = if (state.checkIn is CheckInState.Failure) CheckInState.Failure(checkInUnavailable) else state.checkIn,
                )
            DashboardScreen(displayedState, onEvent = { event ->
                when (event) {
                    is DashboardEvent.StudentCodeChanged -> state = state.copy(studentCode = event.value, checkIn = CheckInState.Idle)
                    DashboardEvent.SubmitCheckIn ->
                        if (state.canCheckIn) {
                            state =
                                state.copy(checkIn = CheckInState.Failure(checkInUnavailable))
                        }
                    DashboardEvent.Retry -> state = dashboardDemoState(userName, dateLabel)
                    is DashboardEvent.Navigate ->
                        when (event.destination) {
                            DashboardDestination.Courses -> root.navigate(Destination.Courses)
                            DashboardDestination.Home -> Unit
                            DashboardDestination.Students -> root.navigate(Destination.Students)
                            DashboardDestination.Payments -> root.navigate(Destination.Financials)
                            DashboardDestination.Reports -> root.navigate(Destination.Reports)
                            DashboardDestination.Schedule, DashboardDestination.Settings ->
                                scope
                                    .launch {
                                        snackbar.showSnackbar(unavailable)
                                    }.let { Unit }
                        }
                    DashboardEvent.ViewAllActivity -> scope.launch { snackbar.showSnackbar(allActivity) }.let { Unit }
                    is DashboardEvent.LanguageSelected -> onLanguageSelected(event.language)
                    DashboardEvent.OpenNotifications, DashboardEvent.LogOut ->
                        scope
                            .launch {
                                snackbar.showSnackbar(unavailable)
                            }.let { Unit }
                }
            })
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun dashboardDemoState(
    userName: String,
    dateLabel: String,
) = DashboardState(
    userName = userName,
    dateLabel = dateLabel,
    content =
        DashboardContent.Ready(
            statistics =
                listOf(
                    DashboardStatistic(StatisticKind.Attendance, "420"),
                    DashboardStatistic(StatisticKind.Revenue, "12,500"),
                    DashboardStatistic(StatisticKind.PendingMessages, "45"),
                    DashboardStatistic(StatisticKind.Groups, "3"),
                ),
            activities =
                listOf(
                    RecentActivity("demo-1", "أحمد محمد", "Ahmed Mohamed", "10234", "09:42", ActivityStatus.Completed),
                    RecentActivity("demo-2", "سارة محمود", "Sara Mahmoud", "10817", "09:38", ActivityStatus.Completed),
                    RecentActivity("demo-3", "عمر مصطفى", "Omar Mostafa", "10452", "09:31", ActivityStatus.Completed),
                    RecentActivity("demo-4", "مريم خالد", "Mariam Khaled", "10903", "09:24", ActivityStatus.Completed),
                ),
        ),
)
