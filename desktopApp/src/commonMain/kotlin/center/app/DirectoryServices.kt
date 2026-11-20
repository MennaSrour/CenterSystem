package center.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import center.domain.RegisterStudent
import center.domain.model.EducationStage
import center.domain.repository.AttendanceRepository
import center.domain.repository.ClassGroupRepository
import center.domain.repository.MonthlyBillingRepository
import center.domain.repository.StudentRepository
import center.domain.repository.TeacherRepository
import center.domain.repository.TreasuryRepository
import center.domain.usecase.EpochClock
import center.domain.usecase.GetOverdueStudentsUseCase
import center.domain.usecase.GetTeacherMonthlySalaryUseCase
import center.domain.usecase.IdGenerator
import center.domain.usecase.PayTeacherSalaryUseCase
import center.domain.usecase.ProcessPaymentUseCase
import center.domain.usecase.QueueReminderUseCase
import center.domain.usecase.RecordAttendanceUseCase
import center.ui.localization.AppLanguage
import center.ui.localization.LocalizedText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

internal class DirectoryServices(
    val students: StudentRepository,
    val teachers: TeacherRepository,
    val groups: ClassGroupRepository,
    val billing: MonthlyBillingRepository,
    val treasury: TreasuryRepository,
    val registerStudent: RegisterStudent,
    val overdue: GetOverdueStudentsUseCase,
    val salary: GetTeacherMonthlySalaryUseCase,
    val paySalary: PayTeacherSalaryUseCase,
    val reminder: QueueReminderUseCase,
    val clock: EpochClock,
    val ids: IdGenerator,
    val attendance: AttendanceRepository,
    val recordAttendance: RecordAttendanceUseCase,
    val processPayment: ProcessPaymentUseCase,
) {
    fun today(): LocalDate = Instant.fromEpochMilliseconds(clock.nowMillis()).toLocalDateTime(Cairo).date

    fun dates() =
        flow {
            while (true) {
                val date = today()
                emit(date)
                delay(((date + DatePeriod(days = 1)).startMillis() - clock.nowMillis()).coerceAtLeast(1))
            }
        }
}

internal val Cairo = TimeZone.of("Africa/Cairo")

internal fun LocalDate.startMillis(): Long = atStartOfDayIn(Cairo).toEpochMilliseconds()

internal fun LocalDate.monthStart(): LocalDate = LocalDate(year, month, 1)

internal fun String.localizedValue(): LocalizedText = LocalizedText(this, this)

internal fun Long.decimalMinor(): String {
    val fraction = kotlin.math.abs(this % 100)
    val whole = this / 100
    if (fraction == 0L) return whole.toString()
    val sign = if (this < 0 && whole == 0L) "-" else ""
    return "$sign$whole.${fraction.toString().padStart(2, '0')}"
}

internal fun String.normalizeSearch(): String = trim().map { it.digitToIntOrNull()?.digitToChar() ?: it }.joinToString("")

internal fun Long.formattedMinor(): String {
    val decimal = decimalMinor()
    val sign = if (decimal.startsWith('-')) "-" else ""
    val parts = decimal.removePrefix("-").split('.')
    val whole =
        parts
            .first()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    return sign + whole + (parts.getOrNull(1)?.let { ".$it" } ?: "")
}

internal fun String.parseMinor(): Long? {
    val parts = trim().map { it.digitToIntOrNull()?.digitToChar() ?: if (it == '٫') '.' else it }.joinToString("").split('.')
    if (parts.size > 2 || parts.any { it.isEmpty() || it.any { char -> !char.isDigit() } }) return null
    val whole = parts.first().toLongOrNull()?.takeIf { it <= 1_000_000_000 } ?: return null
    val decimal = parts.getOrNull(1).orEmpty()
    if (decimal.length > 2) return null
    return whole * 100 + decimal.padEnd(2, '0').toLong()
}

internal data class DirectoryGrade(
    val id: String,
    val label: String,
    val stage: EducationStage,
)

internal fun gradeCatalog(language: AppLanguage): List<DirectoryGrade> =
    EducationStage.entries.flatMap { stage ->
        val prefix =
            when (stage) {
                EducationStage.PRIMARY -> "primary"
                EducationStage.PREP -> "prep"
                EducationStage.SECONDARY -> "secondary"
            }
        val arStage =
            when (stage) {
                EducationStage.PRIMARY -> "الابتدائي"
                EducationStage.PREP -> "الإعدادي"
                EducationStage.SECONDARY -> "الثانوي"
            }
        val start =
            when (stage) {
                EducationStage.PRIMARY -> 0
                EducationStage.PREP -> 6
                EducationStage.SECONDARY -> 9
            }
        (1..if (stage == EducationStage.PRIMARY) 6 else 3).map { level ->
            val label = if (language == AppLanguage.Arabic) "الصف $level $arStage" else "Grade ${start + level}"
            DirectoryGrade("$prefix-$level", label, stage)
        }
    }

internal suspend fun <T> directoryResult(operation: suspend () -> T): Result<T> =
    try {
        Result.success(operation())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

@Composable
internal fun rememberDirectoryScope(key: Any): CoroutineScope {
    val parent = rememberCoroutineScope()
    val scope =
        remember(parent, key) {
            CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
        }
    DisposableEffect(scope) {
        onDispose { scope.cancel() }
    }
    return scope
}
