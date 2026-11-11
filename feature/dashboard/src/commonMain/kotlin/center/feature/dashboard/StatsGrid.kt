package center.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneMetricCard
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Navy
import center.ui.theme.Pending
import center.ui.theme.Pink

@Composable
fun StatsGrid(
    statistics: List<DashboardStatistic>,
    modifier: Modifier = Modifier,
) {
    if (statistics.isEmpty()) {
        Text(dashboardString(DashboardText.EMPTY_STATS), modifier, style = MaterialTheme.typography.bodyMedium)
        return
    }
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth >= 1000.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
            statistics.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
                    group.forEach { statistic -> StatisticCard(statistic, Modifier.weight(1f)) }
                    repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StatisticCard(
    statistic: DashboardStatistic,
    modifier: Modifier = Modifier,
) {
    val label =
        when (statistic.kind) {
            StatisticKind.Attendance -> DashboardText.ATTENDANCE
            StatisticKind.Revenue -> DashboardText.REVENUE
            StatisticKind.PendingMessages -> DashboardText.PENDING_MESSAGES
            StatisticKind.Groups -> DashboardText.GROUPS
        }
    val caption =
        when (statistic.kind) {
            StatisticKind.Attendance -> DashboardText.TODAY
            StatisticKind.Revenue -> DashboardText.CURRENCY
            StatisticKind.PendingMessages -> DashboardText.WAITING
            StatisticKind.Groups -> DashboardText.ACTIVE_NOW
        }
    val icon =
        when (statistic.kind) {
            StatisticKind.Attendance -> Icons.Outlined.AccessTime
            StatisticKind.Revenue -> Icons.AutoMirrored.Outlined.TrendingUp
            StatisticKind.PendingMessages -> Icons.Outlined.ChatBubbleOutline
            StatisticKind.Groups -> Icons.Outlined.Layers
        }
    val accent =
        when (statistic.kind) {
            StatisticKind.PendingMessages -> Pink
            StatisticKind.Attendance -> Pending
            StatisticKind.Revenue, StatisticKind.Groups -> Navy
        }
    CenterOneMetricCard(
        title = dashboardString(label),
        value = statistic.formattedValue,
        caption = dashboardString(caption),
        icon = { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp)) },
        modifier = modifier,
        accent = accent,
        highlighted = statistic.kind == StatisticKind.PendingMessages,
    )
}
