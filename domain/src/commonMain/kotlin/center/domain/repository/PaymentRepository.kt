package center.domain.repository

import center.domain.model.Payment
import center.domain.model.QueuedMessage
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun getPaymentsForStudent(studentId: String): Flow<List<Payment>>

    fun getPaymentsForTeacher(
        teacherId: String,
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<Payment>>

    /** Insert both records atomically; roll back both on failure. IDs must be new. */
    suspend fun saveWithReceipt(
        payment: Payment,
        receipt: QueuedMessage,
    )
}
