package center.feature.dashboard

data class DashboardState(
    val userName: String,
    val dateLabel: String,
    val content: DashboardContent = DashboardContent.Loading,
    val studentCode: String = "",
    val checkIn: CheckInState = CheckInState.Idle,
    val isOnline: Boolean = true,
) {
    val canCheckIn: Boolean get() = studentCode.isNotBlank() && checkIn !is CheckInState.Submitting
}

sealed interface DashboardContent {
    data object Loading : DashboardContent

    data class Failure(
        val message: String,
    ) : DashboardContent

    data class Ready(
        val statistics: List<DashboardStatistic>,
        val activities: List<RecentActivity>,
    ) : DashboardContent
}

sealed interface CheckInState {
    data object Idle : CheckInState

    data object Submitting : CheckInState

    data class Failure(
        val message: String,
    ) : CheckInState

    data class Success(
        val message: String,
    ) : CheckInState
}

enum class StatisticKind { Attendance, Revenue, PendingMessages, Groups }

data class DashboardStatistic(
    val kind: StatisticKind,
    val formattedValue: String,
)

enum class ActivityStatus { Completed, Pending }

data class RecentActivity(
    val id: String,
    val studentName: String,
    val englishName: String,
    val studentCode: String,
    val formattedTime: String,
    val status: ActivityStatus,
)

enum class DashboardDestination { Home, Students, Courses, Schedule, Payments, Reports, Settings }
