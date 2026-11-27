package center.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import center.domain.usecase.MonthlyCenterReport
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOnePageHeader
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTag
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.theme.CenterOneSpacing
import kotlinx.datetime.LocalDate

data class MonthlyReportState(
    val month: LocalDate,
    val report: MonthlyCenterReport? = null,
    val loading: Boolean = false,
    val exporting: Boolean = false,
    val error: String? = null,
)

interface MonthlyReportPlatform {
    fun createPdf(
        report: MonthlyCenterReport,
        language: AppLanguage,
        generatedAt: LocalDate,
    ): ByteArray

    suspend fun savePdf(
        title: String,
        fileName: String,
        overwrite: String,
        bytes: ByteArray,
    ): Boolean

    suspend fun printPdf(
        title: String,
        bytes: ByteArray,
    ): Boolean
}

sealed interface MonthlyReportEvent {
    data class MonthChanged(
        val month: LocalDate,
    ) : MonthlyReportEvent

    data object ExportPdf : MonthlyReportEvent

    data object PrintReport : MonthlyReportEvent
}

@Composable
fun ReportsScreen(
    state: MonthlyReportState,
    onEvent: (MonthlyReportEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection) {
        CenterOneCanvas(modifier) {
            Column(Modifier.fillMaxSize()) {
                CenterOnePageHeader(
                    "التقرير الشهري الشامل",
                    "Monthly Center Report",
                    LocalizedText("ملخص مالي وتشغيلي للسنتر", "Financial and operational overview").resolve(language),
                    actions = {
                        CenterOnePrimaryButton(onClick = onBack) {
                            Text(LocalizedText("الخزنة", "Treasury").resolve(language))
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = CenterOneSpacing.Section, vertical = CenterOneSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CenterOneIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Previous month", {
                        onEvent(MonthlyReportEvent.MonthChanged(state.month.previousMonth()))
                    })
                    Text(state.month.localizedMonth(language), style = MaterialTheme.typography.titleLarge)
                    CenterOneIconButton(Icons.AutoMirrored.Outlined.ArrowForward, "Next month", {
                        onEvent(MonthlyReportEvent.MonthChanged(state.month.nextMonth()))
                    })
                }
                state.error?.let {
                    Text(
                        it,
                        Modifier.padding(horizontal = CenterOneSpacing.Section),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                    state.report != null -> {
                        LazyColumn(
                            Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = CenterOneSpacing.Section, vertical = CenterOneSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Medium),
                        ) {
                            item { TreasurySummaryCard(state.report, language) }
                            item { StudentSummaryCard(state.report, language) }
                            item {
                                ReportSectionTitle(
                                    LocalizedText("تقرير المدرسين", "Teachers breakdown").resolve(language),
                                    state.report.teachers.size,
                                )
                            }
                            items(state.report.teachers, key = { "teacher-${it.id}" }) { row ->
                                ReportRowCard(
                                    row.id,
                                    row.name,
                                    listOf(
                                        LocalizedText("الدروس ${row.lessonCount}", "Lessons ${row.lessonCount}"),
                                        LocalizedText(
                                            "عربي ${row.arabicStudentCount} · لغات ${row.languagesStudentCount} · برايفت ${row.privateStudentCount}",
                                            "Arabic ${row.arabicStudentCount} · Languages ${row.languagesStudentCount} · Private ${row.privateStudentCount}",
                                        ),
                                        LocalizedText(
                                            "الحضور ${row.attendanceDays} يوم · ${hours(row.workedMinutes)} ساعة",
                                            "Attendance ${row.attendanceDays} days · ${hours(row.workedMinutes)} hours",
                                        ),
                                        LocalizedText(
                                            "المستحق ${money(
                                                row.expectedSalaryMinor,
                                                language,
                                            )} · المصروف ${money(row.paidMinor, language)}",
                                            "Expected ${money(row.expectedSalaryMinor, language)} · Paid ${money(row.paidMinor, language)}",
                                        ),
                                    ).map { it.resolve(language) },
                                )
                            }
                            item {
                                ReportSectionTitle(
                                    LocalizedText("تقرير الطلاب", "Students breakdown").resolve(language),
                                    state.report.students.size,
                                )
                            }
                            items(state.report.students, key = { "student-${it.id}" }) { row ->
                                ReportRowCard(
                                    row.code,
                                    row.name,
                                    listOf(
                                        LocalizedText(
                                            "الصف ${row.grade} · الدروس ${row.lessonCount}",
                                            "Grade ${row.grade} · Lessons ${row.lessonCount}",
                                        ),
                                        LocalizedText(
                                            "المستحق ${money(
                                                row.monthlyDueMinor,
                                                language,
                                            )} · المدفوع ${money(
                                                row.paidMinor,
                                                language,
                                            )} · المتبقي ${money(row.remainingMinor, language)}",
                                            "Due ${money(
                                                row.monthlyDueMinor,
                                                language,
                                            )} · Paid ${money(row.paidMinor, language)} · Remaining ${money(row.remainingMinor, language)}",
                                        ),
                                    ).map { it.resolve(language) },
                                )
                            }
                            item {
                                ReportSectionTitle(
                                    LocalizedText("تقرير الموظفين", "Staff breakdown").resolve(language),
                                    state.report.staff.size,
                                )
                            }
                            items(state.report.staff, key = { "staff-${it.id}" }) { row ->
                                ReportRowCard(
                                    row.id,
                                    row.name,
                                    listOf(
                                        LocalizedText("الوظيفة: ${row.role}", "Role: ${row.role}"),
                                        LocalizedText(
                                            "الحضور ${row.attendanceDays} يوم · ${hours(row.workedMinutes)} ساعة",
                                            "Attendance ${row.attendanceDays} days · ${hours(row.workedMinutes)} hours",
                                        ),
                                        LocalizedText(
                                            "الراتب الشهري ${money(row.monthlySalaryMinor, language)}",
                                            "Monthly salary ${money(row.monthlySalaryMinor, language)}",
                                        ),
                                    ).map { it.resolve(language) },
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(CenterOneSpacing.Section),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CenterOnePrimaryButton(
                                onClick = { onEvent(MonthlyReportEvent.ExportPdf) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.exporting,
                            ) {
                                androidx.compose.material3.Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                                Text(LocalizedText("تصدير PDF", "Export PDF").resolve(language))
                            }
                            CenterOnePrimaryButton(
                                onClick = { onEvent(MonthlyReportEvent.PrintReport) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.exporting,
                            ) {
                                androidx.compose.material3.Icon(Icons.Outlined.Print, contentDescription = null)
                                Text(LocalizedText("طباعة التقرير", "Print report").resolve(language))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreasurySummaryCard(
    report: MonthlyCenterReport,
    language: AppLanguage,
) {
    CenterOneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CenterOneSpacing.Large), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                LocalizedText("ملخص الخزنة والسنتر", "Center and treasury summary").resolve(language),
                style = MaterialTheme.typography.titleLarge,
            )
            Metric(LocalizedText("إجمالي التحصيل", "Collections").resolve(language), report.treasury.collectionsMinor, language)
            Metric(LocalizedText("المصروفات التشغيلية", "Operating expenses").resolve(language), report.treasury.expensesMinor, language)
            Metric(
                LocalizedText("مستحقات المدرسين المصروفة", "Teacher payouts").resolve(language),
                report.treasury.teacherPayoutsMinor,
                language,
            )
            Metric(
                LocalizedText("رواتب الموظفين الشهرية", "Monthly staff salaries").resolve(language),
                report.treasury.staffSalariesMinor,
                language,
            )
            Metric(LocalizedText("صافي الربح", "Net profit").resolve(language), report.treasury.netProfitMinor, language, highlight = true)
            Metric(LocalizedText("المتأخرات", "Overdue balance").resolve(language), report.treasury.overdueMinor, language)
        }
    }
}

@Composable
private fun StudentSummaryCard(
    report: MonthlyCenterReport,
    language: AppLanguage,
) {
    CenterOneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CenterOneSpacing.Large), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(LocalizedText("ملخص الطلاب", "Students summary").resolve(language), style = MaterialTheme.typography.titleLarge)
            Text(
                LocalizedText(
                    "الطلاب النشطون: ${report.studentsSummary.activeCount}",
                    "Active students: ${report.studentsSummary.activeCount}",
                ).resolve(language),
            )
            Text(
                LocalizedText(
                    "سجلات الحضور: ${report.studentsSummary.attendanceCount}",
                    "Attendance records: ${report.studentsSummary.attendanceCount}",
                ).resolve(language),
            )
            Text(
                LocalizedText(
                    "المدفوع بالكامل: ${report.studentsSummary.paidCount}",
                    "Fully paid: ${report.studentsSummary.paidCount}",
                ).resolve(language),
            )
            Text(
                LocalizedText(
                    "عليهم متأخرات: ${report.studentsSummary.unpaidCount}",
                    "With outstanding balance: ${report.studentsSummary.unpaidCount}",
                ).resolve(language),
            )
        }
    }
}

