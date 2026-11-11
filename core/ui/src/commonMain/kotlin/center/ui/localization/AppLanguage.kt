package center.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val layoutDirection: LayoutDirection,
) {
    Arabic(LayoutDirection.Rtl),
    English(LayoutDirection.Ltr),
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.Arabic }

@Composable
fun CenterOneLocalization(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppLanguage provides language, LocalLayoutDirection provides language.layoutDirection, content = content)
}
