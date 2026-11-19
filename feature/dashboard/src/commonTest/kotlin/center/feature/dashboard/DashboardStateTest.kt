package center.feature.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DashboardStateTest {
    @Test
    fun fixtureSearchAcceptsBarcodeAndEitherStudentName() {
        listOf("10234", " ١٠٢٣٤ ", "Youssef", "يوسف").forEach { query ->
            assertEquals(DashboardPreviewData.student, DashboardPreviewData.findStudent(query))
        }
    }

    @Test
    fun fixtureSearchRejectsBlankAndUnknownQueries() {
        listOf("", " ", "1023", "missing").forEach { query ->
            assertNull(DashboardPreviewData.findStudent(query))
        }
    }
}
