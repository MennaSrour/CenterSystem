package center.domain

import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupScheduleSlot
import center.domain.model.GroupStatus
import center.domain.model.LessonType
import center.domain.model.Student
import center.domain.model.StudentTrack
import center.domain.model.Teacher
import center.domain.model.activeSchedule
import center.domain.model.effectiveMonthlyPriceMinor
import center.domain.model.lessonAnalytics
import center.domain.model.totalMonthlyDueMinor
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileAnalyticsTest {
    private fun group(
        id: String,
        type: LessonType,
        teacherId: String = "t",
        price: Long = 10_000,
    ) = ClassGroup(
        id,
        id,
        teacherId,
        price,
        null,
        null,
        EducationSystem.BOTH,
        GroupStatus.ACTIVE,
        listOf(GroupScheduleSlot(DayOfWeek.MONDAY, LocalTime(13, 0), LocalTime(14, 0))),
        lessonType = type,
        languagesPriceMinor = if (type == LessonType.MIXED) 8_000 else null,
    )

    @Test
    fun teacherAnalyticsCountsTracksAndAggregatesSchedule() {
        val groups = listOf(group("a", LessonType.MIXED), group("l", LessonType.LANGUAGES), group("p", LessonType.PRIVATE))
        val students =
            listOf(
                Student("s1", "Arabic learner", enrolledGroupIds = setOf("a")),
                Student("s2", "Languages learner", enrolledGroupIds = setOf("l")),
                Student("s3", "Private learner", enrolledGroupIds = setOf("p")),
            )

        val analytics = Teacher("t", "Teacher", "Arabic").lessonAnalytics(groups, students)

        assertEquals(3, analytics.assignedLessonCount)
        assertEquals(1, analytics.arabicStudentCount)
        assertEquals(1, analytics.languagesStudentCount)
        assertEquals(1, analytics.privateStudentCount)
        assertEquals(listOf(DayOfWeek.MONDAY), analytics.expectedSchedule.map { it.day }.distinct())
        assertEquals(1, analytics.expectedSchedule.size)
    }

    @Test
    fun studentExpectedLessonsAndEnrollmentPriceChangesRecalculateMonthlyDue() {
        val arabic = group("mixed", LessonType.MIXED)
        val private = group("private", LessonType.PRIVATE, price = 12_000)
        val student =
            Student(
                "s",
                "Student",
                enrolledGroupIds = setOf("mixed", "private"),
                groupTracks = mapOf("mixed" to StudentTrack.LANGUAGES),
                groupCustomPricesMinor = mapOf("private" to 9_000),
            )
        val enrolled = listOf(arabic, private)

        assertEquals(2, student.activeSchedule(enrolled).size)
        assertEquals(17_000L, student.totalMonthlyDueMinor(enrolled))
        val discounted = student.copy(groupCustomPricesMinor = mapOf("mixed" to 7_500, "private" to 9_000))
        assertEquals(16_500L, discounted.totalMonthlyDueMinor(enrolled))
        val afterUnenroll = discounted.copy(enrolledGroupIds = setOf("private"))
        assertEquals(9_000L, afterUnenroll.totalMonthlyDueMinor(enrolled))
        assertEquals(8_000L, arabic.effectiveMonthlyPriceMinor(student))
    }
}
