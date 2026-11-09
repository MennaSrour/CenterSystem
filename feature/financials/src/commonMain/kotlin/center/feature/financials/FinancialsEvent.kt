package center.feature.financials

sealed interface FinancialsEvent {
    data class TabSelected(
        val log: FinancialLog,
    ) : FinancialsEvent

    data class RecordQueryChanged(
        val value: String,
    ) : FinancialsEvent

    data object OpenReceivePayment : FinancialsEvent

    data object OpenRecordExpense : FinancialsEvent

    data object DismissModal : FinancialsEvent

    data class StudentQueryChanged(
        val value: String,
    ) : FinancialsEvent

    data class StudentSelected(
        val id: String,
    ) : FinancialsEvent

    data class ClassMenuChanged(
        val expanded: Boolean,
    ) : FinancialsEvent

    data class ClassSelected(
        val id: String,
    ) : FinancialsEvent

    data class PaymentAmountChanged(
        val value: String,
    ) : FinancialsEvent

    data object ConfirmReceivePayment : FinancialsEvent

    data class ExpenseTypeSelected(
        val type: ExpenseType,
    ) : FinancialsEvent

    data class TeacherMenuChanged(
        val expanded: Boolean,
    ) : FinancialsEvent

    data class TeacherSelected(
        val id: String,
    ) : FinancialsEvent

    data class ExpenseAmountChanged(
        val value: String,
    ) : FinancialsEvent

    data class ExpenseNotesChanged(
        val value: String,
    ) : FinancialsEvent

    data object AttachReceipt : FinancialsEvent

    data object RemoveAttachment : FinancialsEvent

    data object ConfirmExpense : FinancialsEvent

    data class Navigate(
        val destination: FinancialsDestination,
    ) : FinancialsEvent

    data object OpenNotifications : FinancialsEvent

    data object RequestEnglish : FinancialsEvent

    data object LogOut : FinancialsEvent
}
