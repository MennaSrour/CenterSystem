package center.feature.students

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import center.feature.students.resources.Res
import center.feature.students.resources.add_student
import center.feature.students.resources.all_grades
import center.feature.students.resources.arabic
import center.feature.students.resources.directory
import center.feature.students.resources.directory_subtitle
import center.feature.students.resources.displayed_count
import center.feature.students.resources.english
import center.feature.students.resources.filter_all
import center.feature.students.resources.filter_prep
import center.feature.students.resources.filter_primary
import center.feature.students.resources.filter_secondary
import center.feature.students.resources.grade_filter
import center.feature.students.resources.notifications
import center.feature.students.resources.search
import center.feature.students.resources.student_affairs
import center.feature.students.resources.students_title
import center.feature.students.resources.students_title_en
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOnePageHeader
import center.ui.components.CenterOnePillTabs
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Navy
import org.jetbrains.compose.resources.stringResource

@Composable
fun StudentsScreen(
    state: StudentsState,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier.fillMaxSize()) {
            StudentsContent(state, onEvent)
            state.addStudent?.let { form ->
                Box(Modifier.matchParentSize().background(Navy.copy(alpha = 0.55f)))
                AddStudentModal(form, onEvent)
            }
        }
    }
}

@Composable
internal fun StudentsContent(
    state: StudentsState,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentWidth = maxOf(maxWidth, 1180.dp)
            val sidebarWidth = if (contentWidth >= 1600.dp) 300.dp else 250.dp
            Row(Modifier.horizontalScroll(rememberScrollState()).width(contentWidth)) {
                Column(Modifier.weight(1f)) {
                    StudentsHeader(state, onEvent)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(CenterOneSpacing.Section),
                        verticalArrangement = Arrangement.spacedBy(CenterOneSpacing.Large),
                    ) {
                        item {
                            Column {
                                Text(
                                    stringResource(Res.string.student_affairs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Text(stringResource(Res.string.directory), style = MaterialTheme.typography.headlineLarge)
                                Text(
                                    stringResource(Res.string.directory_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        item { StudentsStats(state.summary, Modifier.fillMaxWidth()) }
                        item {
                            CenterOneCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.fillMaxWidth().padding(CenterOneSpacing.Large),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    CenterOnePillTabs(
                                        labels =
                                            listOf(
                                                stringResource(Res.string.filter_all, state.summary.total),
                                                stringResource(Res.string.filter_primary, state.summary.primary),
                                                stringResource(Res.string.filter_prep, state.summary.prep),
                                                stringResource(Res.string.filter_secondary, state.summary.secondary),
                                            ),
                                        selectedIndex = StudentStageFilter.entries.indexOf(state.stageFilter),
                                        onSelected = { onEvent(StudentsEvent.StageSelected(StudentStageFilter.entries[it])) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    StudentsActionBar(state, onEvent)
                                }
                                StudentsDataGrid(state.visibleStudents, state.grades, onEvent, Modifier.fillMaxWidth())
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                Text(
                                    stringResource(Res.string.displayed_count, state.visibleStudents.size.toString()),
                                    Modifier.padding(CenterOneSpacing.Medium),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                StudentsSidebar(onEvent = onEvent, modifier = Modifier.width(sidebarWidth))
            }
        }
    }
}

@Composable
private fun StudentsActionBar(
    state: StudentsState,
    onEvent: (StudentsEvent) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 900.dp
        val search: @Composable (Modifier) -> Unit = { modifier ->
            CenterOneTextField(
                state.searchQuery,
                { onEvent(StudentsEvent.SearchChanged(it)) },
                modifier,
                placeholder = { Text(stringResource(Res.string.search), style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            )
        }
        val filters: @Composable (Modifier) -> Unit = { modifier ->
            Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CenterOneDropdown(
                    value = state.grades.firstOrNull { it.id == state.selectedGradeId }?.label ?: stringResource(Res.string.all_grades),
                    label = stringResource(Res.string.grade_filter),
                    options = state.availableGrades.map { CenterOneDropdownOption(it.id, it.label) },
                    expanded = state.gradeMenuExpanded,
                    onExpandedChange = { onEvent(StudentsEvent.GradeFilterMenuChanged(it)) },
                    onOptionSelected = { onEvent(StudentsEvent.GradeFilterSelected(it)) },
                    modifier = Modifier.width(220.dp),
                    enabled = state.availableGrades.isNotEmpty(),
                )
                if (state.selectedGradeId != null) {
                    CenterOneIconButton(
                        Icons.Outlined.Close,
                        stringResource(Res.string.all_grades),
                        { onEvent(StudentsEvent.GradeFilterSelected(null)) },
                    )
                }
                CenterOnePrimaryButton(onClick = { onEvent(StudentsEvent.OpenAddStudent) }) { Text(stringResource(Res.string.add_student)) }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                search(Modifier.weight(1f))
                filters(Modifier)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                search(Modifier.fillMaxWidth())
                filters(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StudentsHeader(
    state: StudentsState,
    onEvent: (StudentsEvent) -> Unit,
) {
    CenterOnePageHeader(stringResource(Res.string.students_title), stringResource(Res.string.students_title_en), state.dateLabel) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Text(stringResource(Res.string.arabic), Modifier.padding(12.dp), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = { onEvent(StudentsEvent.RequestEnglish) }) { Text(stringResource(Res.string.english)) }
        CenterOneIconButton(
            Icons.Outlined.Notifications,
            stringResource(Res.string.notifications),
            { onEvent(StudentsEvent.OpenNotifications) },
        )
        Text(state.userName, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleSmall)
    }
}
