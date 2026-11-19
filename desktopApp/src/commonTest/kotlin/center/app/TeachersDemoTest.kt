package center.app

import center.feature.teachers.TeachersEvent
import center.feature.teachers.TeachersModal
import center.feature.teachers.TeachersPreviewData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeachersDemoTest {
    @Test
    fun simulatedSalaryPaymentKeepsFullSalaryAndCollectionDeficitIndependent() {
        val state = TeachersPreviewData.state()
        val original = state.teachers.first()
        val opened = state.applyTeacherDemoEvent(TeachersEvent.OpenTeacher(original.id))
        val paid = opened.applyTeacherDemoEvent(TeachersEvent.PaySalary(original.id))
        assertEquals(4_000L, paid.teachers.first().monthlySalaryEgp)
        assertEquals(
            600L,
            paid.teachers
                .first()
                .unpaidStudents
                .sumOf { it.subscriptionEgp },
        )
        assertEquals(original.copy(salaryPaidInDemo = true), paid.teachers.first())
        assertEquals(paid, paid.applyTeacherDemoEvent(TeachersEvent.PaySalary(original.id)))
    }

    @Test
    fun addTeacherRequiresContactAndSubjectAndDoesNotInventSalary() {
        val opened = TeachersPreviewData.state().applyTeacherDemoEvent(TeachersEvent.OpenAddTeacher)
        assertEquals(opened, opened.applyTeacherDemoEvent(TeachersEvent.SaveTeacher))
        val saved =
            opened
                .applyTeacherDemoEvent(TeachersEvent.NameChanged("  Ali Hassan  "))
                .applyTeacherDemoEvent(TeachersEvent.PhoneChanged("01000001004"))
                .applyTeacherDemoEvent(TeachersEvent.SubjectChanged("Chemistry"))
                .applyTeacherDemoEvent(TeachersEvent.NotesChanged("Available on Sundays"))
                .applyTeacherDemoEvent(TeachersEvent.SaveTeacher)
        val teacher = saved.teachers.first()
        assertEquals("Ali Hassan", teacher.name.english)
        assertEquals("Available on Sundays", teacher.notes)
        assertNull(teacher.monthlySalaryEgp)
        assertEquals(TeachersModal.Closed, saved.modal)
        val details = saved.applyTeacherDemoEvent(TeachersEvent.OpenTeacher(teacher.id))
        assertEquals(details, details.applyTeacherDemoEvent(TeachersEvent.PaySalary(teacher.id)))
        assertTrue(
            saved.teachers
                .map { it.id }
                .distinct()
                .size == saved.teachers.size,
        )
    }
}
