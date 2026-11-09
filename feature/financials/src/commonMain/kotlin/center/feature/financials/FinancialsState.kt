package center.feature.financials

enum class FinancialLog { Income, Expense }

enum class ExpenseType { TeacherPayout, Operational }

enum class FinancialsDestination { Home, Students, Courses, Schedule, Financials, Reports, Settings }

data class FinancialSummary(
    val todayInflow: String,
    val todayOutflow: String,
    val currentBalance: String,
    val monthlyProfit: String,
)

data class FinancialRecord(
    val id: String,
    val partyName: String,
    val englishName: String,
    val category: String,
    val formattedAmount: String,
    val formattedDate: String,
    val formattedTime: String,
)

data class FinancialsState(
    val summary: FinancialSummary,
    val incomeRecords: List<FinancialRecord>,
    val expenseRecords: List<FinancialRecord>,
    val userName: String,
    val dateLabel: String,
    val selectedLog: FinancialLog = FinancialLog.Income,
    val recordQuery: String = "",
    val modal: FinancialsModal = FinancialsModal.Closed,
    val isTreasuryOpen: Boolean = true,
) {
    val visibleRecords: List<FinancialRecord>
        get() {
            val records = if (selectedLog == FinancialLog.Income) incomeRecords else expenseRecords
            val query = recordQuery.trim()
            return if (query.isEmpty()) {
                records
            } else {
                records.filter {
                    it.partyName.contains(query, ignoreCase = true) ||
                        it.englishName.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
                }
            }
        }
}

sealed interface FinancialsModal {
    data object Closed : FinancialsModal

    data class ReceivePayment(
        val form: ReceivePaymentState,
    ) : FinancialsModal

    data class RecordExpense(
        val form: RecordExpenseState,
    ) : FinancialsModal
}

data class StudentOption(
    val id: String,
    val name: String,
    val code: String,
)

data class ClassOption(
    val id: String,
    val label: String,
)

data class TeacherOption(
    val id: String,
    val name: String,
    val formattedDues: String,
)

/** Preformatted split supplied by the caller. Rendering never calculates financial shares. */
data class FinancialSplit(
    val total: String,
    val teacherShare: String,
    val centerShare: String,
    val ratio: String,
)

data class ReceivePaymentState(
    val studentQuery: String = "",
    val studentResults: List<StudentOption> = emptyList(),
    val selectedStudentId: String? = null,
    val classes: List<ClassOption> = emptyList(),
    val selectedClassId: String? = null,
    val classMenuExpanded: Boolean = false,
    val split: FinancialSplit? = null,
    val amount: String = "",
    val studentError: String? = null,
    val classError: String? = null,
    val amountError: String? = null,
    val message: String? = null,
    val isSubmitting: Boolean = false,
    val canConfirm: Boolean = false,
)

data class RecordExpenseState(
    val type: ExpenseType = ExpenseType.TeacherPayout,
    val teachers: List<TeacherOption> = emptyList(),
    val selectedTeacherId: String? = null,
    val teacherMenuExpanded: Boolean = false,
    val amount: String = "",
    val notes: String = "",
    val attachmentName: String? = null,
    val teacherError: String? = null,
    val amountError: String? = null,
    val message: String? = null,
    val isSubmitting: Boolean = false,
    val canConfirm: Boolean = false,
)
