package center.feature.dashboard

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
import center.ui.components.CenterOneBrand
import center.ui.components.CenterOneNavigationItem
import center.ui.components.CenterOneSidebar
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.theme.White

private data class MenuItem(
    val destination: DashboardDestination,
    val title: DashboardText,
    val icon: ImageVector,
)

@Composable
fun SidebarMenu(
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
) {
    val items =
        listOf(
            MenuItem(DashboardDestination.Home, DashboardText.HOME, Icons.Outlined.Home),
            MenuItem(DashboardDestination.Students, DashboardText.STUDENTS, Icons.Outlined.PeopleOutline),
            MenuItem(DashboardDestination.Courses, DashboardText.COURSES, Icons.AutoMirrored.Outlined.MenuBook),
            MenuItem(DashboardDestination.Schedule, DashboardText.SCHEDULE, Icons.Outlined.CalendarToday),
            MenuItem(DashboardDestination.Payments, DashboardText.PAYMENTS, Icons.Outlined.AccountBalanceWallet),
            MenuItem(DashboardDestination.Reports, DashboardText.REPORTS, Icons.Outlined.Insights),
            MenuItem(DashboardDestination.Settings, DashboardText.SETTINGS, Icons.Outlined.Settings),
        )

    CenterOneSidebar(
        modifier = modifier,
        header = {
            CenterOneBrand(dashboardString(DashboardText.BRAND_AR), dashboardString(DashboardText.BRAND_EN))
            HorizontalDivider(color = White.copy(alpha = 0.12f))
            Text(dashboardString(DashboardText.MAIN_MENU), style = MaterialTheme.typography.labelSmall, color = White.copy(alpha = 0.6f))
        },
        footer = {
            TextButton(onClick = { onEvent(DashboardEvent.LogOut) }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = White)
                Spacer(Modifier.size(12.dp))
                Text(dashboardString(DashboardText.LOGOUT), color = White)
            }
            Text(dashboardString(DashboardText.BRANCH), style = MaterialTheme.typography.titleSmall)
            StatusBadge(
                text = dashboardString(if (isOnline) DashboardText.CONNECTED else DashboardText.SYSTEM_OFFLINE),
                tone = if (isOnline) StatusTone.Success else StatusTone.Pending,
            )
        },
    ) {
        items.forEach { item ->
            CenterOneNavigationItem(
                title = dashboardString(item.title),
                subtitle = "",
                selected = item.destination == DashboardDestination.Home,
                onClick = { onEvent(DashboardEvent.Navigate(item.destination)) },
                icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                contentColor = White,
            )
        }
    }
}
