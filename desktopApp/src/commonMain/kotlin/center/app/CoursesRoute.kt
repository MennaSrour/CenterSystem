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
import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupScheduleSlot
import center.domain.model.GroupStatus
import center.feature.courses.CourseGrade
import center.feature.courses.CourseGroup
import center.feature.courses.CourseRoom
import center.feature.courses.CourseSchedule
import center.feature.courses.CourseStage
import center.feature.courses.CourseSystem
import center.feature.courses.CourseTeacher
import center.feature.courses.CoursesDestination
import center.feature.courses.CoursesEvent
import center.feature.courses.CoursesModal
import center.feature.courses.CoursesScreen
import center.feature.courses.CoursesState
import center.feature.courses.CoursesSummary
import center.feature.courses.DeleteGroupState
import center.feature.courses.GroupFormMode
import center.feature.courses.GroupFormState
import center.feature.courses.GroupTimeSlotState
import center.feature.courses.ScheduleDay
import center.ui.localization.AppLanguage
import center.ui.localization.LocalAppLanguage
import center.ui.localization.LocalizedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CoursesRoute(
    root: RootComponent,
    services: DirectoryServices,
) {
    val language = LocalAppLanguage.current
    val scope = rememberDirectoryScope(language)
    val holder = remember(services, scope, language) { CoursesStateHolder(services, scope, language) }
    val state by holder.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val unavailable = stringResource(Res.string.feature_unavailable)

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }
    LaunchedEffect(holder) { holder.feedback.collect { snackbar.showSnackbar(it) } }
    Box(Modifier.fillMaxSize()) {
        CoursesScreen(state, onEvent = { event ->
            when (event) {
                is CoursesEvent.Navigate ->
                    when (event.destination) {
                        CoursesDestination.Home -> root.navigate(Destination.Dashboard)
                        CoursesDestination.Students -> root.navigate(Destination.Students)
                        CoursesDestination.Financials -> root.navigate(Destination.Financials)
                        CoursesDestination.Reports -> root.navigate(Destination.Reports)
                        CoursesDestination.Courses -> Unit
                        CoursesDestination.Teachers -> root.navigate(Destination.Teachers)
                        CoursesDestination.Settings -> root.navigate(Destination.Settings)
                        CoursesDestination.Schedule -> notify(unavailable)
                    }
                CoursesEvent.OpenNotifications, CoursesEvent.RequestEnglish, CoursesEvent.LogOut -> notify(unavailable)
                else -> holder.onEvent(event)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

internal class CoursesStateHolder(
    private val services: DirectoryServices,
    private val scope: CoroutineScope,
    language: AppLanguage,
) {
    private val grades =
        gradeCatalog(language).map {
            CourseGrade(
                it.id,
                it.label,
                CourseStage.valueOf(
                    it.stage.name
                        .lowercase()
                        .replaceFirstChar(Char::uppercase),
                ),
            )
        }
    private val rooms = (1..8).map { CourseRoom(it.toString(), if (language == AppLanguage.Arabic) "قاعة $it" else "Room $it") }
    private val mutableState =
        MutableStateFlow(
            CoursesState(CoursesSummary("0", "0", "0"), emptyList(), emptyList(), grades, rooms, "", services.today().toString()),
        )
    val uiState = mutableState.asStateFlow()
    private val mutableFeedback = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedback = mutableFeedback.asSharedFlow()
    private val failure =
        LocalizedText(
            "تعذر حفظ التغيير. تحقق من البيانات والسجلات المرتبطة.",
            "Could not save the change. Check the values and linked records.",
        ).resolve(language)
    private var state: CoursesState
        get() = mutableState.value
        set(value) {
            mutableState.value = value
        }
    private var nextSlot = 2L

    init {
        scope.launch {
            combine(
                services.groups.observeGroups(),
                services.teachers.observeTeachers(),
                services.students.observeStudents(),
            ) { groups, teachers, students ->
                val counts =
                    students
                        .filter { it.isActive }
                        .flatMap { it.enrolledGroupIds }
                        .groupingBy { it }
                        .eachCount()
                val directory = teachers.map { CourseTeacher(it.id, it.fullName) }
                val models =
                    groups.map { group ->
                        CourseGroup(
                            group.id,
                            group.name,
                            group.teacherId,
                            group.gradeId.orEmpty(),
                            group.system.toUi(),
                            group.monthlyPriceMinor?.decimalMinor().orEmpty(),
                            group.commissionBasisPoints
                                ?.toLong()
                                ?.decimalMinor()
                                .orEmpty(),
                            group.roomNumber?.toString().orEmpty(),
                            counts[group.id] ?: 0,
                            group.maxCapacity ?: 0,
                            group.schedule.mapIndexed {
                                index,
                                it,
                                ->
                                CourseSchedule(
                                    "${group.id}-$index",
                                    ScheduleDay.valueOf(
                                        it.day.name
                                            .lowercase()
                                            .replaceFirstChar(Char::uppercase),
                                    ),
                                    it.from.toString(),
                                    it.to.toString(),
                                )
                            },
                            group.status == GroupStatus.ACTIVE,
                        )
                    }
                directory to models
            }.catch { mutableFeedback.emit(failure) }.collect { (directory, groups) ->
                mutableState.update {
                    val form = it.modal as? CoursesModal.Form
                    it.copy(
                        teachers = directory,
                        groups = groups,
                        summary =
                            CoursesSummary(
                                groups.size.toString(),
                                groups
                                    .count { group ->
                                        group.isActive
                                    }.toString(),
                                directory.size.toString(),
                            ),
                        modal = form?.let { modal -> CoursesModal.Form(modal.state.copy(teachers = directory)) } ?: it.modal,
                    )
                }
            }
        }
    }

    private fun editForm(edit: (GroupFormState) -> GroupFormState) {
        val modal = state.modal as? CoursesModal.Form ?: return
        if (modal.state.isSubmitting) return
        val form = edit(modal.state)
        val complete =
            listOf(form.subject, form.monthlyPrice, form.teacherCommission, form.maxCapacity).all { it.isNotBlank() } &&
                form.selectedTeacherId != null &&
                form.selectedGradeId != null &&
                form.selectedRoomId != null &&
                form.timeSlots.isNotEmpty() &&
                form.timeSlots.all { it.day != null && it.from.isNotBlank() && it.to.isNotBlank() }
        state = state.copy(modal = CoursesModal.Form(form.copy(canSubmit = complete, message = null)))
    }

    fun onEvent(event: CoursesEvent) {
        when (event) {
            is CoursesEvent.TeacherFilterSelected -> state = state.copy(selectedTeacherId = event.id)
            is CoursesEvent.GradeFilterSelected -> state = state.copy(selectedGradeId = event.id)
            is CoursesEvent.StageSelected -> state = state.copy(stageFilter = event.filter, expandedGroupMenuId = null)
            is CoursesEvent.SearchChanged -> state = state.copy(searchQuery = event.value, expandedGroupMenuId = null)
            is CoursesEvent.GroupMenuChanged ->
                if (state.modal == CoursesModal.Closed &&
                    (event.groupId == null || state.groups.any { it.id == event.groupId })
                ) {
                    state =
                        state.copy(expandedGroupMenuId = event.groupId)
                }
            CoursesEvent.OpenAddGroup ->
                state =
                    state.copy(
                        modal = CoursesModal.Form(newForm()),
                        expandedGroupMenuId = null,
                    )
            is CoursesEvent.EditGroup ->
                state.groups.firstOrNull { it.id == event.id }?.let { group ->
                    state =
                        state.copy(
                            modal = CoursesModal.Form(newForm(group)),
                            expandedGroupMenuId = null,
                        )
                }
            is CoursesEvent.DeactivateGroup -> {
                state = state.copy(expandedGroupMenuId = null)
                scope.launch { if (directoryResult { services.groups.archive(event.id) }.isFailure) mutableFeedback.emit(failure) }
            }
            is CoursesEvent.RequestDeleteGroup ->
                state.groups.firstOrNull { it.id == event.id }?.let { group ->
                    state =
                        state.copy(
                            modal = CoursesModal.Delete(DeleteGroupState(group.id, group.subject)),
                            expandedGroupMenuId = null,
                        )
                }
            CoursesEvent.DismissModal -> {
                val busy =
                    when (val modal = state.modal) {
                        CoursesModal.Closed -> false
                        is CoursesModal.Form -> modal.state.isSubmitting
                        is CoursesModal.Delete -> modal.state.isDeleting
                    }
                if (!busy) state = state.copy(modal = CoursesModal.Closed)
            }
            is CoursesEvent.SubjectChanged -> editForm { it.copy(subject = event.value) }
            is CoursesEvent.DropdownChanged -> editForm { it.copy(expandedDropdown = event.dropdown, expandedDaySlotId = null) }
            is CoursesEvent.TeacherSelected ->
                editForm {
                    if (it.teachers.any { teacher ->
                            teacher.id == event.id
                        }
                    ) {
                        it.copy(selectedTeacherId = event.id, expandedDropdown = null)
                    } else {
                        it
                    }
                }
            is CoursesEvent.GradeSelected ->
                editForm {
                    if (it.grades.any { grade ->
                            grade.id == event.id
                        }
                    ) {
                        it.copy(selectedGradeId = event.id, expandedDropdown = null)
                    } else {
                        it
                    }
                }
            is CoursesEvent.SystemSelected -> editForm { it.copy(system = event.system) }
            is CoursesEvent.MonthlyPriceChanged -> editForm { it.copy(monthlyPrice = event.value) }
            is CoursesEvent.CommissionChanged -> editForm { it.copy(teacherCommission = event.value) }
            is CoursesEvent.MaxCapacityChanged -> editForm { it.copy(maxCapacity = event.value) }
            is CoursesEvent.RoomSelected ->
                editForm {
                    if (it.rooms.any { room ->
                            room.id == event.id
                        }
                    ) {
                        it.copy(selectedRoomId = event.id, expandedDropdown = null)
                    } else {
                        it
                    }
                }
            CoursesEvent.AddTimeSlot -> {
                val modal = state.modal as? CoursesModal.Form
                if (modal != null && !modal.state.isSubmitting) {
                    val slot = GroupTimeSlotState("draft-slot-${nextSlot++}")
                    editForm { it.copy(timeSlots = it.timeSlots + slot) }
                }
            }
            is CoursesEvent.RemoveTimeSlot ->
                editForm {
                    it.copy(
                        timeSlots =
                            it.timeSlots.filterNot { slot ->
                                slot.id == event.id
                            },
                        expandedDaySlotId = null,
                    )
                }
            is CoursesEvent.DayMenuChanged -> editForm { it.copy(expandedDaySlotId = event.slotId, expandedDropdown = null) }
            is CoursesEvent.DaySelected ->
                editForm {
                    it.copy(
                        timeSlots =
                            it.timeSlots.map { slot ->
                                if (slot.id ==
                                    event.slotId
                                ) {
                                    slot.copy(day = event.day)
                                } else {
                                    slot
                                }
                            },
                        expandedDaySlotId = null,
                    )
                }
            is CoursesEvent.FromTimeChanged ->
                editForm {
                    it.copy(
                        timeSlots =
                            it.timeSlots.map { slot ->
                                if (slot.id ==
                                    event.slotId
                                ) {
                                    slot.copy(from = event.value)
                                } else {
                                    slot
                                }
                            },
                    )
                }
            is CoursesEvent.ToTimeChanged ->
                editForm {
                    it.copy(
                        timeSlots =
                            it.timeSlots.map { slot ->
                                if (slot.id ==
                                    event.slotId
                                ) {
                                    slot.copy(to = event.value)
                                } else {
                                    slot
                                }
                            },
                    )
                }
            CoursesEvent.SubmitGroup -> submit()
            CoursesEvent.ConfirmDeleteGroup -> delete()
            is CoursesEvent.Navigate, CoursesEvent.OpenNotifications, CoursesEvent.RequestEnglish, CoursesEvent.LogOut -> Unit
        }
    }

    private fun newForm(group: CourseGroup? = null): GroupFormState =
        GroupFormState(
            mode = group?.let { GroupFormMode.Edit(it.id) } ?: GroupFormMode.Add,
            subject = group?.subject.orEmpty(),
            teachers = state.teachers,
            grades = grades,
            rooms = rooms,
            selectedTeacherId = group?.teacherId,
            selectedGradeId = group?.gradeId?.takeIf { it.isNotBlank() },
            system = group?.system ?: CourseSystem.Arabic,
            monthlyPrice = group?.monthlyPrice.orEmpty(),
            teacherCommission = group?.teacherCommission?.takeIf { it.isNotBlank() } ?: "70",
            maxCapacity =
                group
                    ?.maxCapacity
                    ?.takeIf { it > 0 }
                    ?.toString()
                    .orEmpty(),
            selectedRoomId = group?.roomId?.takeIf { it.isNotBlank() },
            timeSlots =
                group
                    ?.schedule
                    .orEmpty()
                    .map { GroupTimeSlotState(it.id, it.day, it.from, it.to) }
                    .ifEmpty { listOf(GroupTimeSlotState("draft-slot-${nextSlot++}")) },
            canSubmit = group?.maxCapacity?.let { it > 0 } == true && group.monthlyPrice.isNotBlank() && group.gradeId.isNotBlank(),
        )

    private fun submit() {
        val form = (state.modal as? CoursesModal.Form)?.state ?: return
        if (!form.canSubmit || form.isSubmitting) return
        state = state.copy(modal = CoursesModal.Form(form.copy(isSubmitting = true, message = null)))
        scope.launch {
            val result =
                directoryResult {
                    val mode = form.mode as? GroupFormMode.Edit
                    val id = mode?.groupId ?: services.ids.newId()
                    val archived = state.groups.firstOrNull { it.id == id }?.isActive == false
                    services.groups.save(
                        ClassGroup(
                            id,
                            form.subject.trim(),
                            requireNotNull(form.selectedTeacherId),
                            requireNotNull(form.monthlyPrice.parseMinor()),
                            requireNotNull(form.teacherCommission.parseMinor()).also { require(it <= 10_000) }.toInt(),
                            requireNotNull(form.selectedRoomId?.toIntOrNull()),
                            form.system.toDomain(),
                            if (archived) GroupStatus.ARCHIVED else GroupStatus.ACTIVE,
                            form.timeSlots.map {
                                GroupScheduleSlot(
                                    DayOfWeek.valueOf(requireNotNull(it.day).name.uppercase()),
                                    LocalTime.parse(it.from.trim()),
                                    LocalTime.parse(it.to.trim()),
                                )
                            },
                            requireNotNull(form.selectedGradeId),
                            requireNotNull(form.maxCapacity.trim().toIntOrNull()).also { require(it > 0) },
                        ),
                    )
                }
            state = state.copy(modal = if (result.isSuccess) CoursesModal.Closed else CoursesModal.Form(form.copy(message = failure)))
        }
    }

    private fun delete() {
        val form = (state.modal as? CoursesModal.Delete)?.state ?: return
        if (form.isDeleting) return
        state = state.copy(modal = CoursesModal.Delete(form.copy(isDeleting = true, message = null)))
        scope.launch {
            val result = directoryResult { services.groups.delete(form.groupId) }
            state = state.copy(modal = if (result.isSuccess) CoursesModal.Closed else CoursesModal.Delete(form.copy(message = failure)))
        }
    }
}

private fun EducationSystem.toUi(): CourseSystem =
    when (this) {
        EducationSystem.ARABIC -> CourseSystem.Arabic
        EducationSystem.LANGUAGES -> CourseSystem.Languages
        EducationSystem.BOTH -> CourseSystem.Mixed
    }

private fun CourseSystem.toDomain(): EducationSystem =
    when (this) {
        CourseSystem.Arabic -> EducationSystem.ARABIC
        CourseSystem.Languages -> EducationSystem.LANGUAGES
        CourseSystem.Mixed -> EducationSystem.BOTH
    }
