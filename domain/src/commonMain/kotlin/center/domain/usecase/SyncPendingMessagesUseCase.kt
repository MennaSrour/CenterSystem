package center.domain.usecase

import center.domain.model.QueuedMessage
import center.domain.repository.MessageQueueRepository
import kotlinx.coroutines.flow.first

/** Returns a snapshot only. Infrastructure owns delivery and explicit sent acknowledgement. */
class SyncPendingMessagesUseCase(
    private val messages: MessageQueueRepository,
) {
    suspend operator fun invoke(): Result<List<QueuedMessage>> = domainResult { messages.getPendingMessages().first() }
}
