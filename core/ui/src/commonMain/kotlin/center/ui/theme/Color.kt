package center.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val Navy = Color(0xFF172554)
internal val RoyalBlue = Color(0xFF1E3A8A)
internal val IndigoDeep = Color(0xFF3730A3)
val Indigo = Color(0xFF4F46E5)
val Gold = Color(0xFFD97706)
val GoldLight = Color(0xFFF59E0B)
val OffWhite = Color(0xFFF6F3EC)
internal val Sand = Color(0xFFEDE8DC)
val WarmSurface = Color(0xFFFFFDF9)
val Rose = Color(0xFFE11D48)
val Pink = Rose
val White = Color(0xFFFFFFFF)
internal val SidebarInk = Color(0xFFE0EBFF)
val Muted = Color(0xFF625C56)
val Outline = Color(0xFFE5DFD3)
val Success = Color(0xFF0D9488)
internal val TealDeep = Color(0xFF0F766E)
val Pending = Gold
val PendingOutline = Pending.copy(alpha = 0.35f)

const val ACCENT_TINT_ALPHA = 0.15f
val SuccessContainer = Success.copy(alpha = ACCENT_TINT_ALPHA)
val PendingContainer = Pending.copy(alpha = ACCENT_TINT_ALPHA)
internal val RoseContainer = Rose.copy(alpha = ACCENT_TINT_ALPHA)
internal val CardShadow = Color(0xFF685235).copy(alpha = 0.26f)
val ModalScrim = Navy.copy(alpha = 0.55f)

object CenterOneBrushes {
    val Canvas = Brush.verticalGradient(listOf(OffWhite, Sand))
    val Sidebar = Brush.verticalGradient(listOf(Navy, RoyalBlue))
    val ActionGold = Brush.verticalGradient(listOf(GoldLight, Gold))
    val Treasury = Brush.verticalGradient(listOf(RoyalBlue, IndigoDeep))
    val Attendance = Brush.verticalGradient(0f to TealDeep, 0.65f to lerp(TealDeep, Success, 0.28f), 1f to Success)
}

fun Color.accentContentColor(): Color =
    when (this) {
        Success -> Color(0xFF115E59)
        Gold -> Color(0xFF92400E)
        Rose -> Color(0xFF9F1239)
        Indigo -> IndigoDeep
        else -> this
    }
