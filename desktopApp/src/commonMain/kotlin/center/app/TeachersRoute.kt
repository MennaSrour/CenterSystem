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
import center.domain.model.GroupScheduleSlot
import center.domain.model.LedgerKind
import center.domain.model.PersonType
import center.domain.model.StaffMember
import center.domain.model.activeSchedule
import center.domain.model.lessonAnalytics
import center.domain.usecase.salaryPayoutId
import center.feature.reports.ContextualPdfPlatform
import center.feature.teachers.StaffCardModel
import center.feature.teachers.StaffDraft
import center.feature.teachers.StaffShiftDraft
import center.feature.teachers.Teacher
import center.feature.teachers.TeacherDraft
import center.feature.teachers.TeacherGroup
import center.feature.teachers.TeachersEvent
import center.feature.teachers.TeachersModal
import center.feature.teachers.TeachersScreen
import center.feature.teachers.TeachersState
import center.feature.teachers.UnpaidStudent
import center.ui.localization.AppLanguage
import center.ui.localization.AppStringKey
import center.ui.localization.AppStrings
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import center.domain.model.Teacher as DomainTeacher

@Composable
internal fun TeachersRoute(
    root: RootComponent,
    services: DirectoryServices,
    onLanguageSelected: (AppLanguage) -> Unit,
    canDelete: () -> Boolean,
    printPlatform: ContextualPdfPlatform? = null,
) {
    val language = LocalAppLanguage.current
    val scope = rememberDirectoryScope(language)
    val holder = remember(services, scope, language) { TeachersStateHolder(services, scope, language, canDelete) }
    val state by holder.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(holder) { holder.feedback.collect { snackbar.showSnackbar(it) } }
    Box(Modifier.fillMaxSize()) {
        TeachersScreen(state, onEvent = { event ->
            when (event) {
                is TeachersEvent.Navigate -> root.navigate(event.destination)
                is TeachersEvent.LanguageSelected -> onLanguageSelected(event.language)
                is TeachersEvent.TeacherStatementAction ->
                    createTeacherStatement(
                        state.teachers.firstOrNull { it.id == event.id },
                        event.print,
                        printPlatform,
                        language,
                        services.today().toString().take(7),
                        scope,
                    ) {
                        holder.notify(it)
                    }
                else -> holder.onEvent(event)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun createTeacherStatement(
    teacher: center.feature.teachers.Teacher?,
    print: Boolean,
    platform: ContextualPdfPlatform?,
    language: AppLanguage,
    month: String,
    scope: CoroutineScope,
    notify: (String) -> Unit,
) {
    if (teacher == null || platform == null) {
        notify(AppStrings.get(AppStringKey.FeatureUnavailable, language))
        return
    }
    val title = AppStrings.get(AppStringKey.PrintTeacherStatement, language)
    val periodLabel = LocalizedText("الشهر: $month", "Month: $month").resolve(language)
    val lines =
        listOf(
            LocalizedText("كود المدرس: ${teacher.id}", "Teacher ID: ${teacher.id}").resolve(language),
            LocalizedText("الاسم: ${teacher.name.arabic}", "Name: ${teacher.name.english}").resolve(language),
            periodLabel,
            LocalizedText(
                "الدروس المسندة: ${teacher.assignedLessonCount}",
                "Assigned lessons: ${teacher.assignedLessonCount}",
            ).resolve(language),
            LocalizedText(
                "عدد الطلاب · عربي ${teacher.arabicStudentCount} · لغات ${teacher.languagesStudentCount} · برايفت ${teacher.privateStudentCount}",
                "Students · Arabic ${teacher.arabicStudentCount} · Languages ${teacher.languagesStudentCount} · Private ${teacher.privateStudentCount}",
            ).resolve(language),
            LocalizedText(
                "الراتب المستحق: ${teacher.monthlySalaryMinor?.decimalMinor().orEmpty()} ج.م",
                "Salary due: ${teacher.monthlySalaryMinor?.decimalMinor().orEmpty()} EGP",
            ).resolve(language),
            LocalizedText("سجل الصرف", "Payout history").resolve(language),
        ) + teacher.payoutHistory.map { it.resolve(language) }
    val bytes = platform.createContextPdf(title, lines, language)
    scope.launch {
        runCatching {
            if (print) {
                platform.printContextPdf(title, bytes)
            } else {
                platform.saveContextPdf(
                    title,
                    "Teacher_Statement_${teacher.id}.pdf",
                    LocalizedText("استبدال الملف الموجود؟", "Replace the existing file?").resolve(language),
                    bytes,
                )
            }
        }.onFailure { notify(AppStrings.get(AppStringKey.Error, language)) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class TeachersStateHolder(
    private val services: DirectoryServices,
    private val scope: CoroutineScope,
    private val language: AppLanguage,
    private val canDelete: () -> Boolean,
) {
    fun notify(message: String) {
        mutableFeedback.tryEmit(message)
    }

    private val mutableState = MutableStateFlow(TeachersState(emptyList(), canDelete = canDelete()))
    val state = mutableState.asStateFlow()
    private val mutableFeedback = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedback = mutableFeedback.asSharedFlow()
    private val failure =
        LocalizedText(
            "تعذر تنفيذ العملية. تحقق من البيانات وحاول مرة أخرى.",
            "Could not complete the operation. Check the details and try again.",
        ).resolve(language)
    private var records = emptyMap<String, DomainTeacher>()
    private var staffRecords = emptyMap<String, StaffMember>()

    init {
        scope.launch {
            services.staff.observeStaff().catch { mutableFeedback.emit(failure) }.collect { members ->
                staffRecords = members.associateBy { it.id }
                mutableState.update { current ->
                    current.copy(
                        staff =
                            members.map {
                                StaffCardModel(
                                    it.id,
                                    it.name,
                                    it.role,
                                    it.phone,
                                    it.isActive,
                                    LocalizedText(
                                        it.workSchedule.scheduleText(AppLanguage.Arabic),
                                        it.workSchedule.scheduleText(AppLanguage.English),
                                    ),
                                    it.monthlySalaryMinor,
                                    canDelete(),
                                )
                            },
                    )
                }
            }
        }
        scope.launch {
            state
                .map { current ->
                    when (val modal = current.modal) {
                        is TeachersModal.Details -> PersonType.TEACHER to modal.teacherId
                        is TeachersModal.StaffDetails -> PersonType.STAFF to modal.id
                        else -> null
                    }
                }.distinctUntilChanged()
                .flatMapLatest { person ->
                    if (person == null) flowOf(emptyList()) else services.sessions.observeHistory(person.first, person.second)
                }.catch { mutableFeedback.emit(failure) }
                .collect { history ->
                    mutableState.update { current -> current.copy(history = history.map { it.summary() }) }
                }
        }
        scope.launch {
            services.treasury.observeLedger(Long.MIN_VALUE, Long.MAX_VALUE).catch { mutableFeedback.emit(failure) }.collect { ledger ->
                val histories =
                    ledger
                        .filter { it.kind == LedgerKind.TEACHER_PAYOUT && it.teacherId != null }
                        .groupBy { it.teacherId }
                        .mapValues { (_, entries) ->
                            entries.sortedByDescending { it.recordedAt }.map { entry ->
                                val date = Instant.fromEpochMilliseconds(entry.recordedAt).toLocalDateTime(Cairo).date
                                LocalizedText(
                                    "$date · ${entry.amountMinor.decimalMinor()} ج.م",
                                    "$date · ${entry.amountMinor.decimalMinor()} EGP",
                                )
                            }
                        }
                mutableState.update { current ->
                    current.copy(
                        teachers =
                            current.teachers.map { teacher ->
                                teacher.copy(payoutHistory = histories[teacher.id].orEmpty())
                            },
                    )
                }
            }
        }
        scope.launch {
            val monthUpdates =
                services.dates().flatMapLatest { date ->
                    val month = date.monthStart()
                    combine(
                        services.billing.observeChanges(),
                        services.treasury.observeLedger(month.startMillis(), (month + DatePeriod(months = 1)).startMillis()),
                    ) { _, ledger -> month to ledger }
                }
            combine(
                services.teachers.observeTeachers(),
                services.groups.observeGroups(),
                services.students.observeStudents(),
                monthUpdates,
            ) { teachers, groups, students, (month, ledger) ->
                val balances = services.billing.remainingBalances(month).groupBy { it.teacherId }
                val paid = ledger.associateBy { it.id }
                val models =
                    teachers.map { teacher ->
                        val payout = paid[salaryPayoutId(teacher.id, month)]
                        val assignedGroups = teacher.activeSchedule(groups)
                        val analytics = teacher.lessonAnalytics(groups, students)
                        Teacher(
                            teacher.id,
                            teacher.fullName.localizedValue(),
                            teacher.phone,
                            teacher.subject.localizedValue(),
                            payout?.amountMinor ?: services.salary(teacher.id).getOrThrow(),
                            assignedGroups.map { group ->
                                TeacherGroup(
                                    group.name.localizedValue(),
                                    LocalizedText(
                                        group.schedule.scheduleText(AppLanguage.Arabic),
                                        group.schedule.scheduleText(AppLanguage.English),
                                    ),
                                )
                            },
                            balances[teacher.id].orEmpty().groupBy { it.studentId }.map { (id, rows) ->
                                UnpaidStudent(rows.first().studentName.localizedValue(), rows.sumOf { it.remainingMinor }, id)
                            },
                            teacher.notes,
                            payout != null,
                            analytics.assignedLessonCount,
                            analytics.arabicStudentCount,
                            analytics.languagesStudentCount,
                            analytics.privateStudentCount,
                            LocalizedText(
                                analytics.expectedSchedule.scheduleText(AppLanguage.Arabic),
                                analytics.expectedSchedule.scheduleText(AppLanguage.English),
                            ),
                        )
                    }
                teachers.associateBy { it.id } to models
            }.catch { mutableFeedback.emit(failure) }.collect { (teachers, models) ->
                records = teachers
                mutableState.update { it.copy(teachers = models) }
            }
        }
    }

    fun onEvent(event: TeachersEvent) {
        val current = state.value
        when (event) {
            is TeachersEvent.StaffTabSelected ->
                mutableState.update {
                    it.copy(showStaff = event.selected, query = "", modal = TeachersModal.Closed, history = emptyList())
                }
            is TeachersEvent.OpenStaff -> if (event.id in staffRecords) setModal(TeachersModal.StaffDetails(event.id))
            is TeachersEvent.EditStaff ->
                staffRecords[event.id]?.let { member ->
                    setModal(
                        TeachersModal.StaffForm(
                            StaffDraft(
                                member.name,
                                member.role,
                                member.phone,
                                member.isActive,
                                member.workSchedule.map { StaffShiftDraft(it.day, it.from.hhmm(), it.to.hhmm()) },
                                member.id,
                                member.monthlySalaryMinor
                                    ?.div(100)
                                    ?.toString()
                                    .orEmpty(),
                            ),
                            member.id,
                        ),
                    )
                }
            TeachersEvent.AddStaff -> setModal(TeachersModal.StaffForm(draft = StaffDraft(code = nextCode("SF", staffRecords.keys))))
            is TeachersEvent.StaffDraftChanged ->
                (current.modal as? TeachersModal.StaffForm)?.let {
                    if (!it.saving) setModal(it.copy(draft = event.draft, message = null))
                }
            TeachersEvent.SaveStaff -> saveStaff()
            is TeachersEvent.RequestDeleteStaff ->
                if (canDelete() && current.modal is TeachersModal.StaffDetails) {
                    staffRecords[event.id]?.let { setModal(TeachersModal.DeleteStaff(it.id, it.name)) }
                }
            TeachersEvent.ConfirmDeleteStaff -> deleteStaff()
            is TeachersEvent.RequestDeleteTeacher ->
                if (canDelete() && (current.modal as? TeachersModal.Details)?.teacherId == event.id && current.payingTeacherId == null) {
                    records[event.id]?.let { setModal(TeachersModal.Delete(it.id, it.fullName)) }
                }
            TeachersEvent.ConfirmDeleteTeacher -> deleteTeacher()
            is TeachersEvent.SearchChanged -> mutableState.update { it.copy(query = event.query) }
            is TeachersEvent.OpenTeacher -> if (event.id in records) setModal(TeachersModal.Details(event.id))
            is TeachersEvent.EditTeacher ->
                records[event.id]?.let {
                    setModal(TeachersModal.Add(TeacherDraft(it.fullName, it.phone, it.subject, it.notes, it.id), teacherId = it.id))
                }
            TeachersEvent.OpenAddTeacher -> setModal(TeachersModal.Add(draft = TeacherDraft(code = nextCode("TR", records.keys))))
            TeachersEvent.CloseModal ->
                when (val modal = current.modal) {
                    is TeachersModal.Add -> if (!modal.saving) setModal(TeachersModal.Closed)
                    is TeachersModal.StaffForm -> if (!modal.saving) setModal(TeachersModal.Closed)
                    is TeachersModal.Delete -> if (!modal.deleting) setModal(TeachersModal.Details(modal.id))
                    is TeachersModal.DeleteStaff -> if (!modal.deleting) setModal(TeachersModal.StaffDetails(modal.id))
                    else -> setModal(TeachersModal.Closed)
                }
            is TeachersEvent.NameChanged -> edit { it.copy(name = event.value) }
            is TeachersEvent.PhoneChanged -> edit { it.copy(phone = event.value) }
            is TeachersEvent.SubjectChanged -> edit { it.copy(subject = event.value) }
            is TeachersEvent.NotesChanged -> edit { it.copy(notes = event.value) }
            TeachersEvent.SaveTeacher -> save()
            is TeachersEvent.PaySalary -> pay(event.id)
            is TeachersEvent.TeacherStatementAction -> Unit
            is TeachersEvent.Navigate, is TeachersEvent.LanguageSelected -> Unit
        }
    }

    private fun deleteTeacher() {
        if (!canDelete()) return
        val modal = state.value.modal as? TeachersModal.Delete ?: return
        if (modal.deleting) return
        setModal(modal.copy(deleting = true, message = null))
        scope.launch {
            val result = directoryResult { services.teachers.deleteTeacher(modal.id) }
            setModal(if (result.isSuccess) TeachersModal.Closed else modal.copy(deleting = false, message = failure))
        }
    }

    private fun deleteStaff() {
        if (!canDelete()) return
        val modal = state.value.modal as? TeachersModal.DeleteStaff ?: return
        if (modal.deleting) return
        setModal(modal.copy(deleting = true, message = null))
        scope.launch {
            val result = directoryResult { services.staff.delete(modal.id) }
            setModal(if (result.isSuccess) TeachersModal.Closed else modal.copy(deleting = false, message = failure))
        }
    }

    private fun setModal(modal: TeachersModal) {
        mutableState.update { it.copy(modal = modal, history = emptyList()) }
    }

    private fun edit(transform: (TeacherDraft) -> TeacherDraft) {
        val form = state.value.modal as? TeachersModal.Add ?: return
        if (!form.saving) setModal(form.copy(draft = transform(form.draft), message = null))
    }

    private fun save() {
        val form = state.value.modal as? TeachersModal.Add ?: return
        if (form.saving || !form.draft.canSave) return
        setModal(form.copy(saving = true, message = null))
        scope.launch {
            val saved =
                directoryResult {
                    val draft = form.draft
                    val existing = form.teacherId?.let { requireNotNull(records[it]) }
                    val teacher = existing ?: DomainTeacher(draft.code, draft.name.trim(), draft.subject.trim())
                    services.teachers.save(
                        teacher.copy(
                            fullName = draft.name.trim(),
                            phone = draft.phone.trim(),
                            subject = draft.subject.trim(),
                            notes = draft.notes.trim(),
                        ),
                    )
                }
            setModal(if (saved.isSuccess) TeachersModal.Closed else form.copy(message = failure))
        }
    }

    private fun saveStaff() {
        val form = state.value.modal as? TeachersModal.StaffForm ?: return
        if (form.saving || !form.draft.canSave) return
        setModal(form.copy(saving = true, message = null))
        scope.launch {
            val saved =
                directoryResult {
                    val draft = form.draft
                    val shifts =
                        draft.shifts.map {
                            GroupScheduleSlot(it.day, LocalTime.parse(it.from.normalizeSearch()), LocalTime.parse(it.to.normalizeSearch()))
                        }
                    val monthlySalaryMinor =
                        draft.monthlySalary
                            .takeIf { it.isNotBlank() }
                            ?.toLongOrNull()
                            ?.times(100)
                    require(draft.monthlySalary.isBlank() || monthlySalaryMinor != null)
                    services.staff.save(
                        StaffMember(form.id ?: draft.code, draft.name, draft.role, draft.phone, shifts, draft.isActive, monthlySalaryMinor),
                    )
                }
            setModal(
                if (saved.isSuccess) {
                    TeachersModal.Closed
                } else {
                    form.copy(
                        message =
                            LocalizedText(
                                "تعذر حفظ الموظف. تحقق من أوقات الورديات وتداخلها.",
                                "Could not save staff member. Check shift times and overlaps.",
                            ).resolve(language),
                    )
                },
            )
        }
    }

    private fun nextCode(
        prefix: String,
        ids: Collection<String>,
    ): String = services.personCodes.next(prefix, ids)

    private fun pay(id: String) {
        val current = state.value
        if (current.payingTeacherId != null || (current.modal as? TeachersModal.Details)?.teacherId != id) return
        if (current.teachers.none { it.id == id && !it.salaryPaid && (it.monthlySalaryMinor ?: 0L) > 0 }) return
        mutableState.update { it.copy(payingTeacherId = id) }
        scope.launch {
            val result = services.paySalary(id)
            mutableState.update { it.copy(payingTeacherId = null) }
            mutableFeedback.emit(
                if (result.isSuccess) {
                    LocalizedText(
                        "تم تسجيل صرف المرتب في الخزنة.",
                        "Salary payout recorded in Treasury.",
                    ).resolve(language)
                } else {
                    failure
                },
            )
        }
    }
}
