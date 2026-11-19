package center.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.theme.ACCENT_TINT_ALPHA
import center.ui.theme.CenterOneBrushes
import center.ui.theme.CenterOneSize

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
    val glow =
        if (selected) {
            val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = if (hovered) 0.70f else 0.45f)
            Modifier.shadow(8.dp, CircleShape, clip = false, ambientColor = glowColor, spotColor = glowColor)
        } else {
            Modifier
        }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().then(glow).semantics { this.selected = selected },
        interactionSource = interactions,
        shape = CircleShape,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else contentColor,
        color = if (!selected && hovered) contentColor.copy(alpha = ACCENT_TINT_ALPHA) else Color.Transparent,
    ) {
        Row(
            modifier =
                (if (selected) Modifier.background(CenterOneBrushes.ActionGold) else Modifier)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = CenterOneSize.ButtonHeight)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
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
}
