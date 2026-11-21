package center.feature.print

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import center.ui.components.MainNavigation
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import java.net.URI
import java.nio.file.Path

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PrintCenter(
    onNavigate: (MainNavigation) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    loadLogo: suspend () -> ByteArray,
) {
    val scope = rememberCoroutineScope()
    val currentLogo by rememberUpdatedState(loadLogo)
    val holder =
        remember(scope) {
            val desktop = DesktopPrintFiles()
            PrintStateHolder(scope, desktop, PrintDocuments(desktop) { currentLogo() })
        }
    val state by holder.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val language by rememberUpdatedState(LocalAppLanguage.current)
    LaunchedEffect(holder) {
        holder.messages.collect { snackbar.showSnackbar(it.resolve(language)) }
    }

    val target =
        remember(holder) {
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val files = (event.dragData() as? DragData.FilesList)?.readFiles() ?: return false
                    val path =
                        files.singleOrNull()?.let { file ->
                            val fileUri = file.startsWith("file:", ignoreCase = true)
                            runCatching { if (fileUri) Path.of(URI(file)) else Path.of(file) }.getOrNull()
                        }
                    holder.drop(path)
                    return path != null
                }
            }
        }
    PrintScreen(
        state,
        { holder.onEvent(it, language) },
        onNavigate,
        onLanguageSelected,
        Modifier.dragAndDropTarget(shouldStartDragAndDrop = { it.dragData() is DragData.FilesList }, target = target),
    ) { SnackbarHost(snackbar) }
}
