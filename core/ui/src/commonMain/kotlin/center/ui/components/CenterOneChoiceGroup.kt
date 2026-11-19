package center.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import center.ui.theme.CenterOneSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterOneChoiceGroup(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (labels.isEmpty()) return
    require(selectedIndex in labels.indices)
    SingleChoiceSegmentedButtonRow(modifier) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                enabled = enabled,
                modifier = Modifier.heightIn(min = CenterOneSize.ButtonHeight),
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
            ) { Text(label) }
        }
    }
}
