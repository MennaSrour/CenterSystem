package center.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage

/** The app supplies its bundled logo through this local. */
val LocalCenterOneLogo = staticCompositionLocalOf<Painter?> { null }

@Composable
fun CenterOneBrand(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    logo: Painter? = LocalCenterOneLogo.current,
) {
    val name = if (LocalAppLanguage.current == AppLanguage.Arabic) title else subtitle.ifBlank { title }
    Box(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = if (logo == null) Alignment.CenterStart else Alignment.Center,
    ) {
        if (logo == null) {
            Text(name, style = MaterialTheme.typography.titleLarge)
        } else {
            Image(
                painter = logo,
                contentDescription = name,
                modifier = Modifier.size(104.dp).clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
