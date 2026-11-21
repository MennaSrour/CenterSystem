package center.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.theme.ACCENT_TINT_ALPHA
import center.ui.theme.CenterOneSize
import center.ui.theme.SidebarInk
import center.ui.theme.White

enum class MainNavigation(
    private val label: LocalizedText,
    internal val icon: ImageVector,
) {
    Reception(LocalizedText("الاستقبال", "Reception"), Icons.Outlined.Home),
    Students(LocalizedText("الطلاب", "Students"), Icons.Outlined.PeopleOutline),
    Teachers(LocalizedText("المدرسين", "Teachers"), Icons.Outlined.School),
    Courses(LocalizedText("المجموعات", "Courses"), Icons.AutoMirrored.Outlined.MenuBook),
    Treasury(LocalizedText("الخزنة", "Treasury"), Icons.Outlined.AccountBalanceWallet),
    Print(LocalizedText("الطباعة", "Print"), Icons.Outlined.Print),
    Settings(LocalizedText("الإعدادات", "Settings"), Icons.Outlined.Settings),
    ;

    fun label(language: AppLanguage): String = label.resolve(language)
}

val LocalCenterOneLogout = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun CenterOneMainNavigation(
    selected: MainNavigation?,
    onNavigate: (MainNavigation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val logout = LocalCenterOneLogout.current
    CenterOneLocalization(language) {
        CenterOneSidebar(
            modifier = modifier,
            header = {
                CenterOneBrand("سنتر ون", "Center One")
                HorizontalDivider(color = SidebarInk.copy(alpha = ACCENT_TINT_ALPHA))
            },
            footer = {
                Row {
                    IconButton(onClick = { onNavigate(MainNavigation.Settings) }, modifier = Modifier.size(CenterOneSize.ButtonHeight)) {
                        Icon(
                            MainNavigation.Settings.icon,
                            contentDescription = MainNavigation.Settings.label(language),
                            modifier = Modifier.size(20.dp),
                            tint = White,
                        )
                    }
                    if (logout != null) {
                        IconButton(onClick = logout, modifier = Modifier.size(CenterOneSize.ButtonHeight)) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = LocalizedText("تسجيل الخروج", "Log out").resolve(language),
                                modifier = Modifier.size(20.dp),
                                tint = White,
                            )
                        }
                    }
                }
            },
        ) {
            MainNavigation.entries.forEach { destination ->
                if (destination != MainNavigation.Settings) {
                    CenterOneNavigationItem(
                        title = destination.label(AppLanguage.Arabic),
                        subtitle = destination.label(AppLanguage.English),
                        selected = destination == selected,
                        onClick = { onNavigate(destination) },
                        icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                        contentColor = SidebarInk,
                    )
                }
            }
        }
    }
}
