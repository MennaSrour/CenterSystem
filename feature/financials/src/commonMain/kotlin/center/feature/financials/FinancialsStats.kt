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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import center.feature.financials.resources.Res
import center.feature.financials.resources.balance
import center.feature.financials.resources.currency
import center.feature.financials.resources.inflow
import center.feature.financials.resources.outflow
import center.feature.financials.resources.profit
import center.ui.components.CenterOneMetricCard
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Navy
import center.ui.theme.Pending
import center.ui.theme.Pink
import center.ui.theme.Success
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class FinancialMetric(
    val title: StringResource,
    val value: String,
    val accent: Color,
    val icon: ImageVector,
    val highlighted: Boolean = false,
)

@Composable
internal fun FinancialsStats(
    summary: FinancialSummary,
    modifier: Modifier = Modifier,
) {
    val metrics =
        listOf(
            FinancialMetric(Res.string.inflow, summary.todayInflow, Success, Icons.Outlined.ArrowUpward),
            FinancialMetric(Res.string.outflow, summary.todayOutflow, Pink, Icons.Outlined.ArrowDownward),
            FinancialMetric(Res.string.balance, summary.currentBalance, Navy, Icons.Outlined.AccountBalanceWallet, highlighted = true),
            FinancialMetric(Res.string.profit, summary.monthlyProfit, Pending, Icons.AutoMirrored.Outlined.TrendingUp),
        )
    val caption = stringResource(Res.string.currency)
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth >= 1000.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
            metrics.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
                    group.forEach { metric ->
                        CenterOneMetricCard(
                            title = stringResource(metric.title),
                            value = metric.value,
                            caption = caption,
                            icon = { Icon(metric.icon, contentDescription = null, tint = metric.accent) },
                            modifier = Modifier.weight(1f),
                            accent = metric.accent,
                            highlighted = metric.highlighted,
                        )
                    }
                }
            }
        }
    }
}
