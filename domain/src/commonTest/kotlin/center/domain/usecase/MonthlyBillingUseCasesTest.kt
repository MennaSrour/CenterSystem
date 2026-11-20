package center.domain.usecase

import center.domain.model.CollectionDeficit
import center.domain.model.SubscriptionBalance
import center.domain.repository.MonthlyBillingRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonthlyBillingUseCasesTest {
    private val zone = TimeZone.of("Africa/Cairo")
    private val month = LocalDate(2026, 11, 1)
    private val balances = listOf(balance("s1", "g1", 500), balance("s1", "g2", 1_200), balance("s2", "g1", 2_000))

    @Test
    fun overdueStartsAtCairoMidnightOnTheFifthAndRequestsTheCurrentMonth() =
        runTest {
            val repository = Billing()
            var time = LocalDate(2026, 11, 5).atStartOfDayIn(zone).toEpochMilliseconds() - 1
            val overdue = GetOverdueStudentsUseCase(repository, EpochClock { time })
            assertTrue(overdue().getOrThrow().isEmpty())
            assertNull(repository.requestedMonth)
            time++
            assertEquals(balances, overdue().getOrThrow())
            assertEquals(month, repository.requestedMonth)
            assertNull(repository.requestedTeacher)
            time = LocalDate(2026, 12, 1).atStartOfDayIn(zone).toEpochMilliseconds()
            assertTrue(overdue().getOrThrow().isEmpty())
        }

    @Test
    fun salaryIsIndependentAndDeficitCountsEachStudentOnce() =
        runTest {
            val repository = Billing()
            val salary = GetTeacherMonthlySalaryUseCase(repository)
            val deficit = GetTeacherCollectionDeficitUseCase(repository)
            assertEquals(40_000L, salary("teacher").getOrThrow())
            assertNull(repository.requestedMonth)
            assertEquals(CollectionDeficit(2, 3_700), deficit("teacher", month).getOrThrow())
            assertEquals("teacher", repository.requestedTeacher)
            assertEquals(40_000L, salary("teacher").getOrThrow())
            assertTrue(salary("").isFailure)
            assertTrue(deficit("teacher", LocalDate(2026, 11, 5)).isFailure)
        }

    private fun balance(
        student: String,
        group: String,
        amount: Long,
    ) = SubscriptionBalance(student, student, student, "01000000000", group, group, "teacher", "Teacher", amount)

    private inner class Billing : MonthlyBillingRepository {
        var requestedMonth: LocalDate? = null
        var requestedTeacher: String? = null

        override suspend fun remainingBalances(
            month: LocalDate,
            teacherId: String?,
        ): List<SubscriptionBalance> {
            requestedMonth = month
            requestedTeacher = teacherId
            return balances
        }

        override suspend fun monthlySalaryMinor(teacherId: String): Long = 40_000L
    }
}
