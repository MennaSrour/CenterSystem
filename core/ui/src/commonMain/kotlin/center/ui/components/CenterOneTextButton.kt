package center.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import center.ui.theme.CenterOneSize
import center.ui.theme.accentContentColor

@Composable
fun CenterOneTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary.accentContentColor(),
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick,
        modifier.heightIn(min = CenterOneSize.ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        content = content,
    )
}
