package center.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import center.domain.model.AttendancePerson
import center.domain.model.AttendanceSession
import center.domain.model.ClassGroup
import center.domain.model.GroupStatus
import center.domain.model.PersonType
import center.domain.model.StaffMember
import center.domain.model.Student
import center.domain.model.Teacher
import center.domain.usecase.scheduledLessonsForCheckIn
import center.feature.dashboard.AttendanceGroup
import center.feature.dashboard.DashboardEvent
import center.feature.dashboard.DashboardScreen
import center.feature.dashboard.DashboardState
import center.feature.dashboard.PaymentStatus
import center.feature.dashboard.QuickAttendance
import center.feature.dashboard.QuickStudent
import center.feature.dashboard.StudentCandidate
import center.ui.localization.AppLanguage
import center.ui.localization.LocalizedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
internal fun DashboardRoute(
    root: RootComponent,
    holder: DashboardStateHolder,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val state by holder.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(holder) { holder.feedback.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        DashboardScreen(state, onEvent = { event ->
            when (event) {
                is DashboardEvent.PaySubscription ->
                    if (state.student?.takeIf { it.isStudent }?.code ==
                        event.studentCode
                    ) {
                        root.open(ReceptionShortcut.CollectSubscription(event.studentCode))
                    }
                DashboardEvent.AddStudent -> root.open(ReceptionShortcut.AddStudent)
                DashboardEvent.CollectSubscription -> root.open(ReceptionShortcut.CollectSubscription())
                DashboardEvent.RecordExpense -> root.open(ReceptionShortcut.RecordExpense)
                is DashboardEvent.Navigate -> root.navigate(event.destination)
                is DashboardEvent.LanguageSelected -> onLanguageSelected(event.language)
                else -> holder.onEvent(event)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class DashboardStateHolder(
    private val services: DirectoryServices,
    private val scope: CoroutineScope,
    language: AppLanguage,
) {
    private val state = MutableStateFlow(DashboardState("0", "0", language = language, dateLabel = services.today().toString()))
    val uiState = state.asStateFlow()
    private val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedback = messages.asSharedFlow()
    private val failure = LocalizedText("تعذر تحميل البيانات أو حفظها.", "Could not load or save the data.")
    private val checkInSaved = LocalizedText("تم تسجيل الحضور.", "Check-in recorded.")
    private val checkOutSaved = LocalizedText("تم تسجيل الانصراف.", "Check-out recorded.")
    private val arabicGrades = gradeCatalog(AppLanguage.Arabic).associateBy { it.id }
    private val englishGrades = gradeCatalog(AppLanguage.English).associateBy { it.id }
    private var students = emptyList<Student>()
    private var groups = emptyMap<String, ClassGroup>()
    private var unpaidStudentIds = emptySet<String>()
    private var selectedCode: String? = null
    private var teachers = emptyList<Teacher>()
    private var staff = emptyList<StaffMember>()
    private var sessions = emptyList<AttendanceSession>()
    private var selectedPerson: AttendancePerson? = null

    private data class ReceptionPerson(
        val person: AttendancePerson,
        val code: String,
        val name: String,
        val student: Student? = null,
    )

    init {
        scope.launch {
            val dayUpdates =
                services.dates().flatMapLatest { day ->
                    val end = (day + DatePeriod(days = 1)).startMillis()
                    combine(
                        services.attendance.observePresentCount(day.startMillis(), end),
                        services.treasury.observeTotals(day),
                        services.billing.observeChanges(),
                        services.sessions.observeDay(day),
                    ) { count, treasury, _, sessions ->
                        DaySnapshot(day, count + sessions.size, treasury.todayIncomeMinor - treasury.todayExpenseMinor, sessions)
                    }
                }
            combine(
                services.students.observeStudents(),
                services.groups.observeGroups(),
                services.teachers.observeTeachers(),
                services.staff.observeStaff(),
                dayUpdates,
            ) { directory, catalog, teachers, staff, (day, attendance, treasury, sessions) ->
                this@DashboardStateHolder.teachers = teachers
                this@DashboardStateHolder.staff = staff.filter { it.isActive }
                this@DashboardStateHolder.sessions = sessions
                students = directory.filter { it.isActive }
                groups = catalog.associateBy { it.id }
                unpaidStudentIds =
                    services.billing
                        .remainingBalances(day.monthStart())
                        .map { it.studentId }
                        .toSet()
                state.update { snapshot ->
                    refreshSearch(
                        snapshot.copy(attendance = attendance.toString(), treasury = treasury.formattedMinor(), dateLabel = day.toString()),
                        day.dayOfMonth,
                    )
                }
            }.catch { messages.emit(failure.resolve(state.value.language)) }.collect {}
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.QueryChanged -> {
                selectedCode = null
                state.update {
                    it.copy(
                        query = event.value,
                        student = null,
                        searchSubmitted = false,
                        candidates = emptyList(),
                        attendanceGroupId = null,
                        groupMenuExpanded = false,
                    )
                }
            }
            DashboardEvent.Search ->
                if (state.value.query.isNotBlank()) {
                    selectedCode = null
                    state.update {
                        refreshSearch(
                            it.copy(searchSubmitted = true, attendanceGroupId = null, groupMenuExpanded = false),
                            services.today().dayOfMonth,
                        )
                    }
                }
            is DashboardEvent.SelectStudent ->
                if (state.value.candidates.any { it.code == event.code }) {
                    selectedCode = event.code
                    state.update {
                        refreshSearch(
                            it.copy(attendanceGroupId = null, groupMenuExpanded = false),
                            services.today().dayOfMonth,
                        )
                    }
                }
            is DashboardEvent.SelectAttendanceGroup ->
                if (state.value.student
                        ?.attendanceGroups
                        ?.any { it.id == event.id } == true
                ) {
                    state.update { it.copy(attendanceGroupId = event.id, groupMenuExpanded = false) }
                }
            is DashboardEvent.AttendanceGroupMenuChanged -> state.update { it.copy(groupMenuExpanded = event.expanded) }
            is DashboardEvent.ToggleLessonAttendance ->
                state.update { current ->
                    current.copy(
                        lessonAttendanceOptions =
                            current.lessonAttendanceOptions.map { lesson ->
                                if (lesson.lessonId == event.lessonId) lesson.copy(attended = !lesson.attended) else lesson
                            },
                    )
                }
            is DashboardEvent.RegisterAttendance -> record(event.studentCode, event.groupId, checkout = false, event.attendedLessonIds)
            is DashboardEvent.CheckOut -> record(event.code, event.groupId, checkout = true)
            is DashboardEvent.LanguageSelected ->
                state.update {
                    refreshSearch(
                        it.copy(language = event.language),
                        services.today().dayOfMonth,
                    )
                }
            else -> Unit
        }
    }

    private fun refreshSearch(
        snapshot: DashboardState,
        dayOfMonth: Int,
    ): DashboardState {
        if (!snapshot.searchSubmitted) return snapshot
        val term = snapshot.query.normalizeSearch()

        val people =
            students.map {
                ReceptionPerson(AttendancePerson(PersonType.STUDENT, it.id), it.code, it.fullName, it)
            } +
                teachers.map {
                    ReceptionPerson(AttendancePerson(PersonType.TEACHER, it.id), it.id, it.fullName)
                } +
                staff.map {
                    ReceptionPerson(AttendancePerson(PersonType.STAFF, it.id), it.id, it.name)
                }
        val exact = people.filter { it.code.normalizeSearch().equals(term, ignoreCase = true) }
        val matches = exact.ifEmpty { people.filter { it.name.normalizeSearch().contains(term, ignoreCase = true) } }
        val selected = matches.firstOrNull { it.selectionKey == selectedCode } ?: matches.singleOrNull()
        selectedPerson = selected?.person
        if (selected == null) {
            return snapshot.copy(
                student = null,
                candidates =
                    matches.map {
                        val type =
                            it.person.type
                                .label()
                                .resolve(snapshot.language)
                        StudentCandidate(it.selectionKey, it.name, if (it.student == null) type else "$type · ${it.code}")
                    },
                lessonAttendanceOptions = emptyList(),
            )
        }
        val today = sessions.filter { it.person.type == selected.person.type && it.person.id == selected.person.id }
        val student = selected.student
        val enrolled =
            student
                ?.enrolledGroupIds
                .orEmpty()
                .mapNotNull(groups::get)
                .filter { it.status == GroupStatus.ACTIVE }
        val status =
            when {
                enrolled.none { (it.monthlyPriceMinor ?: 0) > 0 } -> PaymentStatus.NoSubscription
                student?.id !in unpaidStudentIds -> PaymentStatus.Paid
                dayOfMonth >= 5 -> PaymentStatus.Overdue
                else -> PaymentStatus.Due
            }
        val grade = student?.gradeLevel.orEmpty()
        val detail =
            QuickStudent(
                code = if (student == null) selected.selectionKey else selected.code,
                name = selected.name.localizedValue(),
                grade =
                    if (student == null) {
                        selected.person.type.label()
                    } else {
                        LocalizedText(arabicGrades[grade]?.label ?: grade, englishGrades[grade]?.label ?: grade)
                    },
                paymentStatus = status,
                attendanceGroups =
                    if (student == null) {
                        null
                    } else {
                        (
                            enrolled.map { AttendanceGroup(it.id, it.name) } +
                                today.mapNotNull {
                                    val id = it.person.groupId
                                    if (id.isEmpty()) {
                                        if (enrolled.isEmpty()) {
                                            null
                                        } else {
                                            AttendanceGroup(
                                                "",
                                                LocalizedText("حضور عام", "General attendance").resolve(snapshot.language),
                                            )
                                        }
                                    } else {
                                        AttendanceGroup(id, groups[id]?.name ?: id)
                                    }
                                }
                        ).distinctBy { it.id }
                    },
                isStudent = student != null,
                attendance =
                    today.map {
                        QuickAttendance(it.person.groupId, it.checkInTime.localTimeLabel(), it.checkOutTime?.localTimeLabel())
                    },
            )
        return snapshot.copy(
            student = detail,
            candidates = emptyList(),
            lessonAttendanceOptions =
                if (student == null || today.isNotEmpty()) {
                    emptyList()
                } else {
                    scheduledLessonsForCheckIn(enrolled, services.today(), services.clock.nowMillis())
                },
        )
    }

    private val ReceptionPerson.selectionKey: String get() = "${person.type.name}:${person.id}"

    private fun record(
        code: String,
        groupId: String?,
        checkout: Boolean,
        attendedLessonIds: Set<String> = emptySet(),
    ) {
        val snapshot = state.value
        val detail = snapshot.student ?: return
        if (snapshot.checkingIn || detail.code != code) return
        val groups = detail.attendanceGroups.orEmpty()
        val group = groupId ?: groups.singleOrNull()?.id
        if (groups.isNotEmpty() && groups.none { it.id == group }) return
        val person = selectedPerson?.copy(groupId = group.orEmpty()) ?: return
        state.update { it.copy(checkingIn = true) }
        scope.launch {
            try {
                val lessonChoices = snapshot.lessonAttendanceOptions.map { it.copy(attended = it.lessonId in attendedLessonIds) }
                val result =
                    if (checkout) {
                        services.presence.checkOut(person)
                    } else {
                        services.presence.checkIn(person, lessonChoices)
                    }
                val success = if (checkout) checkOutSaved else checkInSaved
                messages.emit((if (result.isSuccess) success else failure).resolve(state.value.language))
            } finally {
                state.update { it.copy(checkingIn = false) }
            }
        }
    }

    private data class DaySnapshot(
        val date: LocalDate,
        val attendance: Long,
        val treasury: Long,
        val sessions: List<AttendanceSession>,
    )
}
