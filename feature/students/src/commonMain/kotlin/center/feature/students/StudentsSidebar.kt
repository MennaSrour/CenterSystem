package center.feature.students

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
import center.feature.students.resources.Res
import center.feature.students.resources.branch
import center.feature.students.resources.brand
import center.feature.students.resources.brand_en
import center.feature.students.resources.courses
import center.feature.students.resources.courses_en
import center.feature.students.resources.financials
import center.feature.students.resources.financials_en
import center.feature.students.resources.home
import center.feature.students.resources.home_en
import center.feature.students.resources.logout
import center.feature.students.resources.menu
import center.feature.students.resources.reports
import center.feature.students.resources.reports_en
import center.feature.students.resources.schedule
import center.feature.students.resources.schedule_en
import center.feature.students.resources.settings
import center.feature.students.resources.settings_en
import center.feature.students.resources.students
import center.feature.students.resources.students_en
import center.ui.components.CenterOneBrand
import center.ui.components.CenterOneNavigationItem
import center.ui.components.CenterOneSidebar
import center.ui.theme.White
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class StudentsNavigationItem(
    val destination: StudentsDestination,
    val title: StringResource,
    val subtitle: StringResource,
    val icon: ImageVector,
)

@Composable
internal fun StudentsSidebar(
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items =
        listOf(
            StudentsNavigationItem(
                StudentsDestination.Home,
                Res.string.home,
                Res.string.home_en,
                Icons.Outlined.Home,
            ),
            StudentsNavigationItem(
                StudentsDestination.Students,
                Res.string.students,
                Res.string.students_en,
                Icons.Outlined.PeopleOutline,
            ),
            StudentsNavigationItem(
                StudentsDestination.Courses,
                Res.string.courses,
                Res.string.courses_en,
                Icons.AutoMirrored.Outlined.MenuBook,
            ),
            StudentsNavigationItem(
                StudentsDestination.Schedule,
                Res.string.schedule,
                Res.string.schedule_en,
                Icons.Outlined.CalendarToday,
            ),
            StudentsNavigationItem(
                StudentsDestination.Financials,
                Res.string.financials,
                Res.string.financials_en,
                Icons.Outlined.AccountBalanceWallet,
            ),
            StudentsNavigationItem(
                StudentsDestination.Reports,
                Res.string.reports,
                Res.string.reports_en,
                Icons.Outlined.Insights,
            ),
            StudentsNavigationItem(
                StudentsDestination.Settings,
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
            TextButton(onClick = { onEvent(StudentsEvent.LogOut) }) {
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
                selected = item.destination == StudentsDestination.Students,
                onClick = { onEvent(StudentsEvent.Navigate(item.destination)) },
                icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                contentColor = White,
            )
        }
    }
}
