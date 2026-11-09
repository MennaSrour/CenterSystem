package center.domain.usecase

import center.domain.model.Payment
import center.domain.model.QueuedMessage
import center.domain.repository.PaymentRepository
import center.domain.repository.StudentRepository
import center.domain.repository.TeacherRepository
import kotlin.math.round

class ProcessPaymentUseCase(
    private val students: StudentRepository,
    private val teachers: TeacherRepository,
    private val payments: PaymentRepository,
    private val clock: EpochClock,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        studentCode: String,
        classId: String,
        amountPaid: Double,
    ): Result<Payment> =
        domainResult {
            require(studentCode.isNotBlank() && classId.isNotBlank()) { "Student code and class ID are required" }
            require(amountPaid.isFinite() && amountPaid > 0.0 && amountPaid <= MAX_AMOUNT) { "Invalid payment amount" }
            val cents = round(amountPaid * 100).toLong()
            require(cents > 0 && kotlin.math.abs(amountPaid * 100 - cents) < 0.000001) { "Amount must have at most two decimals" }
            val student = students.getStudentByCode(studentCode.trim()) ?: throw StudentNotFoundException(studentCode)
            student.requireNotificationDetails()
            val teacher = teachers.getTeacherForClass(classId) ?: throw ClassTeacherNotFoundException(classId)
            require(teacher.commissionRate.isFinite() && teacher.commissionRate in 0.0..1.0) { "Invalid teacher commission rate" }
            val teacherCents = round(cents * teacher.commissionRate).toLong()
            val now = clock.nowMillis()
            val payment =
                Payment(ids.newId(), student.id, classId, cents / 100.0, teacherCents / 100.0, (cents - teacherCents) / 100.0, now)
            val amountText = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
            val receipt =
                QueuedMessage(
                    ids.newId(),
                    student.parentPhone,
                    "تم استلام مبلغ $amountText للطالب ${student.fullName} في سنتر Center One. رقم الإيصال: ${payment.id}",
                    createdAt = now,
                )
            payments.saveWithReceipt(payment, receipt)
            payment
        }

    private companion object {
        // Keep cent arithmetic and SQLite Double split validation within a reliable numeric range.
        const val MAX_AMOUNT = 1_000_000_000.0
    }
}
