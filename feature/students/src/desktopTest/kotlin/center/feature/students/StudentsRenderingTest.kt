package center.feature.students

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import center.ui.theme.CenterOneTheme
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class StudentsRenderingTest {
    @Test
    fun rendersDirectoryFiltersEmptyAndNarrowLayouts() {
        val state = StudentsPreviewData.state()
        capture("directory", 1905, 960) { StudentsScreen(state, {}) }
        capture("secondary", 1440, 960) { StudentsScreen(state.copy(stageFilter = StudentStageFilter.Secondary), {}) }
        capture("empty", 1440, 960) { StudentsScreen(state.copy(searchQuery = "missing"), {}) }
        capture("narrow", 800, 960) { StudentsScreen(state, {}) }
    }

    @Test
    fun rendersTwoColumnAndStackedModalBodies() {
        capture("add-student", 780, 740) { AddStudentModalContent(StudentsPreviewData.filledForm(), {}) }
        capture("add-student-narrow", 420, 840) { AddStudentModalContent(StudentsPreviewData.addStudent(), {}) }
    }

    private fun capture(
        name: String,
        width: Int,
        height: Int,
        content: @Composable () -> Unit,
    ) {
        val scene =
            ImageComposeScene(width = width, height = height, density = Density(1f)) {
                CenterOneTheme {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
                }
            }
        try {
            repeat(3) { frame -> scene.render(frame * 16_000_000L).close() }
            scene.render(48_000_000L).use { image ->
                assertEquals(width, image.width)
                assertEquals(height, image.height)
                val directory = Path.of("build", "screenshots")
                Files.createDirectories(directory)
                requireNotNull(image.encodeToData()).use { data -> Files.write(directory.resolve("$name.png"), data.bytes) }
            }
        } finally {
            scene.close()
        }
    }
}
