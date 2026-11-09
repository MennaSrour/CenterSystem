package center.database.entity

/** IDs are caller-generated UUID strings; legacy student IDs remain supported. */
data class TeacherEntity(
    val id: String,
    val fullName: String,
    val subject: String,
    val commissionRate: Double = 0.70,
) {
    init {
        require(id.isNotBlank() && fullName.isNotBlank() && subject.isNotBlank())
        require(commissionRate.isFinite() && commissionRate in 0.0..1.0)
    }
}

data class StudentEntity(
    val id: String,
    val fullName: String,
    val gradeLevel: String,
    val studentPhone: String? = null,
    val parentPhone: String,
    val isActive: Boolean = true,
) {
    init {
        require(id.isNotBlank() && fullName.isNotBlank())
    }
}

data class ClassGroupEntity(
    val id: String,
    val name: String,
    val teacherId: String,
    val sessionPrice: Double,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank() && teacherId.isNotBlank())
        require(sessionPrice.isFinite() && sessionPrice >= 0.0)
    }
}

enum class AttendanceStatus { PRESENT, ABSENT }

data class AttendanceEntity(
    val id: String,
    val studentId: String,
    val classId: String,
    /** Unix epoch milliseconds. */
    val timestamp: Long,
    val status: AttendanceStatus,
) {
    init {
        require(id.isNotBlank() && studentId.isNotBlank() && classId.isNotBlank())
    }
}

data class PaymentEntity(
    val id: String,
    val studentId: String,
    val classId: String,
    val amountPaid: Double,
    val teacherShare: Double,
    val centerShare: Double,
    /** Unix epoch milliseconds. */
    val paymentDate: Long,
) {
    init {
        require(id.isNotBlank() && studentId.isNotBlank() && classId.isNotBlank())
        require(listOf(amountPaid, teacherShare, centerShare).all { it.isFinite() && it >= 0.0 })
        require(kotlin.math.abs(amountPaid - teacherShare - centerShare) <= 0.000001)
    }
}

enum class MessageStatus { PENDING, SENT }

data class MessageQueueEntity(
    val id: String,
    val targetPhone: String,
    val messageBody: String,
    val status: MessageStatus = MessageStatus.PENDING,
    /** Unix epoch milliseconds. */
    val createdAt: Long,
) {
    init {
        require(id.isNotBlank() && targetPhone.isNotBlank() && messageBody.isNotBlank())
    }
}

data class StudentWithPayments(
    val student: StudentEntity,
    val payments: List<PaymentEntity>,
)

data class TeacherDues(
    val teacherId: String,
    val totalPaid: Double,
    val teacherShare: Double,
    val centerShare: Double,
)
