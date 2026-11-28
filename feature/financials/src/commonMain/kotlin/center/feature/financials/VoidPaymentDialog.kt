package center.feature.financials

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOnePrimaryButton
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage

@Composable
internal fun VoidPaymentDialog(
    state: FinancialsModal.VoidPayment,
    onEvent: (FinancialsEvent) -> Unit,
) {
    val language = LocalAppLanguage.current
    AlertDialog(
        onDismissRequest = { if (!state.isSubmitting) onEvent(FinancialsEvent.DismissModal) },
        title = { Text(AppStrings.get(AppStringKey.ConfirmVoidPayment, language)) },
        text = {
            OutlinedTextField(
                value = state.reason,
                onValueChange = { onEvent(FinancialsEvent.VoidReasonChanged(it)) },
                label = { Text(AppStrings.get(AppStringKey.VoidReason, language)) },
                enabled = !state.isSubmitting,
            )
        },
        confirmButton = {
            CenterOnePrimaryButton(
                onClick = { onEvent(FinancialsEvent.ConfirmVoidPayment) },
                enabled = !state.isSubmitting,
            ) {
                Text(AppStrings.get(AppStringKey.VoidPayment, language))
            }
        },
        dismissButton = {
            CenterOneOutlinedButton(
                onClick = { onEvent(FinancialsEvent.DismissModal) },
                enabled = !state.isSubmitting,
            ) {
                Text(AppStrings.get(AppStringKey.Cancel, language))
            }
        },
    )
}
