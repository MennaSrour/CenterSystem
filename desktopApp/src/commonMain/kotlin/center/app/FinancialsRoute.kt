package center.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import center.app.resources.Res
import center.app.resources.feature_unavailable
import center.app.resources.financial_attachment_unavailable
import center.app.resources.financial_demo_not_saved
import center.feature.financials.ExpenseType
import center.feature.financials.FinancialsDestination
import center.feature.financials.FinancialsEvent
import center.feature.financials.FinancialsModal
import center.feature.financials.FinancialsPreviewData
import center.feature.financials.FinancialsScreen
import center.feature.financials.ReceivePaymentState
import center.feature.financials.RecordExpenseState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Interactive UI fixture only; no financial operation or platform file picker runs here. */
@Composable
internal fun FinancialsRoute(root: RootComponent) {
    var state by remember { mutableStateOf(FinancialsPreviewData.state()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(Res.string.feature_unavailable)
    val notSaved = stringResource(Res.string.financial_demo_not_saved)
    val attachmentUnavailable = stringResource(Res.string.financial_attachment_unavailable)

    fun payment(edit: (ReceivePaymentState) -> ReceivePaymentState) {
        val modal = state.modal as? FinancialsModal.ReceivePayment ?: return
        if (!modal.form.isSubmitting) state = state.copy(modal = FinancialsModal.ReceivePayment(edit(modal.form)))
    }

    fun expense(edit: (RecordExpenseState) -> RecordExpenseState) {
        val modal = state.modal as? FinancialsModal.RecordExpense ?: return
        if (!modal.form.isSubmitting) state = state.copy(modal = FinancialsModal.RecordExpense(edit(modal.form)))
    }

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    Box(Modifier.fillMaxSize()) {
        FinancialsScreen(state, onEvent = { event ->
            when (event) {
                is FinancialsEvent.TabSelected -> state = state.copy(selectedLog = event.log)
                is FinancialsEvent.RecordQueryChanged -> state = state.copy(recordQuery = event.value)
                FinancialsEvent.OpenReceivePayment ->
                    state =
                        state.copy(modal = FinancialsModal.ReceivePayment(FinancialsPreviewData.receivePayment()))
                FinancialsEvent.OpenRecordExpense ->
                    state =
                        state.copy(modal = FinancialsModal.RecordExpense(FinancialsPreviewData.recordExpense()))
                FinancialsEvent.DismissModal -> state = state.copy(modal = FinancialsModal.Closed)
                is FinancialsEvent.StudentQueryChanged ->
                    payment { form ->
                        form.copy(
                            studentQuery = event.value,
                            studentResults =
                                FinancialsPreviewData.students.filter {
                                    it.name.contains(event.value) ||
                                        it.code.contains(event.value)
                                },
                            selectedStudentId = null,
                            classes = emptyList(),
                            selectedClassId = null,
                            classMenuExpanded = false,
                            split = null,
                            canConfirm = false,
                        )
                    }
                is FinancialsEvent.StudentSelected ->
                    payment { form ->
                        if (form.studentResults.none { it.id == event.id }) {
                            form
                        } else {
                            form.copy(
                                selectedStudentId = event.id,
                                classes = FinancialsPreviewData.classes,
                                selectedClassId = null,
                                classMenuExpanded = false,
                                split = null,
                                canConfirm = false,
                            )
                        }
                    }
                is FinancialsEvent.ClassMenuChanged -> payment { it.copy(classMenuExpanded = event.expanded) }
                is FinancialsEvent.ClassSelected ->
                    payment { form ->
                        if (form.classes.none { it.id == event.id }) {
                            form
                        } else {
                            form.copy(
                                selectedClassId = event.id,
                                classMenuExpanded = false,
                                canConfirm =
                                    form.selectedStudentId != null && form.amount.isNotBlank(),
                            )
                        }
                    }
                is FinancialsEvent.PaymentAmountChanged ->
                    payment {
                        it.copy(
                            amount = event.value,
                            split = null,
                            canConfirm =
                                event.value.isNotBlank() && it.selectedStudentId != null && it.selectedClassId != null,
                            message = null,
                        )
                    }
                FinancialsEvent.ConfirmReceivePayment -> payment { if (it.canConfirm) it.copy(message = notSaved) else it }
                is FinancialsEvent.ExpenseTypeSelected ->
                    expense {
                        it.copy(
                            type = event.type,
                            teacherMenuExpanded = false,
                            canConfirm =
                                it.amount.isNotBlank() && (event.type == ExpenseType.Operational || it.selectedTeacherId != null),
                        )
                    }
                is FinancialsEvent.TeacherMenuChanged -> expense { it.copy(teacherMenuExpanded = event.expanded) }
                is FinancialsEvent.TeacherSelected ->
                    expense { form ->
                        if (form.teachers.none {
                                it.id == event.id
                            }
                        ) {
                            form
                        } else {
                            form.copy(selectedTeacherId = event.id, teacherMenuExpanded = false, canConfirm = form.amount.isNotBlank())
                        }
                    }
                is FinancialsEvent.ExpenseAmountChanged ->
                    expense {
                        it.copy(
                            amount = event.value,
                            canConfirm =
                                event.value.isNotBlank() && (it.type == ExpenseType.Operational || it.selectedTeacherId != null),
                            message = null,
                        )
                    }
                is FinancialsEvent.ExpenseNotesChanged -> expense { it.copy(notes = event.value) }
                FinancialsEvent.AttachReceipt -> expense { it.copy(message = attachmentUnavailable) }
                FinancialsEvent.RemoveAttachment -> expense { it.copy(attachmentName = null) }
                FinancialsEvent.ConfirmExpense -> expense { if (it.canConfirm) it.copy(message = notSaved) else it }
                is FinancialsEvent.Navigate ->
                    when (event.destination) {
                        FinancialsDestination.Courses -> root.navigate(Destination.Courses)
                        FinancialsDestination.Home -> root.navigate(Destination.Dashboard)
                        FinancialsDestination.Students -> root.navigate(Destination.Students)
                        FinancialsDestination.Reports -> root.navigate(Destination.Reports)
                        FinancialsDestination.Financials -> Unit
                        FinancialsDestination.Schedule, FinancialsDestination.Settings -> notify(unavailable)
                    }
                FinancialsEvent.OpenNotifications, FinancialsEvent.RequestEnglish, FinancialsEvent.LogOut -> notify(unavailable)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
