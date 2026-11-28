package center.feature.courses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TimetableBuilderTest {
    @Test
    fun buildsOnlyDistinctActivePeriodsWithoutDuplicatingLessons() {
        val groups =
            listOf(
                group("early", CourseLessonType.Arabic, "08:15", "09:10"),
                group("early-sunday", CourseLessonType.Arabic, "08:15", "09:10", day = ScheduleDay.Sunday),
                group("late", CourseLessonType.Languages, "18:05", "19:45"),
                group("inactive", CourseLessonType.Mixed, "06:00", "21:00", active = false),
            )
        val all = TimetableBuilder.build(groups, emptyList(), gradeId = "grade-a", track = TimetableTrack.ALL)
        assertEquals(2, all.periods.size)
        assertEquals(listOf(1, 2), all.periods.map { it.number })
        assertEquals(listOf(8 * 60 + 15, 18 * 60 + 5), all.periods.map { it.startMinute })
        assertEquals(3, all.lessons.size)
        val renderedLessonIds =
            all.periods.flatMap { period ->
                period.lessonsByDay.values
                    .flatten()
                    .map { it.id }
            }
        assertEquals(3, renderedLessonIds.toSet().size)
        assertEquals(
            2,
            all.periods
                .first()
                .lessonsByDay.values
                .sumOf { it.size },
        )
    }

    @Test
    fun filtersGradeAndIncludesMixedOnBothTracks() {
        val groups =
            listOf(
                group("arabic", CourseLessonType.Arabic, "09:00", "10:00"),
                group("languages", CourseLessonType.Languages, "10:00", "11:00"),
                group("mixed", CourseLessonType.Mixed, "11:00", "12:00"),
                group("other-grade", CourseLessonType.Mixed, "12:00", "13:00", grade = "grade-b"),
                group("private", CourseLessonType.Private, "13:00", "14:00"),
            )
        assertEquals(
            setOf(
                "arabic",
                "mixed",
                "private",
            ),
            TimetableBuilder
                .build(groups, emptyList(), "grade-a", TimetableTrack.ARABIC)
                .lessons
                .map {
                    it.subject
                }.toSet(),
        )
        assertEquals(
            setOf(
                "languages",
                "mixed",
                "private",
            ),
            TimetableBuilder
                .build(groups, emptyList(), "grade-a", TimetableTrack.LANGUAGES)
                .lessons
                .map {
                    it.subject
                }.toSet(),
        )
        assertFalse(TimetableBuilder.build(groups, emptyList(), "grade-b", TimetableTrack.ARABIC).isEmpty)
    }

    private fun group(
        id: String,
        type: CourseLessonType,
        from: String,
        to: String,
        grade: String = "grade-a",
        active: Boolean = true,
        day: ScheduleDay = ScheduleDay.Saturday,
    ) = CourseGroup(
        id = id,
        subject = id,
        teacherId = "teacher",
        gradeId = grade,
        system = CourseSystem.Arabic,
        monthlyPrice = "100",
        teacherCommission = "70",
        roomId = "",
        enrolled = 0,
        maxCapacity = 20,
        schedule = listOf(CourseSchedule("$id-${day.name}", day, from, to)),
        isActive = active,
        lessonType = type,
    )
}
