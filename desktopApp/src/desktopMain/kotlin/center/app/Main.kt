package center.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import center.database.openDatabase
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.nio.file.Path

fun main() {
    val directory = Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "CenterSystem")
    openDatabase(directory).use { driver ->
        val koin = startKoin { modules(persistenceModule(driver)) }.koin
        try {
            val services = koin.get<DirectoryServices>()
            application {
                val lifecycle = remember { LifecycleRegistry() }
                val root = remember { RootComponent(DefaultComponentContext(lifecycle)) }
                DisposableEffect(lifecycle) {
                    lifecycle.resume()
                    onDispose { lifecycle.destroy() }
                }
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "Center One",
                    state = rememberWindowState(width = 1440.dp, height = 960.dp),
                ) {
                    App(root, services)
                }
            }
        } finally {
            stopKoin()
        }
    }
}
