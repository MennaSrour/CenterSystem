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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import center.feature.financials.resources.Res
import center.feature.financials.resources.attach_receipt
import center.feature.financials.resources.attachment_hint
import center.feature.financials.resources.cancel
import center.feature.financials.resources.close
import center.feature.financials.resources.confirm_expense
import center.feature.financials.resources.currency
import center.feature.financials.resources.current_dues
import center.feature.financials.resources.expense_amount
import center.feature.financials.resources.expense_subtitle
import center.feature.financials.resources.expense_title
import center.feature.financials.resources.notes
import center.feature.financials.resources.operational
import center.feature.financials.resources.remove_attachment
import center.feature.financials.resources.select_teacher
import center.feature.financials.resources.teacher_payout
import center.ui.components.CenterOneChoiceGroup
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneInfoPanel
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import org.jetbrains.compose.resources.stringResource

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
        title = stringResource(Res.string.expense_title),
        subtitle = stringResource(Res.string.expense_subtitle),
        closeLabel = stringResource(Res.string.close),
        onClose = { onEvent(FinancialsEvent.DismissModal) },
        modifier = modifier,
        enabled = !state.isSubmitting,
    ) {
        CenterOneChoiceGroup(
            labels = listOf(stringResource(Res.string.teacher_payout), stringResource(Res.string.operational)),
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
                label = stringResource(Res.string.select_teacher),
                options = state.teachers.map { CenterOneDropdownOption(it.id, it.name) },
                expanded = state.teacherMenuExpanded,
                onExpandedChange = { onEvent(FinancialsEvent.TeacherMenuChanged(it)) },
                onOptionSelected = { onEvent(FinancialsEvent.TeacherSelected(it)) },
                enabled = !state.isSubmitting && state.teachers.isNotEmpty(),
                error = state.teacherError,
                modifier = Modifier.fillMaxWidth(),
            )
            state.teachers.firstOrNull { it.id == state.selectedTeacherId }?.let { teacher ->
                CenterOneInfoPanel(stringResource(Res.string.current_dues), Modifier.fillMaxWidth()) {
                    Text(teacher.formattedDues, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        CenterOneTextField(
            value = state.amount,
            onValueChange = { onEvent(FinancialsEvent.ExpenseAmountChanged(it)) },
            label = { Text(stringResource(Res.string.expense_amount)) },
            trailingIcon = { Text(stringResource(Res.string.currency)) },
            enabled = !state.isSubmitting,
            isError = state.amountError != null,
            supportingText = state.amountError?.let { error -> ({ Text(error) }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        CenterOneTextField(
            value = state.notes,
            onValueChange = { onEvent(FinancialsEvent.ExpenseNotesChanged(it)) },
            label = { Text(stringResource(Res.string.notes)) },
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
            Text(state.attachmentName ?: stringResource(Res.string.attach_receipt))
        }
        if (state.attachmentName != null) {
            TextButton(onClick = {
                onEvent(FinancialsEvent.RemoveAttachment)
            }, enabled = !state.isSubmitting) { Text(stringResource(Res.string.remove_attachment)) }
        } else {
            Text(stringResource(Res.string.attachment_hint), style = MaterialTheme.typography.labelSmall)
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOnePrimaryButton(
                onClick = { onEvent(FinancialsEvent.ConfirmExpense) },
                enabled = state.canConfirm && !state.isSubmitting,
            ) {
                Text(stringResource(Res.string.confirm_expense))
            }
            TextButton(
                onClick = { onEvent(FinancialsEvent.DismissModal) },
                enabled = !state.isSubmitting,
            ) { Text(stringResource(Res.string.cancel)) }
        }
    }
}
