package center.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import center.app.resources.Res
import center.app.resources.activity_demo
import center.app.resources.check_in_unavailable
import center.app.resources.demo_label
import center.app.resources.demo_user
import center.app.resources.feature_unavailable
import center.feature.dashboard.ActivityStatus
import center.feature.dashboard.CheckInState
import center.feature.dashboard.DashboardContent
import center.feature.dashboard.DashboardDestination
import center.feature.dashboard.DashboardEvent
import center.feature.dashboard.DashboardScreen
import center.feature.dashboard.DashboardState
import center.feature.dashboard.DashboardStatistic
import center.feature.dashboard.RecentActivity
import center.feature.dashboard.StatisticKind
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Demo composition boundary. Replace the snapshot/event handler with a domain-backed state holder. */
@Composable
internal fun DashboardRoute(root: RootComponent) {
    val userName = stringResource(Res.string.demo_user)
    val dateLabel = stringResource(Res.string.demo_label)
    val checkInUnavailable = stringResource(Res.string.check_in_unavailable)
    val unavailable = stringResource(Res.string.feature_unavailable)
    val allActivity = stringResource(Res.string.activity_demo)
    var state by remember { mutableStateOf(dashboardDemoState(userName, dateLabel)) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        DashboardScreen(state, onEvent = { event ->
            when (event) {
                is DashboardEvent.StudentCodeChanged -> state = state.copy(studentCode = event.value, checkIn = CheckInState.Idle)
                DashboardEvent.SubmitCheckIn -> if (state.canCheckIn) state = state.copy(checkIn = CheckInState.Failure(checkInUnavailable))
                DashboardEvent.Retry -> state = dashboardDemoState(userName, dateLabel)
                is DashboardEvent.Navigate ->
                    when (event.destination) {
                        DashboardDestination.Home -> Unit
                        DashboardDestination.Students -> root.navigate(Destination.Students)
                        DashboardDestination.Payments -> root.navigate(Destination.Financials)
                        DashboardDestination.Reports -> root.navigate(Destination.Reports)
                        DashboardDestination.Courses, DashboardDestination.Schedule, DashboardDestination.Settings ->
                            scope
                                .launch {
                                    snackbar.showSnackbar(unavailable)
                                }.let { Unit }
                    }
                DashboardEvent.ViewAllActivity -> scope.launch { snackbar.showSnackbar(allActivity) }.let { Unit }
                DashboardEvent.OpenNotifications, DashboardEvent.RequestEnglish, DashboardEvent.LogOut ->
                    scope
                        .launch {
                            snackbar.showSnackbar(unavailable)
                        }.let { Unit }
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
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
