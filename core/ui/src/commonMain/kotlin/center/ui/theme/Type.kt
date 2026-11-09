package center.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import center.ui.resources.Res
import center.ui.resources.almarai_bold
import center.ui.resources.almarai_regular
import org.jetbrains.compose.resources.Font

@Composable
internal fun centerOneTypography(): Typography {
    val family =
        FontFamily(
            Font(Res.font.almarai_regular, FontWeight.Normal),
            Font(Res.font.almarai_bold, FontWeight.Bold),
        )

    fun style(
        size: Int,
        lineHeight: Int,
        weight: FontWeight = FontWeight.Normal,
    ) = TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = lineHeight.sp, fontWeight = weight)
    return Typography(
        displayLarge = style(40, 54, FontWeight.Bold),
        displayMedium = style(36, 48, FontWeight.Bold),
        displaySmall = style(32, 44, FontWeight.Bold),
        headlineLarge = style(30, 42, FontWeight.Bold),
        headlineMedium = style(26, 38, FontWeight.Bold),
        headlineSmall = style(24, 34, FontWeight.Bold),
        titleLarge = style(20, 30, FontWeight.Bold),
        titleMedium = style(16, 26, FontWeight.Bold),
        titleSmall = style(14, 24, FontWeight.Bold),
        bodyLarge = style(16, 26),
        bodyMedium = style(14, 24),
        bodySmall = style(12, 20),
        labelLarge = style(14, 24, FontWeight.Bold),
        labelMedium = style(12, 20, FontWeight.Bold),
        labelSmall = style(10, 18),
    )
}
