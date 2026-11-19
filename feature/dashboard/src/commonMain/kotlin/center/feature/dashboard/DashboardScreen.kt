package center.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneAccentIcon
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneGradientCard
import center.ui.components.CenterOneLanguageToggle
import center.ui.components.CenterOneMainNavigation
import center.ui.components.CenterOnePageHeader
import center.ui.components.MainNavigation
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.theme.CenterOneBrushes
import center.ui.theme.White

@Composable
fun DashboardScreen(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneLocalization(state.language) {
        CenterOneCanvas(modifier) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val sidebarWidth = if (maxWidth < 1000.dp) 192.dp else 232.dp
                Row(Modifier.fillMaxSize()) {
                    // Keep the sidebar on the desktop's left edge in either reading direction.
                    if (state.language == AppLanguage.English) ReceptionSidebar(onEvent, Modifier.width(sidebarWidth))
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(Modifier.widthIn(max = 960.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CenterOnePageHeader(
                                title = MainNavigation.Reception.label(AppLanguage.Arabic),
                                englishTitle = MainNavigation.Reception.label(AppLanguage.English),
                                subtitle = dashboardString(DashboardText.DEMO),
                                contentPadding = PaddingValues(),
                            ) {
                                CenterOneLanguageToggle(state.language, { onEvent(DashboardEvent.LanguageSelected(it)) })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ReceptionKpi(
                                    title = dashboardString(DashboardText.ATTENDANCE),
                                    value = state.attendance,
                                    icon = Icons.AutoMirrored.Outlined.FactCheck,
                                    brush = CenterOneBrushes.Attendance,
                                    modifier = Modifier.weight(1f),
                                )
                                ReceptionKpi(
                                    title = dashboardString(DashboardText.TREASURY),
                                    value = "${state.treasury} ${dashboardString(DashboardText.CURRENCY)}",
                                    icon = Icons.Outlined.AccountBalanceWallet,
                                    brush = CenterOneBrushes.Treasury,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            StudentSearch(state, onEvent)
                            state.student?.let { StudentActionCard(it, state.language, onEvent) }
                            ReceptionShortcuts(onEvent)
                        }
                    }
                    if (state.language == AppLanguage.Arabic) ReceptionSidebar(onEvent, Modifier.width(sidebarWidth))
                }
            }
        }
    }
}

@Composable
private fun ReceptionSidebar(
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier,
) {
    CenterOneMainNavigation(MainNavigation.Reception, { onEvent(DashboardEvent.Navigate(it)) }, modifier)
}

@Composable
private fun ReceptionKpi(
    title: String,
    value: String,
    icon: ImageVector,
    brush: Brush,
    modifier: Modifier,
) {
    CenterOneGradientCard(brush, modifier) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CenterOneAccentIcon(White) { Icon(icon, contentDescription = null) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(value, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
