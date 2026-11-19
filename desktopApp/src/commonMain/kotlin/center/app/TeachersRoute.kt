package center.app

import androidx.compose.runtime.Composable
import center.feature.teachers.Teacher
import center.feature.teachers.TeacherDraft
import center.feature.teachers.TeachersEvent
import center.feature.teachers.TeachersModal
import center.feature.teachers.TeachersScreen
import center.feature.teachers.TeachersState
import center.ui.localization.AppLanguage
import center.ui.localization.LocalizedText

@Composable
internal fun TeachersRoute(
    root: RootComponent,
    state: TeachersState,
    onStateChange: ((TeachersState) -> TeachersState) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    TeachersScreen(state, onEvent = { event ->
        when (event) {
            is TeachersEvent.Navigate -> {
                onStateChange { it.copy(modal = TeachersModal.Closed) }
                root.navigate(event.destination)
            }
            is TeachersEvent.LanguageSelected -> onLanguageSelected(event.language)
            else -> onStateChange { it.applyTeacherDemoEvent(event) }
        }
    })
}

internal fun TeachersState.applyTeacherDemoEvent(event: TeachersEvent): TeachersState =
    when (event) {
        is TeachersEvent.SearchChanged -> copy(query = event.query)
        is TeachersEvent.OpenTeacher ->
            if (teachers.any { it.id == event.id }) copy(modal = TeachersModal.Details(event.id)) else this
        is TeachersEvent.PaySalary -> paySalaryInDemo(event.id)
        TeachersEvent.OpenAddTeacher -> copy(modal = TeachersModal.Add())
        TeachersEvent.CloseModal -> copy(modal = TeachersModal.Closed)
        is TeachersEvent.NameChanged -> editTeacherDraft { it.copy(name = event.value) }
        is TeachersEvent.PhoneChanged -> editTeacherDraft { it.copy(phone = event.value) }
        is TeachersEvent.SubjectChanged -> editTeacherDraft { it.copy(subject = event.value) }
        is TeachersEvent.NotesChanged -> editTeacherDraft { it.copy(notes = event.value) }
        TeachersEvent.SaveTeacher -> saveTeacherInDemo()
        is TeachersEvent.Navigate, is TeachersEvent.LanguageSelected -> this
    }

private fun TeachersState.paySalaryInDemo(teacherId: Int): TeachersState {
    if ((modal as? TeachersModal.Details)?.teacherId != teacherId) return this
    val teacher = teachers.firstOrNull { it.id == teacherId } ?: return this
    if (teacher.salaryPaidInDemo || (teacher.monthlySalaryEgp ?: 0) <= 0) return this
    return copy(teachers = teachers.map { if (it.id == teacherId) it.copy(salaryPaidInDemo = true) else it })
}

private fun TeachersState.editTeacherDraft(edit: (TeacherDraft) -> TeacherDraft): TeachersState {
    val add = modal as? TeachersModal.Add ?: return this
    return copy(modal = TeachersModal.Add(edit(add.draft)))
}

private fun TeachersState.saveTeacherInDemo(): TeachersState {
    val draft = (modal as? TeachersModal.Add)?.draft ?: return this
    if (!draft.canSave) return this
    val name = draft.name.trim()
    val subject = draft.subject.trim()
    val teacher =
        Teacher(
            id = (teachers.maxOfOrNull { it.id } ?: 0) + 1,
            name = LocalizedText(name, name),
            phone = draft.phone.trim(),
            subject = LocalizedText(subject, subject),
            monthlySalaryEgp = null,
            notes = draft.notes.trim(),
        )
    return copy(teachers = listOf(teacher) + teachers, query = "", modal = TeachersModal.Closed)
}
