package center.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import center.ui.localization.AppLanguage

/** Language names remain recognizable in either locale; selection belongs to the caller. */
@Composable
fun CenterOneLanguageToggle(
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneChoiceGroup(listOf("عربي", "English"), AppLanguage.entries.indexOf(language), {
        onLanguageSelected(AppLanguage.entries[it])
    }, modifier)
}
