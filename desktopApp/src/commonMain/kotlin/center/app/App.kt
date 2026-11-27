package center.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.domain.repository.Permission
import center.domain.repository.UserRole
import center.domain.usecase.AuthSession
import center.feature.billing.BillingScreen
import center.feature.dashboard.DashboardEvent
import center.feature.reception.ReceptionScreen
import center.feature.reports.ContextualPdfPlatform
import center.feature.reports.MonthlyReportPlatform
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneMainNavigation
import center.ui.components.HeaderIdentity
import center.ui.components.LocalCenterOneLogout
import center.ui.components.LocalHeaderIdentity
import center.ui.components.MainNavigation
import center.ui.localization.AppLanguage

@Composable
internal fun App(
    root: RootComponent,
    services: DirectoryServices,
    session: AuthSession,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    studentImport: @Composable () -> Unit = {},
    databaseBackup: @Composable () -> Unit = {},
    monthlyReportPlatform: MonthlyReportPlatform? = null,
    printCenter: @Composable () -> Unit,
) {
    val user = session.user.value ?: return
    val state = remember(root) { mutableStateOf(root.stack.value) }
    DisposableEffect(root) {
        val subscription = root.stack.subscribe { state.value = it }
        onDispose { subscription.cancel() }
    }
    val stack by state
    val canDelete = remember(session) { { session.allows(Permission.DELETE_DIRECTORY_RECORDS) } }
    val contextualPdfPlatform = monthlyReportPlatform as? ContextualPdfPlatform
    CompositionLocalProvider(
        LocalCenterOneLogout provides session::logout,
        LocalHeaderIdentity provides HeaderIdentity(user.displayName, user.role.localizedRole()),
    ) {
        val dashboardScope = rememberDirectoryScope(services)
        val dashboard = remember(services, dashboardScope) { DashboardStateHolder(services, dashboardScope, language) }
        LaunchedEffect(language) { dashboard.onEvent(DashboardEvent.LanguageSelected(language)) }
        val route = stack.active.configuration
        key(route) {
            when (route.destination) {
                Destination.Dashboard -> DashboardRoute(root, dashboard, onLanguageSelected)
                Destination.Financials ->
                    FinancialsRoute(
                        root,
                        services,
                        route.shortcut,
                        session.allows(Permission.VIEW_MONTHLY_PROFIT),
                        session.allows(Permission.VOID_PAYMENTS),
                        contextualPdfPlatform,
                        onLanguageSelected,
                    )
                Destination.Students ->
                    StudentsRoute(
                        root,
                        services,
                        route.shortcut == ReceptionShortcut.AddStudent,
                        onLanguageSelected,
                        canDelete,
                        studentImport,
                    )
                Destination.Courses -> CoursesRoute(root, services, canDelete, onLanguageSelected, contextualPdfPlatform)
                Destination.Print -> printCenter()
                Destination.Teachers -> TeachersRoute(root, services, onLanguageSelected, canDelete, contextualPdfPlatform)
                else ->
                    SecondaryRoute(
                        root,
                        route.destination,
                        language,
                        session,
                        services,
                        monthlyReportPlatform,
                        onLanguageSelected,
                        databaseBackup,
                    )
            }
        }
    }
}

@Composable
private fun SecondaryRoute(
    root: RootComponent,
    destination: Destination,
    language: AppLanguage,
    session: AuthSession,
    services: DirectoryServices,
    monthlyReportPlatform: MonthlyReportPlatform?,
    onLanguageSelected: (AppLanguage) -> Unit,
    databaseBackup: @Composable () -> Unit,
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
                    Destination.Reports ->
                        if (session.allows(Permission.VIEW_MONTHLY_PROFIT) && monthlyReportPlatform != null) {
                            ReportsRoute(root, services, monthlyReportPlatform)
                        } else {
                            ReportsAccessDenied(language)
                        }
                    Destination.Settings ->
                        SettingsScreen(session, session.allows(Permission.MANAGE_USERS), onLanguageSelected) {
                            if (session.user.value?.role == UserRole.ADMIN) databaseBackup()
                        }
                    else -> Unit
                }
            }
            if (language == AppLanguage.Arabic) sidebar()
        }
    }
}
