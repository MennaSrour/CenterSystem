package center.feature.financials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneInfoPanel
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneSelectionRow
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField

@Composable
fun ReceivePaymentModal(
    state: ReceivePaymentState,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModal(onDismissRequest = { if (!state.isSubmitting) onEvent(FinancialsEvent.DismissModal) }) {
        ReceivePaymentModalContent(state, onEvent, modifier)
    }
}

@Composable
internal fun ReceivePaymentModalContent(
    state: ReceivePaymentState,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneModalContent(
        title = appString("receive_title"),
        subtitle = appString("receive_subtitle"),
        closeLabel = appString("close"),
        onClose = { onEvent(FinancialsEvent.DismissModal) },
        enabled = !state.isSubmitting,
        modifier = modifier,
    ) {
        CenterOneTextField(
            value = state.studentQuery,
            onValueChange = { onEvent(FinancialsEvent.StudentQueryChanged(it)) },
            label = { Text(appString("student_search")) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            enabled = !state.isSubmitting,
            isError = state.studentError != null,
            supportingText = state.studentError?.let { error -> ({ Text(error) }) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.studentResults.isEmpty()) {
            Text(appString("no_students"), style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                items(state.studentResults, key = { it.id }) { student ->
                    CenterOneSelectionRow(
                        title = student.name,
                        subtitle = student.code,
                        selected = student.id == state.selectedStudentId,
                        onClick = { onEvent(FinancialsEvent.StudentSelected(student.id)) },
                        enabled = !state.isSubmitting,
                    )
                }
            }
        }
        CenterOneDropdown(
            value =
                state.classes
                    .firstOrNull { it.id == state.selectedClassId }
                    ?.label
                    .orEmpty(),
            label = appString("select_class"),
            options = state.classes.map { CenterOneDropdownOption(it.id, it.label) },
            expanded = state.classMenuExpanded,
            onExpandedChange = { onEvent(FinancialsEvent.ClassMenuChanged(it)) },
            onOptionSelected = { onEvent(FinancialsEvent.ClassSelected(it)) },
            enabled = !state.isSubmitting && state.selectedStudentId != null && state.classes.isNotEmpty(),
            error = state.classError,
            modifier = Modifier.fillMaxWidth(),
        )
        state.split?.let { FinancialSplitInfo(it, Modifier.fillMaxWidth()) }
        CenterOneTextField(
            value = state.amount,
            onValueChange = { onEvent(FinancialsEvent.PaymentAmountChanged(it)) },
            label = { Text(appString("payment_amount")) },
            trailingIcon = { Text(appString("currency")) },
            enabled = !state.isSubmitting,
            isError = state.amountError != null,
            supportingText = state.amountError?.let { error -> ({ Text(error) }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CenterOnePrimaryButton(
                onClick = { onEvent(FinancialsEvent.ConfirmReceivePayment) },
                enabled =
                    state.canConfirm && !state.isSubmitting,
            ) {
                Text(appString("confirm_payment"))
            }
            CenterOneTextButton(
                onClick = { onEvent(FinancialsEvent.DismissModal) },
                enabled = !state.isSubmitting,
            ) { Text(appString("cancel")) }
        }
    }
}

@Composable
private fun FinancialSplitInfo(
    split: FinancialSplit,
    modifier: Modifier = Modifier,
) {
    CenterOneInfoPanel(appString("split_title"), modifier) {
        Text(split.ratio, style = MaterialTheme.typography.labelSmall)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                appString("total") to split.total,
                appString("teacher_share") to split.teacherShare,
                appString("center_share") to split.centerShare,
            ).forEach { (title, value) ->
                Column {
                    Text(title, style = MaterialTheme.typography.labelSmall)
                    Text(value, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
