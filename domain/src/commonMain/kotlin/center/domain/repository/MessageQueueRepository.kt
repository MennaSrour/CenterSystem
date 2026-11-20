package center.domain.repository

import center.domain.model.QueuedMessage
import kotlinx.coroutines.flow.Flow

interface MessageQueueRepository {
    fun getPendingMessages(): Flow<List<QueuedMessage>>

    /** Enqueue a new pending message. A duplicate ID must leave the original intact. */
    suspend fun save(message: QueuedMessage)

    suspend fun markSent(id: String)
}
