package center.feature.reports

import center.domain.usecase.MonthlyCenterReport
import center.ui.localization.AppLanguage
import kotlinx.datetime.LocalDate

class DesktopMonthlyReportPlatform(
    private val renderer: MonthlyReportPdfRenderer = MonthlyReportPdfRenderer(),
    private val actions: ReportPdfActions = ReportPdfActions(),
    private val contextualRenderer: ContextualPdfRenderer = ContextualPdfRenderer(),
) : MonthlyReportPlatform,
    ContextualPdfPlatform {
    override fun createPdf(
        report: MonthlyCenterReport,
        language: AppLanguage,
        generatedAt: LocalDate,
    ): ByteArray = renderer.render(report, language, generatedAt)

    override suspend fun savePdf(
        title: String,
        fileName: String,
        overwrite: String,
        bytes: ByteArray,
    ): Boolean = actions.save(title, fileName, overwrite, bytes) != null

    override suspend fun printPdf(
        title: String,
        bytes: ByteArray,
    ): Boolean = actions.print(title, bytes)

    override fun createContextPdf(
        title: String,
        lines: List<String>,
        language: AppLanguage,
    ): ByteArray = contextualRenderer.render(title, lines, language)

    override suspend fun saveContextPdf(
        title: String,
        fileName: String,
        overwrite: String,
        bytes: ByteArray,
    ): Boolean = actions.save(title, fileName, overwrite, bytes) != null

    override suspend fun printContextPdf(
        title: String,
        bytes: ByteArray,
    ): Boolean = actions.print(title, bytes)
}
