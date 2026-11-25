package center.feature.students

import androidx.compose.runtime.Composable
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage

@Composable
internal fun appString(
    key: String,
    vararg arguments: Any,
): String = AppStrings.getResource("students", key, LocalAppLanguage.current, *arguments)
