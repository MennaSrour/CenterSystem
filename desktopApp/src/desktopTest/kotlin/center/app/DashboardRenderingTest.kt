package center.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import center.feature.dashboard.DashboardPreviewData
import center.feature.dashboard.DashboardScreen
import center.ui.localization.AppLanguage
import center.ui.theme.CenterOneTheme
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class DashboardRenderingTest {
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
