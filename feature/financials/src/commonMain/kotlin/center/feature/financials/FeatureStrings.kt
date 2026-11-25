package center.feature.financials

import androidx.compose.runtime.Composable
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage

@Composable
internal fun appString(
    key: String,
    vararg arguments: Any,
): String = AppStrings.getResource("financials", key, LocalAppLanguage.current, *arguments)
