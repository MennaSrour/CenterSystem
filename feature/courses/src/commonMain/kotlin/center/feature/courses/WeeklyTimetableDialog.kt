package center.feature.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import center.ui.components.CenterOneCard
import center.ui.components.CenterOneDropdown
import center.ui.components.CenterOneDropdownOption
import center.ui.components.CenterOneModal
import center.ui.components.CenterOneModalContent
import center.ui.components.CenterOnePrimaryButton
import center.ui.components.CenterOneTextButton
import center.ui.localization.AppLanguage
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage
import center.ui.theme.CenterOneSpacing

@Composable
internal fun WeeklyTimetableDialog(
    state: CoursesState,
    onEvent: (CoursesEvent) -> Unit,
    onClose: () -> Unit,
) {
    val activeLanguage = LocalAppLanguage.current
    val selectedGrade = remember { mutableStateOf<String?>(null) }
    val selectedTrack = remember { mutableStateOf(TimetableTrack.ALL) }
    val outputLanguage = remember { mutableStateOf(activeLanguage) }
    val gradeMenu = remember { mutableStateOf(false) }
    val trackMenu = remember { mutableStateOf(false) }
    val languageMenu = remember { mutableStateOf(false) }
    val timetable = TimetableBuilder.build(state.groups, state.teachers, selectedGrade.value, selectedTrack.value)
    val activeGradeLabel =
        selectedGrade.value?.let { id -> state.grades.firstOrNull { it.id == id }?.label }
            ?: AppStrings.get(AppStringKey.AllGrades, activeLanguage)
    val gradeLabel =
        selectedGrade.value?.let { id ->
            state.grades.firstOrNull { it.id == id }?.let {
                if (outputLanguage.value == AppLanguage.Arabic) it.arabicLabel else it.englishLabel
            }
        } ?: AppStrings.get(AppStringKey.AllGrades, outputLanguage.value)
    val trackLabel = trackLabel(selectedTrack.value, outputLanguage.value)
    CenterOneModal(onDismissRequest = onClose) {
        CenterOneModalContent(
            title = AppStrings.get(AppStringKey.WeeklyTimetable, activeLanguage),
            subtitle = "$gradeLabel • $trackLabel",
            closeLabel = AppStrings.get(AppStringKey.Close, activeLanguage),
            onClose = onClose,
            maxWidth = 1100.dp,
        ) {
            CenterOneDropdown(
                value = activeGradeLabel,
                label = AppStrings.get(AppStringKey.GradeSelector, activeLanguage),
                options =
                    listOf(CenterOneDropdownOption("", AppStrings.get(AppStringKey.AllGrades, activeLanguage))) +
                        state.grades.map { CenterOneDropdownOption(it.id, it.label) },
                expanded = gradeMenu.value,
                onExpandedChange = { gradeMenu.value = it },
                onOptionSelected = {
                    selectedGrade.value = it.ifEmpty { null }
                    gradeMenu.value = false
                },
            )
            CenterOneDropdown(
                value = trackLabel(selectedTrack.value, activeLanguage),
                label = AppStrings.get(AppStringKey.TrackSelector, activeLanguage),
                options =
                    listOf(
                        CenterOneDropdownOption("ALL", AppStrings.get(AppStringKey.AllTracks, activeLanguage)),
                        CenterOneDropdownOption("ARABIC", AppStrings.get(AppStringKey.ArabicTrack, activeLanguage)),
                        CenterOneDropdownOption("LANGUAGES", AppStrings.get(AppStringKey.LanguagesTrack, activeLanguage)),
                    ),
                expanded = trackMenu.value,
                onExpandedChange = { trackMenu.value = it },
                onOptionSelected = {
                    selectedTrack.value = TimetableTrack.valueOf(it)
                    trackMenu.value = false
                },
            )
            CenterOneDropdown(
                value =
                    AppStrings.get(
                        if (outputLanguage.value == AppLanguage.Arabic) AppStringKey.ArabicRtl else AppStringKey.EnglishLtr,
                        activeLanguage,
                    ),
                label = AppStrings.get(AppStringKey.TimetableLanguage, activeLanguage),
                options =
                    listOf(
                        CenterOneDropdownOption("Arabic", AppStrings.get(AppStringKey.ArabicRtl, activeLanguage)),
                        CenterOneDropdownOption("English", AppStrings.get(AppStringKey.EnglishLtr, activeLanguage)),
                    ),
                expanded = languageMenu.value,
                onExpandedChange = { languageMenu.value = it },
                onOptionSelected = {
                    outputLanguage.value = AppLanguage.valueOf(it)
                    languageMenu.value = false
                },
            )
            CompositionLocalProvider(LocalLayoutDirection provides outputLanguage.value.layoutDirection) {
                if (timetable.isEmpty) {
                    CenterOneCard(Modifier.fillMaxWidth()) {
                        Text(
                            AppStrings.get(AppStringKey.TimetableEmpty, outputLanguage.value),
                            Modifier.padding(CenterOneSpacing.Large),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    TimetableGrid(timetable, selectedTrack.value, outputLanguage.value)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CenterOnePrimaryButton(
                    onClick = { onEvent(CoursesEvent.ExportTimetable(selectedGrade.value, selectedTrack.value, outputLanguage.value)) },
                    modifier = Modifier.weight(1f),
                ) { Text(AppStrings.get(AppStringKey.ExportPdf, activeLanguage)) }
                CenterOnePrimaryButton(
                    onClick = { onEvent(CoursesEvent.PrintTimetable(selectedGrade.value, selectedTrack.value, outputLanguage.value)) },
                    modifier = Modifier.weight(1f),
                ) { Text(AppStrings.get(AppStringKey.Print, activeLanguage)) }
            }
            CenterOneTextButton(onClick = onClose) { Text(AppStrings.get(AppStringKey.Close, activeLanguage)) }
        }
    }
}

@Composable
private fun TimetableGrid(
    timetable: WeeklyTimetable,
    track: TimetableTrack,
    language: AppLanguage,
) {
    val dayKeys =
        mapOf(
            ScheduleDay.Saturday to AppStringKey.Saturday,
            ScheduleDay.Sunday to AppStringKey.Sunday,
            ScheduleDay.Monday to AppStringKey.Monday,
            ScheduleDay.Tuesday to AppStringKey.Tuesday,
            ScheduleDay.Wednesday to AppStringKey.Wednesday,
            ScheduleDay.Thursday to AppStringKey.Thursday,
            ScheduleDay.Friday to AppStringKey.Friday,
        )
    val timeColumnWidth = 164.dp
    val dayColumnWidth = 160.dp
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row(Modifier.width(timeColumnWidth + dayColumnWidth * 7), verticalAlignment = Alignment.CenterVertically) {
            Text(
                AppStrings.get(AppStringKey.TimeLabel, language),
                Modifier.width(timeColumnWidth).padding(8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            timetable.days.forEach { day ->
                Text(
                    AppStrings.get(dayKeys.getValue(day), language),
                    Modifier.width(dayColumnWidth).padding(8.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        timetable.periods.forEach { period ->
            Row(
                Modifier.width(timeColumnWidth + dayColumnWidth * 7).padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    Modifier
                        .width(timeColumnWidth)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        AppStrings.get(AppStringKey.PeriodNumber, language, period.number.toString()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        "${formatTime(period.startMinute, language)} – ${formatTime(period.endMinute, language)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                timetable.days.forEach { day ->
                    Column(
                        Modifier.width(dayColumnWidth).padding(horizontal = 3.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        period.lessonsByDay[day].orEmpty().forEach { lesson ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                tonalElevation = 1.dp,
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        lesson.subject,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        lesson.teacher,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (track == TimetableTrack.ALL) {
                                        Text(
                                            lessonTrackLabel(lesson.lessonType, language),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(
    minutes: Int,
    language: AppLanguage,
): String {
    val hour = minutes / 60
    val minute = minutes % 60
    val hour12 = (hour % 12).let { if (it == 0) 12 else it }
    val suffix =
        if (language == AppLanguage.Arabic) {
            if (hour < 12) {
                "ص"
            } else {
                "م"
            }
        } else if (hour < 12) {
            "AM"
        } else {
            "PM"
        }
    return "$hour12:${minute.toString().padStart(2, '0')} $suffix"
}

fun trackLabel(
    track: TimetableTrack,
    language: AppLanguage,
): String =
    when (track) {
        TimetableTrack.ALL -> AppStrings.get(AppStringKey.AllTracks, language)
        TimetableTrack.ARABIC -> AppStrings.get(AppStringKey.ArabicTrack, language)
        TimetableTrack.LANGUAGES -> AppStrings.get(AppStringKey.LanguagesTrack, language)
    }

fun lessonTrackLabel(
    type: CourseLessonType,
    language: AppLanguage,
): String =
    when (type) {
        CourseLessonType.Arabic -> AppStrings.get(AppStringKey.Arabic, language)
        CourseLessonType.Languages -> AppStrings.get(AppStringKey.Languages, language)
        CourseLessonType.Mixed -> AppStrings.get(AppStringKey.Mixed, language)
        CourseLessonType.Private -> AppStrings.get(AppStringKey.Private, language)
    }
