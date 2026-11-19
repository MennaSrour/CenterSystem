package center.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import center.ui.theme.White

@Composable
fun CenterOneGradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(modifier.cardShadow(), shape = MaterialTheme.shapes.large, color = Color.Transparent, contentColor = White) {
        Box(Modifier.background(brush), propagateMinConstraints = true) { content() }
    }
}
