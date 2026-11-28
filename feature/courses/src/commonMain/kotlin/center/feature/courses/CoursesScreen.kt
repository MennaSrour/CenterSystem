package center.feature.courses

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneCanvas
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneIconButton
import center.ui.components.CenterOneLanguageToggle
import center.ui.components.CenterOneMetricCard
import center.ui.components.CenterOnePageHeader
import center.ui.components.CenterOnePillTabs
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextField
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.theme.CenterOneBrushes
import center.ui.theme.CenterOneSpacing
import center.ui.theme.Indigo
import center.ui.theme.ModalScrim

@Composable
fun CoursesScreen(
    state: CoursesState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val direction = LocalAppLanguage.current.layoutDirection
    val timetableOpen = remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(modifier.fillMaxSize()) {
            CoursesContent(state, onEvent, onOpenTimetable = { timetableOpen.value = true })
            if (state.modal != CoursesModal.Closed || timetableOpen.value) Box(Modifier.matchParentSize().background(ModalScrim))
            when (val modal = state.modal) {
                CoursesModal.Closed -> Unit
                is CoursesModal.Form -> GroupFormModal(modal.state, onEvent)
                is CoursesModal.Delete -> DeleteConfirmationModal(modal.state, onEvent)
                is CoursesModal.Makeup -> MakeupSessionModal(modal.state, onEvent)
            }
            if (timetableOpen.value) WeeklyTimetableDialog(state, onEvent, onClose = { timetableOpen.value = false })
        }
    }
}

@Composable
internal fun CoursesContent(
    state: CoursesState,
    onEvent: (CoursesEvent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenTimetable: () -> Unit = {},
) {
    val teacherById = state.teachers.associateBy { it.id }
    val gradeById = state.grades.associateBy { it.id }
    val groups = state.visibleGroups
    CenterOneCanvas(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val canvasWidth = maxOf(maxWidth, 1000.dp)
            val sidebarWidth = if (canvasWidth >= 1600.dp) 300.dp else 250.dp
            Row(Modifier.horizontalScroll(rememberScrollState()).width(canvasWidth)) {
                if (LocalAppLanguage.current == AppLanguage.English) CoursesSidebar(onEvent, Modifier.width(sidebarWidth))
                Column(Modifier.weight(1f)) {
                    CoursesHeader(state, onEvent, onOpenTimetable)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(380.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(CenterOneSpacing.Section),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                Text(
                                    appString("section"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Text(appString("groups_title"), style = MaterialTheme.typography.headlineLarge)
                                Text(
                                    appString("subtitle"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        item(key = "statistics", span = { GridItemSpan(maxLineSpan) }) { CoursesStats(state.summary) }
                        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                            CenterOneCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    CenterOnePillTabs(
                                        listOf(
                                            appString("all"),
                                            appString("primary"),
                                            appString("prep"),
                                            appString("secondary"),
                                        ),
                                        CourseStageFilter.entries.indexOf(state.stageFilter),
                                        {
                                            onEvent(CoursesEvent.StageSelected(CourseStageFilter.entries[it]))
                                        },
                                        Modifier.fillMaxWidth(),
                                    )
                                    CourseFilters(state, onEvent)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CenterOneTextField(state.searchQuery, {
                                            onEvent(CoursesEvent.SearchChanged(it))
                                        }, Modifier.weight(1f), placeholder = {
                                            Text(appString("search"))
                                        }, leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) })
                                        CenterOnePrimaryButton(
                                            onClick = { onEvent(CoursesEvent.OpenAddGroup) },
                                        ) { Text(appString("add_group")) }
                                    }
                                }
                            }
                        }
                        if (groups.isEmpty()) {
                            item(key = "empty", span = {
                                GridItemSpan(maxLineSpan)
                            }) { Text(appString("empty"), Modifier.padding(24.dp)) }
                        }
                        items(groups, key = { "group:${it.id}" }) { group ->
                            CourseCard(
                                group = group,
                                teacherName = teacherById[group.teacherId]?.name ?: appString("teacher_missing"),
                                gradeLabel = gradeById[group.gradeId]?.label ?: appString("grade_missing"),
                                menuExpanded = state.expandedGroupMenuId == group.id,
                                canDelete = state.canDelete,
                                onEvent = onEvent,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (LocalAppLanguage.current == AppLanguage.Arabic) CoursesSidebar(onEvent, Modifier.width(sidebarWidth))
            }
        }
    }
}

@Composable
private fun CoursesStats(summary: CoursesSummary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(
            Triple("total_groups", summary.totalGroups, Icons.Outlined.Layers),
            Triple("active_groups", summary.activeGroups, Icons.Outlined.CheckCircleOutline),
            Triple("total_teachers", summary.totalTeachers, Icons.Outlined.PeopleOutline),
        ).forEach { (title, value, icon) ->
            CenterOneMetricCard(
                title = appString(title),
                value = value,
                caption = appString("term"),
                icon = { Icon(icon, contentDescription = null) },
                modifier = Modifier.weight(1f),
                accent = Indigo,
                gradient = CenterOneBrushes.Treasury.takeIf { title == "total_groups" },
            )
        }
    }
}

@Composable
private fun CoursesHeader(
    state: CoursesState,
    onEvent: (CoursesEvent) -> Unit,
    onOpenTimetable: () -> Unit,
) {
    CenterOnePageHeader(appString("courses_title"), appString("courses_title_en"), state.dateLabel) {
        center.ui.components.CenterOnePrimaryButton(onClick = onOpenTimetable) {
            Text(
                center.ui.localization.AppStrings
                    .get(center.ui.localization.AppStringKey.WeeklyTimetable, LocalAppLanguage.current),
            )
        }
        CenterOneLanguageToggle(LocalAppLanguage.current, { onEvent(CoursesEvent.LanguageSelected(it)) })
        CenterOneIconButton(
            Icons.Outlined.Notifications,
            appString("notifications"),
            { onEvent(CoursesEvent.OpenNotifications) },
        )
        Text(state.userName, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun CourseFilters(
    state: CoursesState,
    onEvent: (CoursesEvent) -> Unit,
) {
    val language = LocalAppLanguage.current
    val all = LocalizedText("الكل", "All").resolve(language)
    val teacherMenu = remember { mutableStateOf(false) }
    val gradeMenu = remember { mutableStateOf(false) }
    val teachers = listOf(CenterOneDropdownOption("", all)) + state.teachers.map { CenterOneDropdownOption(it.id, it.name) }
    val grades = listOf(CenterOneDropdownOption("", all)) + state.grades.map { CenterOneDropdownOption(it.id, it.label) }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CenterOneDropdown(
            value = teachers.firstOrNull { it.id == state.selectedTeacherId }?.label ?: all,
            label = LocalizedText("المدرس", "Teacher").resolve(language),
            options = teachers,
            expanded = teacherMenu.value,
            onExpandedChange = { teacherMenu.value = it },
            onOptionSelected = {
                teacherMenu.value = false
                onEvent(CoursesEvent.TeacherFilterSelected(it.ifEmpty { null }))
            },
            modifier = Modifier.weight(1f),
        )
        CenterOneDropdown(
            value = grades.firstOrNull { it.id == state.selectedGradeId }?.label ?: all,
            label = LocalizedText("الصف", "Grade").resolve(language),
            options = grades,
            expanded = gradeMenu.value,
            onExpandedChange = { gradeMenu.value = it },
            onOptionSelected = {
                gradeMenu.value = false
                onEvent(CoursesEvent.GradeFilterSelected(it.ifEmpty { null }))
            },
            modifier = Modifier.weight(1f),
        )
    }
}
