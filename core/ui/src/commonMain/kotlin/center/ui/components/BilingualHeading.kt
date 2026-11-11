package center.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage

/** Compatibility API: display one language, never both side by side. */
@Composable
fun BilingualHeading(
    arabic: String,
    english: String,
    modifier: Modifier = Modifier,
) {
    Text(if (LocalAppLanguage.current == AppLanguage.Arabic) arabic else english, modifier, style = MaterialTheme.typography.titleLarge)
}
