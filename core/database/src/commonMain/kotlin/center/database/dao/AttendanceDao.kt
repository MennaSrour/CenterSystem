package center.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import center.database.CenterDatabase
import center.database.entity.AttendanceEntity
import center.database.entity.AttendanceStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AttendanceDao(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries = database.attendanceQueries

    fun observeAll(): Flow<List<AttendanceEntity>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { AttendanceEntity(it.id, it.studentId, it.classId, it.timestamp, AttendanceStatus.valueOf(it.status)) }
            }

    fun observeById(id: String): Flow<AttendanceEntity?> =
        queries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row ->
                row?.let { AttendanceEntity(it.id, it.studentId, it.classId, it.timestamp, AttendanceStatus.valueOf(it.status)) }
            }

    suspend fun save(entity: AttendanceEntity): Unit =
        withContext(dispatcher) {
            database.transaction {
                if (queries.selectById(entity.id).executeAsOneOrNull() == null) {
                    queries.insertRow(entity.id, entity.studentId, entity.classId, entity.timestamp, entity.status.name)
                } else {
                    queries.updateRow(entity.studentId, entity.classId, entity.timestamp, entity.status.name, entity.id)
                }
            }
        }

    suspend fun delete(id: String): Unit = withContext(dispatcher) { queries.deleteById(id) }

    fun getAttendanceForClass(
        classId: String,
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<AttendanceEntity>> {
        require(startInclusive < endExclusive)
        return queries
            .selectForClass(classId, startInclusive, endExclusive)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { AttendanceEntity(it.id, it.studentId, it.classId, it.timestamp, AttendanceStatus.valueOf(it.status)) }
            }
    }
}
