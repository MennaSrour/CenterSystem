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
import center.feature.dashboard.resources.Res
import center.feature.dashboard.resources.action
import center.feature.dashboard.resources.activity_subtitle
import center.feature.dashboard.resources.check_in_action
import center.feature.dashboard.resources.completed
import center.feature.dashboard.resources.empty_activity
import center.feature.dashboard.resources.pending
import center.feature.dashboard.resources.recent_activity
import center.feature.dashboard.resources.recent_activity_en
import center.feature.dashboard.resources.status
import center.feature.dashboard.resources.student_id
import center.feature.dashboard.resources.student_name
import center.feature.dashboard.resources.time
import center.feature.dashboard.resources.view_all
import center.ui.components.BilingualHeading
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneCard
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.theme.CenterOneSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecentActivityTable(
    activities: List<RecentActivity>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneCard(modifier) {
        Row(Modifier.fillMaxWidth().padding(CenterOneSpacing.Large), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BilingualHeading(stringResource(Res.string.recent_activity), stringResource(Res.string.recent_activity_en))
                Text(
                    stringResource(Res.string.activity_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onViewAll) { Text(stringResource(Res.string.view_all)) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = CenterOneSpacing.Large, vertical = CenterOneSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TableCell(stringResource(Res.string.student_name), 2f)
            TableCell(stringResource(Res.string.student_id))
            TableCell(stringResource(Res.string.action), 1.5f)
            TableCell(stringResource(Res.string.time))
            TableCell(stringResource(Res.string.status))
        }
        if (activities.isEmpty()) {
            Text(stringResource(Res.string.empty_activity), Modifier.padding(CenterOneSpacing.Large))
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
                            CenterOneAvatar(activity.studentName.take(1))
                            Column {
                                Text(activity.studentName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    activity.englishName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TableCell(activity.studentCode, ltr = true)
                        TableCell(stringResource(Res.string.check_in_action), 1.5f)
                        TableCell(activity.formattedTime, ltr = true)
                        Column(Modifier.weight(1f)) {
                            StatusBadge(
                                text =
                                    stringResource(
                                        if (activity.status ==
                                            ActivityStatus.Completed
                                        ) {
                                            Res.string.completed
                                        } else {
                                            Res.string.pending
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
        CompositionLocalProvider(LocalLayoutDirection provides if (ltr) LayoutDirection.Ltr else LayoutDirection.Rtl) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
