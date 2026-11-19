package center.feature.teachers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneAvatar
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneLanguageToggle
import center.ui.components.CenterOneMainNavigation
import center.ui.components.CenterOneModal
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import center.ui.components.MainNavigation
import center.ui.components.StatusBadge
import center.ui.components.StatusTone
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.localization.LocalAppLanguage

@Composable
fun TeachersScreen(
    state: TeachersState,
    onEvent: (TeachersEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val close = { onEvent(TeachersEvent.CloseModal) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints {
            val sidebarWidth = if (maxWidth < 1000.dp) 192.dp else 232.dp
            val sidebar: @Composable () -> Unit = {
                CenterOneMainNavigation(
                    MainNavigation.Teachers,
                    { onEvent(TeachersEvent.Navigate(it)) },
                    Modifier.width(sidebarWidth),
                )
            }
            Row(Modifier.fillMaxSize()) {
                if (language == AppLanguage.English) sidebar()
                TeacherDirectory(state, onEvent, Modifier.weight(1f))
                if (language == AppLanguage.Arabic) sidebar()
            }
        }
    }
    when (val modal = state.modal) {
        TeachersModal.Closed -> Unit
        is TeachersModal.Details ->
            state.teachers.firstOrNull { it.id == modal.teacherId }?.let { teacher ->
                CenterOneModal(onDismissRequest = close) {
                    CenterOneLocalization(language) { TeacherDetails(teacher, onEvent) }
                }
            }
        is TeachersModal.Add ->
            CenterOneModal(onDismissRequest = close) {
                CenterOneLocalization(language) { AddTeacherForm(modal.draft, onEvent) }
            }
    }
}

@Composable
private fun TeacherDirectory(
    state: TeachersState,
    onEvent: (TeachersEvent) -> Unit,
    modifier: Modifier,
) {
    val language = LocalAppLanguage.current
    val teachers = state.visibleTeachers
    Column(modifier.fillMaxHeight().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(MainNavigation.Teachers.label(language), style = MaterialTheme.typography.headlineLarge)
                Text(
                    teacherString(TeacherText.Demo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CenterOneLanguageToggle(language, { onEvent(TeachersEvent.LanguageSelected(it)) })
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CenterOneTextField(
                value = state.query,
                onValueChange = { onEvent(TeachersEvent.SearchChanged(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(teacherString(TeacherText.Search)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            )
            CenterOnePrimaryButton(onClick = { onEvent(TeachersEvent.OpenAddTeacher) }) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(teacherString(TeacherText.Add), Modifier.padding(start = 6.dp))
            }
        }
        if (teachers.isEmpty()) {
            Text(teacherString(TeacherText.NoResults), Modifier.padding(horizontal = 24.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(teachers, key = { it.id }) { teacher ->
                    TeacherCard(teacher, onClick = { onEvent(TeachersEvent.OpenTeacher(teacher.id)) })
                }
            }
        }
    }
}

@Composable
private fun TeacherCard(
    teacher: Teacher,
    onClick: () -> Unit,
) {
    val language = LocalAppLanguage.current
    val name = teacher.name.resolve(language)
    CenterOneCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CenterOneAvatar(name.take(1))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    Text(teacher.subject.resolve(language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(teacherString(TeacherText.GroupsCount, teacher.groups.size.toString()), style = MaterialTheme.typography.labelLarge)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                teacherString(if (teacher.salaryPaidInDemo) TeacherText.MonthlySalary else TeacherText.SalaryDue),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                teacher.monthlySalaryEgp?.let { formatEgp(it) } ?: teacherString(TeacherText.NoSalary),
                style = MaterialTheme.typography.titleLarge,
            )
            if (teacher.salaryPaidInDemo) StatusBadge(teacherString(TeacherText.DemoPaid), StatusTone.Success)
            if (teacher.unpaidStudents.isNotEmpty()) StatusBadge(deficitBadge(teacher.unpaidStudents.size), StatusTone.Pending)
        }
    }
}
