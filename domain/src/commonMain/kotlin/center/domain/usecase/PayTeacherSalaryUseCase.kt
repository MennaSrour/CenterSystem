package center.domain.usecase

import center.domain.repository.TreasuryRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun salaryPayoutId(
    teacherId: String,
    month: LocalDate,
): String = "salary:$month:$teacherId"

class PayTeacherSalaryUseCase(
    private val treasury: TreasuryRepository,
    private val clock: EpochClock,
) {
    suspend operator fun invoke(
        teacherId: String,
        notes: String = "",
    ): Result<Unit> =
        domainResult {
            require(teacherId.isNotBlank())
            val now = clock.nowMillis()
            val date = Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.of("Africa/Cairo")).date
            treasury.recordSalaryPayout(teacherId, LocalDate(date.year, date.month, 1), now, notes)
        }
}
