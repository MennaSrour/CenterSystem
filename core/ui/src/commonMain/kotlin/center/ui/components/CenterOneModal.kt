package center.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import center.ui.theme.CenterOneSpacing

/** The screen supplies its scrim; the native modal owns focus, Escape and outside dismissal. */
@Composable
fun CenterOneModal(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}

/** Separate content enables previews and headless rendering without a native dialog window. */
@Composable
fun CenterOneModalContent(
    title: String,
    subtitle: String,
    closeLabel: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.widthIn(max = 600.dp).heightIn(max = 840.dp), shape = MaterialTheme.shapes.large) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Surface(color = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary) {
                Row(Modifier.fillMaxWidth().padding(CenterOneSpacing.Large), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onClose, enabled = enabled) { Text(closeLabel, color = MaterialTheme.colorScheme.onSecondary) }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(CenterOneSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium),
                content = content,
            )
        }
    }
}
