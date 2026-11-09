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
import center.app.resources.feature_unavailable
import center.app.resources.student_demo_not_saved
import center.app.resources.student_row_action_unavailable
import center.feature.students.AddStudentState
import center.feature.students.StudentsDestination
import center.feature.students.StudentsEvent
import center.feature.students.StudentsPreviewData
import center.feature.students.StudentsScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** UI-only composition boundary. Replace with lifecycle-owned state when connecting domain operations. */
@Composable
internal fun StudentsRoute(root: RootComponent) {
    var state by remember { mutableStateOf(StudentsPreviewData.state()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(Res.string.feature_unavailable)
    val notSaved = stringResource(Res.string.student_demo_not_saved)
    val rowActionUnavailable = stringResource(Res.string.student_row_action_unavailable)

    fun editForm(edit: (AddStudentState) -> AddStudentState) {
        val form = state.addStudent ?: return
        if (form.isSubmitting) return
        val updated = edit(form)
        state =
            state.copy(
                addStudent =
                    updated.copy(
                        message = null,
                        canSubmit =
                            updated.name.isNotBlank() &&
                                updated.code.isNotBlank() &&
                                updated.selectedGradeId != null &&
                                updated.parentPhone.isNotBlank(),
                    ),
            )
    }

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    Box(Modifier.fillMaxSize()) {
        StudentsScreen(state, onEvent = { event ->
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
                StudentsEvent.OpenAddStudent -> state = state.copy(addStudent = StudentsPreviewData.addStudent())
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
                StudentsEvent.SubmitAddStudent ->
                    state.addStudent?.let { form ->
                        if (form.canSubmit && !form.isSubmitting) state = state.copy(addStudent = form.copy(message = notSaved))
                    }
                is StudentsEvent.EditStudent, is StudentsEvent.DeleteStudent -> notify(rowActionUnavailable)
                is StudentsEvent.Navigate ->
                    when (event.destination) {
                        StudentsDestination.Home -> root.navigate(Destination.Dashboard)
                        StudentsDestination.Financials -> root.navigate(Destination.Financials)
                        StudentsDestination.Reports -> root.navigate(Destination.Reports)
                        StudentsDestination.Students -> Unit
                        StudentsDestination.Courses, StudentsDestination.Schedule, StudentsDestination.Settings -> notify(unavailable)
                    }
                StudentsEvent.OpenNotifications, StudentsEvent.RequestEnglish, StudentsEvent.LogOut -> notify(unavailable)
            }
        })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
