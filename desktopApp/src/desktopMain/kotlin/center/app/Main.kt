package center.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import center.database.CenterDaos
import center.database.CenterDatabase
import center.database.SqlStudentRepository
import center.database.openDatabase
import center.domain.RegisterStudent
import center.domain.StudentRepository
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.nio.file.Path

fun main() {
    val directory = Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "CenterSystem")
    val driver = openDatabase(directory)
    val koin =
        startKoin {
            modules(
                module {
                    single { CenterDatabase(driver) }
                    single { CenterDaos(get(), Dispatchers.IO) }
                    single { get<CenterDaos>().teachers }
                    single { get<CenterDaos>().students }
                    single { get<CenterDaos>().classGroups }
                    single { get<CenterDaos>().attendance }
                    single { get<CenterDaos>().payments }
                    single { get<CenterDaos>().messages }
                    single<StudentRepository> { SqlStudentRepository(get()) }
                    factory { RegisterStudent(get()) }
                },
            )
        }
    try {
        application {
            val lifecycle = remember { LifecycleRegistry() }
            val root = remember { RootComponent(DefaultComponentContext(lifecycle)) }
            DisposableEffect(lifecycle) {
                lifecycle.resume()
                onDispose { lifecycle.destroy() }
            }
            Window(onCloseRequest = ::exitApplication, title = "CenterSystem") { App(root) }
        }
    } finally {
        koin.close()
        driver.close()
    }
}
