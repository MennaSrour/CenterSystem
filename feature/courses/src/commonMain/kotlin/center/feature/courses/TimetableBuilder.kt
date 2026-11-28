package center.feature.courses

enum class TimetableTrack { ALL, ARABIC, LANGUAGES }

data class TimetableLesson(
    val id: String,
    val day: ScheduleDay,
    val startMinute: Int,
    val endMinute: Int,
    val subject: String,
    val teacher: String,
    val lessonType: CourseLessonType,
)

data class TimetablePeriod(
    val number: Int,
    val startMinute: Int,
    val endMinute: Int,
    val lessonsByDay: Map<ScheduleDay, List<TimetableLesson>>,
)

data class WeeklyTimetable(
    val lessons: List<TimetableLesson>,
    val periods: List<TimetablePeriod>,
) {
    val isEmpty: Boolean get() = lessons.isEmpty()
    val days: List<ScheduleDay> get() = ScheduleDay.entries
}

/** Builds active lessons into unique, chronologically ordered periods with no filler rows. */
object TimetableBuilder {
    fun build(
        groups: List<CourseGroup>,
        teachers: List<CourseTeacher>,
        gradeId: String?,
        track: TimetableTrack,
    ): WeeklyTimetable {
        val teacherNames = teachers.associate { it.id to it.name }
        val lessons =
            groups
                .asSequence()
                .filter { it.isActive && (gradeId == null || it.gradeId == gradeId) }
                .filter { group ->
                    when (track) {
                        TimetableTrack.ALL -> true
                        TimetableTrack.ARABIC ->
                            group.lessonType in
                                setOf(CourseLessonType.Arabic, CourseLessonType.Mixed, CourseLessonType.Private)
                        TimetableTrack.LANGUAGES ->
                            group.lessonType in
                                setOf(CourseLessonType.Languages, CourseLessonType.Mixed, CourseLessonType.Private)
                    }
                }.flatMap { group ->
                    group.schedule.mapNotNull { schedule ->
                        val from = parseMinutes(schedule.from) ?: return@mapNotNull null
                        val to = parseMinutes(schedule.to) ?: return@mapNotNull null
                        if (to <= from) return@mapNotNull null
                        TimetableLesson(
                            id = schedule.id,
                            day = schedule.day,
                            startMinute = from,
                            endMinute = to,
                            subject = group.subject,
                            teacher = teacherNames[group.teacherId].orEmpty(),
                            lessonType = group.lessonType,
                        )
                    }
                }.sortedWith(compareBy<TimetableLesson> { it.startMinute }.thenBy { it.endMinute }.thenBy { it.day.ordinal })
                .toList()
        val periods =
            lessons
                .groupBy { it.startMinute to it.endMinute }
                .entries
                .sortedWith(compareBy({ it.key.first }, { it.key.second }))
                .mapIndexed { index, (interval, periodLessons) ->
                    TimetablePeriod(
                        number = index + 1,
                        startMinute = interval.first,
                        endMinute = interval.second,
                        lessonsByDay = periodLessons.groupBy { it.day },
                    )
                }
        return WeeklyTimetable(lessons, periods)
    }

    private fun parseMinutes(value: String): Int? {
        val parts = value.split(':')
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].take(2).toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }
}
