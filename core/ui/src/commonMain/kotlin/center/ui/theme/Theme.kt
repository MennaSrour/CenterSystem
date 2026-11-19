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

object CenterOneSize {
    val ButtonHeight = 44.dp
    val CardRadius = 16.dp
    val CardElevation = 16.dp
}

@Composable
fun CenterOneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Gold,
                onPrimary = Navy,
                primaryContainer = PendingContainer,
                onPrimaryContainer = Navy,
                secondary = Navy,
                onSecondary = White,
                secondaryContainer = Indigo.copy(alpha = ACCENT_TINT_ALPHA),
                onSecondaryContainer = Navy,
                tertiary = Pink,
                tertiaryContainer = RoseContainer,
                onTertiaryContainer = Navy,
                background = OffWhite,
                onBackground = Navy,
                surface = WarmSurface,
                surfaceVariant = Sand,
                surfaceContainer = OffWhite,
                surfaceContainerHigh = Sand,
                surfaceContainerHighest = Sand,
                surfaceTint = Navy,
                onSurface = Navy,
                onSurfaceVariant = Muted,
                outline = Outline,
                error = Rose,
                onError = White,
                errorContainer = RoseContainer,
                onErrorContainer = Rose.accentContentColor(),
            ),
        typography = centerOneTypography(),
        shapes =
            Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(CenterOneSize.CardRadius),
                large = RoundedCornerShape(CenterOneSize.CardRadius),
            ),
        content = content,
    )
}
