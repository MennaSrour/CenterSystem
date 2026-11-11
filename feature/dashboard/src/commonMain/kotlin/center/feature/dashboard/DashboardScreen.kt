package center.feature.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.components.BilingualHeading
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneLanguageToggle
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.theme.CenterOneSpacing

@Composable
fun DashboardScreen(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneLocalization(state.language) {
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val contentWidth = maxOf(maxWidth, 1000.dp)
                val sidebarWidth = if (contentWidth >= 1600.dp) 320.dp else 260.dp
                // Keep the sidebar physically left while its content and main content follow the selected direction.
                Row(Modifier.horizontalScroll(rememberScrollState()).width(contentWidth)) {
                    if (state.language == AppLanguage.English) SidebarMenu(onEvent, Modifier.width(sidebarWidth), isOnline = state.isOnline)
                    Column(Modifier.weight(1f)) {
                        DashboardHeader(state, onEvent)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        LazyColumn(
                            Modifier.weight(1f).fillMaxWidth().padding(horizontal = CenterOneSpacing.Section),
                            verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Section),
                            contentPadding =
                                androidx.compose.foundation.layout
                                    .PaddingValues(vertical = CenterOneSpacing.Section),
                        ) {
                            item {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            dashboardString(DashboardText.DAILY_DASHBOARD),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                        Text(
                                            dashboardString(DashboardText.WELCOME, state.userName),
                                            style = MaterialTheme.typography.headlineLarge,
                                        )
                                        Text(
                                            dashboardString(DashboardText.WELCOME_SUBTITLE),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    StatusBadge(
                                        dashboardString(if (state.isOnline) DashboardText.SYSTEM_ONLINE else DashboardText.SYSTEM_OFFLINE),
                                        if (state.isOnline) StatusTone.Success else StatusTone.Pending,
                                    )
                                }
                            }
                            item { QuickCheckInSection(state, onEvent, Modifier.fillMaxWidth()) }
                            when (val content = state.content) {
                                DashboardContent.Loading ->
                                    item {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            CircularProgressIndicator(Modifier.size(24.dp))
                                            Text(dashboardString(DashboardText.LOADING))
                                        }
                                    }
                                is DashboardContent.Failure ->
                                    item {
                                        CenterOneCard(Modifier.fillMaxWidth()) {
                                            Column(
                                                Modifier.padding(CenterOneSpacing.Large),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                            ) {
                                                Text(content.message, color = MaterialTheme.colorScheme.error)
                                                CenterOnePrimaryButton(
                                                    onClick = { onEvent(DashboardEvent.Retry) },
                                                ) { Text(dashboardString(DashboardText.RETRY)) }
                                            }
                                        }
                                    }
                                is DashboardContent.Ready -> {
                                    item {
                                        Column(verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
                                            BilingualHeading(
                                                dashboardString(DashboardText.TODAY_STATS),
                                                dashboardString(DashboardText.TODAY_STATS_EN),
                                            )
                                            Text(
                                                dashboardString(DashboardText.STATS_SUBTITLE),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            StatsGrid(content.statistics, Modifier.fillMaxWidth())
                                        }
                                    }
                                    item {
                                        RecentActivityTable(
                                            content.activities,
                                            onViewAll = { onEvent(DashboardEvent.ViewAllActivity) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (state.language == AppLanguage.Arabic) SidebarMenu(onEvent, Modifier.width(sidebarWidth), isOnline = state.isOnline)
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CenterOneSpacing.Section, vertical = CenterOneSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(state.dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BilingualHeading(dashboardString(DashboardText.OVERVIEW), dashboardString(DashboardText.OVERVIEW_EN))
        }
        CenterOneLanguageToggle(state.language, { onEvent(DashboardEvent.LanguageSelected(it)) })
        IconButton(onClick = { onEvent(DashboardEvent.OpenNotifications) }) {
            Icon(Icons.Default.Notifications, contentDescription = dashboardString(DashboardText.NOTIFICATIONS))
        }
        Spacer(Modifier.width(CenterOneSpacing.Medium))
        Column {
            Text(state.userName, style = MaterialTheme.typography.titleSmall)
            Text(
                dashboardString(DashboardText.MANAGER),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
