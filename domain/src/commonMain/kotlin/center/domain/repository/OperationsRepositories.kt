package center.domain.repository

import center.domain.model.ClassGroup
import center.domain.model.LedgerEntry
import center.domain.model.MakeupSession
import center.domain.model.StudentMonthlyDue
import center.domain.model.SubscriptionBalance
import center.domain.model.TreasuryExpense
import center.domain.model.TreasuryTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ClassGroupRepository {
    fun observeGroups(): Flow<List<ClassGroup>>

    suspend fun save(group: ClassGroup)

    suspend fun archive(id: String)

    suspend fun delete(id: String)
}

interface MakeupSessionRepository {
    fun observeForDate(date: LocalDate): Flow<List<MakeupSession>>

    fun observeForPeriod(
        startInclusive: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<MakeupSession>>

    suspend fun save(session: MakeupSession)
}

interface MonthlyBillingRepository {
    fun observeChanges(): Flow<Unit> = kotlinx.coroutines.flow.flowOf(Unit)

    /** Membership and pricing reflect the current active roster. Billing months use the first day. */
    suspend fun remainingBalances(
        month: LocalDate,
        teacherId: String? = null,
    ): List<SubscriptionBalance>

    suspend fun monthlyDueTotals(month: LocalDate): List<StudentMonthlyDue> =
        remainingBalances(month).groupBy { it.studentId }.map { (studentId, rows) ->
            StudentMonthlyDue(studentId, rows.sumOf { it.remainingMinor })
        }

    /** Full contracted monthly commission across active enrollments, independent of collections. */
    suspend fun monthlySalaryMinor(teacherId: String): Long
}

interface TreasuryRepository {
    fun observeTotals(day: LocalDate): Flow<TreasuryTotals>

    fun observeLedger(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<LedgerEntry>>

    suspend fun recordExpense(expense: TreasuryExpense)

    /** Reads the full salary and writes one payout per teacher and month atomically. */
    suspend fun recordSalaryPayout(
        teacherId: String,
        month: LocalDate,
        recordedAt: Long,
        notes: String = "",
    )
}
