package center.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import center.ui.theme.CenterOneBrushes
import center.ui.theme.CenterOneSize
import center.ui.theme.TealDeep
import center.ui.theme.White

enum class CenterOneButtonTone { Primary, Success }

@Composable
fun CenterOnePrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: CenterOneButtonTone = CenterOneButtonTone.Primary,
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val elevation by animateDpAsState(if (enabled && hovered) 3.dp else 0.dp)
    val primary = tone == CenterOneButtonTone.Primary
    val colors = MaterialTheme.colorScheme
    val container = if (primary) colors.primary else TealDeep
    val ink = if (primary) colors.onPrimary else White

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = if (enabled && primary) Color.Transparent else container.copy(alpha = if (enabled) 1f else 0.35f),
        contentColor = ink.copy(alpha = if (enabled) 1f else 0.65f),
        shadowElevation = elevation,
        interactionSource = interactions,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                modifier =
                    (if (enabled && primary) Modifier.background(CenterOneBrushes.ActionGold) else Modifier)
                        .defaultMinSize(minWidth = ButtonDefaults.MinWidth, minHeight = CenterOneSize.ButtonHeight)
                        .padding(ButtonDefaults.ContentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}
