package center.feature.financials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneMetricCard
import center.ui.theme.CenterOneBrushes
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Indigo
import center.ui.theme.Pending
import center.ui.theme.Rose
import center.ui.theme.Success

private data class FinancialMetric(
    val title: String,
    val value: String,
    val accent: Color,
    val icon: ImageVector,
    val gradient: Brush? = null,
)

@Composable
internal fun FinancialsStats(
    summary: FinancialSummary,
    modifier: Modifier = Modifier,
) {
    val metrics =
        listOfNotNull(
            FinancialMetric("inflow", summary.todayInflow, Success, Icons.Outlined.ArrowUpward, CenterOneBrushes.Attendance),
            FinancialMetric("outflow", summary.todayOutflow, Rose, Icons.Outlined.ArrowDownward),
            FinancialMetric(
                "balance",
                summary.currentBalance,
                Indigo,
                Icons.Outlined.AccountBalanceWallet,
                CenterOneBrushes.Treasury,
            ),
            summary.monthlyProfit?.let { FinancialMetric("profit", it, Pending, Icons.AutoMirrored.Outlined.TrendingUp) },
        )
    val caption = appString("currency")
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth >= 1000.dp) metrics.size else 2
        Column(verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
            metrics.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
                    group.forEach { metric ->
                        CenterOneMetricCard(
                            title = appString(metric.title),
                            value = metric.value,
                            caption = caption,
                            icon = { Icon(metric.icon, contentDescription = null) },
                            modifier = Modifier.weight(1f),
                            accent = metric.accent,
                            gradient = metric.gradient,
                        )
                    }
                }
            }
        }
    }
}
