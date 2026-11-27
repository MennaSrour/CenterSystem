package center.feature.financials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneTable
import center.ui.components.CenterOneTableColumn
import center.ui.components.CenterOneTextButton
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage
import center.ui.theme.Rose
import center.ui.theme.Success
import center.ui.theme.accentContentColor

@Composable
fun RecentActivityTable(
    records: List<FinancialRecord>,
    log: FinancialLog,
    canVoidPayments: Boolean,
    onEvent: (FinancialsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns =
        mutableListOf(
            CenterOneTableColumn(appString("party"), 2f),
            CenterOneTableColumn(appString("category"), 1.5f),
            CenterOneTableColumn(appString("amount")),
            CenterOneTableColumn(appString("date")),
            CenterOneTableColumn(appString("time"), 0.7f),
        )
    if (records.any { it.paymentId != null }) columns += CenterOneTableColumn("", 2.2f)
    CenterOneTable(columns, records, rowKey = {
        it.id
    }, emptyLabel = appString("empty_records"), modifier = modifier) { record, column ->
        when (column) {
            0 ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CenterOneAvatar(record.partyName.take(1))
                    Column {
                        Text(record.partyName, style = MaterialTheme.typography.titleSmall)
                        if (record.englishName.isNotBlank() && record.englishName != record.partyName) {
                            Text(
                                record.englishName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            1 -> Text(record.category, style = MaterialTheme.typography.bodySmall)
            2 ->
                Text(
                    record.formattedAmount,
                    style = MaterialTheme.typography.titleSmall,
                    color = (if (log == FinancialLog.Income) Success else Rose).accentContentColor(),
                )
            3 -> Text(record.formattedDate, style = MaterialTheme.typography.bodySmall)
            4 -> Text(record.formattedTime, style = MaterialTheme.typography.bodySmall)
            5 ->
                record.paymentId?.let { paymentId ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CenterOneTextButton(onClick = { onEvent(FinancialsEvent.PaymentReceiptAction(paymentId, print = false)) }) {
                            Text(AppStrings.get(AppStringKey.ExportPdf, LocalAppLanguage.current))
                        }
                        CenterOneTextButton(onClick = { onEvent(FinancialsEvent.PaymentReceiptAction(paymentId, print = true)) }) {
                            Text(AppStrings.get(AppStringKey.PrintReceipt, LocalAppLanguage.current))
                        }
                        if (canVoidPayments && !record.isVoided) {
                            CenterOneTextButton(onClick = { onEvent(FinancialsEvent.VoidPaymentSelected(paymentId)) }) {
                                Text(AppStrings.get(AppStringKey.VoidPayment, LocalAppLanguage.current))
                            }
                        }
                    }
                }
        }
    }
}
