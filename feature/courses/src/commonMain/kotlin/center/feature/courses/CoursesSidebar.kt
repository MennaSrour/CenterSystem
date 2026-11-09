package center.feature.courses

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
import center.feature.courses.resources.Res
import center.feature.courses.resources.branch
import center.feature.courses.resources.brand
import center.feature.courses.resources.brand_en
import center.feature.courses.resources.courses
import center.feature.courses.resources.courses_en
import center.feature.courses.resources.financials
import center.feature.courses.resources.financials_en
import center.feature.courses.resources.home
import center.feature.courses.resources.home_en
import center.feature.courses.resources.logout
import center.feature.courses.resources.menu
import center.feature.courses.resources.reports
import center.feature.courses.resources.reports_en
import center.feature.courses.resources.schedule_en
import center.feature.courses.resources.schedule_nav
import center.feature.courses.resources.settings
import center.feature.courses.resources.settings_en
import center.feature.courses.resources.students
import center.feature.courses.resources.students_en
import center.ui.components.CenterOneBrand
import center.ui.components.CenterOneNavigationItem
import center.ui.components.CenterOneSidebar
import center.ui.theme.White
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class CoursesNavigationItem(
    val destination: CoursesDestination,
    val title: StringResource,
    val subtitle: StringResource,
    val icon: ImageVector,
)

@Composable
internal fun CoursesSidebar(
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items =
        listOf(
            CoursesNavigationItem(
                CoursesDestination.Home,
                Res.string.home,
                Res.string.home_en,
                Icons.Outlined.Home,
            ),
            CoursesNavigationItem(
                CoursesDestination.Students,
                Res.string.students,
                Res.string.students_en,
                Icons.Outlined.PeopleOutline,
            ),
            CoursesNavigationItem(
                CoursesDestination.Courses,
                Res.string.courses,
                Res.string.courses_en,
                Icons.AutoMirrored.Outlined.MenuBook,
            ),
            CoursesNavigationItem(
                CoursesDestination.Schedule,
                Res.string.schedule_nav,
                Res.string.schedule_en,
                Icons.Outlined.CalendarToday,
            ),
            CoursesNavigationItem(
                CoursesDestination.Financials,
                Res.string.financials,
                Res.string.financials_en,
                Icons.Outlined.AccountBalanceWallet,
            ),
            CoursesNavigationItem(
                CoursesDestination.Reports,
                Res.string.reports,
                Res.string.reports_en,
                Icons.Outlined.Insights,
            ),
            CoursesNavigationItem(
                CoursesDestination.Settings,
                Res.string.settings,
                Res.string.settings_en,
                Icons.Outlined.Settings,
            ),
        )
    CenterOneSidebar(
        modifier = modifier,
        header = {
            CenterOneBrand(stringResource(Res.string.brand), stringResource(Res.string.brand_en))
            HorizontalDivider(color = White.copy(alpha = 0.12f))
            Text(stringResource(Res.string.menu), style = MaterialTheme.typography.labelSmall)
        },
        footer = {
            TextButton(onClick = { onEvent(CoursesEvent.LogOut) }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = White)
                Spacer(Modifier.size(12.dp))
                Text(stringResource(Res.string.logout), color = White)
            }
            Text(stringResource(Res.string.branch), style = MaterialTheme.typography.titleSmall)
        },
    ) {
        items.forEach { item ->
            CenterOneNavigationItem(
                title = stringResource(item.title),
                subtitle = stringResource(item.subtitle),
                selected = item.destination == CoursesDestination.Courses,
                onClick = { onEvent(CoursesEvent.Navigate(item.destination)) },
                icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                contentColor = White,
            )
        }
    }
}
