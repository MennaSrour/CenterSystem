package center.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import center.ui.theme.ACCENT_TINT_ALPHA

@Composable
fun CenterOneTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return
    require(selectedIndex in labels.indices)
    TabRow(selectedTabIndex = selectedIndex, modifier = modifier, containerColor = MaterialTheme.colorScheme.surface) {
        labels.forEachIndexed { index, label ->
            Tab(
                modifier =
                    if (selectedIndex == index) {
                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = ACCENT_TINT_ALPHA))
                    } else {
                        Modifier
                    },
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                text = { Text(label) },
                selectedContentColor = MaterialTheme.colorScheme.secondary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
