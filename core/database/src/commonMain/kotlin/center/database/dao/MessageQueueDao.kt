package center.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import center.database.CenterDatabase
import center.database.entity.MessageQueueEntity
import center.database.entity.MessageStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MessageQueueDao(
    private val database: CenterDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries = database.messageQueueQueries

    fun observeAll(): Flow<List<MessageQueueEntity>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { MessageQueueEntity(it.id, it.targetPhone, it.messageBody, MessageStatus.valueOf(it.status), it.createdAt) }
            }

    fun observeById(id: String): Flow<MessageQueueEntity?> =
        queries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row ->
                row?.let { MessageQueueEntity(it.id, it.targetPhone, it.messageBody, MessageStatus.valueOf(it.status), it.createdAt) }
            }

    suspend fun save(entity: MessageQueueEntity): Unit =
        withContext(dispatcher) {
            database.transaction {
                if (queries.selectById(entity.id).executeAsOneOrNull() == null) {
                    queries.insertRow(entity.id, entity.targetPhone, entity.messageBody, entity.status.name, entity.createdAt)
                } else {
                    queries.updateRow(entity.targetPhone, entity.messageBody, entity.status.name, entity.createdAt, entity.id)
                }
            }
        }

    suspend fun delete(id: String): Unit = withContext(dispatcher) { queries.deleteById(id) }

    fun getPendingMessages(): Flow<List<MessageQueueEntity>> =
        queries
            .selectPending()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { MessageQueueEntity(it.id, it.targetPhone, it.messageBody, MessageStatus.valueOf(it.status), it.createdAt) }
            }

    suspend fun markSent(id: String): Unit = withContext(dispatcher) { queries.markSent(id) }
}
