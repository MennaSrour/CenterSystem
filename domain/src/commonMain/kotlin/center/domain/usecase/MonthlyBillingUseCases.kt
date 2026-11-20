package center.domain.usecase

import center.domain.model.CollectionDeficit
import center.domain.model.SubscriptionBalance
import center.domain.repository.MonthlyBillingRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetOverdueStudentsUseCase(
    private val billing: MonthlyBillingRepository,
    private val clock: EpochClock,
    private val timeZone: TimeZone = TimeZone.of("Africa/Cairo"),
) {
    suspend operator fun invoke(): Result<List<SubscriptionBalance>> =
        domainResult {
            val today = Instant.fromEpochMilliseconds(clock.nowMillis()).toLocalDateTime(timeZone).date
            if (today.dayOfMonth < 5) emptyList() else billing.remainingBalances(LocalDate(today.year, today.month, 1))
        }
}

class GetTeacherMonthlySalaryUseCase(
    private val billing: MonthlyBillingRepository,
) {
    suspend operator fun invoke(teacherId: String): Result<Long> =
        domainResult {
            require(teacherId.isNotBlank()) { "Teacher ID is required" }
            billing.monthlySalaryMinor(teacherId)
        }
}

class GetTeacherCollectionDeficitUseCase(
    private val billing: MonthlyBillingRepository,
) {
    suspend operator fun invoke(
        teacherId: String,
        month: LocalDate,
    ): Result<CollectionDeficit> =
        domainResult {
            require(teacherId.isNotBlank()) { "Teacher ID is required" }
            require(month.dayOfMonth == 1) { "Use the first day of the billing month" }
            val balances = billing.remainingBalances(month, teacherId)
            val total =
                balances.fold(0L) { sum, balance ->
                    check(balance.remainingMinor in 1..(Long.MAX_VALUE - sum)) { "Invalid collection balance or overflow" }
                    sum + balance.remainingMinor
                }
            CollectionDeficit(balances.map { it.studentId }.toSet().size, total)
        }
}
