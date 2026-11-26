package center.feature.financials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneChoiceGroup
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneInfoPanel
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField

@Composable
fun RecordExpenseModal(
    state: RecordExpenseState,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModal(onDismissRequest = { if (!state.isSubmitting) onEvent(FinancialsEvent.DismissModal) }) {
        RecordExpenseModalContent(state, onEvent, modifier)
    }
}

@Composable
internal fun RecordExpenseModalContent(
    state: RecordExpenseState,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModalContent(
        title = appString("expense_title"),
        subtitle = appString("expense_subtitle"),
        closeLabel = appString("close"),
        onClose = { onEvent(FinancialsEvent.DismissModal) },
        modifier = modifier,
        enabled = !state.isSubmitting,
    ) {
        CenterOneChoiceGroup(
            labels = listOf(appString("teacher_payout"), appString("operational")),
            selectedIndex = ExpenseType.entries.indexOf(state.type),
            onSelected = { onEvent(FinancialsEvent.ExpenseTypeSelected(ExpenseType.entries[it])) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.type == ExpenseType.TeacherPayout) {
            CenterOneDropdown(
                value =
                    state.teachers
                        .firstOrNull { it.id == state.selectedTeacherId }
                        ?.name
                        .orEmpty(),
                label = appString("select_teacher"),
                options = state.teachers.map { CenterOneDropdownOption(it.id, it.name) },
                expanded = state.teacherMenuExpanded,
                onExpandedChange = { onEvent(FinancialsEvent.TeacherMenuChanged(it)) },
                onOptionSelected = { onEvent(FinancialsEvent.TeacherSelected(it)) },
                enabled = !state.isSubmitting && state.teachers.isNotEmpty(),
                error = state.teacherError,
                modifier = Modifier.fillMaxWidth(),
            )
            state.teachers.firstOrNull { it.id == state.selectedTeacherId }?.let { teacher ->
                CenterOneInfoPanel(appString("current_dues"), Modifier.fillMaxWidth()) {
                    Text(teacher.formattedDues, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        CenterOneTextField(
            value = state.amount,
            onValueChange = { onEvent(FinancialsEvent.ExpenseAmountChanged(it)) },
            label = { Text(appString("expense_amount")) },
            trailingIcon = { Text(appString("currency")) },
            enabled = !state.isSubmitting,
            readOnly = state.type == ExpenseType.TeacherPayout,
            isError = state.amountError != null,
            supportingText = state.amountError?.let { error -> ({ Text(error) }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        CenterOneTextField(
            value = state.notes,
            onValueChange = { onEvent(FinancialsEvent.ExpenseNotesChanged(it)) },
            label = { Text(appString("notes")) },
            enabled = !state.isSubmitting,
            singleLine = false,
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        CenterOneOutlinedButton(
            onClick = { onEvent(FinancialsEvent.AttachReceipt) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null)
            Text(state.attachmentName ?: appString("attach_receipt"))
        }
        if (state.attachmentName != null) {
            CenterOneTextButton(onClick = {
                onEvent(FinancialsEvent.RemoveAttachment)
            }, enabled = !state.isSubmitting) { Text(appString("remove_attachment")) }
        } else {
            Text(appString("attachment_hint"), style = MaterialTheme.typography.labelSmall)
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOnePrimaryButton(
                onClick = { onEvent(FinancialsEvent.ConfirmExpense) },
                enabled = state.canConfirm && !state.isSubmitting,
            ) {
                Text(appString("confirm_expense"))
            }
            CenterOneTextButton(
                onClick = { onEvent(FinancialsEvent.DismissModal) },
                enabled = !state.isSubmitting,
            ) { Text(appString("cancel")) }
        }
    }
}
