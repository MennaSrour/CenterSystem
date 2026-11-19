package center.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import center.ui.theme.CardShadow
import center.ui.theme.CenterOneSize

@Composable
fun CenterOneCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val hoverBorder by animateColorAsState(
        if (hovered) lerp(borderColor, MaterialTheme.colorScheme.secondary, 0.22f) else borderColor,
    )
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(1.dp, hoverBorder)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick == null) {
        Card(
            modifier = modifier.cardShadow().hoverable(interactions),
            shape = shape,
            border = border,
            colors = colors,
            content = content,
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.cardShadow(),
            shape = shape,
            border = border,
            colors = colors,
            interactionSource = interactions,
            content = content,
        )
    }
}

@Composable
internal fun Modifier.cardShadow(): Modifier =
    shadow(
        elevation = CenterOneSize.CardElevation,
        shape = MaterialTheme.shapes.large,
        clip = false,
        ambientColor = CardShadow,
        spotColor = CardShadow,
    )
