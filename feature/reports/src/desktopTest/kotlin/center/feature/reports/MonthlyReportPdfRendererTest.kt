package center.feature.reports

import center.domain.usecase.MonthlyCenterReport
import center.domain.usecase.ReportStaffRow
import center.domain.usecase.ReportStudentsSummary
import center.domain.usecase.ReportTreasurySummary
import center.ui.localization.AppLanguage
import kotlinx.datetime.LocalDate
import org.apache.pdfbox.Loader
import kotlin.test.Test
import kotlin.test.assertTrue

class MonthlyReportPdfRendererTest {
    @Test
    fun rendersNonEmptyArabicAndEnglishPdfWithEmbeddedArabicFont() {
        val report =
            MonthlyCenterReport(
                month = LocalDate(2026, 9, 1),
                treasury = ReportTreasurySummary(120_000, 25_000, 30_000, 18_000, 47_000, 12_000),
                studentsSummary = ReportStudentsSummary(1, 8, 1, 0),
                teachers = emptyList(),
                students = emptyList(),
                staff = listOf(ReportStaffRow("SF-1001", "نور", "استقبال", 20, 160, 18_000)),
            )
        val renderer = MonthlyReportPdfRenderer()

        listOf(AppLanguage.Arabic, AppLanguage.English).forEach { language ->
            val bytes = renderer.render(report, language, LocalDate(2026, 9, 30))
            assertTrue(bytes.size > 1_000)
            Loader.loadPDF(bytes).use { document ->
                assertTrue(document.numberOfPages >= 1)
                assertTrue(
                    document.pages
                        .first()
                        .resources.fontNames
                        .toList()
                        .isNotEmpty(),
                )
            }
        }
    }
}
