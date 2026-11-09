package center.feature.financials

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import center.feature.financials.resources.Res
import center.feature.financials.resources.branch
import center.feature.financials.resources.brand
import center.feature.financials.resources.brand_en
import center.feature.financials.resources.closed_treasury
import center.feature.financials.resources.courses
import center.feature.financials.resources.courses_en
import center.feature.financials.resources.financials
import center.feature.financials.resources.financials_en
import center.feature.financials.resources.home
import center.feature.financials.resources.home_en
import center.feature.financials.resources.logout
import center.feature.financials.resources.menu
import center.feature.financials.resources.open_treasury
import center.feature.financials.resources.reports
import center.feature.financials.resources.reports_en
import center.feature.financials.resources.schedule
import center.feature.financials.resources.schedule_en
import center.feature.financials.resources.settings
import center.feature.financials.resources.settings_en
import center.feature.financials.resources.students
import center.feature.financials.resources.students_en
import center.ui.components.CenterOneBrand
import center.ui.components.CenterOneNavigationItem
import center.ui.components.CenterOneSidebar
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.theme.White
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class FinancialNavigationItem(
    val destination: FinancialsDestination,
    val title: StringResource,
    val subtitle: StringResource,
    val icon: ImageVector,
)

@Composable
internal fun FinancialsSidebar(
    state: FinancialsState,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items =
        listOf(
            FinancialNavigationItem(
                FinancialsDestination.Home,
                Res.string.home,
                Res.string.home_en,
                Icons.Outlined.Home,
            ),
            FinancialNavigationItem(
                FinancialsDestination.Students,
                Res.string.students,
                Res.string.students_en,
                Icons.Outlined.PeopleOutline,
            ),
            FinancialNavigationItem(
                FinancialsDestination.Courses,
                Res.string.courses,
                Res.string.courses_en,
                Icons.AutoMirrored.Outlined.MenuBook,
            ),
            FinancialNavigationItem(
                FinancialsDestination.Schedule,
                Res.string.schedule,
                Res.string.schedule_en,
                Icons.Outlined.CalendarToday,
            ),
            FinancialNavigationItem(
                FinancialsDestination.Financials,
                Res.string.financials,
                Res.string.financials_en,
                Icons.Outlined.AccountBalanceWallet,
            ),
            FinancialNavigationItem(
                FinancialsDestination.Reports,
                Res.string.reports,
                Res.string.reports_en,
                Icons.Outlined.Insights,
            ),
            FinancialNavigationItem(
                FinancialsDestination.Settings,
                Res.string.settings,
                Res.string.settings_en,
                Icons.Outlined.Settings,
            ),
        )
    CenterOneSidebar(
        modifier = modifier,
        header = {
            CenterOneBrand(stringResource(Res.string.brand), stringResource(Res.string.brand_en))
            HorizontalDivider(color = White.copy(alpha = 0.12f))
            Text(stringResource(Res.string.menu), style = MaterialTheme.typography.labelSmall)
        },
        footer = {
            TextButton(onClick = { onEvent(FinancialsEvent.LogOut) }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = White)
                Spacer(Modifier.size(12.dp))
                Text(stringResource(Res.string.logout), color = White)
            }
            Text(stringResource(Res.string.branch), style = MaterialTheme.typography.titleSmall)
            StatusBadge(
                stringResource(if (state.isTreasuryOpen) Res.string.open_treasury else Res.string.closed_treasury),
                if (state.isTreasuryOpen) StatusTone.Success else StatusTone.Pending,
            )
        },
    ) {
        items.forEach { item ->
            CenterOneNavigationItem(
                title = stringResource(item.title),
                subtitle = stringResource(item.subtitle),
                selected = item.destination == FinancialsDestination.Financials,
                onClick = { onEvent(FinancialsEvent.Navigate(item.destination)) },
                icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                contentColor = White,
            )
        }
    }
}
