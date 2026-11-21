package center.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import center.ui.components.CenterOnePrimaryButton
import center.ui.localization.LocalAppLanguage
import center.ui.theme.CenterOneBrushes
import center.ui.theme.GoldLight
import center.ui.theme.Navy
import center.ui.theme.Rose
import center.ui.theme.White

@Composable
internal fun FrameWindowScope.WindowTitleBar(
    state: WindowState,
    onClose: () -> Unit,
) {
    val maximized = state.placement == WindowPlacement.Maximized
    Row(Modifier.fillMaxWidth().height(44.dp).background(Navy), verticalAlignment = Alignment.CenterVertically) {
        WindowDraggableArea(Modifier.weight(1f).fillMaxHeight()) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                BrandLogo(Modifier.size(28.dp))
                Text(brandTitle.resolve(LocalAppLanguage.current), color = White, style = MaterialTheme.typography.titleSmall)
            }
        }
        WindowControl(Icons.Outlined.Remove, authText("تصغير", "Minimize")) { state.isMinimized = true }
        WindowControl(
            if (maximized) Icons.Outlined.FilterNone else Icons.Outlined.CropSquare,
            if (maximized) authText("استعادة الحجم", "Restore") else authText("تكبير", "Maximize"),
        ) {
            state.placement = if (maximized) WindowPlacement.Floating else WindowPlacement.Maximized
        }
        WindowControl(Icons.Outlined.Close, authText("إغلاق", "Close"), Rose.copy(alpha = 0.48f), onClose)
    }
}

@Composable
private fun WindowControl(
    icon: ImageVector,
    label: String,
    hoverColor: Color = White.copy(alpha = 0.13f),
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    IconButton(
        onClick = onClick,
        modifier = Modifier.width(48.dp).fillMaxHeight().background(if (hovered) hoverColor else Color.Transparent),
        interactionSource = interaction,
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = White)
    }
}

@Composable
internal fun StartupSplash(
    failed: Boolean,
    onRetry: () -> Unit,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(700)) }
    Box(Modifier.fillMaxSize().background(CenterOneBrushes.Sidebar), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            BrandLogo(
                Modifier.size(138.dp).graphicsLayer {
                    alpha = reveal.value
                    scaleX = 0.92f + reveal.value * 0.08f
                    scaleY = scaleX
                },
            )
            Text(brandTitle.resolve(LocalAppLanguage.current), color = GoldLight, style = MaterialTheme.typography.headlineLarge)
            if (failed) {
                Text(authText("تعذر فتح قاعدة البيانات.", "Could not open the database."), color = White)
                CenterOnePrimaryButton(onClick = onRetry) { Text(authText("إعادة المحاولة", "Retry")) }
            } else {
                CircularProgressIndicator(Modifier.size(26.dp), color = GoldLight, strokeWidth = 2.dp)
            }
        }
    }
}
