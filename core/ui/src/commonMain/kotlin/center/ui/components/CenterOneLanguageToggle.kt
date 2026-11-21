package center.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import center.ui.localization.AppLanguage
import center.ui.theme.CenterOneSize

/** Language names remain recognizable in either locale; selection belongs to the caller. */
@Composable
fun CenterOneLanguageToggle(
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    key(language) {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline),
        ) {
            Row(Modifier.selectableGroup().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppLanguage.entries.forEach { option ->
                    val selected = option == language
                    Box(
                        Modifier
                            .heightIn(min = CenterOneSize.ButtonHeight)
                            .clip(CircleShape)
                            .background(if (selected) colors.primaryContainer else colors.surface)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onLanguageSelected(option) })
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (option == AppLanguage.Arabic) "عربي" else "English",
                            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
