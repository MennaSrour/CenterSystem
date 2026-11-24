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
import center.app.resources.Res
import center.app.resources.feature_unavailable
import center.domain.model.AttendanceSession
import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupStatus
import center.domain.model.PersonType
import center.domain.model.Student
import center.domain.model.activeSchedule
import center.feature.financials.ClassOption
import center.feature.financials.StudentOption
import center.feature.students.AddStudentState
import center.feature.students.DeleteStudentState
import center.feature.students.EducationStage
import center.feature.students.StudentDetailsState
import center.feature.students.StudentGrade
import center.feature.students.StudentGroupOption
import center.feature.students.StudentMonthlyPayment
import center.feature.students.StudentRow
import center.feature.students.StudentStageFilter
import center.feature.students.StudentSystem
import center.feature.students.StudentsDestination
import center.feature.students.StudentsEvent
import center.feature.students.StudentsScreen
import center.feature.students.StudentsState
import center.feature.students.StudentsSummary
import center.ui.localization.AppLanguage
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StudentsRoute(
    root: RootComponent,
    services: DirectoryServices,
    openAddStudent: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    canDelete: () -> Boolean,
) {
    val language = LocalAppLanguage.current
    val scope = rememberDirectoryScope(language)
    val holder =
        remember(services, scope, language, openAddStudent) { StudentsStateHolder(services, scope, language, openAddStudent, canDelete) }
    val state by holder.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val unavailable = stringResource(Res.string.feature_unavailable)

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }
    LaunchedEffect(holder) { holder.feedback.collect { snackbar.showSnackbar(it) } }
    Box(Modifier.fillMaxSize()) {
        StudentsScreen(state, onEvent = { event ->
            when (event) {
                is StudentsEvent.Navigate ->
                    when (event.destination) {
                        StudentsDestination.Courses -> root.navigate(Destination.Courses)
                        StudentsDestination.Home -> root.navigate(Destination.Dashboard)
                        StudentsDestination.Financials -> root.navigate(Destination.Financials)
                        StudentsDestination.Reports -> root.navigate(Destination.Reports)
                        StudentsDestination.Students -> Unit
                        StudentsDestination.Teachers -> root.navigate(Destination.Teachers)
                        StudentsDestination.Print -> root.navigate(Destination.Print)
                        StudentsDestination.Settings -> root.navigate(Destination.Settings)
                        StudentsDestination.Schedule -> notify(unavailable)
                    }
                is StudentsEvent.PayNow ->
                    state.visibleOverdueRows.firstOrNull { it.payment.id == event.paymentId }?.let { (student, payment) ->
                        root.open(
                            ReceptionShortcut.PayOverdue(
                                student = StudentOption(student.id, student.name, student.code),
                                group = ClassOption(payment.groupId, payment.group.resolve(language)),
                                remainingMinor = payment.remainingMinor,
                            ),
                        )
                    }
                is StudentsEvent.LanguageSelected -> onLanguageSelected(event.language)
                StudentsEvent.OpenNotifications, StudentsEvent.LogOut -> notify(unavailable)
                else -> holder.onEvent(event)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class StudentsStateHolder(
    private val services: DirectoryServices,
    private val scope: CoroutineScope,
    private val language: AppLanguage,
    openAddStudent: Boolean = false,
    private val canDelete: () -> Boolean,
) {
    private val grades = gradeCatalog(language)
    private val allGradeLabels =
        (
            grades +
                gradeCatalog(
                    if (language ==
                        AppLanguage.English
                    ) {
                        AppLanguage.Arabic
                    } else {
                        AppLanguage.English
                    },
                )
        ).associateBy { it.label }
    private val uiGrades =
        grades.map {
            StudentGrade(
                it.id,
                it.label,
                EducationStage.valueOf(
                    it.stage.name
                        .lowercase()
                        .replaceFirstChar(Char::uppercase),
                ),
            )
        }
    private val detailId = MutableStateFlow<String?>(null)
    private var history = emptyList<AttendanceSession>()
    private var records = emptyMap<String, Student>()
    private var groups = emptyList<ClassGroup>()
    private var directoryLoaded = false
    private val mutableState =
        MutableStateFlow(
            StudentsState(
                StudentsSummary("0", "0", "0", "0"),
                emptyList(),
                uiGrades,
                "",
                services.today().toString(),
                canDelete = canDelete(),
                addStudent = if (openAddStudent) AddStudentState(grades = uiGrades) else null,
            ),
        )
    val uiState = mutableState.asStateFlow()
    private val mutableFeedback = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedback = mutableFeedback.asSharedFlow()
    private val failure =
        LocalizedText(
            "تعذر حفظ التغيير. تحقق من البيانات والسجلات المرتبطة.",
            "Could not save the change. Check the details and linked records.",
        ).resolve(language)
    private var state: StudentsState
        get() = mutableState.value
        set(value) {
            mutableState.value = value
        }

    init {
        scope.launch {
            detailId
                .flatMapLatest { id ->
                    if (id == null) flowOf(emptyList()) else services.sessions.observeHistory(PersonType.STUDENT, id)
                }.catch { mutableFeedback.emit(failure) }
                .collect {
                    history = it
                    mutableState.update { current -> current.copy(details = details()) }
                }
        }
        scope.launch {
            combine(
                services.students.observeStudents(),
                services.groups.observeGroups(),
                services.billing.observeChanges(),
                services.dates(),
            ) { students, groups, _, today ->
                val balances = services.overdue().getOrThrow()
                val rows =
                    students.map { student ->
                        StudentRow(
                            student.id,
                            student.code,
                            student.fullName,
                            student.fullName,
                            student.gradeId(),
                            student.uiSystem(),
                            student.parentPhone,
                            student.stage?.let { EducationStage.valueOf(it.name.lowercase().replaceFirstChar(Char::uppercase)) },
                            student.gradeLevel.takeIf { student.gradeId().isBlank() && it.isNotBlank() },
                        )
                    }
                val catalogById = uiGrades.associateBy { it.id }

                fun count(stage: EducationStage) = rows.count { (it.stage ?: catalogById[it.gradeId]?.stage) == stage }.toString()
                val summary =
                    StudentsSummary(
                        rows.size.toString(),
                        count(EducationStage.Secondary),
                        count(EducationStage.Prep),
                        count(EducationStage.Primary),
                    )
                val payments =
                    balances.map {
                        StudentMonthlyPayment(
                            "${it.studentId}:${it.groupId}",
                            it.studentId,
                            it.groupId,
                            it.groupName.localizedValue(),
                            it.teacherName.localizedValue(),
                            today.monthStart(),
                            it.remainingMinor / 100,
                            it.remainingMinor,
                        )
                    }
                records = students.associateBy { it.id }
                this@StudentsStateHolder.groups = groups
                directoryLoaded = true
                mutableState.update {
                    it.copy(
                        students = rows,
                        summary = summary,
                        asOf = today,
                        dateLabel = today.toString(),
                        monthlyPayments = payments,
                        details = details(),
                        addStudent = it.addStudent?.let { form ->
                            val code = if (form.editingId == null && form.code.isBlank()) nextStudentCode() else form.code
                            form.copy(
                                code = code,
                                groupOptions = availableGroups(form.selectedGroupIds, form.selectedGradeId),
                                canSubmit = form.name.isNotBlank() && code.isNotBlank() && form.selectedGradeId != null &&
                                    form.parentPhone.isNotBlank() && form.invalidDiscounts.isEmpty(),
                            )
                        },
                    )
                }
            }.catch { mutableFeedback.emit(failure) }.collect {}
        }
    }

    private fun details(): StudentDetailsState? {
        val student = records[detailId.value] ?: return null
        val groupLabels = groups.associate { it.id to it.name }
        return StudentDetailsState(
            student.id,
            student.fullName,
            student.code,
            student.studentPhone,
            student.parentPhone,
            student.activeSchedule(groups).map { group ->
                val ar = group.schedule.scheduleText(AppLanguage.Arabic)
                val en = group.schedule.scheduleText(AppLanguage.English)
                LocalizedText(
                    "${group.name} · ${ar.ifBlank { "لم يحدد موعد" }}",
                    "${group.name} · ${en.ifBlank { "No times set" }}",
                )
            },
            history.filter { it.person.id == student.id }.map { it.summary(groupLabels[it.person.groupId]) },
        )
    }

    private fun Student.gradeId(): String = grades.firstOrNull { it.id == gradeLevel }?.id ?: allGradeLabels[gradeLevel]?.id.orEmpty()

    private fun Student.uiSystem(): StudentSystem? =
        when (system) {
            EducationSystem.ARABIC -> StudentSystem.Arabic
            EducationSystem.LANGUAGES -> StudentSystem.Languages
            else -> null
        }

    private fun availableGroups(selected: Set<String>, gradeId: String? = null): List<StudentGroupOption> =
        groups.filter {
            (it.status == GroupStatus.ACTIVE || it.id in selected) &&
                (if (gradeId == null) it.id in selected else it.gradeId == gradeId)
        }
            .map { StudentGroupOption(it.id, it.name) }

    private fun formFor(student: Student?): AddStudentState =
        AddStudentState(
            name = student?.fullName.orEmpty(),
            code = student?.code ?: if (directoryLoaded) nextStudentCode() else "",
            grades = uiGrades,
            selectedGradeId = student?.gradeId()?.takeIf { it.isNotBlank() },
            system = student?.uiSystem() ?: StudentSystem.Arabic,
            parentPhone = student?.parentPhone.orEmpty(),
            canSubmit = student != null && student.gradeId().isNotBlank() && student.parentPhone.isNotBlank(),
            editingId = student?.id,
            groupOptions = availableGroups(student?.enrolledGroupIds.orEmpty(), student?.gradeId()),
            selectedGroupIds = student?.enrolledGroupIds.orEmpty(),
            studentPhone = student?.studentPhone.orEmpty(),
            discounts =
                student?.groupDiscounts.orEmpty().mapValues { (_, discount) ->
                    kotlin.math
                        .round(discount * 100)
                        .toLong()
                        .decimalMinor()
                },
        )

    private fun editForm(edit: (AddStudentState) -> AddStudentState) {
        val form = state.addStudent ?: return
        if (form.isSubmitting) return
        val updated = edit(form)
        val gradeChanged = updated.selectedGradeId != form.selectedGradeId
        val invalidDiscounts =
            updated.selectedGroupIds.filterTo(mutableSetOf()) { id ->
                val discount = updated.discounts[id].orEmpty()
                discount.isNotBlank() && discount.parseMinor() == null
            }
        state =
            state.copy(
                addStudent =
                    updated.copy(
                        message = null,
                        groupOptions = if (gradeChanged) availableGroups(emptySet(), updated.selectedGradeId) else updated.groupOptions,
                        selectedGroupIds = if (gradeChanged) emptySet() else updated.selectedGroupIds,
                        canSubmit =
                            updated.name.isNotBlank() &&
                                updated.code.isNotBlank() &&
                                updated.selectedGradeId != null &&
                                updated.parentPhone.isNotBlank() &&
                                invalidDiscounts.isEmpty(),
                        invalidDiscounts = invalidDiscounts,
                    ),
            )
    }

    private fun nextStudentCode(): String = nextSequentialCode("ST", records.values.map { it.code })

    private fun nextSequentialCode(prefix: String, existing: Collection<String>): String = services.personCodes.next(prefix, existing)

    fun onEvent(event: StudentsEvent) {
        when (event) {
            is StudentsEvent.StageSelected ->
                state =
                    state.copy(stageFilter = event.filter, selectedGradeId = null, gradeMenuExpanded = false)
            is StudentsEvent.SearchChanged -> state = state.copy(searchQuery = event.value)
            is StudentsEvent.GradeFilterMenuChanged -> state = state.copy(gradeMenuExpanded = event.expanded)
            is StudentsEvent.GradeFilterSelected ->
                if (event.id == null ||
                    state.availableGrades.any { it.id == event.id }
                ) {
                    state =
                        state.copy(selectedGradeId = event.id, gradeMenuExpanded = false)
                }
            StudentsEvent.OpenAddStudent -> state = state.copy(addStudent = formFor(null))
            StudentsEvent.DismissAddStudent -> if (state.addStudent?.isSubmitting != true) state = state.copy(addStudent = null)
            is StudentsEvent.NameChanged -> editForm { it.copy(name = event.value) }
            is StudentsEvent.CodeChanged -> editForm { it.copy(code = event.value) }
            is StudentsEvent.AddGradeMenuChanged -> editForm { it.copy(gradeMenuExpanded = event.expanded) }
            is StudentsEvent.AddGradeSelected ->
                editForm { form ->
                    if (form.grades.any { it.id == event.id }) {
                        form.copy(
                            selectedGradeId = event.id,
                            gradeMenuExpanded = false,
                        )
                    } else {
                        form
                    }
                }
            is StudentsEvent.SystemSelected -> editForm { it.copy(system = event.system) }
            is StudentsEvent.ParentPhoneChanged -> editForm { it.copy(parentPhone = event.value) }
            is StudentsEvent.StudentPhoneChanged -> editForm { it.copy(studentPhone = event.value) }
            is StudentsEvent.DiscountChanged -> editForm { it.copy(discounts = it.discounts + (event.groupId to event.value)) }
            is StudentsEvent.OpenDetails ->
                if (event.id in records) {
                    detailId.value = event.id
                    state = state.copy(details = details())
                }
            StudentsEvent.CloseDetails -> {
                detailId.value = null
                state = state.copy(details = null)
            }
            StudentsEvent.SubmitAddStudent -> save()
            is StudentsEvent.EditStudent ->
                records[event.id]?.let { state = state.copy(addStudent = formFor(it)) }
            is StudentsEvent.DeleteStudent ->
                if (canDelete()) records[event.id]?.let { state = state.copy(deleteStudent = DeleteStudentState(it.id, it.fullName)) }
            StudentsEvent.DismissDelete -> if (state.deleteStudent?.busy != true) state = state.copy(deleteStudent = null)
            StudentsEvent.ConfirmDelete -> delete()
            is StudentsEvent.ToggleGroup ->
                editForm { form ->
                    if (form.groupOptions.none { it.id == event.id }) {
                        form
                    } else {
                        form.copy(
                            selectedGroupIds =
                                if (event.id in
                                    form.selectedGroupIds
                                ) {
                                    form.selectedGroupIds - event.id
                                } else {
                                    form.selectedGroupIds + event.id
                                },
                        )
                    }
                }
            is StudentsEvent.SendReminder -> remind(listOf(event.studentId))
            is StudentsEvent.RemindAll -> remind(event.studentIds)
            is StudentsEvent.Navigate,
            is StudentsEvent.PayNow,
            is StudentsEvent.LanguageSelected,
            StudentsEvent.LogOut,
            StudentsEvent.OpenNotifications,
            -> Unit
        }
    }

    private fun save() {
        val form = state.addStudent ?: return
        if (!form.canSubmit || form.isSubmitting) return
        state = state.copy(addStudent = form.copy(isSubmitting = true, message = null))
        scope.launch {
            val result =
                directoryResult {
                    val grade = grades.first { it.id == form.selectedGradeId }
                    val existing = form.editingId?.let { requireNotNull(records[it]) }
                    val student = existing ?: Student(services.ids.newId(), form.name)
                    services.registerStudent(
                        student.copy(
                            fullName = form.name,
                            code = form.code,
                            gradeLevel = grade.id,
                            stage = grade.stage,
                            system = if (form.system == StudentSystem.Arabic) EducationSystem.ARABIC else EducationSystem.LANGUAGES,
                            parentPhone = form.parentPhone,
                            enrolledGroupIds = form.selectedGroupIds,
                            studentPhone = form.studentPhone.trim().ifEmpty { null },
                            groupDiscounts = form.selectedGroupIds.associateWith { (form.discounts[it]?.parseMinor() ?: 0L) / 100.0 },
                        ),
                    )
                }
            state = state.copy(addStudent = if (result.isSuccess) null else form.copy(message = failure))
        }
    }

    private fun delete() {
        if (!canDelete()) return
        val deletion = state.deleteStudent ?: return
        if (deletion.busy) return
        state = state.copy(deleteStudent = deletion.copy(busy = true, message = null))
        scope.launch {
            val result = directoryResult { services.students.delete(deletion.id) }
            state = state.copy(deleteStudent = if (result.isSuccess) null else deletion.copy(message = failure))
        }
    }

    private fun remind(ids: List<String>) {
        if (state.isReminding || state.stageFilter != StudentStageFilter.Overdue) return
        val visible = state.visibleOverdueRows.map { it.student.id }.toSet()
        val targets = state.overdueRows.filter { it.student.id in ids && it.student.id in visible }.groupBy { it.student.id }
        if (targets.isEmpty()) return
        state = state.copy(isReminding = true)
        scope.launch {
            var queued = 0
            try {
                targets.values.forEach { balances ->
                    val student = balances.first().student
                    val total = balances.sumOf { it.payment.remainingMinor }.decimalMinor()
                    val text = "تذكير: اشتراك الطالب ${student.name} لهذا الشهر له رصيد مستحق بقيمة $total ج.م. — سنتر ون"
                    if (services.reminder(student.code, text).isSuccess) queued++
                }
                mutableFeedback.emit(
                    LocalizedText(
                        "تمت إضافة $queued من ${targets.size} تذكيرات إلى قائمة الانتظار.",
                        "Queued $queued of ${targets.size} reminders.",
                    ).resolve(language),
                )
            } finally {
                state = state.copy(isReminding = false)
            }
        }
    }
}
