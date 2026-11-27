package center.domain

import center.domain.model.ClassGroup
import center.domain.model.EducationSystem
import center.domain.model.GroupScheduleSlot
import center.domain.model.GroupStatus
import center.domain.model.LessonType
import center.domain.model.MakeupSession
import center.domain.usecase.applyEarlyCheckOut
import center.domain.usecase.monthlyLessonCounts
import center.domain.usecase.scheduledLessonsForCheckIn
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartAttendanceTest {
    private val date = LocalDate(2026, 11, 5)
    private val zone = TimeZone.of("Africa/Cairo")

    private fun timestamp(
        hour: Int,
        minute: Int,
    ): Long = date.atStartOfDayIn(zone).toEpochMilliseconds() + (hour * 60L + minute) * 60_000L

    private fun lesson(
        id: String,
        from: Int,
        to: Int,
    ) = ClassGroup(
        id,
        id,
        "teacher",
        10_000,
        7_000,
        null,
        EducationSystem.ARABIC,
        GroupStatus.ACTIVE,
        listOf(GroupScheduleSlot(DayOfWeek.THURSDAY, LocalTime(from, 0), LocalTime(to, 0))),
        lessonType = LessonType.ARABIC,
    )

    @Test
    fun lateCheckInIncludesAnInProgressLessonAndAllUpcomingLessons() {
        val options =
            scheduledLessonsForCheckIn(
                listOf(lesson("one", 13, 14), lesson("two", 14, 15), lesson("three", 15, 16)),
                date,
                timestamp(13, 30),
                zone,
            )

        assertEquals(listOf("one", "two", "three"), options.map { it.lessonId })
        assertTrue(options.all { it.attended })
    }

    @Test
    fun makeupsAppearOnTheirDateAndAreCountedAsTheirOwnAttendanceOption() {
        val makeup =
            MakeupSession(
                "makeup-1",
                "lesson",
                LocalDate(2026, 11, 5),
                LocalDate(2026, 11, 12),
                LocalTime(15, 0),
                LocalTime(16, 0),
                createdAt = timestamp(9, 0),
            )
        val group = lesson("lesson", 13, 14)
        val checkInAt = makeup.makeupDate.atStartOfDayIn(zone).toEpochMilliseconds() + 12 * 60 * 60_000L
        val options = scheduledLessonsForCheckIn(listOf(group), makeup.makeupDate, checkInAt, zone, listOf(makeup))

        assertEquals(2, options.size)
        assertEquals("makeup-1", options.single { it.isMakeup }.key)
        assertTrue(options.single { it.isMakeup }.attended)
        val counts = monthlyLessonCounts(listOf(group), LocalDate(2026, 11, 1), listOf(makeup))
        assertEquals(4, counts.scheduledSessionCount)
        assertEquals(1, counts.makeupSessionCount)
        assertEquals(1, counts.rescheduledDateCount)
    }

    @Test
    fun checkInAfterLessonEndListsTheLessonAsAbsent() {
        val options =
            scheduledLessonsForCheckIn(
                listOf(lesson("one", 13, 14), lesson("two", 14, 15), lesson("three", 15, 16)),
                date,
                timestamp(14, 5),
                zone,
            )

        assertEquals(listOf("one", "two", "three"), options.map { it.lessonId })
        assertFalse(options.first().attended)
        assertTrue(options.drop(1).all { it.attended })
    }

    @Test
    fun earlyCheckOutMarksNotYetStartedLessonsMissed() {
        val options =
            scheduledLessonsForCheckIn(
                listOf(lesson("one", 13, 14), lesson("two", 14, 15), lesson("three", 15, 16)),
                date,
                timestamp(13, 30),
                zone,
            )

        val checkedOut = applyEarlyCheckOut(options, timestamp(14, 0), zone).associateBy { it.lessonId }

        assertTrue(checkedOut.getValue("one").attended)
        assertFalse(checkedOut.getValue("two").attended)
        assertFalse(checkedOut.getValue("three").attended)
    }
}
