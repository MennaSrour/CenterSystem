package center.domain.usecase

import center.domain.model.ClassGroup
import center.domain.model.GroupStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class LessonAttendanceOption(
    val lessonId: String,
    val lessonName: String,
    val startMinute: Int,
    val endMinute: Int,
    val attended: Boolean,
)

/** Lists today's lessons and pre-checks those whose end time has not passed. */
fun scheduledLessonsForCheckIn(
    groups: List<ClassGroup>,
    date: LocalDate,
    timestamp: Long,
    timeZone: TimeZone = TimeZone.of("Africa/Cairo"),
): List<LessonAttendanceOption> {
    val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)
    if (local.date != date) return emptyList()
    val minute = local.hour * 60 + local.minute
    return groups
        .asSequence()
        .filter { it.status == GroupStatus.ACTIVE }
        .flatMap { group ->
            group.schedule
                .asSequence()
                .filter { it.day == local.dayOfWeek }
                .map { slot ->
                    LessonAttendanceOption(
                        group.id,
                        group.name,
                        slot.from.hour * 60 + slot.from.minute,
                        slot.to.hour * 60 + slot.to.minute,
                        attended = slot.to.hour * 60 + slot.to.minute >= minute,
                    )
                }
        }.sortedBy { it.startMinute }
        .toList()
}

/** Early check-out marks lessons that have not started yet as missed. */
fun applyEarlyCheckOut(
    options: List<LessonAttendanceOption>,
    timestamp: Long,
    timeZone: TimeZone = TimeZone.of("Africa/Cairo"),
): List<LessonAttendanceOption> {
    val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)
    val checkoutMinute = local.hour * 60 + local.minute
    return options.map { lesson ->
        if (lesson.startMinute >= checkoutMinute) lesson.copy(attended = false) else lesson
    }
}
