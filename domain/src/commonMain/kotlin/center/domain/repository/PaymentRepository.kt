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

    /** Insert the payment and, when available, its receipt atomically; IDs must be new. */
    suspend fun saveWithReceipt(
        payment: Payment,
        receipt: QueuedMessage?,
    )

    /** Soft-voids a payment so history remains auditable and all accounting queries can exclude it. */
    suspend fun voidPayment(
        paymentId: String,
        voidedAt: Long,
        reason: String = "",
    ): Unit = throw UnsupportedOperationException("Payment voiding is unavailable")
}
