package center.feature.reports

import center.ui.localization.AppLanguage
import org.apache.pdfbox.Loader
import kotlin.test.Test
import kotlin.test.assertEquals
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
                timeHeader = "Period / Time",
                dayHeaders = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
                periods =
                    (1..9).map { period ->
                        TimetablePdfPeriod(
                            periodLabel = "Period $period",
                            timeRange = "${8 + period}:00 AM – ${9 + period}:00 AM",
                            dayLessons =
                                List(7) {
                                    listOf(
                                        TimetablePdfLesson(
                                            subject = "A very long lesson title that must wrap within a single narrow weekday column",
                                            teacher = "Teacher with a long name that should wrap without crossing table borders",
                                            trackLabel = "Arabic track",
                                        ),
                                        TimetablePdfLesson("Math", "Another teacher", null),
                                    )
                                },
                        )
                    },
            )
        val english = renderer.renderTimetable(data, AppLanguage.English)
        val arabic =
            renderer.renderTimetable(
                data.copy(
                    title = "جدول الحصص الأسبوعي",
                    subtitle = "الصف الأول · مسار العربي",
                    dayHeaders = listOf("الجمعة", "الخميس", "الأربعاء", "الثلاثاء", "الاثنين", "الأحد", "السبت"),
                    timeHeader = "الفترة / الوقت",
                    periods =
                        data.periods.map { period ->
                            period.copy(
                                periodLabel = "الفترة ${period.periodLabel.substringAfterLast(' ')}",
                                timeRange = "9:00 ص–10:00 ص",
                            )
                        },
                ),
                AppLanguage.Arabic,
            )
        assertTrue(english.isNotEmpty())
        assertTrue(arabic.isNotEmpty())
        val layout = renderer.calculateTimetableLayout(data)
        assertEquals(9, layout.rowHeights.size)
        assertTrue(layout.rowHeights.first() > 22f * layout.scale)
        assertTrue(23f + layout.rowHeights.sum() <= layout.availableHeight)
        Loader.loadPDF(english).use { pdf ->
            assertEquals(1, pdf.numberOfPages)
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
            assertEquals(1, pdf.numberOfPages)
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
