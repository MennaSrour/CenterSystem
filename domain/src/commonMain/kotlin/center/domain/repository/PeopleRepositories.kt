package center.domain.repository

import center.domain.model.AttendancePerson
import center.domain.model.AttendanceSession
import center.domain.model.LessonAttendance
import center.domain.model.PersonType
import center.domain.model.QueuedMessage
import center.domain.model.StaffMember
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface StaffRepository {
    fun observeStaff(): Flow<List<StaffMember>>

    suspend fun save(member: StaffMember)

    suspend fun delete(id: String): Unit = throw UnsupportedOperationException("Staff deletion is unavailable")
}

interface AttendanceSessionRepository {
    fun observeDay(date: LocalDate): Flow<List<AttendanceSession>>

    fun observePeriod(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<AttendanceSession>> = kotlinx.coroutines.flow.flowOf(emptyList())

    fun observeHistory(
        type: PersonType,
        personId: String,
    ): Flow<List<AttendanceSession>>

    suspend fun checkIn(
        session: AttendanceSession,
        notification: QueuedMessage?,
        lessonAttendance: List<LessonAttendance> = emptyList(),
    )

    suspend fun checkOut(
        person: AttendancePerson,
        date: LocalDate,
        timestamp: Long,
    )

    fun observeLessonHistory(studentId: String): Flow<List<LessonAttendance>> = kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun saveLessonAttendance(records: List<LessonAttendance>) = Unit

    suspend fun markFutureLessonsMissed(
        studentId: String,
        date: LocalDate,
        timestamp: Long,
    ) = Unit
}
