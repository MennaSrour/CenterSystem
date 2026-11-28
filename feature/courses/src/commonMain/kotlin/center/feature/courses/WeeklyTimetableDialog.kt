package center.feature.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
    val lessonsByDay = timetable.lessons.groupBy { it.day }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                AppStrings.get(AppStringKey.TimeLabel, language),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.height(42.dp),
            )
            timetable.slots.forEach { minute ->
                Text(formatTime(minute, language), style = MaterialTheme.typography.labelSmall, modifier = Modifier.height(70.dp))
            }
        }
        timetable.days.forEach { day ->
            Column(Modifier.width(136.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    AppStrings.get(dayKeys.getValue(day), language),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.height(42.dp),
                )
                timetable.slots.forEach { slot ->
                    val concurrent = lessonsByDay[day].orEmpty().filter { it.startMinute < slot + 30 && it.endMinute > slot }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(
                                (maxOf(1, concurrent.size) * 64).dp,
                            ).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        concurrent.forEach { lesson ->
                            Text(lesson.subject, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            Text(
                                lesson.teacher,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${formatTime(
                                    lesson.startMinute,
                                    language,
                                )}–${formatTime(lesson.endMinute, language)} · ${trackLabel(lesson.trackFor(track), language)}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                        if (concurrent.isEmpty()) Spacer(Modifier.height(1.dp))
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
