package center.feature.courses

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import center.ui.components.CenterOneMainNavigation
import center.ui.components.MainNavigation

@Composable
internal fun CoursesSidebar(
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneMainNavigation(
        selected = MainNavigation.Courses,
        onNavigate = { destination ->
            onEvent(
                CoursesEvent.Navigate(
                    when (destination) {
                        MainNavigation.Reception -> CoursesDestination.Home
                        MainNavigation.Students -> CoursesDestination.Students
                        MainNavigation.Teachers -> CoursesDestination.Teachers
                        MainNavigation.Courses -> CoursesDestination.Courses
                        MainNavigation.Treasury -> CoursesDestination.Financials
                        MainNavigation.Settings -> CoursesDestination.Settings
                    },
                ),
            )
        },
        modifier = modifier,
    )
}
