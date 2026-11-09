package center.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import center.feature.dashboard.ActivityStatus
import center.feature.dashboard.DashboardContent
import center.feature.dashboard.DashboardScreen
import center.feature.dashboard.DashboardState
import center.feature.dashboard.DashboardStatistic
import center.feature.dashboard.RecentActivity
import center.feature.dashboard.StatisticKind
import center.ui.theme.CenterOneTheme
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class DashboardRenderingTest {
    @Test
    fun rendersRtlDashboardAtDesktopAndMinimumWidths() {
        val content =
            DashboardContent.Ready(
                statistics =
                    listOf(
                        DashboardStatistic(StatisticKind.Attendance, "420"),
                        DashboardStatistic(StatisticKind.Revenue, "12,500"),
                        DashboardStatistic(StatisticKind.PendingMessages, "45"),
                        DashboardStatistic(StatisticKind.Groups, "3"),
                    ),
                activities =
                    listOf(
                        RecentActivity("1", "أحمد محمد", "Ahmed Mohamed", "10234", "09:42", ActivityStatus.Completed),
                        RecentActivity("2", "سارة محمود", "Sara Mahmoud", "10817", "09:38", ActivityStatus.Pending),
                    ),
            )
        listOf(800, 1440, 1905).forEach { width -> render(width, content, "dashboard-$width") }
    }

    @Test
    fun rendersLoadingFailureAndEmptyContent() {
        listOf(
            DashboardContent.Loading,
            DashboardContent.Failure("تعذر تحميل البيانات"),
            DashboardContent.Ready(emptyList(), emptyList()),
        ).forEachIndexed { index, content -> render(1440, content, "dashboard-state-$index") }
    }

    private fun render(
        width: Int,
        content: DashboardContent,
        name: String,
    ) {
        val scene =
            ImageComposeScene(width = width, height = 960, density = Density(1f)) {
                CenterOneTheme {
                    DashboardScreen(
                        state = DashboardState(userName = "نور أحمد", dateLabel = "عرض تجريبي", content = content),
                        onEvent = {},
                    )
                }
            }
        try {
            scene.render().use { image ->
                assertEquals(width, image.width)
                assertEquals(960, image.height)
                val directory = Path.of("build", "screenshots")
                Files.createDirectories(directory)
                image.encodeToData()?.use { data -> Files.write(directory.resolve("$name.png"), data.bytes) }
            }
        } finally {
            scene.close()
        }
    }
}