@Composable
private fun ReportSectionTitle(
    title: String,
    count: Int,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        CenterOneTag(count.toString())
    }
}

@Composable
private fun ReportRowCard(
    id: String,
    name: String,
    details: List<String>,
) {
    CenterOneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(CenterOneSpacing.Medium), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(id, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            details.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    amount: Long,
    language: AppLanguage,
    highlight: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(money(amount, language), style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
    }
}

private fun money(
    minor: Long,
    language: AppLanguage,
): String = "${minor.formatMinor()} ${if (language == AppLanguage.Arabic) "ج.م" else "EGP"}"

private fun Long.formatMinor(): String {
    val absolute = kotlin.math.abs(this)
    val whole = absolute / 100
    val fraction = absolute % 100
    return (if (this < 0) "−" else "") + whole.toString() + if (fraction == 0L) "" else ".${fraction.toString().padStart(2, '0')}"
}

private fun hours(minutes: Long): String = "${minutes / 60}.${((minutes % 60) * 10 / 60)}"

private fun LocalDate.previousMonth(): LocalDate = if (monthNumber == 1) LocalDate(year - 1, 12, 1) else LocalDate(year, monthNumber - 1, 1)

private fun LocalDate.nextMonth(): LocalDate = if (monthNumber == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, monthNumber + 1, 1)

private fun LocalDate.localizedMonth(language: AppLanguage): String {
    val arabic = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
    val english =
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    return if (language == AppLanguage.Arabic) "${arabic[monthNumber - 1]} $year" else "${english[monthNumber - 1]} $year"
}
