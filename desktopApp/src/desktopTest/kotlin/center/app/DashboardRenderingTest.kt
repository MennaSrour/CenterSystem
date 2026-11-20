package center.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import center.database.openDatabase
import center.feature.dashboard.DashboardPreviewData
import center.feature.dashboard.DashboardScreen
import center.ui.localization.AppLanguage
import center.ui.theme.CenterOneTheme
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class DashboardRenderingTest {
    @Test
    fun rendersDesktopSidebarWithPackagedLogo() {
        val databaseDirectory = Files.createTempDirectory("center-render")
        try {
            openDatabase(databaseDirectory).use { driver ->
                val app = koinApplication { modules(persistenceModule(driver)) }
                val root = RootComponent(DefaultComponentContext(LifecycleRegistry()))
                val services = app.koin.get<DirectoryServices>()
                val scene = ImageComposeScene(width = 800, height = 960, density = Density(1f)) { App(root, services) }
                try {
                    repeat(3) { frame -> scene.render(frame * 16_000_000L).close() }
                    scene.render(48_000_000L).use { image ->
                        val directory = Path.of("build", "screenshots")
                        Files.createDirectories(directory)
                        requireNotNull(image.encodeToData()).use { data ->
                            Files.write(directory.resolve("reception-sidebar-logo.png"), data.bytes)
                        }
                    }
                } finally {
                    scene.close()
                    app.close()
                }
            }
        } finally {
            databaseDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun rendersReceptionAtDesktopAndMinimumWidths() {
        AppLanguage.entries.forEach { language ->
            listOf(800, 1440, 1905).forEach { width ->
                val scene =
                    ImageComposeScene(width = width, height = 960, density = Density(1f)) {
                        CenterOneTheme { DashboardScreen(DashboardPreviewData.state(language), {}) }
                    }
                try {
                    repeat(3) { frame -> scene.render(frame * 16_000_000L).close() }
                    scene.render(48_000_000L).use { image ->
                        assertEquals(width, image.width)
                        assertEquals(960, image.height)
                        val directory = Path.of("build", "screenshots")
                        Files.createDirectories(directory)
                        image.encodeToData()?.use { data ->
                            Files.write(directory.resolve("reception-${language.name}-$width.png"), data.bytes)
                        }
                    }
                } finally {
                    scene.close()
                }
            }
        }
    }
}
