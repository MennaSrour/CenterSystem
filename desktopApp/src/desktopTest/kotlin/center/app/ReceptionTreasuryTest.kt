package center.app

import center.database.openDatabase
import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupStatus
import center.domain.model.LedgerKind
import center.domain.model.Student
import center.domain.model.Teacher
import center.domain.model.TreasuryExpense
import center.domain.repository.MessageQueueRepository
import center.domain.usecase.EpochClock
import center.feature.dashboard.DashboardEvent
import center.feature.dashboard.PaymentStatus
import center.feature.financials.ExpenseType
import center.feature.financials.FinancialSummary
import center.feature.financials.FinancialsEvent
import center.feature.financials.FinancialsModal
import center.feature.students.StudentStageFilter
import center.feature.students.StudentsEvent
import center.ui.localization.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.koin.dsl.koinApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReceptionTreasuryTest {
    @Test
    fun paymentAttendanceAndExpensesUpdateAllOpenScreens() =
        runTest {
            val directory = Files.createTempDirectory("center-reception")
            val now = LocalDate(2026, 11, 5).startMillis()
            try {
                openDatabase(directory).use { driver ->
                    val app =
                        koinApplication { modules(persistenceModule(driver, StandardTestDispatcher(testScheduler), EpochClock { now })) }
                    try {
                        val services = app.koin.get<DirectoryServices>()
                        services.teachers.save(Teacher("t", "Karim", "Physics"))
                        services.groups.save(ClassGroup("g", "Physics A", "t", 20_050, 6_000, 1, EducationSystem.BOTH, GroupStatus.ACTIVE))
                        services.students.save(
                            Student("s", "Youssef Ahmed", parentPhone = "01000000002", code = "10234", enrolledGroupIds = setOf("g")),
                        )

                        services.treasury.recordExpense(
                            TreasuryExpense("past", LedgerKind.OPERATIONAL, 1_000L, LocalDate(2026, 10, 31).startMillis(), "Last month"),
                        )
                        services.treasury.recordExpense(
                            TreasuryExpense(
                                "earlier",
                                LedgerKind.OPERATIONAL,
                                500L,
                                LocalDate(2026, 11, 4).startMillis(),
                                "Earlier this month",
                            ),
                        )

                        val reception = DashboardStateHolder(services, backgroundScope, AppLanguage.English)
                        val treasury =
                            FinancialsStateHolder(
                                services,
                                backgroundScope,
                                AppLanguage.English,
                                ReceptionShortcut.CollectSubscription("10234"),
                            )
                        val students = StudentsStateHolder(services, backgroundScope, AppLanguage.English)
                        runCurrent()

                        reception.onEvent(DashboardEvent.QueryChanged("١٠٢٣٤"))
                        reception.onEvent(DashboardEvent.Search)
                        assertEquals(
                            PaymentStatus.Overdue,
                            reception.uiState.value.student
                                ?.paymentStatus,
                        )
                        students.onEvent(StudentsEvent.StageSelected(StudentStageFilter.Overdue))
                        assertEquals(1, students.uiState.value.visibleOverdueRows.size)

                        repeat(2) {
                            reception.onEvent(DashboardEvent.RegisterAttendance("10234"))
                            runCurrent()
                        }
                        assertEquals("1", reception.uiState.value.attendance)
                        val prefilled = (treasury.uiState.value.modal as FinancialsModal.ReceivePayment).form
                        assertEquals("s", prefilled.selectedStudentId)
                        assertEquals(listOf("g"), prefilled.classes.map { it.id })
                        treasury.onEvent(FinancialsEvent.ClassSelected("g"))
                        treasury.onEvent(FinancialsEvent.ConfirmReceivePayment)
                        runCurrent()
                        assertEquals(
                            PaymentStatus.Paid,
                            reception.uiState.value.student
                                ?.paymentStatus,
                        )
                        assertTrue(
                            students.uiState.value.visibleOverdueRows
                                .isEmpty(),
                        )
                        assertEquals(FinancialSummary("200.50", "0", "185.50", "195.50"), treasury.uiState.value.summary)
                        assertEquals(
                            "+ 200.50 EGP",
                            treasury.uiState.value.incomeRecords
                                .single()
                                .formattedAmount,
                        )
                        val queue =
                            app.koin
                                .get<MessageQueueRepository>()
                                .getPendingMessages()
                                .first()
                        assertEquals(2, queue.size)
                        assertTrue(queue.any { it.messageBody.contains("200.50") })

                        treasury.onEvent(FinancialsEvent.OpenRecordExpense)
                        treasury.onEvent(FinancialsEvent.ExpenseAmountChanged("20"))
                        treasury.onEvent(FinancialsEvent.ExpenseNotesChanged("Electricity"))
                        treasury.onEvent(FinancialsEvent.ConfirmExpense)
                        runCurrent()
                        treasury.onEvent(FinancialsEvent.OpenRecordExpense)
                        treasury.onEvent(FinancialsEvent.ExpenseTypeSelected(ExpenseType.TeacherPayout))
                        treasury.onEvent(FinancialsEvent.TeacherSelected("t"))
                        assertEquals("120.30", (treasury.uiState.value.modal as FinancialsModal.RecordExpense).form.amount)
                        treasury.onEvent(FinancialsEvent.ExpenseNotesChanged("November payout"))
                        treasury.onEvent(FinancialsEvent.ConfirmExpense)
                        runCurrent()
                        assertEquals(FinancialSummary("200.50", "140.30", "45.20", "55.20"), treasury.uiState.value.summary)
                        assertEquals("60.20", reception.uiState.value.treasury)
                        assertEquals(
                            setOf("Electricity", "Karim", "Earlier this month", "Last month"),
                            treasury.uiState.value.expenseRecords
                                .map { it.partyName }
                                .toSet(),
                        )
                        assertEquals(
                            "November payout",
                            services.treasury
                                .observeLedger(now, now + 1)
                                .first()
                                .single {
                                    it.kind == LedgerKind.TEACHER_PAYOUT
                                }.description,
                        )
                    } finally {
                        app.close()
                    }
                }
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
}
