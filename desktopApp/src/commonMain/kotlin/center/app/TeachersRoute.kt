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
import center.domain.usecase.salaryPayoutId
import center.feature.teachers.Teacher
import center.feature.teachers.TeacherDraft
import center.feature.teachers.TeacherGroup
import center.feature.teachers.TeachersEvent
import center.feature.teachers.TeachersModal
import center.feature.teachers.TeachersScreen
import center.feature.teachers.TeachersState
import center.feature.teachers.UnpaidStudent
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import center.ui.text.isolateLtr
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.plus
import center.domain.model.Teacher as DomainTeacher

@Composable
internal fun TeachersRoute(
    root: RootComponent,
    services: DirectoryServices,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val language = LocalAppLanguage.current
    val scope = rememberDirectoryScope(language)
    val holder = remember(services, scope, language) { TeachersStateHolder(services, scope, language) }
    val state by holder.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(holder) { holder.feedback.collect { snackbar.showSnackbar(it) } }
    Box(Modifier.fillMaxSize()) {
        TeachersScreen(state, onEvent = { event ->
            when (event) {
                is TeachersEvent.Navigate -> root.navigate(event.destination)
                is TeachersEvent.LanguageSelected -> onLanguageSelected(event.language)
                else -> holder.onEvent(event)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class TeachersStateHolder(
    private val services: DirectoryServices,
    private val scope: CoroutineScope,
    private val language: AppLanguage,
) {
    private val mutableState = MutableStateFlow(TeachersState(emptyList()))
    val state = mutableState.asStateFlow()
    private val mutableFeedback = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedback = mutableFeedback.asSharedFlow()
    private val failure =
        LocalizedText(
            "تعذر تنفيذ العملية. تحقق من البيانات وحاول مرة أخرى.",
            "Could not complete the operation. Check the details and try again.",
        ).resolve(language)
    private var records = emptyMap<String, DomainTeacher>()

    init {
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
                monthUpdates,
            ) { teachers, groups, (month, ledger) ->
                val balances = services.billing.remainingBalances(month).groupBy { it.teacherId }
                val groupsByTeacher = groups.groupBy { it.teacherId }
                val paid = ledger.associateBy { it.id }
                val models =
                    teachers.map { teacher ->
                        val payout = paid[salaryPayoutId(teacher.id, month)]
                        Teacher(
                            teacher.id,
                            teacher.fullName.localizedValue(),
                            teacher.phone,
                            teacher.subject.localizedValue(),
                            payout?.amountMinor ?: services.salary(teacher.id).getOrThrow(),
                            groupsByTeacher[teacher.id].orEmpty().map { group ->
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
            is TeachersEvent.SearchChanged -> mutableState.update { it.copy(query = event.query) }
            is TeachersEvent.OpenTeacher -> if (event.id in records) setModal(TeachersModal.Details(event.id))
            is TeachersEvent.EditTeacher ->
                records[event.id]?.let {
                    setModal(TeachersModal.Add(TeacherDraft(it.fullName, it.phone, it.subject, it.notes), teacherId = it.id))
                }
            TeachersEvent.OpenAddTeacher -> setModal(TeachersModal.Add())
            TeachersEvent.CloseModal ->
                if ((current.modal as? TeachersModal.Add)?.saving != true) setModal(TeachersModal.Closed)
            is TeachersEvent.NameChanged -> edit { it.copy(name = event.value) }
            is TeachersEvent.PhoneChanged -> edit { it.copy(phone = event.value) }
            is TeachersEvent.SubjectChanged -> edit { it.copy(subject = event.value) }
            is TeachersEvent.NotesChanged -> edit { it.copy(notes = event.value) }
            TeachersEvent.SaveTeacher -> save()
            is TeachersEvent.PaySalary -> pay(event.id)
            is TeachersEvent.Navigate, is TeachersEvent.LanguageSelected -> Unit
        }
    }

    private fun setModal(modal: TeachersModal) {
        mutableState.update { it.copy(modal = modal) }
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
                    val teacher = existing ?: DomainTeacher(services.ids.newId(), draft.name.trim(), draft.subject.trim())
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

private fun List<GroupScheduleSlot>.scheduleText(language: AppLanguage): String =
    joinToString(" · ") { slot ->
        val day =
            if (language == AppLanguage.English) {
                slot.day.name
                    .lowercase()
                    .replaceFirstChar(Char::uppercase)
            } else {
                when (slot.day) {
                    DayOfWeek.MONDAY -> "الاثنين"
                    DayOfWeek.TUESDAY -> "الثلاثاء"
                    DayOfWeek.WEDNESDAY -> "الأربعاء"
                    DayOfWeek.THURSDAY -> "الخميس"
                    DayOfWeek.FRIDAY -> "الجمعة"
                    DayOfWeek.SATURDAY -> "السبت"
                    DayOfWeek.SUNDAY -> "الأحد"
                }
            }
        "$day ${"${slot.from}–${slot.to}".isolateLtr()}"
    }
