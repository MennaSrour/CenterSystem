package center.domain.usecase

import center.domain.model.ClassGroup
import center.domain.model.GroupStatus
import center.domain.model.MakeupSession
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class LessonAttendanceOption(
    val lessonId: String,
    val lessonName: String,
    val startMinute: Int,
    val endMinute: Int,
    val attended: Boolean,
    val makeupSessionId: String? = null,
) {
    val key: String get() = makeupSessionId ?: lessonId
    val isMakeup: Boolean get() = makeupSessionId != null
}

/** Lists today's recurring and make-up lessons and pre-checks those whose end time has not passed. */
fun scheduledLessonsForCheckIn(
    groups: List<ClassGroup>,
    date: LocalDate,
    timestamp: Long,
    timeZone: TimeZone = TimeZone.of("Africa/Cairo"),
    makeupSessions: List<MakeupSession> = emptyList(),
): List<LessonAttendanceOption> {
    val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)
    if (local.date != date) return emptyList()
    val minute = local.hour * 60 + local.minute
    val activeGroups = groups.filter { it.status == GroupStatus.ACTIVE }.associateBy { it.id }
    val regular =
        activeGroups.values.flatMap { group ->
            group.schedule
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
        }
    val makeups =
        makeupSessions
            .filter { it.makeupDate == date }
            .mapNotNull { session ->
                val group = activeGroups[session.lessonId] ?: return@mapNotNull null
                val start = session.startTime.hour * 60 + session.startTime.minute
                val end = session.endTime.hour * 60 + session.endTime.minute
                LessonAttendanceOption(session.lessonId, group.name, start, end, end >= minute, session.id)
            }
    return (regular + makeups).sortedWith(compareBy(LessonAttendanceOption::startMinute, LessonAttendanceOption::key))
}

/** Counts active lesson occurrences; a linked canceled date is replaced by its make-up occurrence. */
fun monthlyLessonCounts(
    groups: List<ClassGroup>,
    month: LocalDate,
    makeupSessions: List<MakeupSession>,
): MonthlyLessonCounts {
    require(month.dayOfMonth == 1)
    val end = month + DatePeriod(months = 1)
    val activeGroups = groups.filter { it.status == GroupStatus.ACTIVE }
    val activeGroupIds = activeGroups.mapTo(mutableSetOf()) { it.id }
    val makeups = makeupSessions.filter { it.lessonId in activeGroupIds && it.makeupDate >= month && it.makeupDate < end }
    val rescheduledDates =
        makeupSessions
            .asSequence()
            .filter { it.lessonId in activeGroupIds }
            .mapNotNull { it.originalCanceledDate }
            .filter { it >= month && it < end }
            .distinct()
            .toList()
    var regularCount = 0
    activeGroups.forEach { group ->
        val rescheduledDates =
            makeupSessions
                .asSequence()
                .filter { it.lessonId == group.id }
                .mapNotNull { it.originalCanceledDate }
                .toSet()
        var date = month
        while (date < end) {
            if (group.schedule.any { it.day == date.dayOfWeek } && date !in rescheduledDates) regularCount++
            date = date + DatePeriod(days = 1)
        }
    }
    return MonthlyLessonCounts(
        scheduledSessionCount = regularCount + makeups.size,
        makeupSessionCount = makeups.size,
        rescheduledDateCount = rescheduledDates.size,
    )
}

data class MonthlyLessonCounts(
    val scheduledSessionCount: Int,
    val makeupSessionCount: Int,
    val rescheduledDateCount: Int,
)

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
