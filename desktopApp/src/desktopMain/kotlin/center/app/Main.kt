package center.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import center.app.resources.Res
import center.app.resources.center_one_logo
import center.database.DatabaseBackup
import center.database.SqlAuthRepository
import center.database.SqlStudentRepository
import center.database.lockDatabaseDirectory
import center.database.openDatabase
import center.domain.usecase.AuthSession
import center.feature.print.PrintCenter
import center.ui.components.LocalCenterOneLogo
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.theme.CenterOneTheme
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.dsl.koinApplication
import java.nio.file.Path

fun main() =
    application {
        val directory = remember { Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "CenterSystem") }
        var language by remember { mutableStateOf(AppLanguage.Arabic) }
        val logo = painterResource(Res.drawable.center_one_logo)
        val windowState = rememberWindowState(width = 1440.dp, height = 960.dp)
        Window(
            onCloseRequest = ::exitApplication,
            title = brandTitle.resolve(language),
            icon = logo,
            state = windowState,
            undecorated = true,
        ) {
            CompositionLocalProvider(LocalCenterOneLogo provides logo) {
                CenterOneLocalization(language) {
                    CenterOneTheme {
                        Column(Modifier.fillMaxSize()) {
                            WindowTitleBar(windowState, ::exitApplication)
                            Startup(directory, language, onLanguageSelected = { language = it }, onClose = ::exitApplication)
                        }
                    }
                }
            }
        }
    }

private data class StartupData(
    val services: DirectoryServices,
    val auth: AuthSession,
    val students: SqlStudentRepository,
)

@Composable
private fun Startup(
    directory: Path,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onClose: () -> Unit,
) {
    val backup = remember(directory) { DatabaseBackup(directory) }
    var data by remember(directory) { mutableStateOf<StartupData?>(null) }
    var failed by remember(directory) { mutableStateOf(false) }
    var attempt by remember(directory) { mutableStateOf(0) }
    LaunchedEffect(directory, attempt) {
        // Assign owned resources on the IO thread so cleanup also covers cancellation during startup.
        var directoryLock: AutoCloseable? = null
        var database: AutoCloseable? = null
        var dependencies: org.koin.core.KoinApplication? = null
        try {
            val started = System.nanoTime()
            val result =
                directoryResult {
                    withContext(Dispatchers.IO) {
                        directoryLock = lockDatabaseDirectory(directory)
                        backup.applyPendingRestore()
                        val driver = openDatabase(directory).also { database = it }
                        val app = koinApplication { modules(persistenceModule(driver)) }.also { dependencies = it }
                        app.koin.get<SqlAuthRepository>().seedIfEmpty()
                        StartupData(app.koin.get(), app.koin.get(), app.koin.get())
                    }
                }
            delay((1_500 - (System.nanoTime() - started) / 1_000_000).coerceAtLeast(0))
            result.getOrNull()?.let { ready ->
                data = ready
                awaitCancellation()
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    dependencies?.close()
                } finally {
                    try {
                        database?.close()
                    } finally {
                        directoryLock?.close()
                    }
                }
            }
        }
        failed = true
    }

    val ready = data
    if (ready == null) {
        StartupSplash(failed, onRetry = {
            failed = false
            attempt++
        })
        return
    }
    val user by ready.auth.user.collectAsState()
    if (user == null) {
        LoginScreen(ready.auth, onLanguageSelected)
    } else {
        val lifecycle = remember { LifecycleRegistry() }
        val root = remember(lifecycle) { RootComponent(DefaultComponentContext(lifecycle)) }
        DisposableEffect(lifecycle) {
            lifecycle.resume()
            onDispose { lifecycle.destroy() }
        }
        App(
            root,
            ready.services,
            ready.auth,
            language,
            onLanguageSelected,
            studentImport = { StudentImportActions(ready.services, ready.students) },
            databaseBackup = { DatabaseBackupCard(backup, ready.auth, onClose) },
        ) {
            PrintCenter(root::navigate, onLanguageSelected) { Res.readBytes("drawable/center_one_logo.webp") }
        }
    }
}
