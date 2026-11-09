package center.feature.students

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudentsStateTest {
    @Test
    fun stageFiltersSelectTheCorrectRows() {
        val state = StudentsPreviewData.state()
        assertEquals(listOf("student-4"), state.copy(stageFilter = StudentStageFilter.Primary).visibleStudents.map { it.id })
        assertEquals(listOf("student-2"), state.copy(stageFilter = StudentStageFilter.Prep).visibleStudents.map { it.id })
        assertEquals(listOf("student-1", "student-3"), state.copy(stageFilter = StudentStageFilter.Secondary).visibleStudents.map { it.id })
        assertEquals(4, state.visibleStudents.size)
    }

    @Test
    fun gradeAndSearchFiltersCombineWithoutChangingTheOriginalSnapshot() {
        val state = StudentsPreviewData.state()
        val filtered = state.copy(stageFilter = StudentStageFilter.Secondary, selectedGradeId = "secondary-3", searchQuery = "  AHMED  ")
        assertEquals(listOf("student-1"), filtered.visibleStudents.map { it.id })
        assertEquals(listOf("student-2"), state.copy(searchQuery = "سارة").visibleStudents.map { it.id })
        assertEquals(listOf("student-3"), state.copy(searchQuery = "st-10452").visibleStudents.map { it.id })
        assertTrue(state.copy(searchQuery = "missing").visibleStudents.isEmpty())
        assertEquals(4, state.visibleStudents.size)
    }

    @Test
    fun gradeOptionsFollowStageAndUnknownGradesAreNotAssignedAStage() {
        val state = StudentsPreviewData.state()
        val primary = state.copy(stageFilter = StudentStageFilter.Primary)
        assertTrue(primary.availableGrades.all { it.stage == EducationStage.Primary })
        val unknown = state.students.first().copy(id = "unknown", gradeId = "missing")
        assertTrue(primary.copy(students = listOf(unknown)).visibleStudents.isEmpty())
        assertEquals(listOf(unknown), state.copy(students = listOf(unknown)).visibleStudents)
    }
}
