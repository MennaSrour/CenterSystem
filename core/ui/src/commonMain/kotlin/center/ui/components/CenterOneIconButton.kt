package center.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import center.ui.theme.ACCENT_TINT_ALPHA

@Composable
fun CenterOneIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactions,
        shape = MaterialTheme.shapes.small,
        colors =
            IconButtonDefaults.outlinedIconButtonColors(
                containerColor = if (hovered) color.copy(alpha = ACCENT_TINT_ALPHA) else Color.Transparent,
                contentColor = color,
            ),
    ) {
        Icon(icon, contentDescription = description)
    }
}
