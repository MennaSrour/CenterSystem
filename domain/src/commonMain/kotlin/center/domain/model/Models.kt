package center.domain.model

data class Student(
    val id: String,
    val fullName: String,
    val gradeLevel: String = "",
    val studentPhone: String? = null,
    val parentPhone: String = "",
    val isActive: Boolean = true,
) {
    val name: String get() = fullName
    val code: String get() = id
}

data class Teacher(
    val id: String,
    val fullName: String,
    val subject: String,
    val commissionRate: Double = 0.70,
)

enum class AttendanceStatus { PRESENT, ABSENT }

data class Attendance(
    val id: String,
    val studentId: String,
    val classId: String,
    val timestamp: Long,
    val status: AttendanceStatus,
)

data class Payment(
    val id: String,
    val studentId: String,
    val classId: String,
    val amountPaid: Double,
    val teacherShare: Double,
    val centerShare: Double,
    val paymentDate: Long,
)

enum class MessageStatus { PENDING, SENT }

data class QueuedMessage(
    val id: String,
    val targetPhone: String,
    val messageBody: String,
    val status: MessageStatus = MessageStatus.PENDING,
    val createdAt: Long,
)
