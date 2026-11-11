package center.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage

/** Optional branding painter; the original artwork is fitted without cropping or regeneration. */
val LocalCenterOneLogo = staticCompositionLocalOf<Painter?> { null }

@Composable
fun CenterOneBrand(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    logo: Painter? = LocalCenterOneLogo.current,
) {
    val name = if (LocalAppLanguage.current == AppLanguage.Arabic) title else subtitle.ifBlank { title }
    Row(
        modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (logo != null) {
            Image(painter = logo, contentDescription = name, modifier = Modifier.size(88.dp), contentScale = ContentScale.Fit)
        }
        Text(name, style = MaterialTheme.typography.titleLarge)
    }
}
