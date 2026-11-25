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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneConfirmationContent
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOneLanguageToggle
import center.ui.components.CenterOneModal
import center.ui.components.CenterOnePageHeader
import center.ui.components.CenterOnePillTabs
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTag
import center.ui.components.CenterOneTextField
import center.ui.localization.AppLanguage
import center.ui.localization.CenterOneLocalization
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.theme.CenterOneSpacing
import center.ui.theme.ModalScrim
import center.ui.theme.Rose

@Composable
fun StudentsScreen(
    state: StudentsState,
    onEvent: (StudentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterOneLocalization(LocalAppLanguage.current) {
        Box(modifier.fillMaxSize()) {
            StudentsContent(state, onEvent)
            state.details?.let { details ->
                StudentDetails(
                    state = details,
                    onClose = { onEvent(StudentsEvent.CloseDetails) },
                    onEdit = { onEvent(StudentsEvent.EditStudent(details.id)) },
                    onEditEnrollment = { groupId -> onEvent(StudentsEvent.EditEnrollmentPrice(details.id, groupId)) },
                    onRemoveEnrollment = { groupId -> onEvent(StudentsEvent.RemoveEnrollment(details.id, groupId)) },
                )
            }
            state.addStudent?.let { form ->
                Box(Modifier.matchParentSize().background(ModalScrim))
                AddStudentModal(form, onEvent)
            }
            state.deleteStudent?.let { deletion ->
                Box(Modifier.matchParentSize().background(ModalScrim))
                val language = LocalAppLanguage.current
                CenterOneModal(onDismissRequest = { if (!deletion.busy) onEvent(StudentsEvent.DismissDelete) }) {
                    CenterOneLocalization(language) {
                        CenterOneConfirmationContent(
                            title = LocalizedText("حذف الطالب", "Delete student").resolve(language),
                            message = deletion.name,
                            confirmLabel = LocalizedText("حذف", "Delete").resolve(language),
                            cancelLabel = LocalizedText("إلغاء", "Cancel").resolve(language),
                            onConfirm = { onEvent(StudentsEvent.ConfirmDelete) },
                            onCancel = { onEvent(StudentsEvent.DismissDelete) },
                            icon = {},
                            enabled = !deletion.busy,
                            error = deletion.message,
                        )
                    }
                }
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
    CenterOneCanvas(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentWidth = maxOf(maxWidth, 1180.dp)
            val sidebarWidth = if (contentWidth >= 1600.dp) 300.dp else 250.dp
            val direction = LocalLayoutDirection.current
            val sidebar: @Composable () -> Unit = { StudentsSidebar(onEvent, Modifier.width(sidebarWidth)) }
            Row(Modifier.horizontalScroll(rememberScrollState()).width(contentWidth)) {
                if (direction == LayoutDirection.Ltr) sidebar()
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
                                    appString("student_affairs"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Text(appString("directory"), style = MaterialTheme.typography.headlineLarge)
                                Text(
                                    appString("directory_subtitle"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        item { StudentsStats(state.summary, Modifier.fillMaxWidth()) }
                        item {
                            val overdue = state.stageFilter == StudentStageFilter.Overdue
                            val overdueRows = if (overdue) state.visibleOverdueRows else emptyList()
                            CenterOneCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.fillMaxWidth().padding(CenterOneSpacing.Large),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    StudentsFilters(state, onEvent)
                                    if (overdue) {
                                        OverdueActions(state.searchQuery, overdueRows, onEvent, enabled = !state.isReminding)
                                    } else {
                                        StudentsActionBar(state, onEvent)
                                    }
                                }
                                if (overdue) {
                                    OverdueStudentsTable(
                                        overdueRows,
                                        onEvent,
                                        Modifier.fillMaxWidth(),
                                        remindersEnabled = !state.isReminding,
                                    )
                                } else {
                                    val visible = state.visibleStudents
                                    StudentsDataGrid(visible, state.grades, onEvent, Modifier.fillMaxWidth(), canDelete = state.canDelete)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        appString("displayed_count", visible.size.toString()),
                                        Modifier.padding(CenterOneSpacing.Medium),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                if (direction == LayoutDirection.Rtl) sidebar()
            }
        }
    }
}

@Composable
private fun StudentsFilters(
    state: StudentsState,
    onEvent: (StudentsEvent) -> Unit,
) {
    val language = LocalAppLanguage.current
    val summary = state.summary
    val stageLabels =
        if (language == AppLanguage.Arabic) {
            listOf(
                appString("filter_all", summary.total),
                appString("filter_primary", summary.primary),
                appString("filter_prep", summary.prep),
                appString("filter_secondary", summary.secondary),
            )
        } else {
            listOf("All (${summary.total})", "Primary (${summary.primary})", "Prep (${summary.prep})", "Secondary (${summary.secondary})")
        }
    val tabs = StudentStageFilter.entries
    val overdueCount = state.overdueRows.distinctBy { it.student.id }.size
    CenterOnePillTabs(
        labels = stageLabels + OverdueText.Tab.label(language),
        selectedIndex = tabs.indexOf(state.stageFilter),
        onSelected = { onEvent(StudentsEvent.StageSelected(tabs[it])) },
        modifier = Modifier.fillMaxWidth(),
        badge = { index ->
            if (tabs[index] == StudentStageFilter.Overdue) {
                CenterOneTag(overdueCount.toString(), color = Rose)
            }
        },
    )
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
                placeholder = { Text(appString("search"), style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            )
        }
        val filters: @Composable (Modifier) -> Unit = { modifier ->
            Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CenterOneDropdown(
                    value = state.grades.firstOrNull { it.id == state.selectedGradeId }?.label ?: appString("all_grades"),
                    label = appString("grade_filter"),
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
                        appString("all_grades"),
                        { onEvent(StudentsEvent.GradeFilterSelected(null)) },
                    )
                }
                CenterOnePrimaryButton(onClick = { onEvent(StudentsEvent.OpenAddStudent) }) { Text(appString("add_student")) }
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
    CenterOnePageHeader(appString("students_title"), appString("students_title_en"), state.dateLabel) {
        CenterOneLanguageToggle(LocalAppLanguage.current, { onEvent(StudentsEvent.LanguageSelected(it)) })
        CenterOneIconButton(
            Icons.Outlined.Notifications,
            appString("notifications"),
            { onEvent(StudentsEvent.OpenNotifications) },
        )
        Text(state.userName, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleSmall)
    }
}
