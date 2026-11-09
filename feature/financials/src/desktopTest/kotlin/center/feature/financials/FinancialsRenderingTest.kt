package center.feature.financials

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
class FinancialsRenderingTest {
    @Test
    fun rendersBothLogsAndNarrowLayout() {
        val state = FinancialsPreviewData.state()
        capture("income", 1905, 960) { FinancialsScreen(state, {}) }
        capture("expense", 1440, 960) { FinancialsScreen(state.copy(selectedLog = FinancialLog.Expense), {}) }
        capture("narrow", 800, 960) { FinancialsScreen(state, {}) }
        capture("empty", 1440, 960) { FinancialsScreen(state.copy(recordQuery = "missing"), {}) }
    }

    @Test
    fun rendersPaymentAndBothExpenseModalBodies() {
        capture("receive-payment", 600, 900) { ReceivePaymentModalContent(FinancialsPreviewData.receivePayment(), {}) }
        capture("teacher-payout", 600, 900) { RecordExpenseModalContent(FinancialsPreviewData.recordExpense(), {}) }
        capture(
            "operational-expense",
            600,
            900,
        ) { RecordExpenseModalContent(FinancialsPreviewData.recordExpense(ExpenseType.Operational), {}) }
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
            // Settle initial measurement and focus/bring-into-view invalidations before capture.
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
