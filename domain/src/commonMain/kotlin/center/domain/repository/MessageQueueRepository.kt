package center.domain.repository

import center.domain.model.QueuedMessage
import kotlinx.coroutines.flow.Flow

interface MessageQueueRepository {
    fun getPendingMessages(): Flow<List<QueuedMessage>>

    suspend fun save(message: QueuedMessage)

    suspend fun markSent(id: String)
}
