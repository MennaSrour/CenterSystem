package center.ui.theme

import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0B192C)
internal val SidebarNavy = Color(0xFF13294B)
val Gold = Color(0xFFD4AF37)
internal val GoldLight = Color(0xFFE5C158)
internal val GoldDeep = Color(0xFFC59B27)
val OffWhite = Color(0xFFF1F5F9)
val Rose = Color(0xFFE11D48)
val Pink = Rose
val White = Color(0xFFFFFFFF)
val Muted = Color(0xFF6B778C)
val Outline = Color(0xFFE2E8F0)
val Success = Color(0xFF059669)
val Pending = Color(0xFFD97706)

internal const val ACCENT_TINT_ALPHA = 0.12f
val SuccessContainer = Success.copy(alpha = ACCENT_TINT_ALPHA)
val PendingContainer = Pending.copy(alpha = ACCENT_TINT_ALPHA)
internal val RoseContainer = Rose.copy(alpha = ACCENT_TINT_ALPHA)

internal fun Color.accentContentColor(): Color =
    when (this) {
        Success -> Color(0xFF065F46)
        Pending -> Color(0xFF92400E)
        Rose -> Color(0xFF9F1239)
        else -> this
    }
