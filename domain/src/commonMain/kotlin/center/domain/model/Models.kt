package center.domain.model

import kotlinx.datetime.LocalDate

data class Student(
    val id: String,
    val fullName: String,
    val gradeLevel: String = "",
    val studentPhone: String? = null,
    val parentPhone: String = "",
    val isActive: Boolean = true,
    val code: String = id,
    val stage: EducationStage? = null,
    val system: EducationSystem? = null,
    val enrolledGroupIds: Set<String> = emptySet(),
    val groupDiscounts: Map<String, Double> = emptyMap(),
    val groupCustomPricesMinor: Map<String, Long> = emptyMap(),
    val groupTracks: Map<String, StudentTrack> = emptyMap(),
) {
    val name: String get() = fullName
}

enum class StudentTrack { ARABIC, LANGUAGES, PRIVATE }

data class Teacher(
    val id: String,
    val fullName: String,
    val subject: String,
    val commissionRate: Double = 0.70,
    val phone: String = "",
    val notes: String = "",
    val assignedGroupIds: Set<String> = emptySet(),
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
    val billingMonth: LocalDate? = null,
)

enum class MessageStatus { PENDING, SENT }

enum class ReminderChannel { WHATSAPP, NOTIFICATION }

data class QueuedMessage(
    val id: String,
    /** Parent phone for WhatsApp or the student ID for a notification. */
    val targetPhone: String,
    val messageBody: String,
    val status: MessageStatus = MessageStatus.PENDING,
    val createdAt: Long,
    val channel: ReminderChannel = ReminderChannel.WHATSAPP,
    val reminderStudentId: String? = null,
)
