package center.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage

@Composable
fun CenterOneNavigationItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { this.selected = selected },
        shape = MaterialTheme.shapes.small,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else contentColor,
                containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            ),
    ) {
        icon()
        Spacer(Modifier.size(12.dp))
        Text(
            if (LocalAppLanguage.current ==
                AppLanguage.Arabic
            ) {
                title
            } else {
                subtitle.ifBlank { title }
            },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
