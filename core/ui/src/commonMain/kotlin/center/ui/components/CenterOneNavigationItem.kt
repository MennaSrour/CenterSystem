package center.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { this.selected = selected },
        interactionSource = interactions,
        shape = MaterialTheme.shapes.small,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else contentColor,
                containerColor =
                    when {
                        selected && hovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                        selected -> MaterialTheme.colorScheme.primary
                        hovered -> contentColor.copy(alpha = 0.12f)
                        else -> Color.Transparent
                    },
            ),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
}
