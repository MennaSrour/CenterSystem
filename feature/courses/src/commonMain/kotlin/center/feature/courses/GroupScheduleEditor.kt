package center.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOneInfoPanel
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText

@Composable
internal fun GroupScheduleEditor(
    state: GroupFormState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    CenterOneInfoPanel(appString("schedule"), modifier) {
        Text(LocalizedText("أيام الدرس", "Lesson days").resolve(language), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ScheduleDay.entries.forEach { day ->
                FilterChip(
                    selected = state.timeSlots.any { it.day == day },
                    onClick = { onEvent(CoursesEvent.ToggleScheduleDay(day)) },
                    label = { Text(day.label()) },
                    enabled = !state.isSubmitting,
                )
            }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(state.timeSlots, key = { _, slot -> slot.id }) { index, slot ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(slot.day?.label().orEmpty(), Modifier.weight(0.7f), style = MaterialTheme.typography.labelLarge)
                    TimePickerField(
                        slot.from,
                        appString("from_time"),
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f),
                        onSelected = { onEvent(CoursesEvent.FromTimeChanged(slot.id, it)) },
                    )
                    TimePickerField(
                        slot.to,
                        appString("to_time"),
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f),
                        onSelected = { onEvent(CoursesEvent.ToTimeChanged(slot.id, it)) },
                    )
                    CenterOneIconButton(
                        Icons.Outlined.Close,
                        appString("remove_slot", (index + 1).toString()),
                        { slot.day?.let { onEvent(CoursesEvent.ToggleScheduleDay(it)) } },
                        enabled = !state.isSubmitting,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                slot.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    value: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit,
) {
    val language = LocalAppLanguage.current
    var showPicker by remember { mutableStateOf(false) }
    val (hour, minute) =
        value.split(':').takeIf { it.size == 2 }?.let {
            (it[0].toIntOrNull() ?: 9) to (it[1].toIntOrNull() ?: 0)
        } ?: (9 to 0)
    OutlinedButton(
        onClick = { showPicker = true },
        enabled = enabled,
        modifier = modifier.semantics { contentDescription = label },
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                value.takeIf { it.isNotBlank() }?.let { it.toPickerLabel(language) } ?: "09:00 AM",
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
            )
        }
    }
    if (showPicker) {
        val picker = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = { TimePicker(state = picker, modifier = Modifier.padding(8.dp)) },
            confirmButton = {
                TextButton(onClick = {
                    onSelected("${picker.hour.toString().padStart(2, '0')}:${picker.minute.toString().padStart(2, '0')}")
                    showPicker = false
                }) { Text(LocalizedText("اختيار", "Select").resolve(language)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(appString("cancel")) }
            },
        )
    }
}

private fun String.toPickerLabel(language: AppLanguage): String {
    val parts = split(':')
    val hour = parts.firstOrNull()?.toIntOrNull() ?: return this
    val minute = parts.getOrNull(1)?.take(2) ?: "00"
    val afternoon = hour >= 12
    val twelveHour = (hour % 12).let { if (it == 0) 12 else it }
    val suffix =
        when (language) {
            AppLanguage.Arabic -> if (afternoon) "م" else "ص"
            AppLanguage.English -> if (afternoon) "PM" else "AM"
        }
    return "${twelveHour.toString().padStart(2, '0')}:$minute $suffix"
}
