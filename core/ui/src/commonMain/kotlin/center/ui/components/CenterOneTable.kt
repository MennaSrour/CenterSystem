package center.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.ui.theme.CenterOneSpacing

data class CenterOneTableColumn(
    val title: String,
    val weight: Float = 1f,
) {
    init {
        require(weight > 0 && weight.isFinite())
    }
}

/** Caller supplies unique stable keys and cell content; the table owns no selection or data. */
@Composable
fun <T> CenterOneTable(
    columns: List<CenterOneTableColumn>,
    rows: List<T>,
    rowKey: (T) -> Any,
    emptyLabel: String,
    modifier: Modifier = Modifier,
    cell: @Composable (T, Int) -> Unit,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(CenterOneSpacing.Medium)) {
            columns.forEach { column ->
                Text(
                    column.title,
                    Modifier.weight(column.weight),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (rows.isEmpty()) {
            Text(emptyLabel, Modifier.padding(CenterOneSpacing.Large))
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(rows, key = rowKey) { row ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Row(Modifier.fillMaxWidth().padding(CenterOneSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                        columns.forEachIndexed { index, column ->
                            Box(Modifier.weight(column.weight).padding(end = 8.dp)) { cell(row, index) }
                        }
                    }
                }
            }
        }
    }
}
