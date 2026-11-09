package center.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import center.feature.dashboard.resources.Res
import center.feature.dashboard.resources.branch
import center.feature.dashboard.resources.brand_ar
import center.feature.dashboard.resources.brand_en
import center.feature.dashboard.resources.connected
import center.feature.dashboard.resources.courses
import center.feature.dashboard.resources.home
import center.feature.dashboard.resources.logout
import center.feature.dashboard.resources.main_menu
import center.feature.dashboard.resources.payments
import center.feature.dashboard.resources.reports
import center.feature.dashboard.resources.schedule
import center.feature.dashboard.resources.settings
import center.feature.dashboard.resources.students
import center.feature.dashboard.resources.system_offline
import center.ui.components.CenterOneNavigationItem
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Gold
import center.ui.theme.Navy
import center.ui.theme.White
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class MenuItem(
    val destination: DashboardDestination,
    val title: StringResource,
    val english: String,
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
            MenuItem(DashboardDestination.Home, Res.string.home, "Home", Icons.Outlined.Home),
            MenuItem(DashboardDestination.Students, Res.string.students, "Students", Icons.Outlined.PeopleOutline),
            MenuItem(DashboardDestination.Courses, Res.string.courses, "Courses", Icons.AutoMirrored.Outlined.MenuBook),
            MenuItem(DashboardDestination.Schedule, Res.string.schedule, "Schedule", Icons.Outlined.CalendarToday),
            MenuItem(DashboardDestination.Payments, Res.string.payments, "Payments", Icons.Outlined.AccountBalanceWallet),
            MenuItem(DashboardDestination.Reports, Res.string.reports, "Reports", Icons.Outlined.Insights),
            MenuItem(DashboardDestination.Settings, Res.string.settings, "Settings", Icons.Outlined.Settings),
        )
    Surface(modifier, color = Navy, contentColor = White) {
        Column(
            Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(CenterOneSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(shape = CircleShape, color = Gold, contentColor = Navy) {
                    Box(
                        Modifier.size(64.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("1", style = MaterialTheme.typography.headlineLarge) }
                }
                Column {
                    Text(stringResource(Res.string.brand_ar), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(Res.string.brand_en), style = MaterialTheme.typography.labelSmall, color = Gold)
                }
            }
            HorizontalDivider(color = White.copy(alpha = 0.12f))
            Text(
                stringResource(Res.string.main_menu),
                style = MaterialTheme.typography.labelSmall,
                color = White.copy(alpha = 0.6f),
            )
            items.forEach { item ->
                val title = stringResource(item.title)
                CenterOneNavigationItem(
                    title = title,
                    subtitle = item.english,
                    selected = item.destination == DashboardDestination.Home,
                    onClick = { onEvent(DashboardEvent.Navigate(item.destination)) },
                    icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                    contentColor = White,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onEvent(DashboardEvent.LogOut) }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = White)
                Spacer(Modifier.size(12.dp))
                Text(stringResource(Res.string.logout), color = White)
            }
            Text(stringResource(Res.string.branch), style = MaterialTheme.typography.titleSmall)
            StatusBadge(
                text = stringResource(if (isOnline) Res.string.connected else Res.string.system_offline),
                tone = if (isOnline) StatusTone.Success else StatusTone.Pending,
            )
        }
    }
}
