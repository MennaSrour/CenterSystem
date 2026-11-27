package center.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import center.domain.usecase.MonthlyReportGenerator
import center.domain.usecase.MonthlyReportInput
import center.feature.reports.MonthlyReportEvent
import center.feature.reports.MonthlyReportPlatform
import center.feature.reports.MonthlyReportState
import center.feature.reports.ReportsScreen
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
internal fun ReportsRoute(
    root: RootComponent,
    services: DirectoryServices,
    platform: MonthlyReportPlatform,
) {
    val language = LocalAppLanguage.current
    val scope = rememberDirectoryScope(services)
    val holder = remember(services, scope, platform, language) { MonthlyReportStateHolder(services, scope, platform, language) }
    val state by holder.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(holder) { holder.messages.collect { snackbar.showSnackbar(it.resolve(language)) } }
    Box(Modifier.fillMaxSize()) {
        ReportsScreen(state, holder::onEvent, onBack = { root.navigate(Destination.Financials) })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun ReportsAccessDenied(language: AppLanguage) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            LocalizedText("هذا التقرير متاح للمدير فقط", "This report is available to administrators only").resolve(language),
            textAlign = TextAlign.Center,
        )
    }
}

internal class MonthlyReportStateHolder(
    private val services: DirectoryServices,
    private val scope: CoroutineScope,
    private val platform: MonthlyReportPlatform,
    private val language: AppLanguage,
) {
    private val generator = MonthlyReportGenerator()
    private val mutableState = MutableStateFlow(MonthlyReportState(services.today().monthStart(), loading = true))
    val state = mutableState.asStateFlow()
    private val mutableMessages = MutableSharedFlow<LocalizedText>(extraBufferCapacity = 4)
    val messages = mutableMessages.asSharedFlow()

    init {
        load(mutableState.value.month)
    }

    fun onEvent(event: MonthlyReportEvent) {
        when (event) {
            is MonthlyReportEvent.MonthChanged -> {
                val month = event.month.monthStart()
                if (month != mutableState.value.month) load(month)
            }
            MonthlyReportEvent.ExportPdf -> export(print = false)
            MonthlyReportEvent.PrintReport -> export(print = true)
        }
    }

    private fun load(month: LocalDate) {
        mutableState.update { it.copy(month = month, report = null, loading = true, exporting = false, error = null) }
        scope.launch {
            val result =
                runCatching {
                    val nextMonth = month + DatePeriod(months = 1)
                    val startMillis = month.startMillis()
                    val endMillis = nextMonth.startMillis()
                    val students = services.students.observeStudents().first()
                    val teachers = services.teachers.observeTeachers().first()
                    val groups = services.groups.observeGroups().first()
                    val staff = services.staff.observeStaff().first()
                    val ledger = services.treasury.observeLedger(startMillis, endMillis).first()
                    val sessions = services.sessions.observePeriod(startMillis, endMillis).first()
                    val makeupSessions = services.makeupSessions.observeForPeriod(month, nextMonth).first()
                    val due = services.billing.monthlyDueTotals(month).associate { it.studentId to it.totalMinor }
                    val remaining =
                        services.billing.remainingBalances(month).groupBy { it.studentId }.mapValues { (_, rows) ->
                            rows.sumOf { it.remainingMinor }
                        }
                    val paid =
                        ledger
                            .filter {
                                it.kind == center.domain.model.LedgerKind.STUDENT_PAYMENT &&
                                    it.direction == center.domain.model.LedgerDirection.INCOME
                            }.groupBy { it.studentId.orEmpty() }
                            .mapValues { (_, rows) -> rows.sumOf { it.amountMinor } }
                    val expectedTeacherSalary = teachers.associate { it.id to services.billing.monthlySalaryMinor(it.id) }
                    generator.generate(
                        MonthlyReportInput(
                            month,
                            students,
                            teachers,
                            staff,
                            groups,
                            ledger,
                            sessions,
                            due,
                            // Receivables are authoritative per enrollment. Ledger payments provide actual
                            // collected cash; for reporting use the larger per-student aggregate to avoid
                            // losing legacy payments that predate billingMonth attribution.
                            due
                                .mapValues { (studentId, amountDue) -> (amountDue - (paid[studentId] ?: 0L)).coerceAtLeast(0L) }
                                .mapValues { (studentId, ledgerRemaining) ->
                                    minOf(ledgerRemaining, remaining[studentId] ?: ledgerRemaining)
                                },
                            expectedTeacherSalary,
                            makeupSessions,
                        ),
                    )
                }
            result.fold(
                onSuccess = { report -> mutableState.update { it.copy(report = report, loading = false) } },
                onFailure = {
                    mutableState.update {
                        it.copy(
                            loading = false,
                            error =
                                LocalizedText(
                                    "تعذر تحميل بيانات التقرير. حاول مرة أخرى.",
                                    "Could not load report data. Try again.",
                                ).resolve(language),
                        )
                    }
                },
            )
        }
    }

    private fun export(print: Boolean) {
        val report = mutableState.value.report ?: return
        if (mutableState.value.exporting) return
        mutableState.update { it.copy(exporting = true, error = null) }
        scope.launch {
            val title = LocalizedText("التقرير الشهري الشامل", "Monthly Center Report").resolve(language)
            val result =
                try {
                    val bytes = platform.createPdf(report, language, services.today())
                    val completed =
                        if (print) {
                            platform.printPdf(title, bytes)
                        } else {
                            val monthNumber =
                                report.month.monthNumber
                                    .toString()
                                    .padStart(2, '0')
                            val fileName = "CenterOne_Report_${report.month.year}-$monthNumber.pdf"
                            platform.savePdf(
                                title = LocalizedText("حفظ التقرير", "Save report").resolve(language),
                                fileName = fileName,
                                overwrite =
                                    LocalizedText(
                                        "الملف موجود. هل تريد استبداله؟",
                                        "The file exists. Replace it?",
                                    ).resolve(language),
                                bytes = bytes,
                            )
                        }
                    Result.success(completed)
                } catch (failure: Throwable) {
                    Result.failure(failure)
                }
            mutableState.update { it.copy(exporting = false) }
            result.fold(
                onSuccess = { completed ->
                    if (completed) {
                        mutableMessages.emit(
                            if (print) {
                                LocalizedText("تم إرسال التقرير إلى الطابعة", "Report sent to printer")
                            } else {
                                LocalizedText("تم حفظ التقرير بصيغة PDF", "Report saved as PDF")
                            },
                        )
                    }
                },
                onFailure = {
                    mutableMessages.emit(
                        LocalizedText(
                            "تعذر إنشاء التقرير أو طباعته. تحقق من إعدادات الطابعة وحاول مرة أخرى.",
                            "Could not create or print the report. Check the printer and try again.",
                        ),
                    )
                },
            )
        }
    }
}
