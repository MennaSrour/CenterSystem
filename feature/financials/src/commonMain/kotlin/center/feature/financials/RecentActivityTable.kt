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
import center.feature.financials.resources.Res
import center.feature.financials.resources.amount
import center.feature.financials.resources.category
import center.feature.financials.resources.date
import center.feature.financials.resources.empty_records
import center.feature.financials.resources.party
import center.feature.financials.resources.time
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneTable
import center.ui.components.CenterOneTableColumn
import center.ui.theme.Pink
import center.ui.theme.Success
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecentActivityTable(
    records: List<FinancialRecord>,
    log: FinancialLog,
    modifier: Modifier = Modifier,
) {
    val columns =
        listOf(
            CenterOneTableColumn(stringResource(Res.string.party), 2f),
            CenterOneTableColumn(stringResource(Res.string.category), 1.5f),
            CenterOneTableColumn(stringResource(Res.string.amount)),
            CenterOneTableColumn(stringResource(Res.string.date)),
            CenterOneTableColumn(stringResource(Res.string.time), 0.7f),
        )
    CenterOneTable(columns, records, rowKey = {
        it.id
    }, emptyLabel = stringResource(Res.string.empty_records), modifier = modifier) { record, column ->
        when (column) {
            0 ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CenterOneAvatar(record.partyName.take(1))
                    Column {
                        Text(record.partyName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            record.englishName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            1 -> Text(record.category, style = MaterialTheme.typography.bodySmall)
            2 ->
                Text(
                    record.formattedAmount,
                    style = MaterialTheme.typography.titleSmall,
                    color =
                        if (log ==
                            FinancialLog.Income
                        ) {
                            Success
                        } else {
                            Pink
                        },
                )
            3 -> Text(record.formattedDate, style = MaterialTheme.typography.bodySmall)
            4 -> Text(record.formattedTime, style = MaterialTheme.typography.bodySmall)
        }
    }
}
