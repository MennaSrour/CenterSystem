package center.domain.repository

import center.domain.model.AttendancePerson
import center.domain.model.AttendanceSession
import center.domain.model.PersonType
import center.domain.model.QueuedMessage
import center.domain.model.StaffMember
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface StaffRepository {
    fun observeStaff(): Flow<List<StaffMember>>

    suspend fun save(member: StaffMember)
}

interface AttendanceSessionRepository {
    fun observeDay(date: LocalDate): Flow<List<AttendanceSession>>

    fun observeHistory(
        type: PersonType,
        personId: String,
    ): Flow<List<AttendanceSession>>

    suspend fun checkIn(
        session: AttendanceSession,
        notification: QueuedMessage?,
    )

    suspend fun checkOut(
        person: AttendancePerson,
        date: LocalDate,
        timestamp: Long,
    )
}
