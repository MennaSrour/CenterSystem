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
import center.feature.dashboard.resources.Res
import center.feature.dashboard.resources.active_now
import center.feature.dashboard.resources.attendance
import center.feature.dashboard.resources.currency
import center.feature.dashboard.resources.empty_stats
import center.feature.dashboard.resources.groups
import center.feature.dashboard.resources.pending_messages
import center.feature.dashboard.resources.revenue
import center.feature.dashboard.resources.today
import center.feature.dashboard.resources.waiting
import center.ui.components.CenterOneMetricCard
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Navy
import center.ui.theme.Pending
import center.ui.theme.Pink
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatsGrid(
    statistics: List<DashboardStatistic>,
    modifier: Modifier = Modifier,
) {
    if (statistics.isEmpty()) {
        Text(stringResource(Res.string.empty_stats), modifier, style = MaterialTheme.typography.bodyMedium)
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
            StatisticKind.Attendance -> Res.string.attendance
            StatisticKind.Revenue -> Res.string.revenue
            StatisticKind.PendingMessages -> Res.string.pending_messages
            StatisticKind.Groups -> Res.string.groups
        }
    val caption =
        when (statistic.kind) {
            StatisticKind.Attendance -> Res.string.today
            StatisticKind.Revenue -> Res.string.currency
            StatisticKind.PendingMessages -> Res.string.waiting
            StatisticKind.Groups -> Res.string.active_now
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
        title = stringResource(label),
        value = statistic.formattedValue,
        caption = stringResource(caption),
        icon = { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp)) },
        modifier = modifier,
        accent = accent,
        highlighted = statistic.kind == StatisticKind.PendingMessages,
    )
}
