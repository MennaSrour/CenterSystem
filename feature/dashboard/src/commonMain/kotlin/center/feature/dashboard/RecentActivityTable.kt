package center.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import center.ui.components.BilingualHeading
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneCard
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.theme.CenterOneSpacing

@Composable
fun RecentActivityTable(
    activities: List<RecentActivity>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneCard(modifier) {
        Row(Modifier.fillMaxWidth().padding(CenterOneSpacing.Large), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BilingualHeading(dashboardString(DashboardText.RECENT_ACTIVITY), dashboardString(DashboardText.RECENT_ACTIVITY_EN))
                Text(
                    dashboardString(DashboardText.ACTIVITY_SUBTITLE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onViewAll) { Text(dashboardString(DashboardText.VIEW_ALL)) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = CenterOneSpacing.Large, vertical = CenterOneSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TableCell(dashboardString(DashboardText.STUDENT_NAME), 2f)
            TableCell(dashboardString(DashboardText.STUDENT_ID))
            TableCell(dashboardString(DashboardText.ACTION), 1.5f)
            TableCell(dashboardString(DashboardText.TIME))
            TableCell(dashboardString(DashboardText.STATUS))
        }
        if (activities.isEmpty()) {
            Text(dashboardString(DashboardText.EMPTY_ACTIVITY), Modifier.padding(CenterOneSpacing.Large))
        } else {
            // Bounded inner list keeps large caller-owned snapshots lazy within the dashboard scroll.
            LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                items(activities, key = { it.id }) { activity ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = CenterOneSpacing.Large, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier.weight(2f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val name = if (LocalAppLanguage.current == AppLanguage.Arabic) activity.studentName else activity.englishName
                            CenterOneAvatar(name.take(1))
                            Text(name, style = MaterialTheme.typography.titleSmall)
                        }
                        TableCell(activity.studentCode, ltr = true)
                        TableCell(dashboardString(DashboardText.CHECK_IN_ACTION), 1.5f)
                        TableCell(activity.formattedTime, ltr = true)
                        Column(Modifier.weight(1f)) {
                            StatusBadge(
                                text =
                                    dashboardString(
                                        if (activity.status ==
                                            ActivityStatus.Completed
                                        ) {
                                            DashboardText.COMPLETED
                                        } else {
                                            DashboardText.PENDING
                                        },
                                    ),
                                tone = if (activity.status == ActivityStatus.Completed) StatusTone.Success else StatusTone.Pending,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float = 1f,
    ltr: Boolean = false,
) {
    Column(Modifier.weight(weight)) {
        CompositionLocalProvider(LocalLayoutDirection provides if (ltr) LayoutDirection.Ltr else LocalAppLanguage.current.layoutDirection) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
