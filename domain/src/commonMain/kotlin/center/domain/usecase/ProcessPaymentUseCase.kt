package center.domain.usecase

import center.domain.model.Payment
import center.domain.model.QueuedMessage
import center.domain.repository.PaymentRepository
import center.domain.repository.StudentRepository
import center.domain.repository.TeacherRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToLong

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
        billingMonth: LocalDate? = null,
    ): Result<Payment> =
        domainResult {
            require(studentCode.isNotBlank() && classId.isNotBlank()) { "Student code and class ID are required" }
            require(amountPaid.isFinite() && amountPaid > 0.0 && amountPaid <= MAX_AMOUNT) { "Invalid payment amount" }
            val cents = round(amountPaid * 100).toLong()
            require(cents > 0 && abs(amountPaid * 100 - cents) < 0.000001) { "Amount must have at most two decimals" }
            val student = students.getStudentByCode(studentCode.trim()) ?: throw StudentNotFoundException(studentCode)
            student.requireNotificationDetails()
            val rate = teachers.getCommissionForClass(classId) ?: throw ClassTeacherNotFoundException(classId)
            require(rate.isFinite() && rate in 0.0..1.0) { "Invalid teacher commission rate" }
            require(
                billingMonth == null || (billingMonth.dayOfMonth == 1 && billingMonth.year in 1..9999),
            ) { "Use the first day of the billing month" }
            val basisPoints = (rate * 10_000).roundToLong()
            val teacherCents =
                if (abs(rate * 10_000 - basisPoints) < 1e-9) {
                    (cents * basisPoints + 5_000) / 10_000
                } else {
                    (cents * rate).roundToLong()
                }
            val now = clock.nowMillis()
            val date = Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.of("Africa/Cairo")).date
            val month = billingMonth ?: LocalDate(date.year, date.month, 1)
            val payment =
                Payment(ids.newId(), student.id, classId, cents / 100.0, teacherCents / 100.0, (cents - teacherCents) / 100.0, now, month)
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
