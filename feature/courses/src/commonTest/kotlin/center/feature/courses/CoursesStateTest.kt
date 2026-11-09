package center.feature.courses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoursesStateTest {
    @Test
    fun filtersStageSubjectAndTeacherWithoutMutatingTheSnapshot() {
        val state = CoursesPreviewData.state()
        assertEquals(listOf("group-6"), state.copy(stageFilter = CourseStageFilter.Primary).visibleGroups.map { it.id })
        assertEquals(listOf("group-5"), state.copy(stageFilter = CourseStageFilter.Prep).visibleGroups.map { it.id })
        assertEquals(listOf("group-1"), state.copy(searchQuery = "  فيزياء ").visibleGroups.map { it.id })
        assertEquals(listOf("group-2"), state.copy(searchQuery = "أحمد سامي").visibleGroups.map { it.id })
        assertTrue(state.copy(searchQuery = "missing").visibleGroups.isEmpty())
        assertTrue(state.copy(stageFilter = CourseStageFilter.Primary, searchQuery = "فيزياء").visibleGroups.isEmpty())
        assertEquals(6, state.visibleGroups.size)
    }

    @Test
    fun capacityProgressHandlesZeroAndOverCapacityWithoutChangingTheLabelValues() {
        val group = CoursesPreviewData.state().groups[2]
        assertEquals(0.85f, group.capacityProgress, 0.0001f)
        assertEquals(0f, group.copy(maxCapacity = 0).capacityProgress)
        assertEquals(1f, group.copy(enrolled = 150).capacityProgress)
        assertEquals(150, group.copy(enrolled = 150).enrolled)
    }

    @Test
    fun editFixturePreservesIdentitySchedulesAndDefaultCommission() {
        val group = CoursesPreviewData.state().groups.first()
        val form = CoursesPreviewData.editForm(group)
        assertEquals(GroupFormMode.Edit(group.id), form.mode)
        assertEquals(group.schedule.map { it.id }, form.timeSlots.map { it.id })
        assertEquals("70", CoursesPreviewData.newForm().teacherCommission)
        assertEquals(8, form.rooms.size)
        assertEquals(
            8,
            form.rooms
                .map { it.id }
                .distinct()
                .size,
        )
    }
}
