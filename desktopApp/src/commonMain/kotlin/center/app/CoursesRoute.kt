package center.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import center.app.resources.Res
import center.app.resources.course_demo_not_saved
import center.app.resources.feature_unavailable
import center.feature.courses.CoursesDestination
import center.feature.courses.CoursesEvent
import center.feature.courses.CoursesModal
import center.feature.courses.CoursesPreviewData
import center.feature.courses.CoursesScreen
import center.feature.courses.DeleteGroupState
import center.feature.courses.GroupFormState
import center.feature.courses.GroupTimeSlotState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Interactive fixture boundary; no group, timetable or financial data is persisted. */
@Composable
internal fun CoursesRoute(root: RootComponent) {
    var state by remember { mutableStateOf(CoursesPreviewData.state()) }
    var nextSlot by remember { mutableStateOf(2L) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notSaved = stringResource(Res.string.course_demo_not_saved)
    val unavailable = stringResource(Res.string.feature_unavailable)

    fun editForm(edit: (GroupFormState) -> GroupFormState) {
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

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    Box(Modifier.fillMaxSize()) {
        CoursesScreen(state, onEvent = { event ->
            when (event) {
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
                            modal = CoursesModal.Form(CoursesPreviewData.newForm()),
                            expandedGroupMenuId = null,
                        )
                is CoursesEvent.EditGroup ->
                    state.groups.firstOrNull { it.id == event.id }?.let { group ->
                        state =
                            state.copy(
                                modal = CoursesModal.Form(CoursesPreviewData.editForm(group)),
                                expandedGroupMenuId = null,
                            )
                    }
                is CoursesEvent.DeactivateGroup -> {
                    state = state.copy(expandedGroupMenuId = null)
                    notify(notSaved)
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
                CoursesEvent.SubmitGroup ->
                    (state.modal as? CoursesModal.Form)?.let { modal ->
                        if (modal.state.canSubmit &&
                            !modal.state.isSubmitting
                        ) {
                            state = state.copy(modal = CoursesModal.Form(modal.state.copy(message = notSaved)))
                        }
                    }
                CoursesEvent.ConfirmDeleteGroup ->
                    (state.modal as? CoursesModal.Delete)?.let { modal ->
                        if (!modal.state.isDeleting) state = state.copy(modal = CoursesModal.Delete(modal.state.copy(message = notSaved)))
                    }
                is CoursesEvent.Navigate ->
                    when (event.destination) {
                        CoursesDestination.Home -> root.navigate(Destination.Dashboard)
                        CoursesDestination.Students -> root.navigate(Destination.Students)
                        CoursesDestination.Financials -> root.navigate(Destination.Financials)
                        CoursesDestination.Reports -> root.navigate(Destination.Reports)
                        CoursesDestination.Courses -> Unit
                        CoursesDestination.Schedule, CoursesDestination.Settings -> notify(unavailable)
                    }
                CoursesEvent.OpenNotifications, CoursesEvent.RequestEnglish, CoursesEvent.LogOut -> notify(unavailable)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
