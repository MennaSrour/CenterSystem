package center.feature.dashboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import center.ui.localization.AppLanguage
import center.ui.theme.CenterOneTheme
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class DashboardLocaleRenderingTest {
    @Test
    fun rendersSingleLanguageDashboardInBothDirections() {
        AppLanguage.entries.forEach { language ->
            val state =
                DashboardState(
                    userName = DashboardText.DEMO_USER.text(language),
                    dateLabel = DashboardText.DEMO_DATE.text(language),
                    language = language,
                    content =
                        DashboardContent.Ready(
                            listOf(
                                DashboardStatistic(StatisticKind.Attendance, "420"),
                                DashboardStatistic(StatisticKind.Revenue, "12,500"),
                            ),
                            listOf(RecentActivity("1", "أحمد محمد", "Ahmed Mohamed", "10234", "09:42", ActivityStatus.Completed)),
                        ),
                )
            val scene = ImageComposeScene(1440, 1100, Density(1f)) { CenterOneTheme { DashboardScreen(state, {}) } }
            try {
                repeat(3) { frame -> scene.render(frame * 16_000_000L).close() }
                scene.render(48_000_000L).use { image ->
                    assertEquals(1440, image.width)
                    val directory = Path.of("build", "screenshots")
                    Files.createDirectories(directory)
                    requireNotNull(image.encodeToData()).use { data ->
                        Files.write(directory.resolve("dashboard-${language.name}.png"), data.bytes)
                    }
                }
            } finally {
                scene.close()
            }
        }
    }
}
