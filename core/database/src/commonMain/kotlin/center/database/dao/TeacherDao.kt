package center.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import center.database.CenterDatabase
import center.database.entity.TeacherEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TeacherDao(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries = database.teacherQueries

    fun observeAll(): Flow<List<TeacherEntity>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { TeacherEntity(it.id, it.fullName, it.subject, it.commissionRate) } }

    fun observeById(id: String): Flow<TeacherEntity?> =
        queries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.let { TeacherEntity(it.id, it.fullName, it.subject, it.commissionRate) } }

    suspend fun save(entity: TeacherEntity): Unit =
        withContext(dispatcher) {
            database.transaction {
                if (queries.selectById(entity.id).executeAsOneOrNull() == null) {
                    queries.insertRow(entity.id, entity.fullName, entity.subject, entity.commissionRate)
                } else {
                    queries.updateRow(entity.fullName, entity.subject, entity.commissionRate, entity.id)
                }
            }
        }

    suspend fun delete(id: String): Unit = withContext(dispatcher) { queries.deleteById(id) }
}
