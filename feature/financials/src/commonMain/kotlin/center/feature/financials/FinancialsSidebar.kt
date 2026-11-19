package center.feature.financials

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import center.ui.components.CenterOneMainNavigation
import center.ui.components.MainNavigation

@Composable
internal fun FinancialsSidebar(
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneMainNavigation(
        selected = MainNavigation.Treasury,
        onNavigate = { destination ->
            onEvent(
                FinancialsEvent.Navigate(
                    when (destination) {
                        MainNavigation.Reception -> FinancialsDestination.Home
                        MainNavigation.Students -> FinancialsDestination.Students
                        MainNavigation.Teachers -> FinancialsDestination.Teachers
                        MainNavigation.Courses -> FinancialsDestination.Courses
                        MainNavigation.Treasury -> FinancialsDestination.Financials
                        MainNavigation.Settings -> FinancialsDestination.Settings
                    },
                ),
            )
        },
        modifier = modifier,
    )
}
