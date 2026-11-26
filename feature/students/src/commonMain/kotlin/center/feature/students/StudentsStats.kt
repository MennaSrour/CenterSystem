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
import center.ui.components.CenterOneMetricCard
import center.ui.theme.CenterOneBrushes
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Indigo
import center.ui.theme.Pending
import center.ui.theme.Success

private data class StudentMetric(
    val title: String,
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
            StudentMetric("stat_total", summary.total, Indigo, Icons.Outlined.PeopleOutline),
            StudentMetric("stat_secondary", summary.secondary, Pending, Icons.AutoMirrored.Outlined.MenuBook),
            StudentMetric("stat_prep", summary.prep, Indigo, Icons.AutoMirrored.Outlined.MenuBook),
            StudentMetric("stat_primary", summary.primary, Success, Icons.AutoMirrored.Outlined.MenuBook),
        )
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth >= 1000.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
            metrics.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
                    group.forEach { metric ->
                        CenterOneMetricCard(
                            title = appString(metric.title),
                            value = metric.value,
                            caption = appString("student_unit"),
                            icon = { Icon(metric.icon, contentDescription = null) },
                            accent = metric.accent,
                            gradient = CenterOneBrushes.Treasury.takeIf { metric.title == "stat_total" },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
