package center.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object CenterOneSpacing {
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val Section = 32.dp
}

@Composable
fun CenterOneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Gold,
                onPrimary = Navy,
                secondary = Navy,
                onSecondary = White,
                tertiary = Pink,
                background = OffWhite,
                onBackground = Navy,
                surface = White,
                onSurface = Navy,
                onSurfaceVariant = Muted,
                outline = Outline,
                error = Pink,
            ),
        typography = centerOneTypography(),
        shapes =
            Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(20.dp),
            ),
        content = content,
    )
}
