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
import center.domain.model.AttendanceStatus
import center.domain.model.ClassGroup
import center.domain.model.GroupStatus
import center.domain.model.Student
import center.domain.usecase.DuplicateAttendanceException
import center.feature.dashboard.AttendanceGroup
import center.feature.dashboard.DashboardEvent
import center.feature.dashboard.DashboardScreen
import center.feature.dashboard.DashboardState
import center.feature.dashboard.PaymentStatus
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
                    if (state.student?.code == event.studentCode) root.open(ReceptionShortcut.CollectSubscription(event.studentCode))
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
    private val success = LocalizedText("تم تسجيل الحضور.", "Attendance recorded.")
    private val duplicate =
        LocalizedText(
            "تم تسجيل حضور الطالب لهذه المجموعة اليوم.",
            "Attendance for this group was already recorded today.",
        )
    private val arabicGrades = gradeCatalog(AppLanguage.Arabic).associateBy { it.id }
    private val englishGrades = gradeCatalog(AppLanguage.English).associateBy { it.id }
    private var students = emptyList<Student>()
    private var groups = emptyMap<String, ClassGroup>()
    private var unpaidStudentIds = emptySet<String>()
    private var selectedCode: String? = null

    init {
        scope.launch {
            val dayUpdates =
                services.dates().flatMapLatest { day ->
                    val end = (day + DatePeriod(days = 1)).startMillis()
                    combine(
                        services.attendance.observePresentCount(day.startMillis(), end),
                        services.treasury.observeTotals(day),
                        services.billing.observeChanges(),
                    ) { count, treasury, _ -> Triple(day, count, treasury.todayIncomeMinor - treasury.todayExpenseMinor) }
                }
            combine(
                services.students.observeStudents(),
                services.groups.observeGroups(),
                dayUpdates,
            ) { directory, catalog, (day, attendance, treasury) ->
                students = directory.filter { it.isActive }
                groups = catalog.filter { it.status == GroupStatus.ACTIVE }.associateBy { it.id }
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
            is DashboardEvent.RegisterAttendance -> checkIn(event)
            is DashboardEvent.LanguageSelected -> state.update { it.copy(language = event.language) }
            else -> Unit
        }
    }

    private fun refreshSearch(
        snapshot: DashboardState,
        dayOfMonth: Int,
    ): DashboardState {
        if (!snapshot.searchSubmitted) return snapshot
        val term = snapshot.query.normalizeSearch()
        val exact = students.filter { it.code.normalizeSearch().equals(term, ignoreCase = true) }
        val matches = exact.ifEmpty { students.filter { it.fullName.normalizeSearch().contains(term, ignoreCase = true) } }
        val selected = matches.firstOrNull { it.code == selectedCode } ?: matches.singleOrNull()
        if (selected == null) {
            return snapshot.copy(student = null, candidates = matches.map { StudentCandidate(it.code, it.fullName) })
        }
        val enrolled = selected.enrolledGroupIds.mapNotNull(groups::get)
        val status =
            when {
                enrolled.none { (it.monthlyPriceMinor ?: 0) > 0 } -> PaymentStatus.NoSubscription
                selected.id !in unpaidStudentIds -> PaymentStatus.Paid
                dayOfMonth >= 5 -> PaymentStatus.Overdue
                else -> PaymentStatus.Due
            }
        val grade = selected.gradeLevel
        val detail =
            QuickStudent(
                selected.code,
                selected.fullName.localizedValue(),
                LocalizedText(arabicGrades[grade]?.label ?: grade, englishGrades[grade]?.label ?: grade),
                status,
                enrolled.map { AttendanceGroup(it.id, it.name) },
            )
        return snapshot.copy(student = detail, candidates = emptyList())
    }

    private fun checkIn(event: DashboardEvent.RegisterAttendance) {
        val snapshot = state.value
        if (snapshot.checkingIn || snapshot.student?.code != event.studentCode) return
        val groups = snapshot.student?.attendanceGroups.orEmpty()
        val group = event.groupId ?: groups.singleOrNull()?.id ?: return
        if (groups.none { it.id == group }) return
        state.update { it.copy(checkingIn = true) }
        scope.launch {
            try {
                val result = services.recordAttendance(event.studentCode, group, AttendanceStatus.PRESENT)
                messages.emit(
                    when {
                        result.isSuccess -> success
                        result.exceptionOrNull() is DuplicateAttendanceException -> duplicate
                        else -> failure
                    }.resolve(state.value.language),
                )
            } finally {
                state.update { it.copy(checkingIn = false) }
            }
        }
    }
}
