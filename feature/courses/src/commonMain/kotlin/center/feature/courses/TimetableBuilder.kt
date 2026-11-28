package center.feature.courses

enum class TimetableTrack { ALL, ARABIC, LANGUAGES }

data class TimetableLesson(
    val id: String,
    val day: ScheduleDay,
    val startMinute: Int,
    val endMinute: Int,
    val subject: String,
    val teacher: String,
    val track: TimetableTrack,
    val startLabel: String,
    val endLabel: String,
)

data class WeeklyTimetable(
    val lessons: List<TimetableLesson>,
    val startMinute: Int?,
    val endMinute: Int?,
) {
    val isEmpty: Boolean get() = lessons.isEmpty()
    val days: List<ScheduleDay> get() = ScheduleDay.entries
    val slots: List<Int>
        get() = if (startMinute == null || endMinute == null) emptyList() else (startMinute until endMinute step 30).toList()
}

/** Builds a grade/track filtered weekly timetable from active lessons only. */
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
                            track = group.lessonType.toTimetableTrack(),
                            startLabel = schedule.from,
                            endLabel = schedule.to,
                        )
                    }
                }.sortedWith(compareBy<TimetableLesson> { it.day.ordinal }.thenBy { it.startMinute }.thenBy { it.endMinute })
                .toList()
        if (lessons.isEmpty()) return WeeklyTimetable(emptyList(), null, null)
        val first = lessons.minOf { it.startMinute }
        val last = lessons.maxOf { it.endMinute }
        return WeeklyTimetable(lessons, first / 30 * 30, ((last + 29) / 30 * 30).coerceAtMost(24 * 60))
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

private fun CourseLessonType.toTimetableTrack(): TimetableTrack =
    when (this) {
        CourseLessonType.Arabic -> TimetableTrack.ARABIC
        CourseLessonType.Languages -> TimetableTrack.LANGUAGES
        CourseLessonType.Mixed -> TimetableTrack.ALL
        CourseLessonType.Private -> TimetableTrack.ALL
    }

fun TimetableLesson.trackFor(track: TimetableTrack): TimetableTrack =
    if (this.track == TimetableTrack.ALL) track.takeIf { it != TimetableTrack.ALL } ?: TimetableTrack.ALL else this.track
