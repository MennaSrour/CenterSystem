package center.feature.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import center.feature.students.resources.Res
import center.feature.students.resources.stat_prep
import center.feature.students.resources.stat_primary
import center.feature.students.resources.stat_secondary
import center.feature.students.resources.stat_total
import center.feature.students.resources.student_unit
import center.ui.components.CenterOneMetricCard
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Navy
import center.ui.theme.Pending
import center.ui.theme.Pink
import center.ui.theme.Success
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class StudentMetric(
    val title: StringResource,
    val value: String,
    val accent: Color,
    val icon: ImageVector,
)

@Composable
internal fun StudentsStats(
    summary: StudentsSummary,
    modifier: Modifier = Modifier,
) {
    val metrics =
        listOf(
            StudentMetric(Res.string.stat_total, summary.total, Navy, Icons.Outlined.PeopleOutline),
            StudentMetric(Res.string.stat_secondary, summary.secondary, Pending, Icons.AutoMirrored.Outlined.MenuBook),
            StudentMetric(Res.string.stat_prep, summary.prep, Pink, Icons.AutoMirrored.Outlined.MenuBook),
            StudentMetric(Res.string.stat_primary, summary.primary, Success, Icons.AutoMirrored.Outlined.MenuBook),
        )
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth >= 1000.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
            metrics.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
                    group.forEach { metric ->
                        CenterOneMetricCard(
                            title = stringResource(metric.title),
                            value = metric.value,
                            caption = stringResource(Res.string.student_unit),
                            icon = { Icon(metric.icon, contentDescription = null, tint = metric.accent) },
                            accent = metric.accent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
