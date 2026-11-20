package center.domain.usecase

import center.domain.model.QueuedMessage
import center.domain.model.ReminderChannel
import center.domain.repository.MessageQueueRepository
import center.domain.repository.StudentRepository

class QueueReminderUseCase(
    private val students: StudentRepository,
    private val messages: MessageQueueRepository,
    private val clock: EpochClock,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        studentCode: String,
        message: String,
        channel: ReminderChannel = ReminderChannel.WHATSAPP,
    ): Result<QueuedMessage> =
        domainResult {
            require(studentCode.isNotBlank() && message.isNotBlank()) { "Student code and reminder text are required" }
            val student = students.getStudentByCode(studentCode.trim()) ?: throw StudentNotFoundException(studentCode)
            require(student.isActive) { "Student is inactive" }
            val recipient =
                when (channel) {
                    ReminderChannel.WHATSAPP -> student.parentPhone.trim().also { require(it.isNotBlank()) { "Parent phone is required" } }
                    ReminderChannel.NOTIFICATION -> student.id
                }
            QueuedMessage(
                ids.newId(),
                recipient,
                message.trim(),
                createdAt = clock.nowMillis(),
                channel = channel,
            ).also { messages.save(it) }
        }
}
