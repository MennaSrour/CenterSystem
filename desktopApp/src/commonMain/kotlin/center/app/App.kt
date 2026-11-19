package center.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.app.resources.Res
import center.app.resources.center_one_logo
import center.feature.billing.BillingScreen
import center.feature.dashboard.DashboardText
import center.feature.reception.ReceptionScreen
import center.feature.reports.ReportsScreen
import center.feature.teachers.TeachersPreviewData
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneMainNavigation
import center.ui.components.LocalCenterOneLogo
import center.ui.components.MainNavigation
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.theme.CenterOneTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun App(root: RootComponent) {
    val state = remember(root) { mutableStateOf(root.stack.value) }
    DisposableEffect(root) {
        val subscription = root.stack.subscribe { state.value = it }
        onDispose { subscription.cancel() }
    }
    val stack by state
    var language by remember { mutableStateOf(AppLanguage.Arabic) }
    var teachers by remember(root) { mutableStateOf(TeachersPreviewData.state()) }
    CompositionLocalProvider(LocalCenterOneLogo provides painterResource(Res.drawable.center_one_logo)) {
        CenterOneLocalization(language) {
            CenterOneTheme {
                val route = stack.active.configuration
                key(route) {
                    when (route.destination) {
                        Destination.Dashboard -> DashboardRoute(root, language, onLanguageSelected = { language = it })
                        Destination.Financials -> FinancialsRoute(root, route.shortcut)
                        Destination.Students ->
                            StudentsRoute(
                                root,
                                openAddStudent = route.shortcut == ReceptionShortcut.AddStudent,
                                onLanguageSelected = { language = it },
                            )
                        Destination.Courses -> CoursesRoute(root)
                        Destination.Teachers ->
                            TeachersRoute(
                                root,
                                teachers,
                                onStateChange = { update -> teachers = update(teachers) },
                                onLanguageSelected = { language = it },
                            )
                        else -> SecondaryRoute(root, route.destination, language)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryRoute(
    root: RootComponent,
    destination: Destination,
    language: AppLanguage,
) {
    val selected =
        when (destination) {
            Destination.Reception -> MainNavigation.Reception
            Destination.Settings -> MainNavigation.Settings
            else -> null
        }
    val sidebar: @Composable () -> Unit = { CenterOneMainNavigation(selected, root::navigate, Modifier.width(232.dp)) }
    CenterOneCanvas {
        Row(Modifier.fillMaxSize()) {
            if (language == AppLanguage.English) sidebar()
            Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (destination) {
                    Destination.Reception -> ReceptionScreen()
                    Destination.Billing -> BillingScreen()
                    Destination.Reports -> ReportsScreen()
                    Destination.Settings -> {
                        Text(requireNotNull(selected).label(language), style = MaterialTheme.typography.headlineLarge)
                        Text(DashboardText.FEATURE_UNAVAILABLE.text(language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> Unit
                }
            }
            if (language == AppLanguage.Arabic) sidebar()
        }
    }
}
