package center.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.feature.courses.resources.Res
import center.feature.courses.resources.add_slot
import center.feature.courses.resources.day
import center.feature.courses.resources.from_time
import center.feature.courses.resources.remove_slot
import center.feature.courses.resources.schedule
import center.feature.courses.resources.time_hint
import center.feature.courses.resources.to_time
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOneInfoPanel
import center.ui.components.CenterOneOutlinedButton
import center.ui.components.CenterOneTextField
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupScheduleEditor(
    state: GroupFormState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneInfoPanel(stringResource(Res.string.schedule), modifier) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(state.timeSlots, key = { _, slot -> slot.id }) { index, slot ->
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 600.dp
                    val day: @Composable (Modifier) -> Unit = { fieldModifier ->
                        CenterOneDropdown(
                            value = slot.day?.label().orEmpty(),
                            label = stringResource(Res.string.day),
                            options = ScheduleDay.entries.map { CenterOneDropdownOption(it.name, it.label()) },
                            expanded =
                                state.expandedDaySlotId == slot.id,
                            onExpandedChange = { onEvent(CoursesEvent.DayMenuChanged(if (it) slot.id else null)) },
                            onOptionSelected = { id ->
                                ScheduleDay.entries.firstOrNull { it.name == id }?.let { onEvent(CoursesEvent.DaySelected(slot.id, it)) }
                            },
                            enabled = !state.isSubmitting,
                            modifier = fieldModifier,
                        )
                    }
                    val times: @Composable (Modifier) -> Unit = { timesModifier ->
                        Row(timesModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CenterOneTextField(slot.from, {
                                onEvent(CoursesEvent.FromTimeChanged(slot.id, it))
                            }, Modifier.weight(1f), enabled = !state.isSubmitting, label = {
                                Text(stringResource(Res.string.from_time))
                            }, placeholder = {
                                Text(stringResource(Res.string.time_hint))
                            }, textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr))
                            CenterOneTextField(slot.to, {
                                onEvent(CoursesEvent.ToTimeChanged(slot.id, it))
                            }, Modifier.weight(1f), enabled = !state.isSubmitting, label = {
                                Text(stringResource(Res.string.to_time))
                            }, placeholder = {
                                Text(stringResource(Res.string.time_hint))
                            }, textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr))
                            CenterOneIconButton(Icons.Outlined.Close, stringResource(Res.string.remove_slot, (index + 1).toString()), {
                                onEvent(CoursesEvent.RemoveTimeSlot(slot.id))
                            }, enabled = !state.isSubmitting, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (wide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                day(Modifier.weight(1f))
                                times(Modifier.weight(2f))
                            }
                        } else {
                            day(Modifier.fillMaxWidth())
                            times(Modifier.fillMaxWidth())
                        }
                        slot.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        CenterOneOutlinedButton(onClick = {
            onEvent(CoursesEvent.AddTimeSlot)
        }, enabled = !state.isSubmitting, color = MaterialTheme.colorScheme.secondary) { Text(stringResource(Res.string.add_slot)) }
    }
}
