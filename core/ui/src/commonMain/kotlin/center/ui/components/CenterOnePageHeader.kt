package center.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.theme.CenterOneSpacing

data class HeaderIdentity(
    val displayName: String,
    val role: LocalizedText,
)

val LocalHeaderIdentity = staticCompositionLocalOf<HeaderIdentity?> { null }

@Composable
fun CenterOnePageHeader(
    title: String,
    englishTitle: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(CenterOneSpacing.Large),
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CenterOneSpacing.Small),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (LocalAppLanguage.current == AppLanguage.Arabic) title else englishTitle,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LocalHeaderIdentity.current?.let { identity ->
            Column(Modifier.weight(0.55f, fill = false), horizontalAlignment = Alignment.End) {
                Text(identity.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(identity.role.resolve(LocalAppLanguage.current), style = MaterialTheme.typography.labelSmall)
            }
        }
        actions()
    }
}
