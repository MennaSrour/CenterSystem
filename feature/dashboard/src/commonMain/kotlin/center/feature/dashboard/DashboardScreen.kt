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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import center.feature.dashboard.resources.Res
import center.feature.dashboard.resources.daily_dashboard
import center.feature.dashboard.resources.language_ar
import center.feature.dashboard.resources.language_en
import center.feature.dashboard.resources.loading
import center.feature.dashboard.resources.manager
import center.feature.dashboard.resources.notifications
import center.feature.dashboard.resources.overview
import center.feature.dashboard.resources.overview_en
import center.feature.dashboard.resources.retry
import center.feature.dashboard.resources.stats_subtitle
import center.feature.dashboard.resources.system_offline
import center.feature.dashboard.resources.system_online
import center.feature.dashboard.resources.today_stats
import center.feature.dashboard.resources.today_stats_en
import center.feature.dashboard.resources.welcome
import center.feature.dashboard.resources.welcome_subtitle
import center.ui.components.BilingualHeading
import center.ui.components.CenterOneCard
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.theme.CenterOneSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardScreen(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val contentWidth = maxOf(maxWidth, 1000.dp)
                val sidebarWidth = if (contentWidth >= 1600.dp) 320.dp else 260.dp
                // RTL puts the final child (sidebar) at the physical left, matching the reference.
                Row(Modifier.horizontalScroll(rememberScrollState()).width(contentWidth)) {
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
                                            stringResource(Res.string.daily_dashboard),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                        Text(
                                            stringResource(Res.string.welcome, state.userName),
                                            style = MaterialTheme.typography.headlineLarge,
                                        )
                                        Text(
                                            stringResource(Res.string.welcome_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    StatusBadge(
                                        stringResource(if (state.isOnline) Res.string.system_online else Res.string.system_offline),
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
                                            Text(stringResource(Res.string.loading))
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
                                                ) { Text(stringResource(Res.string.retry)) }
                                            }
                                        }
                                    }
                                is DashboardContent.Ready -> {
                                    item {
                                        Column(verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium)) {
                                            BilingualHeading(
                                                stringResource(Res.string.today_stats),
                                                stringResource(Res.string.today_stats_en),
                                            )
                                            Text(
                                                stringResource(Res.string.stats_subtitle),
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
                    SidebarMenu(onEvent, Modifier.width(sidebarWidth), isOnline = state.isOnline)
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
            BilingualHeading(stringResource(Res.string.overview), stringResource(Res.string.overview_en))
        }
        Surface(
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                stringResource(Res.string.language_ar),
                Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(onClick = { onEvent(DashboardEvent.RequestEnglish) }) { Text(stringResource(Res.string.language_en)) }
        IconButton(onClick = { onEvent(DashboardEvent.OpenNotifications) }) {
            Icon(Icons.Default.Notifications, contentDescription = stringResource(Res.string.notifications))
        }
        Spacer(Modifier.width(CenterOneSpacing.Medium))
        Column {
            Text(state.userName, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.manager),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
