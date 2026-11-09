package center.feature.financials

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinancialsStateTest {
    @Test
    fun selectedLogChoosesItsRecordsWithoutMutatingTheSnapshot() {
        val income = FinancialsPreviewData.state()
        val expense = income.copy(selectedLog = FinancialLog.Expense)
        assertEquals(income.incomeRecords, income.visibleRecords)
        assertEquals(income.expenseRecords, expense.visibleRecords)
        assertEquals(FinancialLog.Income, income.selectedLog)
    }

    @Test
    fun searchMatchesArabicEnglishAndCategoryAndTrimsWhitespace() {
        val state = FinancialsPreviewData.state()
        assertEquals(listOf("in-1"), state.copy(recordQuery = "  يوسف ").visibleRecords.map { it.id })
        assertEquals(listOf("in-1"), state.copy(recordQuery = "YOUSSEF").visibleRecords.map { it.id })
        assertEquals(listOf("in-2"), state.copy(recordQuery = "فيزياء").visibleRecords.map { it.id })
        assertEquals(state.incomeRecords, state.copy(recordQuery = "   ").visibleRecords)
        assertTrue(state.copy(recordQuery = "missing").visibleRecords.isEmpty())
    }

    @Test
    fun searchDoesNotLeakIncomeRecordsIntoTheExpenseTab() {
        val state = FinancialsPreviewData.state().copy(selectedLog = FinancialLog.Expense, recordQuery = "يوسف")
        assertTrue(state.visibleRecords.isEmpty())
    }
}
