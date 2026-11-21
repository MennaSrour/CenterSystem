package center.feature.students

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import center.ui.components.CenterOneMainNavigation
import center.ui.components.MainNavigation

@Composable
internal fun StudentsSidebar(
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneMainNavigation(
        selected = MainNavigation.Students,
        onNavigate = { destination ->
            onEvent(
                StudentsEvent.Navigate(
                    when (destination) {
                        MainNavigation.Reception -> StudentsDestination.Home
                        MainNavigation.Students -> StudentsDestination.Students
                        MainNavigation.Teachers -> StudentsDestination.Teachers
                        MainNavigation.Courses -> StudentsDestination.Courses
                        MainNavigation.Treasury -> StudentsDestination.Financials
                        MainNavigation.Print -> StudentsDestination.Print
                        MainNavigation.Settings -> StudentsDestination.Settings
                    },
                ),
            )
        },
        modifier = modifier,
    )
}
