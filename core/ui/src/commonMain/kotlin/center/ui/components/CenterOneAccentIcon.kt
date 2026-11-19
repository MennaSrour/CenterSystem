package center.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import center.ui.theme.ACCENT_TINT_ALPHA
import center.ui.theme.accentContentColor

@Composable
fun CenterOneAccentIcon(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(Modifier.size(44.dp).then(modifier).background(accent.copy(alpha = ACCENT_TINT_ALPHA), CircleShape), Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides accent.accentContentColor(), content = content)
    }
}
