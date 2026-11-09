package center.domain.repository

import center.domain.model.Attendance
import center.domain.model.QueuedMessage
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun getAttendanceForClass(
        classId: String,
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<Attendance>>

    /** Insert attendance and its pending notification in the same local transaction. */
    suspend fun saveWithNotification(
        attendance: Attendance,
        notification: QueuedMessage,
    )
}
