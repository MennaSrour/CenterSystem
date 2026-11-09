package center.feature.courses

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
class CoursesRenderingTest {
    @Test
    fun rendersResponsiveGridAndEmptyState() {
        val state = CoursesPreviewData.state()
        listOf(1905, 1440, 800).forEach { width -> capture("courses-$width", width, 960) { CoursesScreen(state, {}) } }
        capture("empty", 1440, 960) { CoursesScreen(state.copy(searchQuery = "missing"), {}) }
    }

    @Test
    fun rendersAddEditNarrowAndDeleteModalBodies() {
        capture("add-group", 920, 900) { GroupFormModalContent(CoursesPreviewData.newForm(), {}) }
        val edit = CoursesPreviewData.editForm(CoursesPreviewData.state().groups.first())
        capture("edit-group", 920, 900) { GroupFormModalContent(edit, {}) }
        capture("narrow-form", 440, 900) { GroupFormModalContent(edit, {}) }
        capture("delete-group", 480, 480) { DeleteConfirmationModalContent(DeleteGroupState("group-1", "فيزياء — الثالث الثانوي"), {}) }
    }

    private fun capture(
        name: String,
        width: Int,
        height: Int,
        content: @Composable () -> Unit,
    ) {
        val scene =
            ImageComposeScene(width, height, Density(1f)) {
                CenterOneTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() } }
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
