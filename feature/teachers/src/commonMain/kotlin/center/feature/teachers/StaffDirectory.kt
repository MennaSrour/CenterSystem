package center.feature.teachers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import center.ui.components.AttendanceHistory
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.components.CenterOneTextField
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.text.isolateLtr
import kotlinx.datetime.DayOfWeek

data class StaffCardModel(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val isActive: Boolean,
    val schedule: LocalizedText,
)

data class StaffDraft(
    val name: String = "",
    val role: String = "",
    val phone: String = "",
    val isActive: Boolean = true,
    val shifts: List<StaffShiftDraft> = emptyList(),
) {
    val canSave: Boolean get() = name.isNotBlank() && role.isNotBlank() && phone.isNotBlank()
}

data class StaffShiftDraft(
    val day: DayOfWeek = DayOfWeek.SATURDAY,
    val from: String = "",
    val to: String = "",
)

@Composable
internal fun StaffList(
    staff: List<StaffCardModel>,
    onEvent: (TeachersEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (staff.isEmpty()) Text(staffLabel("لا توجد نتائج", "No staff found"), Modifier.padding(horizontal = 24.dp))
    LazyVerticalGrid(
        columns = GridCells.Adaptive(260.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(staff, key = { it.id }) { member ->
            CenterOneCard(Modifier.fillMaxWidth(), onClick = { onEvent(TeachersEvent.OpenStaff(member.id)) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(member.name, style = MaterialTheme.typography.titleLarge)
                    Text(member.role)
                    Text(member.phone.isolateLtr(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!member.isActive) Text(staffLabel("غير نشط", "Inactive"))
                }
            }
        }
    }
}

@Composable
internal fun StaffDetails(
    member: StaffCardModel,
    history: List<LocalizedText>,
    onEvent: (TeachersEvent) -> Unit,
) {
    CenterOneModalContent(
        title = member.name,
        subtitle = member.role,
        closeLabel = staffLabel("إغلاق", "Close"),
        onClose = { onEvent(TeachersEvent.CloseModal) },
    ) {
        Text(staffLabel("الهاتف", "Phone") + " · " + member.phone.isolateLtr())
        Text(if (member.isActive) staffLabel("نشط", "Active") else staffLabel("غير نشط", "Inactive"))
        Text(staffLabel("جدول العمل", "Work schedule"), style = MaterialTheme.typography.titleSmall)
        Text(member.schedule.resolve(LocalAppLanguage.current).ifBlank { staffLabel("لم تحدد ورديات", "No shifts set") })
        AttendanceHistory(history)
        CenterOnePrimaryButton(onClick = { onEvent(TeachersEvent.EditStaff(member.id)) }) {
            Text(staffLabel("تعديل", "Edit"))
        }
    }
}

@Composable
internal fun StaffForm(
    modal: TeachersModal.StaffForm,
    onEvent: (TeachersEvent) -> Unit,
) {
    val draft = modal.draft
    val enabled = !modal.saving

    fun update(value: StaffDraft) = onEvent(TeachersEvent.StaffDraftChanged(value))
    CenterOneModalContent(
        title = if (modal.id == null) staffLabel("إضافة موظف", "Add staff member") else staffLabel("تعديل الموظف", "Edit staff member"),
        subtitle = "",
        closeLabel = staffLabel("إغلاق", "Close"),
        enabled = enabled,
        onClose = { onEvent(TeachersEvent.CloseModal) },
    ) {
        CenterOneTextField(
            draft.name,
            { update(draft.copy(name = it)) },
            Modifier.fillMaxWidth(),
            label = { Text(staffLabel("الاسم", "Name")) },
            enabled = enabled,
        )
        CenterOneTextField(
            draft.role,
            { update(draft.copy(role = it)) },
            Modifier.fillMaxWidth(),
            label = { Text(staffLabel("الوظيفة", "Role")) },
            enabled = enabled,
        )
        CenterOneTextField(
            draft.phone,
            { update(draft.copy(phone = it)) },
            Modifier.fillMaxWidth(),
            label = { Text(staffLabel("الهاتف", "Phone")) },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(draft.isActive, { update(draft.copy(isActive = it)) }, enabled = enabled)
            Text(staffLabel("نشط", "Active"))
        }
        Text(staffLabel("جدول العمل", "Work schedule"), style = MaterialTheme.typography.titleSmall)
        draft.shifts.forEachIndexed { index, shift ->
            StaffShiftEditor(
                shift = shift,
                enabled = enabled,
                onChange = { updated ->
                    update(draft.copy(shifts = draft.shifts.mapIndexed { i, old -> if (i == index) updated else old }))
                },
                remove = { update(draft.copy(shifts = draft.shifts.filterIndexed { i, _ -> i != index })) },
            )
        }
        CenterOneTextButton(onClick = { update(draft.copy(shifts = draft.shifts + StaffShiftDraft())) }, enabled = enabled) {
            Text(staffLabel("إضافة وردية", "Add shift"))
        }
        modal.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        CenterOnePrimaryButton(onClick = { onEvent(TeachersEvent.SaveStaff) }, enabled = enabled && draft.canSave) {
            Text(staffLabel("حفظ", "Save"))
        }
    }
}

@Composable
private fun StaffShiftEditor(
    shift: StaffShiftDraft,
    enabled: Boolean,
    onChange: (StaffShiftDraft) -> Unit,
    remove: () -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }
    val labels = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    val options =
        DayOfWeek.entries.map { day ->
            CenterOneDropdownOption(day.name, staffLabel(labels[day.ordinal], day.name.lowercase().replaceFirstChar(Char::uppercase)))
        }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CenterOneDropdown(
            value = options[shift.day.ordinal].label,
            label = staffLabel("اليوم", "Day"),
            options = options,
            expanded = expanded.value,
            onExpandedChange = { expanded.value = it },
            onOptionSelected = {
                expanded.value = false
                onChange(shift.copy(day = DayOfWeek.valueOf(it)))
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CenterOneTextField(
                shift.from,
                { onChange(shift.copy(from = it)) },
                Modifier.weight(1f),
                label = { Text(staffLabel("من (HH:mm)", "From (HH:mm)")) },
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            )
            CenterOneTextField(
                shift.to,
                { onChange(shift.copy(to = it)) },
                Modifier.weight(1f),
                label = { Text(staffLabel("إلى (HH:mm)", "To (HH:mm)")) },
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            )
            CenterOneTextButton(onClick = remove, enabled = enabled) { Text(staffLabel("إزالة", "Remove")) }
        }
    }
}

@Composable
internal fun staffLabel(
    arabic: String,
    english: String,
): String = LocalizedText(arabic, english).resolve(LocalAppLanguage.current)
