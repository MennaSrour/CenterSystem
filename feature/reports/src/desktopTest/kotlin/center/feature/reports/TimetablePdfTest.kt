package center.feature.reports

import center.ui.localization.AppLanguage
import org.apache.pdfbox.Loader
import kotlin.test.Test
import kotlin.test.assertTrue

class TimetablePdfTest {
    @Test
    fun createsLandscapeTimetableUsingRequestedOutputLanguage() {
        val renderer = ContextualPdfRenderer()
        val data =
            TimetablePdfDocument(
                title = "Weekly Timetable",
                subtitle = "Primary 1 · Arabic track",
                generatedDate = "Generated: 2026-11-28",
                dayHeaders = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
                times = listOf("9:00 AM", "9:30 AM"),
                cells = listOf(List(7) { "Math · Teacher · 9:00 AM–10:00 AM" }, List(7) { "" }),
            )
        val english = renderer.renderTimetable(data, AppLanguage.English)
        val arabic =
            renderer.renderTimetable(
                data.copy(
                    title = "جدول الحصص الأسبوعي",
                    subtitle = "الصف الأول · مسار العربي",
                    dayHeaders = listOf("الجمعة", "الخميس", "الأربعاء", "الثلاثاء", "الاثنين", "الأحد", "السبت"),
                ),
                AppLanguage.Arabic,
            )
        assertTrue(english.isNotEmpty())
        assertTrue(arabic.isNotEmpty())
        Loader.loadPDF(english).use { pdf ->
            assertTrue(
                pdf.pages
                    .first()
                    .mediaBox.width >
                    pdf.pages
                        .first()
                        .mediaBox.height,
            )
        }
        Loader.loadPDF(arabic).use { pdf ->
            assertTrue(
                pdf.pages
                    .first()
                    .mediaBox.width >
                    pdf.pages
                        .first()
                        .mediaBox.height,
            )
        }
    }
}
