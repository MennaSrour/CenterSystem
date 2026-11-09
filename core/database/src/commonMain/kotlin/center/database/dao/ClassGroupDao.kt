package center.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import center.database.CenterDatabase
import center.database.entity.ClassGroupEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ClassGroupDao(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries = database.classGroupQueries

    fun observeAll(): Flow<List<ClassGroupEntity>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { ClassGroupEntity(it.id, it.name, it.teacherId, it.sessionPrice) } }

    fun observeById(id: String): Flow<ClassGroupEntity?> =
        queries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.let { ClassGroupEntity(it.id, it.name, it.teacherId, it.sessionPrice) } }

    suspend fun save(entity: ClassGroupEntity): Unit =
        withContext(dispatcher) {
            database.transaction {
                val existing = queries.selectById(entity.id).executeAsOneOrNull()
                if (existing != null && existing.teacherId != entity.teacherId) {
                    check(database.paymentQueries.countForClass(entity.id).executeAsOne() == 0L) {
                        "Cannot reassign a class with payment history"
                    }
                }
                if (existing == null) {
                    queries.insertRow(entity.id, entity.name, entity.teacherId, entity.sessionPrice)
                } else {
                    queries.updateRow(entity.name, entity.teacherId, entity.sessionPrice, entity.id)
                }
            }
        }

    suspend fun delete(id: String): Unit = withContext(dispatcher) { queries.deleteById(id) }
}
