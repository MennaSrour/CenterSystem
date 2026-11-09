package center.domain.usecase

import center.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.math.round

class GetTeacherMonthlyDuesUseCase(
    private val payments: PaymentRepository,
    private val timeZone: TimeZone = TimeZone.of("Africa/Cairo"),
) {
    suspend operator fun invoke(
        teacherId: String,
        month: Int,
        year: Int,
    ): Result<Double> =
        domainResult {
            require(teacherId.isNotBlank()) { "Teacher ID is required" }
            require(year in 1..9999 && month in 1..12) { "Invalid year or month" }
            val start = LocalDate(year, month, 1)
            val end = start.plus(DatePeriod(months = 1))
            val entries =
                payments
                    .getPaymentsForTeacher(
                        teacherId,
                        start.atStartOfDayIn(timeZone).toEpochMilliseconds(),
                        end.atStartOfDayIn(timeZone).toEpochMilliseconds(),
                    ).first()
            val total =
                entries.sumOf {
                    require(it.teacherShare.isFinite() && it.teacherShare >= 0.0) { "Invalid stored teacher share" }
                    it.teacherShare
                }
            require(total.isFinite()) { "Teacher dues overflow" }
            val rounded = round(total * 100) / 100
            require(rounded.isFinite()) { "Teacher dues overflow" }
            rounded
        }
}
