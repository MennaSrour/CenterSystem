package center.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import center.ui.theme.ACCENT_TINT_ALPHA
import center.ui.theme.accentContentColor

@Composable
fun CenterOneOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.tertiary,
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactions,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, color.copy(alpha = if (enabled) 1f else 0.4f)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = if (hovered) color.copy(alpha = ACCENT_TINT_ALPHA) else Color.Transparent,
                contentColor = color.accentContentColor(),
            ),
        content = content,
    )
}
