package center.domain.usecase

import center.domain.model.AttendancePerson
import center.domain.model.AttendanceSession
import center.domain.model.LessonAttendance
import center.domain.model.PersonType
import center.domain.model.QueuedMessage
import center.domain.repository.AttendanceSessionRepository
import center.domain.repository.StudentRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class RegisterPresenceUseCase(
    private val attendance: AttendanceSessionRepository,
    private val students: StudentRepository,
    private val clock: EpochClock,
    private val ids: IdGenerator,
    private val timeZone: TimeZone = TimeZone.of("Africa/Cairo"),
) {
    suspend fun checkIn(
        person: AttendancePerson,
        lessons: List<LessonAttendanceOption> = emptyList(),
    ): Result<Unit> =
        domainResult {
            val now = clock.nowMillis()
            val date = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date
            val student = if (person.type == PersonType.STUDENT) students.getStudentById(person.id) else null
            val message =
                student?.takeIf { it.parentPhone.isNotBlank() }?.let {
                    QueuedMessage(
                        ids.newId(),
                        it.parentPhone,
                        "تم تسجيل حضور الطالب ${it.fullName} لسنتر Center One",
                        createdAt = now,
                    )
                }
            attendance.checkIn(
                AttendanceSession(person, date, now, null),
                message,
                lessons.map { LessonAttendance(person.id, it.lessonId, date, it.attended, now, makeupSessionId = it.makeupSessionId) },
            )
        }

    suspend fun checkOut(person: AttendancePerson): Result<Unit> =
        domainResult {
            val now = clock.nowMillis()
            val date = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date
            attendance.checkOut(person, date, now)
            if (person.type == PersonType.STUDENT) attendance.markFutureLessonsMissed(person.id, date, now)
        }
}
