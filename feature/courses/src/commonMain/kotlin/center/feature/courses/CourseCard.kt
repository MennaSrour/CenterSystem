package center.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import center.feature.courses.resources.Res
import center.feature.courses.resources.capacity
import center.feature.courses.resources.deactivate_group
import center.feature.courses.resources.delete_group
import center.feature.courses.resources.edit_group
import center.feature.courses.resources.inactive
import center.feature.courses.resources.menu_for
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneCapacityMeter
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOneTag
import center.ui.text.isolateLtr
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseCard(
    group: CourseGroup,
    teacherName: String,
    gradeLabel: String,
    menuExpanded: Boolean,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (group.isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    CenterOneCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // The first item is physically right in RTL, as required for the kebab anchor.
                Box {
                    CenterOneIconButton(Icons.Outlined.MoreVert, stringResource(Res.string.menu_for, group.subject), {
                        onEvent(CoursesEvent.GroupMenuChanged(if (menuExpanded) null else group.id))
                    })
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { onEvent(CoursesEvent.GroupMenuChanged(null)) }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit_group)) },
                            onClick = { onEvent(CoursesEvent.EditGroup(group.id)) },
                        )
                        DropdownMenuItem(text = {
                            Text(stringResource(Res.string.deactivate_group))
                        }, onClick = { onEvent(CoursesEvent.DeactivateGroup(group.id)) }, enabled = group.isActive)
                        DropdownMenuItem(text = {
                            Text(stringResource(Res.string.delete_group), color = MaterialTheme.colorScheme.tertiary)
                        }, onClick = { onEvent(CoursesEvent.RequestDeleteGroup(group.id)) })
                    }
                }
                Spacer(Modifier.weight(1f))
                if (!group.isActive) CenterOneTag(stringResource(Res.string.inactive), color = accent)
            }
            Text(group.subject, style = MaterialTheme.typography.titleLarge)
            Text(gradeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CenterOneAvatar(teacherName.removePrefix("أ. ").take(1))
                Text(teacherName, style = MaterialTheme.typography.titleSmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.schedule.forEach { slot ->
                    val timeRange = "${slot.from}–${slot.to}".isolateLtr()
                    CenterOneTag("${slot.day.label()} $timeRange")
                }
            }
            CenterOneCapacityMeter(
                stringResource(Res.string.capacity, "${group.enrolled} / ${group.maxCapacity}".isolateLtr()),
                group.capacityProgress,
                Modifier.fillMaxWidth(),
                accent,
            )
        }
    }
}
